package pt.promatik.moss;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import pt.promatik.moss.vo.UserVO;

public abstract class Moss
{
	public static int APP_TIMER = 60000; // 1 minute
	public static int SERVER_PORT = 30480;
	public static final int USER_THROTTLE = 100;
	public static final int ROOM_THROTTLE = 200;
	public static final String MSG_DELIMITER = "&!";
	public static Moss instance;
	
	public boolean log = true;
	public Server srv;
	public Timer pingTimer = new Timer();
	public Timer appTimer = new Timer();
	
    public Moss()
    {
    	instance = this;
    }
    
    protected void start()
    {
    	start(30480, true);
    }
    
    protected void start(boolean log)
    {
    	start(30480, log);
    }
    
    protected void start(String[] args)
    {
    	if(args.length > 0) start(args[0] == "log");
    	if(args.length > 1) start(Integer.parseInt(args[1]), args[0] == "log");
		else start();
    }
    
    protected void start(int port, boolean log)
    {
    	System.out.println("MOSS v0.1 - Multiplayer Online Socket Server\n"/*Copyright @promatik\n*/);
    	if(instance == null) instance = this;
    	SERVER_PORT = port;
    	this.log = log;
    	
    	Utils.log("Starting Server");
    	srv = new Server(port);
    	new Thread(srv).start();
    	
    	pingTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				srv.pingUsers();
			}
		}, 60000); // 1 minute
    }
    
    protected void startTimer(int interval)
    {
    	appTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				appTimer();
			}
		}, interval); 
    }
    
    protected void appTimer()
    {
    	System.out.println("To use timer you should override appTimer()");
    }
    
    protected void stop()
    {
    	Utils.log("Stopping Server");
    	srv.quit();
    }
    
    // -----------------
    // Events
    
    abstract public void serverStarted();
    abstract public void serverStopped();
    abstract public void userConnected(User user);
    abstract public void userDisconnected(User user);
    abstract public void userUpdatedStatus(User user, String status);
    abstract public void userMessage(User user, String command, String message);
    
    // -----------------
    // User
    
	protected void updateStatus(UserVO user, String status) {
		try {
			srv.getRoom(user.room).users.get(user.id).setStatus(status);
		} catch (Exception e) {
			Utils.log("User " + user.id + " not found");
		}
    }
    
	protected List<User> getUsers(String room) {
    	return getUsers(room, 20, 0);
    }
    
	protected List<User> getUsers(String room, int limit, int page) {
		ArrayList<User> users = new ArrayList<User>(srv.getRoom(room).users.values());
		if(users.size() == 0)
			return null;
		
		int limit_min = page * limit;
		int limit_max = (page + 1) * limit;
		if(limit_max >= users.size()) limit_max = users.size();
		if(limit_min >= users.size()) limit_min = users.size() - 1;
		
		List<User> result = null;
		try {
			result = users.subList(limit_min, limit_max);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    }
    
	protected User getUserByID(UserVO user) {
		User u = srv.getRoom(user.room).users.get(user.id);
		return u;
    }
    
	protected List<User> getUsersByID(UserVO[] users) {
		List<User> result = new ArrayList<User>();
		for (UserVO user : users) {
			User u = getUserByID(user);
			if(u != null)
				result.add(u);
		}
    	return result;
    }
    
	protected int getUsersCount(String room) {
		return srv.getRoom(room).users.size();
    }
    
	protected boolean invoke(User from, String id, String room, String command, String message) {
		try {
			srv.getRoom(room).users.get(id).invoke(from, command, message);
			return true;
		} catch (Exception e) {
			Utils.log("User " + id + " not found");
			return false;
		}
    }
    
	protected void invokeOnRoom(User from, String room, String command, String message) {
    	srv.getRoom(room).invoke(from, command, message);
    }
    
	protected void invokeOnAll(User from, String command, String message) {
    	for (Room room : srv.getRooms()) {
			for (User user : room.users.values()) {
				user.invoke(from, command, message);
			}
		}
    }
}