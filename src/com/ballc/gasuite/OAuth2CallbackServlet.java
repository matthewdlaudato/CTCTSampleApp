package com.ballc.gasuite;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletResponse;  
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.ProtocolException;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.scribe.model.Token;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.JsonNode;

import com.ballc.gasuite.CTCTApi;

public class OAuth2CallbackServlet extends HttpServlet {

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {
		String code = req.getParameter("code");
		String username = req.getParameter("username");

		HttpSession httpsession = req.getSession(true);
		httpsession.setAttribute("username", username);

		// connect to the token request URL
		ServletContext servletContext = req.getServletContext();
		Properties applicationProperties = (Properties) servletContext.getAttribute("applicationProperties");

		if (code != null) { // we are making the initial token request
			String tokenURL = "https://oauth2.constantcontact.com/oauth2/oauth/token"; 
			String tokenURLdata = "grant_type=authorization_code" + 
					"&client_id=" + applicationProperties.getProperty("apiKey") +
					"&client_secret=" + applicationProperties.getProperty("apiSecret") +
					"&code=" + code +
					"&redirect_uri=" + applicationProperties.getProperty("oauth2RedirectURI");
			
			URL url = new URL(tokenURL);
			String jsonToken = "";
			try {
				HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
				urlc.setRequestMethod("POST");
				urlc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				urlc.setRequestProperty("Content-Length", "" + Integer.toString(tokenURLdata.getBytes().length));				
				urlc.setDoOutput(true);
				urlc.setDoInput(true);
				urlc.setUseCaches(false);
				urlc.setAllowUserInteraction(false);
				DataOutputStream wr = new DataOutputStream(urlc.getOutputStream());
				wr.writeBytes(tokenURLdata);
				wr.flush();
				wr.close();
				
				int rc = urlc.getResponseCode();
				String responseMsg = urlc.getResponseMessage();
				System.out.println("Token response code:" + rc + ";Token response message: " + responseMsg);
				if (rc == 200) {
					// Success, so get the response body
					BufferedReader reader = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
				    for (String line; (line = reader.readLine()) != null;) {
				    	jsonToken += line;
				    }
				    
					System.out.println("Content: " + jsonToken);
				}

			}  catch (ProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			// Get the token from the JSON object
			ObjectMapper m = new ObjectMapper();
			JsonNode rootNode = m.readValue(jsonToken, JsonNode.class);
			String access_token = rootNode.path("access_token").getTextValue();

			username = (String) httpsession.getAttribute("username");

			Long accessTokenId = null; 
			AccessToken at = new AccessToken();
			at.setLoginName(username);
			at.setAccessToken(access_token);
			at.setSecret(applicationProperties.getProperty("apiSecret"));
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

			Token t = new Token(access_token, applicationProperties.getProperty("apiSecret"));
			CTCTApi lister = new CTCTApi(
					username,
					t, 
					applicationProperties.getProperty("apiKey"),
					applicationProperties.getProperty("apiSecret")
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
