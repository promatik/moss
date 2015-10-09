package pt.promatik.moss;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils{
	
    public static void log(String message)
    {
    	log(message, "");
    }
    
    public static void log(String message, String ref)
    {
    	if(Moss.instance.log) {
    		System.out.println("MOSS " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "> " + (ref.equals("") ? message : ref + ": " + message));
    	}
    }
}
