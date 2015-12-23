package pt.promatik.moss.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import org.json.JSONObject;

import pt.promatik.moss.Moss;

public class Utils{
	public final static int LOG_NONE = 0;
	public final static int LOG_DEFAULT = 1;
	public final static int LOG_ERRORS = 2;
	
	public static Random random = new Random(System.nanoTime());
	
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
    	if(Moss.instance.log >= LOG_DEFAULT) {
    		System.out.println("MOSS " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "> " + (ref.equals("") ? message : ref + ": " + message));
    	}
    }
    
    public static void log(Exception e)
    {
    	if(Moss.instance.log >= LOG_ERRORS) {
    		e.printStackTrace();
    	}
    }

    public static String JSONStringify(HashMap<String, Object> args)
    {
    	JSONObject json = new JSONObject();
    	for (String key : args.keySet())
			json.put(key, args.get(key));
    	
    	return json.toString();
    }
    
    public static JSONObject JSONParse(String json)
    {
    	return new JSONObject(json);
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
}
