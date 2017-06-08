package pt.promatik.moss;

import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Observable;

import pt.promatik.moss.utils.Utils;
import pt.promatik.moss.vo.UserVO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class User extends Observable
{
	public static final int PIPE = 124;
	public static final int CARDINAL = 35;
	
	public static final String MSG_DELIMITER = "&!";
	public static final String MSG_USER_DELIMITER = "&;";

	public static final int GET_USERS_FILTER_ALL = 0;
	public static final int GET_USERS_FILTER_ONLINE = 1;
	public static final int GET_USERS_FILTER_OFFLINE = 2;

	private static Pattern patternMessage = Pattern.compile("^#MOSS#<!(.+)!>#<!(.+)?!>#<!(.+)?!>#$");
	private static Pattern patternPingPong = Pattern.compile("p[i|o]ng");
	
	// Commands
	private static final String CONNECT = "_connect";
	private static final String DISCONNECT = "_disconnect";
	private static final String UPDATE_STATUS = "_updateStatus";
	private static final String UPDATE_AVAILABILITY = "_updateAvailability";
	private static final String GET_USER = "_getUser";
	private static final String GET_USERS = "_getUsers";
	private static final String GET_USERS_COUNT = "_getUsersCount";
	private static final String SET_DATA = "_setData";
	private static final String GET_ROOMS = "_getRooms";
	private static final String UPDATE_ROOM = "_updateRoom";
	private static final String RANDOM_PLAYER = "_randomPlayer";
	private static final String INVOKE = "_invoke";
	private static final String INVOKE_ON_ROOM = "_invokeOnRoom";
	private static final String INVOKE_ON_ALL = "_invokeOnAll";
	private static final String SET_TIME_OUT = "_setTimeOut";
	private static final String LOG = "_log";
	private static final String PING = "_ping";
	private static final String PONG = "_pong";
	
	// Response messages
	private static final String OK = "ok";
	private static final String ERROR = "error";
	private static final String AUTH_ERROR = "auth_error";
	private static final String AUTH_REQUIRED = "AUTH_REQUIRED";
	private static final String WAITING = "waiting";
	private static final String DOUBLE_LOGIN = "doublelogin";
	private static final String ALREADY_LOGIN = "alreadyLogin";

	public static final String ON = "on";
	public static final String OFF = "off";
	public static final String AVAILABLE = "1";
	public static final String UNAVAILABLE = "0";

	private Moss MOSS;
	
	private Socket socket = null;
	private InputStream in;
	private OutputStream out;

	protected String id = null;
	protected String room = "";
	protected String status = "";
	private boolean connected = false;
	private boolean available = true;
	private boolean waiting = false;
	private HashMap<String, Object> data = new HashMap<String, Object>();

	private boolean validConn = false;
	private boolean encodeMessages = false;
	private Matcher match;

	// Getters
	public String id(){ return id; }
	public String room(){ return room; }
	public String status(){ return status; }
	public boolean isWaiting(){ return waiting; }
	public boolean isAvailable(){ return available; }
	public boolean isConnected(){ return connected; }
	public HashMap<String, Object> data(){ return data; }
	
	public Object privateData;

	public User(Moss instance, Socket newSocket, boolean waiting_status)
	{
		MOSS = instance;
		socket = newSocket;
		waiting = waiting_status;
		encodeMessages = MOSS.charset_in != StandardCharsets.UTF_8;
		start();
	}
	
	public User(Moss instance, Socket newSocket)
	{
		this(instance, newSocket, false);
	}
	
	public User()
	{
		
	}
	
	public User(String id)
	{
		this.id = id;
	}
	
	public User(String id, String room, String status)
	{
		this.id = id;
		this.room = room;
		this.status = status;
	}
	
	public void start(Moss instance, Socket newSocket)
	{
		MOSS = instance;
		socket = newSocket;
		start();
	}
	
	public void start()
	{
		if(socket != null) {
			connected = true;
			new Inport().start();
		} else {
			Utils.error("Socket isn't defined!");
		}
	}
	
	public String toString()
	{
		String[] user = {id, room, status, (connected ? ON : OFF), (available ? AVAILABLE : UNAVAILABLE), Utils.JSONStringify(data)};
		return String.join(MSG_USER_DELIMITER, user);
	}
	
	public UserVO getVO()
	{
		return new UserVO(id, room);
	}
	
	public boolean invoke(String command)
	{
		return invoke(null, command, "", "");
	}
	
	public boolean invoke(User from, String command)
	{
		return invoke(from, command, "", "");
	}
	
	public boolean invoke(String command, String message)
	{
		return invoke(null, command, message, "");
	}
	
	public boolean invoke(String command, String message, String request)
	{
		return invoke(null, command, message, request);
	}
	
	public boolean invoke(User from, String command, String message)
	{
		return invoke(from, command, message, "");
	}
	
	public boolean invoke(User from, String command, String message, String request)
	{
		logMessage(command, message, ">");
		return sendMessage("#MOSS#<!" + command + "!>#<!" + (from != null ? from.toString() : "") + "!>#<!" + message + "!>#<!" + request + "!>#|");
	}
	
	private boolean sendMessage(String message)
	{
		boolean sent = false;
		try {
			if(connected) {
				out.write(message.getBytes(MOSS.charset_out));
				sent = true;
			}
		} catch (IOException e) {
			Utils.error(id + ", connection IOException");
			disconnect();
		}
		return sent;
	}
	
	// GETS & SETS
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }

	public Boolean getAvailability() { return available; }
	public void setAvailability(Boolean available) { this.available = available; }
	public void setAvailability(String available) { this.available = available.equals(AVAILABLE); }
	
	private class Inport extends Thread
	{
		public void run()
		{
			try {
				in = socket.getInputStream();
				out = socket.getOutputStream();
			}
			catch(IOException e) {
				Utils.log(e);
				return;
			}
			
			try {
				String result = "";
				
				int k = in.read();
				while( connected ) {
					result += (char) k;
					
					if(k == PIPE) {
						if(encodeMessages)
							result = new String(result.getBytes(MOSS.charset_in), StandardCharsets.UTF_8);
						
						validConn = true;
						if(result.charAt(0) == CARDINAL) // Easily pre-validates moss message
							processMessage(result);
						result = "";
					}
					
					// Flash privacy policy
					if(!validConn && result.equals("<policy-file-request/>")) {
						sendMessage("<?xml version=\"1.0\"?><cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"" + MOSS.server_port + "\" /></cross-domain-policy>\0");
					}

					if((k = in.read()) < 0)
						break;
				}
			} catch (SocketTimeoutException e) {
				if(id != null)
					Utils.error(id + ", connection reset SocketTimeoutException");
			} catch (SocketException e) {
				Utils.error(id + ", connection reset SocketException");
			} catch (Exception e) {
				Utils.log(id + ", connection reset Exception: " + e.toString(), e);
			} finally {
				disconnect();
			}
		}
	}
	
	private void logMessage(String command, String message, String separator) {
		if(Utils.log_level >= Utils.LOG_FULL) {
			match = patternPingPong.matcher(command + message);
			if (!match.find())
				Utils.log(this.id + separator + " " + command.replaceFirst("^_", "") + "(" + message.replaceAll(MSG_DELIMITER, ", ") + ")");
		}
	}
	
	public void processMessage(String msg)
	{
		// Protocol: #MOSS#<!(command)!>#<!(messages)!>#<!(request)!>#|
		// #MOSS#<!_connect				!>#<!(id)&!(room)&!(status)?&!(login)?&!(password)?		!>#<!(request)!>#|
		// #MOSS#<!_disconnect			!>#<!													!>#<!(request)!>#|
		// #MOSS#<!_updateStatus		!>#<!(status)											!>#<!(request)!>#|
		// #MOSS#<!_updateAvailability	!>#<!(available)										!>#<!(request)!>#|
		// #MOSS#<!_getUser				!>#<!(id)&!(room)										!>#<!(request)!>#|
		// #MOSS#<!_getRooms			!>#<!													!>#<!(request)!>#|
		// #MOSS#<!_updateRoom			!>#<!(room)												!>#<!(request)!>#|
		// #MOSS#<!_getUsers			!>#<!(room)&!(limit)?&!(page)?&!(available)?&!(search)?	!>#<!(request)!>#|
		// #MOSS#<!_getUsersCount		!>#<!(room)												!>#<!(request)!>#|
		// #MOSS#<!_invoke				!>#<!(id)&!(room)&!(command)&!(message)					!>#<!(request)!>#|
		// #MOSS#<!_invokeOnRoom		!>#<!(room)&!(command)&!(message)						!>#<!(request)!>#|
		// #MOSS#<!_invokeOnAll			!>#<!(command)&!(message)								!>#<!(request)!>#|
		// #MOSS#<!_setTimeOut			!>#<!(milliseconds)										!>#<!(request)!>#|
		// #MOSS#<!_randomPlayer		!>#<!(room)												!>#<!(request)!>#|
		// #MOSS#<!_setData				!>#<!(attribute)&!(data)								!>#<!(request)!>#|
		// #MOSS#<!_log					!>#<!(data)												!>#<!(request)!>#|
		
		msg = msg.replaceAll("\\|", "");
		match = patternMessage.matcher(msg);
		
		if (match.matches()) {
			String command = match.group(1) + "";
			String message = match.group(2) + "";
			String request = match.group(3) + "";
			String[] messages = null;
			if(!message.equals(""))
				messages = message.split(MSG_DELIMITER);
			
			if(!command.equals(CONNECT) && id == null)
				return;
			
			String result = "";
			boolean opStatus = false;
			boolean first = true;
			
			// Log
			logMessage(command, message, "<");
			
			// Reserved commands start with underscore
			if(command.charAt(0) == '_')
			{
				switch(command) {
					case CONNECT: 
						// Validate user login
						if((MOSS.validateLogin && messages.length >= 4) || (!MOSS.validateLogin && messages.length >= 2)) {
							this.id = MOSS.validateLogin ? 
								MOSS.validateLogin(messages[3], messages[4], messages[5]):
								messages[0];
							
							if(this.id != null) {
								this.room = messages[1];
								if (messages.length >= 3)
									this.status = messages[2];
								
								// Check double login
								boolean found = MOSS.server.checkDoubleLogin(this.id) != null;
								if(MOSS.autoLogoutOnDoubleLogin || !found)
								{
									MOSS.server.getRoom(this.room).add(this.id, this);
									if(!this.id.equals("0"))
										MOSS.userConnected(this);
								
									approveLogin(!waiting, request);
								} else {
									invoke(CONNECT, ALREADY_LOGIN, request);
								}
							} else {
								invoke(CONNECT, AUTH_ERROR, request);
							}
						} else {
							invoke(CONNECT, AUTH_REQUIRED, request);
						}
						break;
					case DISCONNECT: 
						invoke(DISCONNECT, OK, request);
						disconnect();
						break;
					case UPDATE_STATUS: 
						if (messages.length == 1) {
							status = messages[0];
							invoke(UPDATE_STATUS, messages[0], request);
							MOSS.userUpdatedStatus(this, status);
						}
						break;
					case UPDATE_AVAILABILITY: 
						if (messages.length == 1) {
							available = messages[0].equals(AVAILABLE);
							invoke(UPDATE_AVAILABILITY, messages[0], request);
							MOSS.userUpdatedAvailability(this, this.available);
						}
						break;
					case GET_ROOMS: 
						Collection<Room> rooms = MOSS.server.getRooms();
						if(rooms != null) {
							for (Room room : rooms) {
								result += (!first ? MSG_DELIMITER : "") + room.name;
								first = false;
							}
						}
						invoke(GET_ROOMS, result, request);
						break;
					case UPDATE_ROOM:
						if (messages.length == 1 && messages[0] != null) {
							MOSS.server.getRoom(this.room).remove(this.id);
							MOSS.server.getRoom(messages[0]).add(this.id, this);
							MOSS.userUpdatedRoom(this, messages[0]);
						}
						invoke(UPDATE_ROOM, OK, request);
						break;
					case GET_USER: 
						if (messages.length == 2) {
							UserVO uvo = new UserVO(messages[0], messages[1]);
							User user = MOSS.getUserByID(uvo);
							if(user != null)
								result = user.toString();
							else
								result = uvo.toString();
						}
						invoke(GET_USER, result, request);
						break;
					case GET_USERS: 
						List<User> users = null;
						
						String room = "";
						int limit = 20, page = 0, available = 0;
						HashMap<String, Object> search = null;
						
						if(messages.length > 0) room = messages[0];
						if(messages.length > 1) limit = Integer.parseInt(messages[1]);
						if(messages.length > 2) page = Integer.parseInt(messages[2]);
						if(messages.length > 3) available = Integer.parseInt(messages[3]);
						if(messages.length > 4) search = (HashMap<String, Object>) Utils.map(messages[4].split(","));
						
						users = MOSS.getUsers(room, limit, page, available, search);
						
						if(users != null) {
							for (User user : users) {
								result += (!first ? MSG_DELIMITER : "") + user.toString();
								first = false;
							}
						}
						invoke(GET_USERS, result, request);
						break;
					case GET_USERS_COUNT: 
						if (messages.length == 1) {
							int total = MOSS.getUsersCount(messages[0]);
							invoke(GET_USERS_COUNT, String.valueOf(total), request);
						}
						break;
					case SET_DATA: 
						if (messages.length == 2) {
							data.put(messages[0], messages[1]);
						} else if (messages.length == 1) {
							data.remove(messages[0]);
						}
						
						invoke(SET_DATA, OK, request);
						break;
					case RANDOM_PLAYER: 
						if (messages.length == 1) {
							User player = MOSS.pickRandomPlayer(id, messages[0]);
							invoke(RANDOM_PLAYER, (player != null ? player.toString() : "null"), request);
						}
						break;
					case INVOKE:
						String optionalMessage = (messages.length == 4 ? messages[3] : "");
						if (messages.length >= 3) 
							opStatus = MOSS.invoke(this, messages[0], messages[1], messages[2], optionalMessage);
						
						invoke(INVOKE, opStatus ? OK : ERROR, request);
						break;
					case INVOKE_ON_ROOM: 
						if (messages.length == 3) {
							MOSS.invokeOnRoom(this, messages[0], messages[1], messages[2]);
							opStatus = true;
						}
						invoke(INVOKE_ON_ROOM, opStatus ? OK : ERROR, request);
						break;
					case INVOKE_ON_ALL: 
						if (messages.length >= 1) {
							MOSS.invokeOnAll(this, messages[0], messages[1]);
							opStatus = true;
						}
						invoke(INVOKE_ON_ALL, opStatus ? OK : ERROR, request);
						break;
					case SET_TIME_OUT: 
						try {
							int timeout = Integer.valueOf(message);
							if(timeout >= 0 && timeout <= 600000) { // 10 minutes max
								socket.setSoTimeout(timeout);
								opStatus = true;
							}
						} catch (SocketException e) {
							e.printStackTrace();
						}
						invoke(SET_TIME_OUT, opStatus ? OK : ERROR, request);
						break;
					case LOG:
						MOSS.filelog.add(id(), message);
						invoke(LOG, OK, request);
						break;
					case PING: 
						invoke(PONG, OK, request);
						break;
					case PONG: 
					case "":
						break;
					default:
						invoke(command, ERROR, request);
						break;
				}
			}
			else
			{
				dispatchMessage(command, message, request);
			}
		}
	}
	
	protected void dispatchMessage(String command)
	{
		dispatchMessage(command, "", "");
	}
	
	protected void dispatchMessage(String command, String message)
	{
		dispatchMessage(command, message, "");
	}
	
	protected void dispatchMessage(String command, String message, String request)
	{
		UserNotification u = new UserNotification(UserNotification.MESSAGE, this, command, message, request);
		dispatchNotification(u);
		MOSS.userMessage(this, command, message, request);
	}
	
	private void approveLogin(boolean approved, String request)
	{
		if(id != null)
			invoke(CONNECT, approved ? OK : WAITING, request);
	}
	
	protected void approveLogin()
	{
		approveLogin(true, "");
	}

	private void dispatchNotification(UserNotification notification)
	{
		setChanged();
		notifyObservers(notification);
	}
	
	protected void doubleLogin()
	{
		invoke(CONNECT, DOUBLE_LOGIN);
		disconnect();
	}
	
	public void ping()
	{
		invoke(User.PING);
	}
	
	public void disconnect()
	{
		if(!connected)
			return;
		
		connected = false;
		dispatchNotification(new UserNotification(UserNotification.DISCONNECTED, this));
		
		Utils.log(this.id + ", " + socket + " has disconnected.");
		
		try
		{
			MOSS.server.removeUser(this);
			MOSS.server.getRoom(this.room).remove(this.id);
			if(this.id != null)
				MOSS.userDisconnected(this);
			
			in.close();
			out.close();
			socket.close();
			
			socket = null;
		}
		catch(IOException e)
		{
			Utils.log("Could not purge " + socket + ".", e);
		}
	}
}

