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
			OAuthService service = new ServiceBuilder()
	        .provider(ConstantContactApi.class)
	        .callback(applicationProperties.getProperty("oauth1CallbackURI"))
	        .apiKey(applicationProperties.getProperty("apiKey"))
	        .apiSecret(applicationProperties.getProperty("apiSecret"))
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
					applicationProperties.getProperty("apiKey"),
					applicationProperties.getProperty("apiSecret"),
					1
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
