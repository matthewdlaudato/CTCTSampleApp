package com.ballc.gasuite;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Hashtable;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.ConstantContactApi;
import org.scribe.builder.api.ConstantContactApi2;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

public class CTCTApi {

	private OAuthService _service;
	private String _userName;
	private Token _accessToken;
    private Namespace _ns = Namespace.getNamespace("http://ws.constantcontact.com/ns/1.0/");
	private Namespace _ens = Namespace.getNamespace("http://www.w3.org/2005/Atom");

	public CTCTApi(String username, Token token, String apiKey, String apiSecret){
		_userName = username;
		
		if (token == null) {
			_loadAccessToken(username);
		} else {
			_accessToken = token;
		}

		_service = new ServiceBuilder()
        .provider(ConstantContactApi2.class)
        .apiKey(apiKey)
        .apiSecret(apiSecret)
        .build();
	}
	
	
	public Hashtable getMailingLists() {
		
	    // List the lists
		String serviceURI = "https://api.constantcontact.com/ws/customers/" + _userName + "/lists";
	    Hashtable uriList = getListURIs(_accessToken, _service, serviceURI);
	    return uriList;
	}

	private Hashtable getListURIs(Token accessToken, OAuthService service, String serviceURI)
	{
		Hashtable <String, String> mailingLists = new Hashtable();
		
		OAuthRequest listsRequest = new OAuthRequest(Verb.GET, serviceURI);
	    service.signRequest(accessToken, listsRequest);
	    Response listsResponse = listsRequest.send();

	    // Now use JDOM to find the ContactList(s)
	    SAXBuilder builder = new SAXBuilder();
        try {
         
           // Create the document object from the response text and get the root element
           StringReader sr = new StringReader(listsResponse.getBody());
     	   Document document = (Document) builder.build(sr);
     	   Element rootNode = document.getRootElement();
     	   ElementFilter ef = new ElementFilter("ContactList", _ns);
     	   Iterator <Element> descendants = rootNode.getDescendants(ef);
     	   while (descendants.hasNext()) {
     		   String name;
     		   String uri;
     		   Element e = descendants.next();
     		   Element cl = e.getChild("Name", _ns);
     		   if (cl == null) {
     			   name = "NULL";
     		   } else {
     			   name = cl.getText();
     		   }
     		   
     		   Element mem = e.getChild("Members", _ns);
     		   if (mem == null) {
     			   uri = "";
     		   } else {
             	   uri = mem.getAttributeValue("id");
           	       uri = uri.replaceAll("^http", "https");

     		   }
     		   mailingLists.put(name, uri);
     			   
     	   }

     	 } catch(IOException io) {
     		System.out.println(io.getMessage());
     	 } catch(JDOMException jdomex) {
     		System.out.println(jdomex.getMessage());
     	 }
		 return mailingLists;
	}
	
	
	public ArrayList <String> getMailingListContacts(String listURI)
	{

		ArrayList <String> contacts = new ArrayList <String> ();

		OAuthRequest listRequest = new OAuthRequest(Verb.GET, listURI);
	    _service.signRequest(_accessToken, listRequest);
	    Response listResponse = listRequest.send();
	    
	    String listEntry = listResponse.getBody();

	    SAXBuilder builder = new SAXBuilder();

	    try {
            
            // Create the document object from the response text and get the root element
           StringReader sr = new StringReader(listEntry);
      	   Document document = (Document) builder.build(sr);
      	   Element rootNode = document.getRootElement();

      	   ElementFilter ef = new ElementFilter("entry", _ens);
      	   Iterator <Element> descendants = rootNode.getDescendants(ef);
      	   while (descendants.hasNext()) {
      		   Element entry = descendants.next();
      		   String email = entry.getChild("content", _ens).getChild("ContactListMember", _ns).getChild("EmailAddress", _ns).getText();
      		   contacts.add(email);
     	   }

      	 } catch(IOException io) {
      		System.out.println(io.getMessage());
      	 } catch(JDOMException jdomex) {
      		System.out.println(jdomex.getMessage());
      	 }
	    
		return contacts;
		
	}
	
	
	public String addContactList(String newListName){
		String newListURI = null;
		
		String listAtomXML = 
			"<entry xmlns=\"http://www.w3.org/2005/Atom\">" +
		  "<id>data:,</id>" +
		  "<title />" +
		  "<author />" +
		    "<updated>2008-04-16</updated>" +
		    "<content type=\"application/vnd.ctct+xml\">" +
		    "<ContactList xmlns=\"http://ws.constantcontact.com/ns/1.0/\">" +
		      "<OptInDefault>false</OptInDefault>" +
		            "<Name>" + newListName + "</Name>" +
		            "<SortOrder>99</SortOrder>" +
		    "</ContactList>" +
		    "</content>" +
		"</entry>";

		String serviceURI = "https://api.constantcontact.com/ws/customers/" + _userName + "/lists";
		OAuthRequest addContactListRequest = new OAuthRequest(Verb.POST, serviceURI);
		addContactListRequest.addHeader("Content-Type", "application/atom+xml");
		addContactListRequest.addPayload(listAtomXML);
	    _service.signRequest(_accessToken, addContactListRequest);
	    Response addContactListResponse = addContactListRequest.send();
		
	    newListURI = addContactListResponse.getBody();
		return newListURI;
	}
	
	/*
	 * Get a contact from their email address. Returns the Contact entry XML
	 */
	public String getContact(String email)
	{

   	    // first get the basic contact entry
		String serviceURI = "https://api.constantcontact.com/ws/customers/" + _userName + "/contacts?email=" + email;
		OAuthRequest getContactRequest = new OAuthRequest(Verb.GET, serviceURI);
	    _service.signRequest(_accessToken, getContactRequest);
	    Response getContactResponse = getContactRequest.send();
	    String contactEntry = getContactResponse.getBody();

	    // now get the URI to the contact detail
	    String contactURI = getContactURI(contactEntry);
	    contactURI = contactURI.replaceAll("^http", "https");

	    // Then get the detailed entry
		OAuthRequest getContactDetailRequest = new OAuthRequest(Verb.GET, contactURI);
		getContactDetailRequest.addHeader("Content-Type", "application/atom+xml;type=entry");
	    _service.signRequest(_accessToken, getContactDetailRequest);
	    Response getContactDetailResponse = getContactDetailRequest.send();
	    
	    String contactDetailEntry = getContactDetailResponse.getBody();
	    return contactDetailEntry;
	}
	
	public String addContactToList(String email, String list)
	{

   	    String listRelURI = list;
   	    listRelURI = listRelURI.replace((CharSequence) "http://api.constantcontact.com", (CharSequence)"");

   	    // first get the contact's entry
	    String contactEntry = getContact(email);
	    
	    // then get the URI for later use
	    String contactURI = getContactURI(contactEntry);		
	    contactURI = contactURI.replaceAll("^http", "https");
	    SAXBuilder builder = new SAXBuilder();
        try {
            
            // Create the document object and add a new ContactList
           StringReader sr = new StringReader(contactEntry);
      	   Document document = (Document) builder.build(sr);
      	   Element rootNode = document.getRootElement();
      	   Element content = rootNode.getChild("content", _ens);
      	   Element Contact = content.getChild("Contact", _ns);
      	   Element ContactLists = Contact.getChild("ContactLists", _ns);
      	   
      	   Element el = null;
      	   List <Element> allContactLists = ContactLists.getChildren();
      	   Iterator i = allContactLists.iterator();
      	   while (i.hasNext()) {
      		   Element e = (Element) i.next();
      		   el = (Element) e.clone();
      	   }

  		   el.setAttribute("id", list);
  		   el.getChild("link", _ens).setAttribute("href", listRelURI);
  		   
      	   ContactLists.addContent(el);

      	   // DOM is updated, now output to XML string
      	   Format formatter = Format.getPrettyFormat();
      	   formatter.setOmitDeclaration(true);
      	   contactEntry = new XMLOutputter(formatter).outputString(document);

      	 } catch(IOException io) {
      		System.out.println(io.getMessage());
      	 } catch(JDOMException jdomex) {
      		System.out.println(jdomex.getMessage());
      	 }
 
		//	contactEntry has been updated, so now add contact to list
		OAuthRequest addContactToListRequest = new OAuthRequest(Verb.PUT, contactURI);
		addContactToListRequest.addHeader("Content-Type", "application/atom+xml;type=entry");
		addContactToListRequest.addPayload(contactEntry);
	    _service.signRequest(_accessToken, addContactToListRequest);
	    Response addContactToListResponse = addContactToListRequest.send();

		return addContactToListResponse.getBody();
	}

	public String addNewContact(String lastName, String firstName, String email, String list)
	{
		// build the atom
		String contactAtomXml  = 
			"<entry xmlns=\"http://www.w3.org/2005/Atom\">" +
			  "<id>data:,none</id>" +
			  "<title />" +
			  "<author />" +
			  "<updated>2011-02-26</updated>" +
			  "<summary type=\"text\">Contact</summary>" +
			  "<content type=\"application/vnd.ctct+xml\">" +
			  "<Contact xmlns=\"http://ws.constantcontact.com/ns/1.0/\">" +
		      "<EmailAddress>" + email + "</EmailAddress>" +
		      "<FirstName>" + firstName + "</FirstName>" +
		      "<LastName>" + lastName + "</LastName>" +
		      "<OptInSource>ACTION_BY_CONTACT</OptInSource>";

		if (!list.isEmpty()) {
			contactAtomXml += 
		      "<ContactLists>" +
		        "<ContactList id=\"" + list + "\" />" +
		      "</ContactLists>";
		}
		contactAtomXml +=
		    "</Contact>" +
			  "</content>" +
			"</entry>";
		
		//	Contact can be created and added to list simultaneously through the contacts collection
		String serviceURI = "https://api.constantcontact.com/ws/customers/" + _userName + "/contacts";
		OAuthRequest createContactRequest = new OAuthRequest(Verb.POST, serviceURI);
		createContactRequest.addHeader("Content-Type", "application/atom+xml;type=entry");
		createContactRequest.addPayload(contactAtomXml);
	    _service.signRequest(_accessToken, createContactRequest);
	    Response createContactResponse = createContactRequest.send();

		return createContactResponse.getBody();
	}


	// get the URI to the contact detail from the contact xml
	private String getContactURI(String contactEntry)
	{

   	    String contactURI = null;
	    SAXBuilder builder = new SAXBuilder();
        try {
            
            // Create the document object from the response text and get the root element
           StringReader sr = new StringReader(contactEntry);
      	   Document document = (Document) builder.build(sr);
      	   Element rootNode = document.getRootElement();

      	   ElementFilter ef = new ElementFilter("entry", _ens);
      	   Iterator <Element> descendants = rootNode.getDescendants(ef);
      	   while (descendants.hasNext()) {
      		   Element entry = descendants.next();
      		   contactURI = entry.getChild("id", _ens).getText();

     	   }
      	   if (contactURI == null) {
      		   // If we were looking at a detailed contact entry, the above logic
      		   // fails, because the root of the DOM is the <entry>. In that
      		   // case, use alternate logic to look up the contactURI
      		   
      		   contactURI = rootNode.getChild("id", _ens).getText();
      	   }

      	 } catch(IOException io) {
      		System.out.println(io.getMessage());
      	 } catch(JDOMException jdomex) {
      		System.out.println(jdomex.getMessage());
      	 }

      	 return contactURI;
      	 
	}
	
	// get the contact URI given the contact email address
	public String getContactURIFromEmail(String email) {

		String contactXML = getContact(email);
		String contactURI = getContactURI(contactXML);
		
		return contactURI;
		
	}

	public Hashtable<String, String> getContactEvents(String email)
	{
		Hashtable <String, String> contactEvents = new Hashtable<String, String>();
		String contactURI = getContactURIFromEmail(email);
	    contactURI = contactURI.replaceAll("^http", "https");

   	    // get the contact open events for this contact
		String serviceURI = contactURI + "/events/opens";
		OAuthRequest getContactEventsRequest = new OAuthRequest(Verb.GET, serviceURI);
	    _service.signRequest(_accessToken, getContactEventsRequest);
	    Response getContactEventsResponse = getContactEventsRequest.send();
	    String contactEventsXML = getContactEventsResponse.getBody();

	    // parse the results and put them in the hashtable as <CampaignName, Date>.
	    // Example: <"Cinco de Mayo 2011 Mailing", Date>
	    SAXBuilder builder = new SAXBuilder();
        try {
            
            // Create a document object and get the Campaign Name and Open date
            StringReader sr = new StringReader(contactEventsXML);
       	   Document document = (Document) builder.build(sr);
       	   Element rootNode = document.getRootElement();

      	   ElementFilter ef = new ElementFilter("entry", _ens);
      	   Iterator <Element> descendants = rootNode.getDescendants(ef);
      	   while (descendants.hasNext()) {
      		   Element entry = descendants.next();
      		   String campaignName = entry.getChild("content", _ens).getChild("OpenEvent", _ns).getChild("Campaign", _ns).getChild("Name", _ns).getText();
      		   String openDate = entry.getChild("content", _ens).getChild("OpenEvent", _ns).getChild("EventTime", _ns).getText();
      		   contactEvents.put(campaignName, openDate);
     	   }


      	 } catch(IOException io) {
      		System.out.println(io.getMessage());
      	 } catch(JDOMException jdomex) {
      		System.out.println(jdomex.getMessage());
      	 }

	    
		return contactEvents;
	}

	public Hashtable<String, String[]> getContactFields(String email)
	{
   	    Hashtable <String, String[]> contactFields = new Hashtable<String, String[]>();
		String contactEntry = getContact(email);
	    SAXBuilder builder = new SAXBuilder();
        try {
            
            // Create the document object and add a new ContactList
           StringReader sr = new StringReader(contactEntry);
      	   Document document = (Document) builder.build(sr);
      	   Element rootNode = document.getRootElement();
      	   Element content = rootNode.getChild("content", _ens);
      	   Element Contact = content.getChild("Contact", _ns);
      	   String name = Contact.getChild("Name", _ns).getText();
      	   String emailAddress = Contact.getChild("EmailAddress", _ns).getText();
      	   String joinDate = Contact.getChild("InsertTime", _ns).getText();
      	   String[] fields = {emailAddress, joinDate};
      	   contactFields.put(name, fields);


      	 } catch(IOException io) {
      		System.out.println(io.getMessage());
      	 } catch(JDOMException jdomex) {
      		System.out.println(jdomex.getMessage());
      	 }
		
		return contactFields;
		
	}

	public Hashtable<String, String> getEmailCampaigns() {
   	   
		Hashtable <String, String> emailCampaigns = new Hashtable<String, String>();
   	    
   	    // get email campaigns for this contact
   	    String serviceURI = "https://api.constantcontact.com/ws/customers/" + _userName + "/campaigns";
   	   
		OAuthRequest getEmailCampaignsRequest = new OAuthRequest(Verb.GET, serviceURI);
	    _service.signRequest(_accessToken, getEmailCampaignsRequest);
	    Response getEmailCampaignsResponse = getEmailCampaignsRequest.send();
	    String emailCampaignsXML = getEmailCampaignsResponse.getBody();

	    // parse the results and put them in the hashtable as <CampaignName, Date>.
	    // Example: <"Cinco de Mayo 2011 Mailing", Date>
	    SAXBuilder builder = new SAXBuilder();
        try {
            
           // Create a document object and get the Campaign Name and Open date
           StringReader sr = new StringReader(emailCampaignsXML);
       	   Document document = (Document) builder.build(sr);
       	   Element rootNode = document.getRootElement();

      	   ElementFilter ef = new ElementFilter("entry", _ens);
      	   Iterator <Element> descendants = rootNode.getDescendants(ef);
      	   while (descendants.hasNext()) {
      		   Element entry = descendants.next();
      		   String campaignid = entry.getChild("id", _ens).getText();
      		   String campaignName = entry.getChild("content", _ens).getChild("Campaign", _ns).getChild("Name", _ns).getText();
      		   emailCampaigns.put(campaignid, campaignName);
     	   }


      	 } catch(Exception ex) {
      		System.out.println(ex.getMessage());
      	 } 

   	    return emailCampaigns;

	}
	
	// test method - not fully baked
	public String addContactsActivity(){
   	    // get email campaigns for this contact
   	    String serviceURI = "https://api.constantcontact.com/ws/customers/" + _userName + "/activities";
   	    //String encodedActivity = "activityType=SV_ADD &data=Email+Address%2CFirst+Name%2CLast+Name%0A wstest3%40example.com%2C+Fred%2C+Test%0A wstest4%40example.com%2C+Joan%2C+Test%0A wstest5%40example.com%2C+Ann%2C+Test &lists=http%3A%2F%2Fapi.constantcontact.com%2Fws%2Fcustomers%2Fmdlaudato%2Flists%2F2";
   	    String encodedActivity = "activityType=EXPORT_CONTACTS&fileType=CSV&exportOptDate=true&exportOptSource=true&exportListName=true&sortBy=EMAIL_ADDRESS&columns=EMAIL%20ADDRESS&listId=http%3A%2F%2Fapi.constantcontact.com%2Fws%2Fcustomers%2Fmdlaudato2%2Flists%2F1";
		OAuthRequest addContactsActivityRequest = new OAuthRequest(Verb.POST, serviceURI);
		addContactsActivityRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
		addContactsActivityRequest.addPayload(encodedActivity);
	    _service.signRequest(_accessToken, addContactsActivityRequest);
	    Response addContactsActivityResponse = addContactsActivityRequest.send();
	    String activityResponseBody = addContactsActivityResponse.getBody();

	    return activityResponseBody;
	}
	
	public static void storeAccessToken(Token accessToken){
		  try {
		      FileOutputStream fout = new FileOutputStream("token.dat");
		      ObjectOutputStream oos = new ObjectOutputStream(fout);
		      oos.writeObject(accessToken);
		      oos.close();
		  } catch (Exception e) { 
			  e.printStackTrace();
		  }
	}
	
	public static Token loadAccessToken(){
		   Token theToken = null;

		   // unserialize the token
		   try {
			   FileInputStream fin = new FileInputStream("token.dat");
			   ObjectInputStream ois = new ObjectInputStream(fin);
			   theToken = (Token) ois.readObject();
			   ois.close();
		   }
		   catch (Exception e) {
			   System.out.println("Exception in loadAccessToken()");
			   e.printStackTrace();
			   theToken = null;
		   }
		     
		   return theToken;
	}
	
	public static Token _loadAccessToken(String username){
		Session session = HibernateUtil.getSessionFactory().openSession(); 
		Transaction transaction = null; 
		List rt = null;
		try { 
			transaction = session.beginTransaction(); 
			rt = session.createQuery("from AccessToken where loginName=" + "'" + username + "'").list(); 
			transaction.commit(); 
		} catch (HibernateException e) { 
			transaction.rollback(); 
			e.printStackTrace(); 
		}
		
		AccessToken at = null;
		for (Iterator iterator = rt.iterator(); iterator.hasNext();) 
		{ 
			 at = (AccessToken) iterator.next();
		}

		Token accessToken = null;
		if (at != null) {
			accessToken = new Token(at.getAccessToken(), at.getSecret());
		}
		return accessToken;

	}

}
