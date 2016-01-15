package pt.promatik.moss;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import pt.promatik.moss.utils.HttpRequest;
import pt.promatik.moss.utils.MySQL;
import pt.promatik.moss.utils.Utils;
import pt.promatik.moss.vo.UserVO;

public abstract class Moss
{
	public static Moss instance;
	public static int SERVER_PORT = 30480;
	public static final String MSG_DELIMITER = "&!";
	
	public Server srv;
	public MySQL mysql = new MySQL();
	public HttpRequest http = new HttpRequest();
	public int log = Utils.LOG_ERRORS;
	public int socketTimeout = 0;
	
	private Timer appTimer = new Timer();
	
    public Moss()
    {
    	instance = this;
    }
    
    protected void start()
    {
    	start(SERVER_PORT, log);
    }
    
    protected void start(int log)
    {
    	start(SERVER_PORT, log);
    }
    
    protected void start(String[] args)
    {
    	if(args.length >= 2) start(args[0].equals("log") ? Integer.parseInt(args[1]) : log);
		else start();
    }
    
    protected void start(int port, int log)
    {
    	System.out.println("MOSS v1.0.2 - Multiplayer Online Socket Server\nCopyright @promatik");
    	if(instance == null) instance = this;
    	SERVER_PORT = port;
    	this.log = log;
    	
    	Utils.log("Starting Server");
    	Utils.log("Log level " + String.valueOf(log));
    	
    	srv = new Server(port);
    	new Thread(srv).start();
    	
    	Utils.patternMessage = Pattern.compile("^#MOSS#<!(.+)!>#<!(.+)?!>#<!(.+)?!>#$");
    	Utils.patternPingPong = Pattern.compile("p[i|o]ng");
    }
    
    protected void startPingTimer(int interval)
    {
    	Timer pingTimer = new Timer();
    	pingTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				srv.pingUsers();
			}
		}, 0, interval);
    }
    
    protected void startTimer(int interval)
    {
    	appTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				appTimer();
			}
		}, 1000, interval); 
    }
    
    protected void stopTimer()
    {
    	appTimer.cancel();
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
    abstract public void userMessage(User user, String command, String message, String request);
    
    // -----------------
    // User
    
	public synchronized void updateStatus(UserVO user, String status) {
		try {
			srv.getRoom(user.room).users.get(user.id).setStatus(status);
		} catch (Exception e) {
			Utils.log("User " + user.id + " not found", e);
		}
    }
    
	public List<User> getUsers(String room) {
    	return getUsers(room, 20, 0);
    }
    
	public synchronized List<User> getUsers(String room, int limit, int page) {
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
			Utils.log(e);
			return null;
		}
    }
    
	public synchronized User getUserByID(UserVO user) {
		User u = srv.getRoom(user.room).users.get(user.id);
		return u;
    }
    
	public synchronized List<User> getUsersByID(UserVO[] users) {
		List<User> result = new ArrayList<User>();
		for (UserVO user : users) {
			User u = getUserByID(user);
			if(u != null)
				result.add(u);
		}
    	return result;
    }
    
	public synchronized int getUsersCount(String room) {
		return srv.getRoom(room).users.size();
    }
    
	public synchronized boolean invoke(User from, String id, String room, String command, String message) {
		try {
			srv.getRoom(room).users.get(id).invoke(from, command, message);
			return true;
		} catch (Exception e) {
			Utils.log("User " + id + " not found (" + command + " was not sent)");
			return false;
		}
    }
    
	public synchronized void invokeOnRoom(User from, String room, String command, String message) {
    	srv.getRoom(room).invoke(from, command, message);
    }
    
	public synchronized void invokeOnAll(User from, String command, String message) {
    	for (Room room : srv.getRooms()) {
			for (User user : room.users.values()) {
				user.invoke(from, command, message);
			}
		}
    }
}