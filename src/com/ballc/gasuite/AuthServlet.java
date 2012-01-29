package com.ballc.gasuite;  


import java.util.Properties;
import java.io.FileInputStream;

import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletResponse;  

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.ConstantContactApi;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

public class AuthServlet extends HttpServlet {  

	/**  
	 * Servlet to initiate the OAuth authentication flow  
	 */ 
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
			OAuthService service = new ServiceBuilder()
	        .provider(ConstantContactApi.class)
	        .callback("http://localhost:8080/CTCTSampleApp/OAuthCallbackServlet.do")
	        .apiKey(apiKeyProperties.getProperty("apiKey"))
	        .apiSecret(apiKeyProperties.getProperty("apiSecret"))
	        .build();
			httpsession.setAttribute("oauth.service", service);
			
			Token requestToken = service.getRequestToken();
			httpsession.setAttribute("oauth.request_token_secret", requestToken.getSecret());
			
			String confirmAccessURL = service.getAuthorizationUrl(requestToken);
	
			System.out.println(confirmAccessURL);
			try {
				res.sendRedirect(res.encodeRedirectURL(confirmAccessURL));
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		} else {
			httpsession.setAttribute("username", username);
			httpsession.setAttribute("oauth.access_token", accessToken);
			CTCTApi lister = new CTCTApi(
					username,
					accessToken, 
					apiKeyProperties.getProperty("apiKey"),
					apiKeyProperties.getProperty("apiSecret")
					);
			httpsession.setAttribute("ctctapi", lister);
			String destinationURL = res.encodeRedirectURL("http://localhost:8080/CTCTSampleApp/lister.jsp");
			res.sendRedirect(destinationURL);

		}
	  }
 
  	public void doGet(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {  
  		doPost(req, res);  
  	}
}
