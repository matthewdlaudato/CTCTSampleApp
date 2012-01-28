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
		Properties apiKeyProperties = (Properties) servletContext.getAttribute("apiKeyProperties");
		if (apiKeyProperties == null) {
			apiKeyProperties = new Properties();
			FileInputStream in = new FileInputStream("apikey.properties");
			apiKeyProperties.load(in);
			in.close();
			servletContext.setAttribute("apiKeyProperties", apiKeyProperties);
		}
		
		// Check for an existing valid access token
		Token accessToken = CTCTApi._loadAccessToken(username);
		if (accessToken == null) {
			
			// connect to the auth URL
			String authURL = res.encodeRedirectURL("https://oauth2.constantcontact.com/oauth2/oauth/siteowner/authorize" +
					"?response_type=code" +
					"&client_id=" + apiKeyProperties.getProperty("iaapiKey") +
					"&redirect_uri=http://localhost:8080/CTCTWeb/OAuth2CallbackServlet.do");

			res.sendRedirect(authURL);
		}  else {
			httpsession.setAttribute("username", username);
			CTCTApi lister = new CTCTApi(
					username,
					accessToken, 
					apiKeyProperties.getProperty("apiKey"),
					apiKeyProperties.getProperty("apiSecret")
					);
			httpsession.setAttribute("ctctapi", lister);
			String destinationURL = res.encodeRedirectURL("http://localhost:8080/CTCTWeb/lister.jsp");
			res.sendRedirect(destinationURL);
		}
		
	}
	
  	public void doGet(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {  
  		doPost(req, res);  
  	}

}
