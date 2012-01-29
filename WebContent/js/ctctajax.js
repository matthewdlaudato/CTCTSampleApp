/*  
 * CTCT jQuery access via the CTCTWeb app  
 */ 
var serverResponse = null;
var lastDatasets = null;
var lastRunparams = null;
var overlayText = null;

function makeAuthRequest(url, parameters, type) { 
    var form = document.createElement("form");
    form.setAttribute("method", type);
    form.setAttribute("action", url);
    for(var key in parameters) {
    	var hiddenField = document.createElement("input");
    	hiddenField.setAttribute("type", "hidden");
    	hiddenField.setAttribute("name", key);
    	hiddenField.setAttribute("value", parameters[key]);
    	form.appendChild(hiddenField);
    }     
    document.body.appendChild(form);
    form.submit();
}

function listMailingLists() {
	var options = {
		url: 'http://localhost:8080/CTCTSampleApp/MailingListServlet.do',
		type: "GET",
		dataType: "html",
		success: function(data){
			jQuery('.serverResponse').html(data);
		}
	};
	jQuery.ajax(options);

}

function addContactList(newListName){
	var poststr = encodeURI("newListName=" + newListName);
    jQuery('.debugArea').html("poststr=" + poststr); 

    var options = {
			url: 'http://localhost:8080/CTCTSampleApp/MailingListServlet.do',
			type: "POST",
			data: poststr,
			dataType: "html",
			success: function(data){
				jQuery('.serverResponse').html(data);
			}
	};
	jQuery.ajax(options);
	
}

function addNewContact(lastName,firstName,email,list) {
	var poststr =  encodeURI("lastName=" + lastName) + encodeURI("&firstName=" + firstName) + encodeURI("&email=" + email) + encodeURI("&list=" + list);
    jQuery('.debugArea').html("poststr=" + poststr); 
    var options = {
			url: 'http://localhost:8080/CTCTSampleApp/ContactServlet.do',
			type: "POST",
			data: poststr,
			dataType: "html",
			success: function(data){
				jQuery('.serverResponse').html(data);
			}
	};
	jQuery.ajax(options);

}

function addContactToList(email,list) {
	var poststr =  encodeURI("email=" + email) + encodeURI("&list=" + list);
    jQuery('.debugArea').html("poststr=" + poststr); 

    var options = {
			url: 'http://localhost:8080/CTCTSampleApp/MailingListServlet.do',
			type: "POST",
			data: poststr,
			dataType: "html",
			success: function(data){
				jQuery('.serverResponse').html(data);
			}
	};
	jQuery.ajax(options);

}

function getContact(email) {
	var poststr = encodeURI("email=" + email);
    jQuery('.debugArea').html("poststr=" + poststr); 
    var options = {
			url: 'http://localhost:8080/CTCTSampleApp/ContactServlet.do',
			type: "GET",
			data: poststr,
			dataType: "html",
			success: function(data){
				jQuery('.serverResponse').html(data);
			}
		};
	jQuery.ajax(options);
    
}

function getContactInOverlay(email) {
	var poststr = encodeURI("email=" + email);
	poststr += encodeURI("&overlay=1");
    jQuery('.debugArea').html("poststr=" + poststr); 
    var options = {
			url: 'http://localhost:8080/CTCTSampleApp/ContactServlet.do',
			type: "GET",
			data: poststr,
			dataType: "html",
			success: function(overlayText){
   	  			jQuery('#overlayDialog').html(overlayText);
				if (!jQuery('#overlayDialog').dialog("isOpen")) {
	   	  			jQuery('#overlayDialog').dialog('open');
				}
			}
	};
	jQuery.ajax(options);

}

function getMailingListContacts(list){
	var poststr = encodeURI("list=" + list);
    jQuery('.debugArea').html("poststr=" + poststr); 

    var options = {
			url: 'http://localhost:8080/CTCTSampleApp/MailingListServlet.do',
			type: "GET",
			data: poststr,
			dataType: "html",
			success: function(data){
				jQuery('.serverResponse').html(data);
			}
		};
	jQuery.ajax(options);
	
}

function authenticate(loginname) {
	var poststr = {'username':loginname};
	makeAuthRequest("AuthServlet.do", poststr, "GET");
}

function authenticate2(loginname) {
	var poststr = {'username':loginname};
	makeAuthRequest("OAuth2Servlet.do", poststr, "GET");
}

function addContactActivity() {
	var poststr =  null;

    var options = {
			url: 'http://localhost:8080/CTCTSampleApp/ActivityServlet.do',
			type: "POST",
			data: poststr,
			dataType: "html",
			success: function(data){
				jQuery('.serverResponse').html(data);
			}
	};
	jQuery.ajax(options);
}

function clearForm() {  
	  document.getElementById("serverResponse").innerHTML = "No results to display.";
	  document.getElementById("debugArea").innerHTML = "No DEBUG messages to display."; 
	  document.getElementById("lastName").value = ""; 
	  document.getElementById("firstName").value = ""; 
	  document.getElementById("email").value = ""; 
	  document.getElementById("list").value = ""; 
	  document.getElementById("emailtoadd").value = ""; 
	  document.getElementById("listtoupdate").value = "";
	  document.getElementById("contactemail").value = ""; 
	  document.getElementById("listuri").value = ""; 
	  document.getElementById("newlistname").value = ""; 

	  lastQueryResults = null;
	  serverResponse = null;
}  
   
//Check if string is a valid email address  
function regIsEmail(fData)  
{  
	var reg = new RegExp("^[0-9a-zA-Z]+@[0-9a-zA-Z]+[\.]{1}[0-9a-zA-Z]+[\.]?[0-9a-zA-Z]+$");  
	return reg.test(fData);  
} 

function traverse(node) { 
	if (node.nodeType == 3) {
		// it's a text node - it might have an email address, so process it
		var w_a = node.nodeValue.split(" ");
		if (w_a != null) {
			var neww_a;
			for (var j=0; j<w_a.length; j++) {
				neww_a += w_a[j];
				if (regIsEmail(w_a[j])) {
					var newNode = document.createElement("a");
		            newNode.setAttribute("onmouseover", "getContactInOverlay(encodeURI('" + w_a[j] + "'));");
		            newNode.innerHTML = node.nodeValue;
		            node.parentNode.insertBefore(newNode, node);
		            node.parentNode.removeChild(node);
				}
			}
		}
	}
	if (node.childNodes != null) { 
		for (var i=0; i < node.childNodes.length; i++) { 
	      traverse(node.childNodes.item(i)); 
      } 
  } 
}

//scan the document for email addresses and make them active
//so that you can mouse over to get CTCT contact dashboard overlay
function windowLoadHandler() {
	var allNodes = document.body.childNodes;
	for (var i=0; i<allNodes.length; i++){
		var n = allNodes[i];
		traverse( n );
	}
}

jQuery(window).load(function() {
	jQuery('#overlayDialog').dialog({ autoOpen: false, title: 'Contact Information'});
});
jQuery(window).load(windowLoadHandler);
