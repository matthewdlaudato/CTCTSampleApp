package com.ballc.gasuite;

import java.io.FileInputStream;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletResponse;  
import javax.servlet.http.HttpSession;

import org.scribe.model.Token;

import com.ballc.gasuite.CTCTApi;

public class OAuth2Servlet extends HttpServlet {

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {
		HttpSession httpsession = req.getSession(true);
		String username = req.getParameter("username");

		// Get the api key properties
		ServletContext servletContext = req.getServletContext();
		Properties applicationProperties = (Properties) servletContext.getAttribute("applicationProperties");
		if (applicationProperties == null) {
			applicationProperties = new Properties();
			FileInputStream in = new FileInputStream("local_application.properties");
			applicationProperties.load(in);
			in.close();
			servletContext.setAttribute("applicationProperties", applicationProperties);
		}
		
		// Check for an existing valid access token
		Token accessToken = CTCTApi._loadAccessToken(username);
		if (accessToken == null) {
			
			// connect to the auth URL
			String authURL = res.encodeRedirectURL("https://oauth2.constantcontact.com/oauth2/oauth/siteowner/authorize" +
					"?response_type=code" +
					"&client_id=" + applicationProperties.getProperty("apiKey") +
					"&redirect_uri=" + applicationProperties.getProperty("oauth2RedirectURI"));

			res.sendRedirect(authURL);
		}  else {
			httpsession.setAttribute("username", username);
			CTCTApi lister = new CTCTApi(
					username,
					accessToken, 
					applicationProperties.getProperty("iaapiKey"),
					applicationProperties.getProperty("iaapiSecret")
					);
			httpsession.setAttribute("ctctapi", lister);
			String destinationURL = res.encodeRedirectURL(applicationProperties.getProperty("landingPageURI"));
			res.sendRedirect(destinationURL);
		}
		
	}
	
  	public void doGet(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {  
  		doPost(req, res);  
  	}

}
