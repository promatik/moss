package pt.promatik.moss;

public class Main
{
	public static final int USER_THROTTLE = 100;
	public static final int ROOM_THROTTLE = 200;
	
	public static Boolean log = true;
	private static Server srv;
	
    public static void main ( String [] args )
    {
    	System.out.println("MOSS v0.1 - Multiplayer Online Socket Server");
    	System.out.println("Copyright @promatik");
    	System.out.println("");
    	
    	start(30480);
    }
    
    public static void start(int port)
    {
    	log("Starting Server");
    	srv = new Server(port);
    	new Thread(srv).start();
    }
    
    public void stop()
    {
    	log("Stopping Server");
    	srv.quit();
    }
    
    private static void log(String message)
    {
    	log(message, "");
    }
    
    private static void log(String message, String ref)
    {
    	if(!log) return;
    	System.out.println("MOSS> " + (ref.equals("") ? message : ref + ": " + message));
    }
    
    // -----------------
    // User
    
    public void connect() {
    	
    }
    
    public void disconnect() {
    	
    }
    
    public void updateStatus(String status) {
    	
    }
    
    // -----------------
    // User
    
    public int getUsersCount() {
		return 0;
    }
    
    public int getUsersCountInRoom() {
		return 0;
    }
    
    public int[] getUsers() {
		return null;
    }
    
    public int[] getUsersInRoom() {
		return null;
    }
    
    // -----------------
    // Invokes
    
    public void invoke() {
    	
    }
    
    public void invokeOnRoom() {
    	
    }
    
    public void invokeOnAll() {
    	
    }
}