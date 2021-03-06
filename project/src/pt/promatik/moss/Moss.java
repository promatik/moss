package pt.promatik.moss;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pt.promatik.moss.utils.Delegate;
import pt.promatik.moss.utils.FileLogger;
import pt.promatik.moss.utils.HttpRequest;
import pt.promatik.moss.utils.MySQL;
import pt.promatik.moss.utils.Settings;
import pt.promatik.moss.utils.Utils;
import pt.promatik.moss.vo.UserVO;

public abstract class Moss
{
	public static final String VERSION = "2.1.0";

	public int server_port = 30480;
	public String server_ip = null;
	public Server server;
	public Console console;
	public MySQL mysql = new MySQL();
	public FileLogger filelog = new FileLogger();
	public HttpRequest http = new HttpRequest();
	public Settings settings;
	
	public int socketTimeout = 0;
	public boolean autoLogoutOnDoubleLogin = true;
	public boolean validateLogin = false;
	public int connections_max = 0;
	public int connections_waiting = 0;
	public Charset charset_in = StandardCharsets.UTF_8;
	public Charset charset_out = StandardCharsets.UTF_8;
	
	private Delegate appTimer;
	
	public Moss()
	{
		
	}
	
	protected void start()
	{
		start(0, null);
	}
	
	protected void start(String[] args)
	{
		start(0, args);
	}
	
	protected void start(int port, String[] args)
	{
		int log = 0;
		
		try {
			if(args != null) {
				HashMap<String, Object> map = Utils.map(args);
				if(map.get("log") != null)
					log = Integer.parseInt((String) map.get("log"));
			}
		} catch (Exception e) {
			
		}
		
		start(port, log, args);
	}
	
	protected void start(int port, int log, String[] args)
	{
		String configFile = null;
		
		try {
			if(args != null) {
				HashMap<String, Object> map = Utils.map(args);
				if(map.get("config") != null)
					configFile = (String) map.get("config");
			}
		} catch (Exception e) {
			
		}
		
		start(port, log, configFile);
	}
	
	protected void start(int port, int log, String configFile)
	{
		System.out.println("MOSS v" + VERSION + " - Multiplayer Online Socket Server");
		System.out.println("https://github.com/promatik/moss");

		// Load Settings
		settings = new Settings(this, configFile);
		
		// Defaults
		server_port = port > 0 ? port : settings.server_port;
		Utils.log_level = log > 0 ? log : settings.server_log_level;

		Utils.log("Starting Server on port " + String.valueOf(server_port));
		Utils.log("Log level " + String.valueOf(Utils.log_level));
		
		// Start default modules 
		if(settings.mysql_host != null && !settings.mysql_host.equals("")) {
			mysql.connect(settings.mysql_host, settings.mysql_port, settings.mysql_database, settings.mysql_user, settings.mysql_pass);
			Utils.log("MySQL running on '" + settings.mysql_host + ":" + String.valueOf(mysql.port()) + "' with '" + settings.mysql_database + "' database");
		}
		if(settings.http_host != null && !settings.http_host.equals("")) {
			http.init(settings.http_host);
			Utils.log("HTTP service set to '" + settings.http_host + "'");
		}
		if(settings.filelog_filename != null && !settings.filelog_filename.equals("")) {
			filelog.init(settings.filelog_path, settings.filelog_filename);
			Utils.log("Log to file is enable on '" + settings.filelog_path + settings.filelog_filename + "'");
		}
		
		server = new Server(this, settings.server_ip, server_port);
		new Thread(server).start();
		
		console = new Console(this);
		new Thread(console).start();
	}
	
	protected void startPingTimer(int interval)
	{
		Delegate.run(() -> server.pingUsers(), 0, interval);
	}
	
	protected void startTimer(int interval)
	{
		appTimer = Delegate.run(() -> appTimer(), 1000, interval);
	}
	
	protected void stopTimer()
	{
		appTimer.cancel();
	}
	
	protected void appTimer()
	{
		Utils.error("To use timer you should override appTimer()");
	}
	
	protected void stop()
	{
		Utils.log("Stopping Server");
		server.quit();
	}
	
	// -----------------
	// Events
	
	abstract public void serverStarted();
	abstract public void serverStopped();
	abstract public void userConnected(User user);
	abstract public void userDisconnected(User user);
	abstract public void userUpdatedStatus(User user, String status);
	abstract public void userUpdatedAvailability(User user, boolean availability);
	abstract public void userUpdatedRoom(User user, String room);
	abstract public void userMessage(User user, String command, String message, String request);
	abstract public void commandInput(String command, String value);
	abstract public String validateLogin(String login, String password, String data);
	
	// -----------------
	// User
	
	public List<User> getUsers(String room)
	{
		return getUsers(room, 20, 0);
	}
	
	public List<User> getUsers(String room, int limit, int page)
	{
		return getUsers(room, limit, page, 0, null);
	}
	
	public synchronized List<User> getUsers(String room, int limit, int page, int available, HashMap<String, Object> search)
	{
		Stream<User> streamUsers = server.getRoom(room).users.values().stream();
		
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
			return result;
		} catch (Exception e) {
			Utils.log(e);
			return null;
		}
	}
	
	public synchronized User getUserByID(UserVO user)
	{
		return getUserByID(user.id, user.room);
	}
	
	public synchronized User getUserByID(String id)
	{
		User user = null;
		for (Room room : server.getRooms()) {
			user = room.users.get(id);
			if(user != null)
				return user; 
		}
		return null;
	}
	
	public synchronized User getUserByID(String id, String room)
	{
		User u = server.getRoom(room).users.get(id);
		return u;
	}
	
	public synchronized List<User> getUsersByID(UserVO[] users)
	{
		List<User> result = new ArrayList<User>();
		for (UserVO user : users) {
			User u = getUserByID(user);
			if(u != null)
				result.add(u);
		}
		return result;
	}
	
	public synchronized int getUsersCount(String room)
	{
		return server.getRoom(room).users.size();
	}
	
	public synchronized User pickRandomPlayer(String excludeID)
	{
		return randomPlayer(excludeID, server.getUsers());
	}
	
	public synchronized User pickRandomPlayer(String excludeID, String room)
	{
		return randomPlayer(excludeID, server.getRoom(room).users.values());
	}
	
	public synchronized User pickRandomPlayer(String[] excludeIDs)
	{
		return randomPlayer(excludeIDs, server.getUsers());
	}
	
	public synchronized User pickRandomPlayer(String[] excludeIDs, String room)
	{
		return randomPlayer(excludeIDs, server.getRoom(room).users.values());
	}
	
	private synchronized User randomPlayer(String excludeID, Collection<User> users)
	{
		return randomPlayer(new String[] {excludeID}, users);
	}
	
	private synchronized User randomPlayer(String[] excludeIDs, Collection<User> users)
	{
		Vector<User> list = new Vector<User>();
		Set<String> excludes = new HashSet<String>(Arrays.asList(excludeIDs));
		
		for (User user : users) {
			if(user.isAvailable() && !excludes.contains(user.id()))
				list.add(user);
		}
		
		return Utils.random(list);
	}
	
	public synchronized boolean invoke(User from, String id, String room, String command, String message)
	{
		boolean sent = false;
		try {
			sent = server.getRoom(room).users.get(id).invoke(from, command, message);
		} catch (Exception e) {
			Utils.log("User " + id + " not found (" + command + " was not sent)");
		}
		return sent;
	}
	
	public synchronized void invokeOnRoom(User from, String room, String command, String message)
	{
		server.getRoom(room).invoke(from, command, message);
	}
	
	public synchronized void invokeOnAll(User from, String command, String message)
	{
		for (Room room : server.getRooms()) {
			for (User user : room.users.values()) {
				user.invoke(from, command, message);
			}
		}
	}
}