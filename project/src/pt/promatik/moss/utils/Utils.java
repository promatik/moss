package pt.promatik.moss.utils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;

import org.json.JSONObject;

public class Utils
{
	public final static int LOG_NONE = 0;
	public final static int LOG_DEFAULT = 1;
	public final static int LOG_ERRORS = 2;
	public final static int LOG_FULL = 3;
	public final static int LOG_VERBOSE = 4;
	
	public static int log_level = LOG_FULL;
	
	public static Random random = new Random(System.nanoTime());
	public static Pattern patternMessage, patternPingPong;
	
	private final static String TAG = "MOSS";
	private static long nanoTime = 0;

	public static void log(String message)
	{
		log(message, "");
	}
	
	public static void log(String message, String ref)
	{
		log(message, ref, false);
	}
	
	public static void log(String message, boolean forceLog)
	{
		log(message, "", forceLog);
	}
	
	public static void log(String message, Exception e)
	{
		log(message, "", e);
	}
	
	public static void log(String message, String ref, Exception e)
	{
		error(message, ref);
		log(e);
	}
	
	public static void log(String message, String ref, boolean forceLog)
	{
		if(log_level >= LOG_DEFAULT || forceLog) {
			System.out.println( (ref.equals("") ? TAG : ref) + " " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "> " + message);
		}
	}
	
	public static void log(Exception e)
	{
		if(log_level >= LOG_ERRORS) {
			e.printStackTrace();
		}
	}
	
	public static void error(String message)
	{
		error(message, "");
	}
	
	public static void error(String message, String ref)
	{
		System.err.println( (ref.equals("") ? TAG : ref) + " " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "> " + message);
	}

	public static String JSONStringify(Object... args)
	{
		return JSONStringify(map(args));
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
	
	public static HashMap<String, Object> map(String... args)
	{
		return map((Object[]) args);
	}

	public static HashMap<String, Object> map(Object... args)
	{
		if(args.length % 2 == 1)
			new Exception("Odd number of arguments");
		
		HashMap<String, Object> r = new HashMap<>();
		for (int i = 0; i < args.length; i+=2) {
			r.put((String) args[i], args[i+1]);
		}
		
		return r;
	}
	
	public static <T extends Enum<T>> T random(Class<T> enumerator)
	{
		return random(Arrays.asList(enumerator.getEnumConstants()));
	}
	
	public static <T> T random(Collection<T> coll)
	{
		if(coll.size() == 0) return null;
		int num = (int) (Math.random() * coll.size());
		for(T t: coll) if (--num < 0) return t;
		throw new AssertionError();
	}
	
	public static int random(int min, int max)
	{
		return min + (int)(Math.random() * (max - min + 1));
	}
	
	public static boolean isEmptyOrNull(String message)
	{
		return message == null || message.isEmpty() || message.equals("null");
	}
	
	public static void sleep(int miliseconds) {
		try { Thread.sleep(miliseconds); } catch (InterruptedException e) { }
	}
	
	public static void nanoTime()
	{
		nanoTime(false, "");
	}
	
	public static void nanoTime(boolean print, String suffix)
	{
		float val = (System.nanoTime() - nanoTime) / 1000000f;
		if(print) log(val + " ms " + suffix);
		nanoTime = System.nanoTime();
	}
	
	public static <T extends Enum<T>> boolean enumContains(Class<T> enumerator, String value)
	{
		for (T c : enumerator.getEnumConstants()) {
			if (c.name().equals(value)) {
				return true;
			}
		}
		return false;
	}
	
	public static String encodeString(String str, Charset to_charset) {
		return encodeString(str, StandardCharsets.UTF_8, to_charset);
	}
	
	public static String encodeString(String str, Charset from_charset, Charset to_charset) {
		ByteBuffer s = from_charset.encode(str);
		return new String( s.array(), to_charset ).trim();
	}
}
