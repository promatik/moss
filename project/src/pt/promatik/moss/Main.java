package pt.promatik.moss;

public class Main
{
	public static final int USER_THROTTLE = 100;
	public static final int ROOM_THROTTLE = 200;
	private Server srv;
	
	
    public void main ( String [] arguments )
    {
    	System.out.println("MOSS v0.1 - Multiplayer Online Socket Server");
    	System.out.println("Copyright @promatik");
    	System.out.println("");
    	
    	start(30480);
    }
    
    public void start(int port)
    {
    	System.out.println("Starting Server");
    	srv = new Server(port);
    	new Thread(srv).start();
    }
    
    public void stop()
    {
    	System.out.println("Stopping Server");
    	srv.quit();
    }
}