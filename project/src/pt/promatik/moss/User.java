package pt.promatik.moss;

import java.net.Socket;
import java.net.SocketException;
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
	public Moss MOSS;
	public static final String MSG_USER_DELIMITER = "&;";
	
	private Socket socket = null;
	private BufferedReader in;
	private BufferedWriter out;

	private boolean connected = false;
	private String id = null;
	private String room = "";
	private String status = "";
	private boolean available = true;
	private HashMap<String, Object> data = new HashMap<String, Object>();
	
	private boolean protocolConn = false;
	private boolean validConn = false;
	private Matcher match;

	// Getters
	public String id(){ return id; }
	public String room(){ return room; }
	public String status(){ return status; }
	public boolean isAvailable(){ return available; }
	public boolean isConnected(){ return connected; }
	public HashMap<String, Object> data(){ return data; }
	
	public User(Moss instance, Socket newSocket)
	{
		MOSS = instance;
		socket = newSocket;
		start();
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
	
	public String toString(){
		String[] user = {id, room, status, (connected ? "on" : "off"), (available ? "1" : "0"), Utils.JSONStringify(data)};
		return String.join(MSG_USER_DELIMITER, user);
	}
	
	public UserVO getVO(){
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
	
	public boolean invoke(String command, String request)
	{
		return invoke(null, command, "", request);
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
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
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
						protocolConn = true;
						out.write("<?xml version=\"1.0\"?><cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"" + MOSS.server_port + "\" /></cross-domain-policy>\0");
						out.flush();
					}
				}
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
		// #MOSS#<!getUsers		!>#<!(room)&!(limit)?&!(page)?			!>#<!request!>#|
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
				messages = message.split(Moss.MSG_DELIMITER);
			
			if(!command.equals("connect") && id == null)
				return;
			
			String result = "";
			boolean opStatus = false;
			switch(command){
				case "connect": 
					if (messages.length >= 2) {
						this.id = messages[0];
						this.room = messages[1];
						if (messages.length == 3)
							status = messages[2];
						
						MOSS.srv.checkDoubleLogin(this.id);
						MOSS.srv.getRoom(this.room).add(this.id, this);
						if(!this.id.equals("0"))
							MOSS.userConnected(this);
						invoke("connected", request);
					}
					break;
				case "disconnect": 
					invoke("disconnected", request);
					disconnect();
					break;
				case "updateStatus": 
					if (messages.length == 1) {
						status = messages[0];
						invoke("statusUpdated", messages[0], request);
						MOSS.userUpdatedStatus(this, status);
					}
					break;
				case "updateAvailability": 
					if (messages.length == 1) {
						available = messages[0].equals("1");
						invoke("availabilityUpdated", messages[0], request);
						MOSS.userUpdatedAvailability(this, this.available);
					}
					break;
				case "getUser": 
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
				case "getUsers": 
					List<User> users = null;
					if (messages.length == 1) {
						users = MOSS.getUsers(messages[0]);
					}
					else if (messages.length == 3) {
						users = MOSS.getUsers(messages[0], Integer.parseInt(messages[1]), Integer.parseInt(messages[2]));
					}
					
					boolean first = true;
					if(users != null) {
						for (User user : users) {
							result += (!first ? Moss.MSG_DELIMITER : "") + user.toString();
							first = false;
						}
					}
					invoke("users", result, request);
					break;
				case "getUsersCount": 
					if (messages.length == 1) {
						int total = MOSS.getUsersCount(messages[0]);
						invoke("usersCount", String.valueOf(total), request);
					}
					break;
				case "setData": 
					if (messages.length == 2) {
						data.put(messages[0], messages[1]);
					} else if (messages.length == 1) {
						data.remove(messages[0]);
					}
					
					invoke("setData", "ok", request);
					break;
				case "randomPlayer": 
					if (messages.length == 1) {
						User player = MOSS.pickRandomPlayer(id, messages[0]);
						invoke("randomPlayer", (player != null ? player.toString() : "null"), request);
					}
					break;
				case "invoke":
					String optionalMessage = (messages.length == 4 ? messages[3] : "");
					if (messages.length >= 3) 
						opStatus = MOSS.invoke(this, messages[0], messages[1], messages[2], optionalMessage);
					
					invoke("invoke", opStatus ? "ok" : "error", request);
					break;
				case "invokeOnRoom": 
					if (messages.length == 3) {
						MOSS.invokeOnRoom(this, messages[0], messages[1], messages[2]);
						opStatus = true;
					}
					invoke("invokeOnRoom", opStatus ? "ok" : "error", request);
					break;
				case "invokeOnAll": 
					if (messages.length >= 1) {
						MOSS.invokeOnAll(this, messages[0], messages[1]);
						opStatus = true;
					}
					invoke("invokeOnAll", opStatus ? "ok" : "error", request);
					break;
				case "setTimeOut": 
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
				case "log":
					MOSS.filelog.add(id(), message);
				case "ping": 
					invoke("pong", "ok", request);
					break;
				case "pong": 
				case "": 
					break;
				default: 
					notifyObservers(new UserNotification(UserNotification.MESSAGE, this, new Object[] {command, message, request}));
					MOSS.userMessage(this, command, message, request);
					break;
			}
			
			match = Utils.patternPingPong.matcher(command + message);
			if (!match.find() || MOSS.log >= Utils.LOG_FULL)
				Utils.log(this.id + ", " + command + ", " + message);
		}
	}
	
	protected void doubleLogin()
	{
		invoke("doublelogin");
		disconnect();
	}
	
	public void disconnect()
	{
		setChanged();
		notifyObservers(new UserNotification(UserNotification.DISCONNECTED, this, null));
		
		if(!connected)
			return;
		
		if(!protocolConn)
			Utils.log(this.id + ", " + socket + " has disconnected.");
		
		try
		{
			MOSS.srv.getRoom(this.room).remove(this);
			MOSS.srv.removeUser(this);
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

