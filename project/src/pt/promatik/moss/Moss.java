package pt.promatik.moss;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pt.promatik.moss.utils.FileLogger;
import pt.promatik.moss.utils.HttpRequest;
import pt.promatik.moss.utils.MySQL;
import pt.promatik.moss.utils.Utils;
import pt.promatik.moss.vo.UserVO;

public abstract class Moss
{
	public static final String MSG_DELIMITER = "&!";
	public static final String VERSION = "1.1.3";

	public int server_port = 30480;
	public Server srv;
	public MySQL mysql = new MySQL();
	public FileLogger filelog = new FileLogger();
	public HttpRequest http = new HttpRequest();
	public int log = Utils.LOG_ERRORS;
	public int socketTimeout = 0;

	public int MAX_CONNECTIONS = 0;
	public String CHARSET_IN = "UTF-8";
	public String CHARSET_OUT = "UTF-8";
	
	private Timer appTimer = new Timer();
	
	public Moss()
	{
		Utils.log_level = log;
	}
	
	protected void start()
	{
		start(server_port, log);
	}
	
	protected void start(int log)
	{
		start(server_port, log);
	}
	
	protected void start(String[] args)
	{
		start(server_port, args);
	}
	
	protected void start(int port, String[] args)
	{
		int argLog = log;
		try {
			HashMap<String, Object> map = Utils.map(args);
			if(map.get("log") != null)
				argLog = Integer.parseInt((String) map.get("log"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(args.length >= 2) start(port, argLog);
		else start(port);
	}
	
	protected void start(int port, int log)
	{
		System.out.println("MOSS v" + VERSION + " - Multiplayer Online Socket Server\nCopyright @promatik");
		server_port = port;
		this.log = log;
		
		Utils.log("Starting Server on port " + String.valueOf(port));
		Utils.log("Log level " + String.valueOf(log));
		
		srv = new Server(this, port);
		new Thread(srv).start();
		
		runTimeSettingsTimer();
		
		Utils.patternMessage = Pattern.compile("^#MOSS#<!(.+)!>#<!(.+)?!>#<!(.+)?!>#$");
		Utils.patternPingPong = Pattern.compile("p[i|o]ng");
	}
	
	protected void runTimeSettingsTimer()
	{
		Timer settingsTimer = new Timer();
		settingsTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				Properties prop = new Properties();
				InputStream input = null;

				try {
					input = new FileInputStream("moss_config");
					prop.load(input);
					
					// Load values
					MAX_CONNECTIONS = Integer.parseInt(prop.getProperty("max_connections"));
					
					input.close();
				} catch (Exception e) {
					Utils.log(e);
					settingsTimer.cancel();
				}
			}
		}, 0, 5000000);
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
	abstract public void userUpdatedAvailability(User user, boolean availability);
	abstract public void userMessage(User user, String command, String message, String request);
	
	// -----------------
	// User
	
	public List<User> getUsers(String room) {
		return getUsers(room, 20, 0);
	}
	
	public List<User> getUsers(String room, int limit, int page) {
		return getUsers(room, limit, page, 0, null);
	}
	
	public synchronized List<User> getUsers(String room, int limit, int page, int available, HashMap<String, Object> search) {
		Utils.log("getUsers: " + room + ", " + limit + ", " + page + ", " + available);
		
		Stream<User> streamUsers = srv.getRoom(room).users.values().stream();
		
		// Apply filters
		switch (available) {
			case User.GET_USERS_FILTER_ONLINE:
				streamUsers = streamUsers.filter(user -> user.isAvailable());
				break;
			case User.GET_USERS_FILTER_OFFLINE:
				streamUsers = streamUsers.filter(user -> !user.isAvailable());
				break;
		}
		
		// Search user data
		if(search != null) {
			for(Entry<String, Object> entry : search.entrySet()) {
				streamUsers = streamUsers.filter(user -> {
					if(user == null || user.data().get(entry.getKey()) == null)
						return false;
					return user.data().get(entry.getKey()).toString().toLowerCase().indexOf(entry.getValue().toString().toLowerCase()) > -1;
				});
			}
		}
		
		// Final list
		List<User> users = streamUsers.collect(Collectors.toList());
		
		if(users.size() == 0)
			return null;
		
		int limit_min = page * limit;
		int limit_max = (page + 1) * limit;
		if(limit_max >= users.size()) limit_max = users.size();
		if(limit_min >= users.size()) limit_min = users.size() - 1;
		
		try {
			List<User> result = users.subList(limit_min, limit_max);
			Utils.log("getUsers result: " + result.size());
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
	
	public synchronized User pickRandomPlayer(String myId) {
		return randomPlayer(myId, srv.getUsers());
	}
	
	public synchronized User pickRandomPlayer(String myId, String room) {
		return randomPlayer(myId, srv.getRoom(room).users.values());
	}
	
	private synchronized User randomPlayer(String myId, Collection<User> users){
		Vector<User> list = new Vector<User>();
		for (User user : users) {
			if(!user.id().equals(myId) && user.isAvailable())
				list.add(user);
		}
		
		return Utils.random(list);
	}
	
	public synchronized boolean invoke(User from, String id, String room, String command, String message) {
		boolean sent = false;
		try {
			sent = srv.getRoom(room).users.get(id).invoke(from, command, message);
		} catch (Exception e) {
			Utils.log("User " + id + " not found (" + command + " was not sent)");
		}
		return sent;
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