package br.com.sankhya.dashviewer;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class NotificationService {

	private final static String	NOTIFICATION_SERVICE	= "/mge/service.sbr?serviceName=PushNotificationSP.getMobileNotificationCounter&counter={0}&application=Sankhya-W%20Mobile";

	private final static String	STATUS_OK				= "1";

	private long				lastExecution;
	private String				mgeSession;
	private String				serverUrl;
	private String				kID;
	private Context 			context;
	private JSONObject sessionID;

	public NotificationService(Context context){
		this.context = context;
	}
	
	public NotificationInfo getNotification() {
		try {
			JSONObject result = executeService();

			if (result != null) {
				NotificationInfo notification = new NotificationInfo();
				notification.amount = result.getInt("amount");
				
				if(result.has("lastMessageID")){
					notification.lastMessageID = result.getString("lastMessageID");
				}
				
				if(result.has("lastMessage")){
					notification.lastMessage = result.getString("lastMessage");
				}
				
				return notification;
			}
		} catch (JSONException e) {
			Log.e("Error:", "Problemas ao processar o JSON de resposta das notificações.");
		}

		return null;
	}

	private JSONObject executeService() {
		try {
			if (serverUrl != null) {
				String notificationService = NOTIFICATION_SERVICE.replace("{0}", String.valueOf(System.currentTimeMillis()));

				if (mgeSession != null) {
					notificationService += "&mgeSession=" + mgeSession;
				}

				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httpPost = new HttpPost(serverUrl + notificationService);
				httpPost.addHeader("Content-Type", "text/xml; charset=utf-8");

				if (kID != null) {
					httpPost.addHeader("Authorization", "Bearer " + kID);
				}

				CookieStore reqCookieStore = new BasicCookieStore();
				loadSessionID(reqCookieStore);
				
				HttpContext context = new BasicHttpContext();
				context.setAttribute(ClientContext.COOKIE_STORE, reqCookieStore);
				
				Document reqDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

				Element serviceRequestElem = reqDoc.createElement("serviceRequest");
				serviceRequestElem.setAttribute("serviceName", "PushNotificationSP.getMobileNotificationCounter");
				reqDoc.appendChild(serviceRequestElem);

				Element requestBodyElem = reqDoc.createElement("requestBody");
				serviceRequestElem.appendChild(requestBodyElem);

				Element paramsElem = reqDoc.createElement("params");
				requestBodyElem.appendChild(paramsElem);

				Element lastNotificationElem = reqDoc.createElement("lastNotification");
				lastNotificationElem.setAttribute("startAt", String.valueOf(lastExecution));
				paramsElem.appendChild(lastNotificationElem);

				String requestBody = getStringFromDoc(reqDoc);

				StringEntity entity = new StringEntity(requestBody, HTTP.UTF_8);
				entity.setContentType("text/xml; charset=utf-8");
				httpPost.setEntity(entity);

				HttpResponse response = httpclient.execute(httpPost, context);
				StatusLine statusLine = response.getStatusLine();

				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
					if (context.getAttribute(ClientContext.COOKIE_STORE) != null) {
						CookieStore respCookieStore = (CookieStore) context.getAttribute(ClientContext.COOKIE_STORE);
						storeSessionID(respCookieStore);
					}
					
					String responseString = EntityUtils.toString(response.getEntity());

					XMLParser parser = new XMLParser();
					Document respDoc = parser.getDomElement(responseString);

					Element responseElem = respDoc.getDocumentElement();
					String serverStatus = responseElem.getAttribute("status");

					if (STATUS_OK.equals(serverStatus)) {
						Element responseBodyElem = (Element) responseElem.getElementsByTagName("responseBody").item(0);
						String json = parser.getValue(responseBodyElem, "json");
						return new JSONObject(json);
					}
				} else {
					//Closes the connection.
					response.getEntity().getContent().close();
				}
			}
		} catch (Exception e) {
			Log.e("Error:", "Problemas ao chamar o serviço de notificações.\n" + e.getMessage());
		}

		return null;
	}
	
	private void loadSessionID(CookieStore cookieStore){
		try{
			if(sessionID == null){
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
				String sessionJson = pref.getString("sessionID", null);
				
				if(sessionJson != null){
					sessionID = new JSONObject(sessionJson);
				}
			}
			
			if(sessionID != null){
				BasicClientCookie cookie = new BasicClientCookie(sessionID.getString("name"), sessionID.getString("value"));
				cookie.setDomain(sessionID.getString("domain"));
				cookie.setPath(sessionID.getString("path"));
				
				cookieStore.addCookie(cookie);
			}
		}catch(JSONException e){
			Log.e("Erro: ", "Oops, problemas ao tentar resgatar o cookie de sessão.");
		}
	}
	
	private void storeSessionID(CookieStore cookieStore){
		for(Cookie cookie : cookieStore.getCookies()){
			if("JSESSIONID".equalsIgnoreCase(cookie.getName())){
				boolean storeCookie = true;
				
				if(sessionID != null){
					try{
						String sessionValue = sessionID.getString("value");
						
						//Se o ID de sessão é o mesmo não precisa guardar o cookie
						if(sessionValue.equals(cookie.getValue())){
							storeCookie = false;
						}
					}catch(JSONException ignored){}
				}
				
				if(storeCookie){
					try{
						sessionID = new JSONObject();
						sessionID.put("name", cookie.getName());
						sessionID.put("value", cookie.getValue());
						sessionID.put("domain", cookie.getDomain());
						sessionID.put("path", cookie.getPath());
						
						SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
						
						Editor prefEdit = pref.edit();
						prefEdit.putString("sessionID", sessionID.toString());
						
						prefEdit.commit();
					}catch(JSONException e){
						Log.e("Erro: ", "Oops, problemas ao tentar gravar o cookie de sessão.");
					}
				}
				
				break;
			}
		}
	}

	public String getStringFromDoc(Document doc) {
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			return writer.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return null;
	}

	public void setLastExecution(long lastExecution) {
		this.lastExecution = lastExecution;
	}

	public void setMgeSession(String mgeSession) {
		this.mgeSession = mgeSession;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public void setkID(String kID) {
		this.kID = kID;
	}

	public static class NotificationInfo {
		int		amount;
		String	lastMessage;
		String	lastMessageID;
	}
	
	private static class SessionID{
		String name;
		String value;
		String domain;
		String path;
	}
}
