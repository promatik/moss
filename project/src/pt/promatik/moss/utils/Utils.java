package pt.promatik.moss.utils;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;

import org.json.JSONObject;

public class Utils{
	public static int log_level = 3;
	public final static int LOG_NONE = 0;
	public final static int LOG_DEFAULT = 1;
	public final static int LOG_ERRORS = 2;
	public final static int LOG_FULL = 3;
	
	public static Random random = new Random(System.nanoTime());
	public static Pattern patternMessage, patternPingPong;
	
	public static void log(String message)
	{
		log(message, "");
	}
	
	public static void log(String message, Exception e)
	{
		log(message, "", e);
	}
	
	public static void log(String message, String ref, Exception e)
	{
		log(message, ref);
		log(e);
	}
	
	public static void log(String message, String ref)
	{
		if(log_level >= LOG_DEFAULT) {
			System.out.println( (ref.equals("") ? "MOSS" : ref) + " " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "> " + message);
		}
	}
	
	public static void log(Exception e)
	{
		if(log_level >= LOG_ERRORS) {
			e.printStackTrace();
		}
	}

	public static String JSONStringify(HashMap<String, Object> args)
	{
		JSONObject json = new JSONObject();
		if(args != null)
			for (String key : args.keySet())
				json.put(key, args.get(key));
		
		return json.toString();
	}
	
	public static JSONObject JSONParse(String json)
	{
		return new JSONObject(json);
	}
	
	public static HashMap<String, Object> map(String... args) throws Exception {
		return map((Object[]) args);
	}

	public static HashMap<String, Object> map(Object... args) throws Exception {
		if(args.length % 2 == 1)
			throw new Exception("Odd number of arguments");
		
		HashMap<String, Object> r = new HashMap<>();
		for (int i = 0; i < args.length; i+=2) {
			r.put((String) args[i], args[i+1]);
		}
		
		return r;
	}
	
	public static <T> T random(Collection<T> coll) {
		if(coll.size() == 0) return null;
		int num = (int) (Math.random() * coll.size());
		for(T t: coll) if (--num < 0) return t;
		throw new AssertionError();
	}
}
