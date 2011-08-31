package com.ballc.gasuite;  

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.http.HttpServlet;  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletResponse;  
import javax.servlet.http.HttpSession;

   
public class MailingListServlet extends HttpServlet {  

   
  /**  
   * Servlet to manage CTCT Mailing list objects
   */ 
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {  
  		String newListName = req.getParameter("newListName");
  		if (newListName != null) {
  			addList(req, res);
  		} else {
  			addContactToList(req, res);
  		}  
	}

  	public void doGet(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {  
  		String list = req.getParameter("list");
  		if (list != null) {
  			getMailingListContacts(req, res);
  		} else {
  			listMailingLists(req,res);
  		}
  	}
  
	public void listMailingLists(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException 
	{ 
		HttpSession session = req.getSession(true);
		CTCTApi lister = (CTCTApi) session.getAttribute("ctctapi");
		if (lister != null) {
			Hashtable <String, String> mailingLists = lister.getMailingLists();
			// Logic to read mailing lists and return a java List object
	
			res.setContentType("text/html");  
			res.getWriter().write("<br>");
			res.getWriter().write("<table id=\"datasets\"><tr><th>List Name</th></tr>");
			for (Enumeration<String> keys = mailingLists.keys(); keys.hasMoreElements();) 
			{ 
				String name = keys.nextElement().toString();
				String uri = mailingLists.get(name);
				res.getWriter().write("<tr>");
			    res.getWriter().write("<td>");
			    if (uri != null && uri.equals("")) {
			    	res.getWriter().write(name);
			    } else {
			    	res.getWriter().write("<a onclick=\"getMailingListContacts(encodeURI('" + uri + "'));\">" + name + "</a>");
			    }
			    res.getWriter().write("</td>");
				res.getWriter().write("</tr>");
			} 
			res.getWriter().write("</table>");
		}	
	}
	
	public void addContactToList(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException
	{
		String newContactAtom = null;

		HttpSession session = req.getSession(true);
		CTCTApi lister = (CTCTApi) session.getAttribute("ctctapi");

		if (lister != null) {

			newContactAtom = lister.addContactToList(req.getParameter("email"), req.getParameter("list"));
			res.setContentType("text/html");  
			res.getWriter().write("<br>");
			res.getWriter().write("<table id=\"datasets\"><tr><th>Contact XML</th></tr>");
			res.getWriter().write("<tr>");
			res.getWriter().write("<td>" + newContactAtom + "</td>");
			res.getWriter().write("</tr>");
			res.getWriter().write("</table>");
		}		
	}
	
	public void getMailingListContacts(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException 
	{ 
		HttpSession session = req.getSession(true);
		CTCTApi lister = (CTCTApi) session.getAttribute("ctctapi");
		
		if (lister != null) {
			ArrayList <String> contactList = lister.getMailingListContacts(req.getParameter("list"));
			
			res.setContentType("text/html");  
			res.getWriter().write("<br>");
			//res.getWriter().write("<h3>" + req.getParameter("list") + "</h3>");
			res.getWriter().write("<table id=\"datasets\"><tr><th>Email</th></tr>");
			
			for (int i = 0; i < contactList.size(); i++) {
				res.getWriter().write("<tr>");
			    res.getWriter().write("<td>");
				res.getWriter().write("<a name=\"" + i + "\" href=\"#" + i + "\"onmouseover=\"getContactInOverlay(encodeURI('" + contactList.get(i) + "'));\""); 
				res.getWriter().write(">");
			    res.getWriter().write(contactList.get(i) + "</a>");
	   		    res.getWriter().write("</td>");
	            res.getWriter().write("</tr>");
			}
			res.getWriter().write("</table>");
		}
	
	}
	
	public void addList(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException
	{
		String newListName = req.getParameter("newListName");

		HttpSession session = req.getSession(true);
		CTCTApi lister = (CTCTApi) session.getAttribute("ctctapi");

		if (lister != null) {
			String newListXML = lister.addContactList(newListName);
			res.setContentType("text/html");  
			res.getWriter().write(newListXML);

		}
	}
} 
