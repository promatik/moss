package pt.promatik.moss;

public class Utils{
	
    public static void log(String message)
    {
    	log(message, "");
    }
    
    public static void log(String message, String ref)
    {
    	if(Moss.log)
    		System.out.println("MOSS> " + (ref.equals("") ? message : ref + ": " + message));
    }
}
