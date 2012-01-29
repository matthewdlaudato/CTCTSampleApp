package com.ballc.gasuite;  

import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletResponse;  
import javax.servlet.http.HttpSession;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.scribe.model.Verifier;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import com.ballc.gasuite.CTCTApi;

public class OAuthCallbackServlet extends HttpServlet {  

	/**  
	 * Servlet to handle the HTTP Callback URL for OAuth  
	 */ 
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {  

		String oauth_token = req.getParameter("oauth_token");
		String oauth_verifier = req.getParameter("oauth_verifier");
		String username = req.getParameter("username");


		if (oauth_verifier.length() > 0){
			Verifier verifier = new Verifier(oauth_verifier);

			HttpSession httpsession = req.getSession(true);
			OAuthService service = (OAuthService) httpsession.getAttribute("oauth.service");
			Token requestToken = new Token(oauth_token, (String)httpsession.getAttribute("oauth.request_token_secret"));
			httpsession.setAttribute("username", username);

			Token accessToken = service.getAccessToken(requestToken, verifier);

			httpsession.setAttribute("oauth.access_token", accessToken);
			
			Long accessTokenId = null; 
			AccessToken at = new AccessToken();
			at.setLoginName(username);
			at.setAccessToken(accessToken.getToken());
			at.setSecret(accessToken.getSecret());
			Date dt = new Date();
			Timestamp ts = new Timestamp(dt.getTime());
			at.setModifiedDate(ts);

			Session session = HibernateUtil.getSessionFactory().openSession(); 
			Transaction transaction = null; 

			try { 
				transaction = session.beginTransaction(); 
				accessTokenId = (Long) session.save(at); 
				transaction.commit(); 
			} catch (HibernateException e) { 
				transaction.rollback(); 
				e.printStackTrace(); 
			} finally { 
				session.close(); 
			} 

			httpsession.setAttribute("oauth.request_token", null);
			ServletContext servletContext = req.getServletContext();
			Properties applicationProperties = (Properties) servletContext.getAttribute("applicationProperties");
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
