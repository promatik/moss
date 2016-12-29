package pt.promatik.moss;

import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import java.util.Observable;

import pt.promatik.moss.utils.Utils;
import pt.promatik.moss.vo.UserVO;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class User extends Observable
{
	public static final String MSG_DELIMITER = "&!";
	public static final String MSG_USER_DELIMITER = "&;";

	public static final int GET_USERS_FILTER_ALL = 0;
	public static final int GET_USERS_FILTER_ONLINE = 1;
	public static final int GET_USERS_FILTER_OFFLINE = 2;

	private static final String CONNECT = "connect";
	private static final String DISCONNECT = "disconnect";
	private static final String UPDATE_STATUS = "updateStatus";
	private static final String UPDATE_AVAILABILITY = "updateAvailability";
	private static final String GET_USER = "getUser";
	private static final String GET_USERS = "getUsers";
	private static final String GET_USERS_COUNT = "getUsersCount";
	private static final String SET_DATA = "setData";
	private static final String RANDOM_PLAYER = "randomPlayer";
	private static final String INVOKE = "invoke";
	private static final String INVOKE_ON_ROOM = "invokeOnRoom";
	private static final String INVOKE_ON_ALL = "invokeOnAll";
	private static final String SET_TIME_OUT = "setTimeOut";
	private static final String LOG = "log";
	private static final String PING = "ping";
	private static final String PONG = "pong";

	public static final String ON = "on";
	public static final String OFF = "off";
	public static final String AVAILABLE = "1";
	public static final String UNAVAILABLE = "0";

	private Moss MOSS;
	
	private Socket socket = null;
	private BufferedReader in;
	private BufferedWriter out;

	private boolean connected = false;
	private String id = null;
	private String room = "";
	private String status = "";
	private boolean available = true;
	private boolean waiting = false;
	private HashMap<String, Object> data = new HashMap<String, Object>();
	
	private boolean validConn = false;
	private Matcher match;

	// Getters
	public String id(){ return id; }
	public String room(){ return room; }
	public String status(){ return status; }
	public boolean isWaiting(){ return waiting; }
	public boolean isAvailable(){ return available; }
	public boolean isConnected(){ return connected; }
	public HashMap<String, Object> data(){ return data; }

	public User(Moss instance, Socket newSocket, boolean waiting_status)
	{
		MOSS = instance;
		socket = newSocket;
		waiting = waiting_status;
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
	
	public User(String id, String room)
	{
		this.id = id;
		this.room = room;
	}
	
	public void start(Moss instance, Socket newSocket)
	{
		socket = newSocket;
		start();
	}
	
	public void start()
	{
		if(socket != null) {
			connected = true;
			new Inport().start();
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
		boolean sent = false;
		try {
			if(connected) {
				out.write("#MOSS#<!" + command + "!>#<!" + (from != null ? from.toString() : "") + "!>#<!" + message + "!>#<!" + request + "!>#|");
				out.flush();
				sent = true;
			}
		} catch (IOException e) {
			Utils.log("Connection io exception: " + e.toString(), e);
			disconnect();
		}
		return sent;
	}
	
	// GETS & SETS
	public String getStatus() { return status; }
	
	public String getAvailability() { return status; }
	public void setAvailability(String status) { this.status = status; }
	
	
	private class Inport extends Thread
	{
		public void run()
		{
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName(MOSS.CHARSET_IN)));
				out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName(MOSS.CHARSET_OUT)));
			}
			catch(IOException e) {
				Utils.log(e);
				return;
			}
			
			try {
				String result = "";
				
				char[] buff = new char[1];
				int k = -1;
				while( connected && (k = in.read(buff, 0, 1)) > -1 ) {
					result += new String(buff, 0, k);
					
					if(result.contains("|")) {
						validConn = true;
						processMessage(result);
						result = "";
					}
					
					// Flash privacy policy
					if(!validConn && result.equals("<policy-file-request/>")) {
						out.write("<?xml version=\"1.0\"?><cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"" + MOSS.server_port + "\" /></cross-domain-policy>\0");
						out.flush();
					}
				}
			} catch (SocketTimeoutException e) {
				Utils.log("Connection reset exception: " + e.toString());
			} catch (SocketException e) {
				Utils.log("Connection reset exception: " + e.toString(), e);
			} catch (Exception e) {
				Utils.log("Connection reset exception: " + e.toString(), e);
			} finally {
				disconnect();
			}
		}
	}
	
	public void processMessage(String msg)
	{
		// Protocol: #MOSS#(command)#(message)#(request)#|
		// #MOSS#<!connect		!>#<!(id)&!(room)&!(status)?			!>#<!request!>#|
		// #MOSS#<!disconnect	!>#<!									!>#<!request!>#|
		// #MOSS#<!updateStatus	!>#<!(status)							!>#<!request!>#|
		// #MOSS#<!getUser		!>#<!(id)&!(room)						!>#<!request!>#|
		// #MOSS#<!getUsers		!>#<!(room)&!(limit)?&!(page)?&!(available)?&!(search)?!>#<!request!>#|
		// #MOSS#<!getUsersCount!>#<!(room)								!>#<!request!>#|
		// #MOSS#<!invoke		!>#<!(id)&!(room)&!(command)&!(message)	!>#<!request!>#|
		// #MOSS#<!invokeOnRoom	!>#<!(room)&!(command)&!(message)		!>#<!request!>#|
		// #MOSS#<!invokeOnAll	!>#<!(command)&!(message)				!>#<!request!>#|
		// #MOSS#<!setTimeOut	!>#<!(milliseconds)						!>#<!request!>#|
		// #MOSS#<!randomPlayer	!>#<!(room)								!>#<!request!>#|
		// #MOSS#<!setData		!>#<!(attribute)&!(data)				!>#<!request!>#|
		// #MOSS#<!log			!>#<!(data)								!>#<!request!>#|
		
		msg = msg.replaceAll("\\|", "");
		match = Utils.patternMessage.matcher(msg);
		
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
			switch(command) {
				case CONNECT: 
					if (messages.length >= 2) {
						this.id = messages[0];
						this.room = messages[1];
						if (messages.length == 3)
							status = messages[2];
						
						MOSS.server.checkDoubleLogin(this.id);
						MOSS.server.getRoom(this.room).add(this.id, this);
						if(!this.id.equals("0"))
							MOSS.userConnected(this);
						
						approveLogin(!waiting, request);
					}
					break;
				case DISCONNECT: 
					invoke("disconnected", "", request);
					disconnect();
					break;
				case UPDATE_STATUS: 
					if (messages.length == 1) {
						status = messages[0];
						invoke("statusUpdated", messages[0], request);
						MOSS.userUpdatedStatus(this, status);
					}
					break;
				case UPDATE_AVAILABILITY: 
					if (messages.length == 1) {
						available = messages[0].equals(AVAILABLE);
						invoke("availabilityUpdated", messages[0], request);
						MOSS.userUpdatedAvailability(this, this.available);
					}
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
					invoke("user", result, request);
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
						boolean first = true;
						for (User user : users) {
							result += (!first ? MSG_DELIMITER : "") + user.toString();
							first = false;
						}
					}
					invoke("users", result, request);
					break;
				case GET_USERS_COUNT: 
					if (messages.length == 1) {
						int total = MOSS.getUsersCount(messages[0]);
						invoke("usersCount", String.valueOf(total), request);
					}
					break;
				case SET_DATA: 
					if (messages.length == 2) {
						data.put(messages[0], messages[1]);
					} else if (messages.length == 1) {
						data.remove(messages[0]);
					}
					
					invoke("setData", "ok", request);
					break;
				case RANDOM_PLAYER: 
					if (messages.length == 1) {
						User player = MOSS.pickRandomPlayer(id, messages[0]);
						invoke("randomPlayer", (player != null ? player.toString() : "null"), request);
					}
					break;
				case INVOKE:
					String optionalMessage = (messages.length == 4 ? messages[3] : "");
					if (messages.length >= 3) 
						opStatus = MOSS.invoke(this, messages[0], messages[1], messages[2], optionalMessage);
					
					invoke("invoke", opStatus ? "ok" : "error", request);
					break;
				case INVOKE_ON_ROOM: 
					if (messages.length == 3) {
						MOSS.invokeOnRoom(this, messages[0], messages[1], messages[2]);
						opStatus = true;
					}
					invoke("invokeOnRoom", opStatus ? "ok" : "error", request);
					break;
				case INVOKE_ON_ALL: 
					if (messages.length >= 1) {
						MOSS.invokeOnAll(this, messages[0], messages[1]);
						opStatus = true;
					}
					invoke("invokeOnAll", opStatus ? "ok" : "error", request);
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
					invoke("setTimeOut", opStatus ? "ok" : "error", request);
					break;
				case LOG:
					MOSS.filelog.add(id(), message);
					invoke("log", "ok", request);
					break;
				case PING: 
					invoke("pong", "ok", request);
					break;
				case PONG: 
				case "": 
					break;
				default: 
					UserNotification u = new UserNotification(UserNotification.MESSAGE, this, command, message, request);
					dispatchNotification(u);
					MOSS.userMessage(this, command, message, request);
					break;
			}
			
			match = Utils.patternPingPong.matcher(command + message);
			if (!match.find() && Utils.log_level >= Utils.LOG_FULL)
				Utils.log(this.id + ", " + command + ", " + message);
		}
	}
	
	private void approveLogin(boolean approved, String request)
	{
		if(id != null)
			invoke(approved ? "connected" : "waiting", request);
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
		invoke("doublelogin");
		disconnect();
	}
	
	public void disconnect()
	{
		if(!connected)
			return;

		dispatchNotification(new UserNotification(UserNotification.DISCONNECTED, this));
		
		Utils.log(this.id + ", " + socket + " has disconnected.");
		
		try
		{
			MOSS.server.removeUser(this);
			MOSS.server.getRoom(this.room).remove(this.id);
			if(this.id != null)
				MOSS.userDisconnected(this);
			
			connected = false;
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

