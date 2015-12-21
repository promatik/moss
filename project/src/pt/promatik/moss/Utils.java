package pt.promatik.moss;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import org.json.JSONObject;

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
}
