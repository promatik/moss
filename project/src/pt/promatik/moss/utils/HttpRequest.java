package pt.promatik.moss.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpRequest
{
	private String host = "";
	private Charset encoding = StandardCharsets.UTF_8;
	private String contentType = "application/x-www-form-urlencoded";
	private Method method = Method.POST;
	
	public static enum Method { POST, GET };
	
	public HttpRequest() {
		
	}

	public void init(String host)
	{
		init(host, encoding, contentType, method);
	}
	
	public void init(String host, Charset encoding, String contentType, Method method)
	{
		this.host = host;
		this.encoding = encoding;
		this.contentType = contentType;
		this.method = method;
	}
	
	public String request() throws Exception
	{
		return request(null, null);
	}
	
	public String request(Map<String, Object> params) throws Exception
	{
		return request(params, null);
	}
	
	public String request(String raw) throws Exception
	{
		return request(null, raw);
	}
	
	public String request(Map<String, Object> params, String raw) throws Exception
	{
		if(host.equals(""))
			new Exception("HttpRequest is not initialized, run http.init()");
		
		StringBuilder postData = new StringBuilder();
		if(params != null) {
			for (Map.Entry<String,Object> param : params.entrySet()) {
				if (postData.length() != 0) postData.append('&');
				postData.append(URLEncoder.encode(param.getKey(), encoding.name()));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), encoding.name()));
			}
		}
		else if(raw != null) {
			postData.append(raw);
		}
		
		byte[] postDataB = postData.toString().getBytes(StandardCharsets.UTF_8);
		URL url = new URL(host);
		String res = "";
		InputStream s;
		
		switch (method) {
			case POST:
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setInstanceFollowRedirects(false);
				conn.setRequestMethod(method.name());
				conn.setRequestProperty("Content-Type", contentType); 
				conn.setRequestProperty("charset", encoding.name());
				conn.setRequestProperty("Content-Length", Integer.toString(postDataB.length));
				conn.setUseCaches(false);
				
				DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
				wr.write(postDataB);
				
				s = conn.getInputStream();
				break;
			default:
			case GET:
				URLConnection connGet = url.openConnection();
				s = connGet.getInputStream();
				break;
		}
		
		Reader in = new BufferedReader(new InputStreamReader(s, encoding));
		for (int c; (c = in.read()) >= 0; res += (char) c);
		
		return res;
	}
	
	
}