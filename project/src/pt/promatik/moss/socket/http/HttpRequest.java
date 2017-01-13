package pt.promatik.moss.socket.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import pt.promatik.moss.socket.io.WebSocketServerInputStream;

public class HttpRequest implements Map<String, String> {
	public static final String REQUEST_LINE = "REQUEST_LINE";

	private Map<String, String> headerMap;

	public HttpRequest(final InputStream in) {
		if (in == null) throw new NullPointerException();
		read(in);
	}

	public final int size() {
		return headerMap.size();
	}

	public final boolean isEmpty() {
		return headerMap.isEmpty();
	}

	public final boolean containsKey(final Object key) {
		return headerMap.containsKey(key);
	}

	public final boolean containsValue(final Object value) {
		return headerMap.containsValue(value);
	}

	public final String get(final Object key) {
		return headerMap.get(key);
	}

	public final String put(final String key, final String value) {
		return headerMap.put(key, value);
	}

	public final String remove(final Object key) {
		return headerMap.remove(key);
	}

	public final void putAll(final Map<? extends String, ? extends String> m) {
		headerMap.putAll(m);
	}

	public final void clear() {
		headerMap.clear();
	}

	public final Set<String> keySet() {
		return headerMap.keySet();
	}

	public final Collection<String> values() {
		return headerMap.values();
	}

	public final Set<Entry<String, String>> entrySet() {
		return headerMap.entrySet();
	}

	private void read(final InputStream in) {
		WebSocketServerInputStream lis = new WebSocketServerInputStream(in);
		headerMap = new HashMap<String, String>();
		try {
			String line = lis.readLine();
			if(line.equals("#MOSS#"))
			{
				headerMap.put(REQUEST_LINE, line);
			} else
			{
				while (line != null && line.isEmpty()) {
					line = lis.readLine();
				}
				headerMap.put(REQUEST_LINE, line);
				while (line != null && !line.isEmpty()) {
					int firstColonPos = line.indexOf(":");
					if (firstColonPos > 0) {
						String key = line.substring(0, firstColonPos).trim();
						int length = line.length();
						String value = line.substring(firstColonPos + 1, length);
						value = value.trim();
						if (!key.isEmpty() && !value.isEmpty()) {
							headerMap.put(key, value);
							headerMap.put(key.toLowerCase(), value);
						}
					}	
					line = lis.readLine();
				}
			}
			lis.close();
		} catch (IOException e) {
			System.out.println("Unable to read HTTP Request in HttpRequest.read():");
		}
		headerMap = Collections.unmodifiableMap(headerMap);

	}
}
