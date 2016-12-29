package pt.promatik.moss.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpRequest
{
	private String host = "";
	private String encoding = "UTF-8";
	private String contentType = "application/x-www-form-urlencoded";
	private String method = "POST";
	
	public HttpRequest() {
		
	}

	public void init(String host)
	{
		init(host, encoding, contentType, method);
	}
	
	public void init(String host, String encoding, String contentType, String method)
	{
		this.host = host;
		this.encoding = encoding;
		this.contentType = contentType;
		this.method = method;
	}
	
	public String request(Map<String, Object> params) throws Exception
	{
		if(host.equals(""))
			new Exception("HttpRequest is not initialized, run http.init()");
		
		StringBuilder postData = new StringBuilder();
		for (Map.Entry<String,Object> param : params.entrySet()) {
			if (postData.length() != 0) postData.append('&');
			postData.append(URLEncoder.encode(param.getKey(), encoding));
			postData.append('=');
			postData.append(URLEncoder.encode(String.valueOf(param.getValue()), encoding));
		}
		
		byte[] postDataB = postData.toString().getBytes(StandardCharsets.UTF_8);
		URL url = new URL(host);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setInstanceFollowRedirects(false);
		conn.setRequestMethod(method);
		conn.setRequestProperty("Content-Type", contentType); 
		conn.setRequestProperty("charset", encoding);
		conn.setRequestProperty("Content-Length", Integer.toString(postDataB.length));
		conn.setUseCaches(false);
		
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.write(postDataB);
		
		String res = "";
		Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), encoding));
		for (int c; (c = in.read()) >= 0; res += (char) c);
		return res;
		
	}
	
	
}