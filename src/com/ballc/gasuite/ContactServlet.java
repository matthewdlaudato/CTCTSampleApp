package com.ballc.gasuite;  

import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServlet;  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletResponse;  

public class ContactServlet extends HttpServlet {  

   
  /**  
   * Servlet to manage CTCT Contact objects
   */ 
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {  
  		addNewContact(req, res);
	}  

  	public void doGet(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {  
  		getContact(req, res);
  	}  
  
	public void getContact(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException 
	{ 
		HttpSession session = req.getSession(true);
		CTCTApi lister = (CTCTApi) session.getAttribute("ctctapi");
		
		if (lister != null) {
			Hashtable <String, String[]> ht = lister.getContactFields(req.getParameter("email"));
			Hashtable <String, String> events = lister.getContactEvents(req.getParameter("email"));
			Hashtable <String, String> emailCampaigns = lister.getEmailCampaigns();
			
			String overlay = req.getParameter("overlay");
			if (overlay == null) {
				res.setContentType("text/html");  
				res.getWriter().write("<br>");
				res.getWriter().write("<table id=\"datasets\"><tr><th>Name</th><th>Email</th></tr>");
				for (Enumeration<String> keys = ht.keys(); keys.hasMoreElements();)
				{
					String name = keys.nextElement().toString();
					String[] fields = ht.get(name);
					String email = fields[0];
					res.getWriter().write("<tr>");
				    res.getWriter().write("<td>" + name + "</td>");
				    res.getWriter().write("<td>" + email + "</td>"); 
					res.getWriter().write("</tr>");
				}
		
				res.getWriter().write("</table>");
			} else {
				res.setContentType("text/plain");
				for (Enumeration<String> keys = ht.keys(); keys.hasMoreElements();)
				{
					String name = keys.nextElement().toString();
					String[] fields = ht.get(name);
					String email = fields[0];
					String joinDateTime = fields[1];
					res.getWriter().println(name + "<br>");
					res.getWriter().println(email + "<br>");
					String joinDate = joinDateTime.substring(0, joinDateTime.indexOf("T"));
					res.getWriter().println("Member since: " + joinDate + "<br>");
				}
				res.getWriter().println("<br>");
				res.getWriter().println("Recent Activity:<br>");
				for (Enumeration<String> eventkeys = events.keys(); eventkeys.hasMoreElements();)
				{
					String campaignName = eventkeys.nextElement().toString();
					String openDateTime = events.get(campaignName);
					String openDate = openDateTime.substring(0, openDateTime.indexOf("T"));
					res.getWriter().println(openDate + "<b> OPENED: " + campaignName + "</b><br>" );
				}
				
	
				res.getWriter().println("<br>");
				res.getWriter().println("Click Email Campaign Name to send:<br>");
	
				int i = 0;  // counter to display 3
				for (Enumeration<String> campaignsKeys = emailCampaigns.keys(); campaignsKeys.hasMoreElements();)
				{
					++i;
					String campaignId = campaignsKeys.nextElement().toString();
					String campaignName = emailCampaigns.get(campaignId);
					res.getWriter().println("<a href=\"" + campaignId + "\">" + campaignName + "</a><br>" );
					// display only 3 campaigns now
					if (i == 3){
						break;
					}
				}
				
				res.getWriter().println("<br>");
			}
		}
	}
	
	public void addNewContact(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException
	{
		String newContactAtom = null;

		HttpSession session = req.getSession(true);
		CTCTApi lister = (CTCTApi) session.getAttribute("ctctapi");
		
		if (lister != null) {
			newContactAtom = lister.addNewContact(req.getParameter("lastName"), req.getParameter("firstName"), req.getParameter("email"), req.getParameter("list"));
			res.setContentType("text/html");  
			res.getWriter().write("<br>");
			res.getWriter().write("<table id=\"datasets\"><tr><th>Contact XML</th></tr>");
			res.getWriter().write("<tr>");
			res.getWriter().write("<td>" + newContactAtom + "</td>");
			res.getWriter().write("</tr>");
			res.getWriter().write("</table>");
		}		
	}


} 
