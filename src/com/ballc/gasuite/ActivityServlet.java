package com.ballc.gasuite;  

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.http.HttpServlet;  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletResponse;  
import javax.servlet.http.HttpSession;

   
public class ActivityServlet extends HttpServlet {  

   
  /**  
   * Servlet to manage CTCT Mailing list objects
   */ 
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {  
  		addContactActivity(req, res);
	}  

  	public void doGet(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {  
  	}
  
	public void addContactActivity(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException 
	{ 
		HttpSession session = req.getSession(true);
		CTCTApi lister = (CTCTApi) session.getAttribute("ctctapi");
		if (lister != null) {
			String activityResponse = lister.addContactsActivity();
	
			res.setContentType("text/html");  
			res.getWriter().write("<br>");
			res.getWriter().write("<table id=\"datasets\"><tr><th>Response Body</th></tr>");
				res.getWriter().write("<tr>");
			    res.getWriter().write("<td>");
			    res.getWriter().write("<pre>" + activityResponse + "</pre>");
			    res.getWriter().write("</td>");
				res.getWriter().write("</tr>");
			} 
			res.getWriter().write("</table>");
	}
	
} 
