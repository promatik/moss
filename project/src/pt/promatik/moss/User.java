package pt.promatik.moss;

import java.net.Socket;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.promatik.moss.vo.UserVO;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class User
{
	private Socket socket;
	private BufferedReader in;
	private BufferedWriter out;

	public boolean isConnected;
	public String id = null;
	public String room = "";
	private String status = "";
	private boolean protocolConn = false;
	private boolean validConn = false;
	
	public User(Socket newSocket)
	{
		socket = newSocket;
		isConnected = true;
		new Inport().start();
	}
	
	public String toString(){
		return id + "," + room + "," + status;
	}
	
	public void invoke(String command)
	{
		invoke(null, command, "", "");
	}
	
	public void invoke(User from, String command)
	{
		invoke(from, command, "", "");
	}
	
	public void invoke(String command, String request)
	{
		invoke(null, command, "", request);
	}
	
	public void invoke(String command, String message, String request)
	{
		invoke(null, command, message, request);
	}
	
	public void invoke(User from, String command, String message)
	{
		invoke(from, command, message, "");
	}
	
	public void invoke(User from, String command, String message, String request)
	{
		try {
			out.write("#MOSS#<!" + command + "!>#<!" + (from != null ? from.toString() : "") + "!>#<!" + message + "!>#<!" + request + "!>#|");
			out.flush();
		} catch (IOException e) {
			disconnect();
		}
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	private class Inport extends Thread
	{
		public void run()
		{
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			}
			catch(IOException e) { return; }
			
			try {
				String result = "";
				
				char[] buff = new char[1];
				int k = -1;
				while( (k = in.read(buff, 0, 1)) > -1 ) {
					result += new String(buff, 0, k);
					
					if(result.contains("|")) {
						validConn = true;
						processMessage(result);
					    result = "";
					}
					
					// Flash privacy policy
					if(!validConn && result.equals("<policy-file-request/>")) {
						protocolConn = true;
						out.write("<?xml version=\"1.0\"?><cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"" + Moss.SERVER_PORT + "\" /></cross-domain-policy>\0");
						out.flush();
					}
				}
				
				Thread.sleep( Moss.USER_THROTTLE );
			} catch (Exception e) {
				Utils.log("Connection reset exception: " + e.toString());
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
		
		msg = msg.replaceAll("\\|", "");
		Pattern p = Pattern.compile("^#MOSS#<!(.+)!>#<!(.+)?!>#<!(.+)?!>#$");
		Matcher m = p.matcher(msg);
		
		if (m.matches()) {
			String command = m.group(1);
			String message = m.group(2);
			String request = m.group(3);
			String[] messages = null;
			if(!message.equals(""))
				messages = message.split(Moss.MSG_DELIMITER);
			
			if(!command.equals("connect") && id == null)
				return;
			
			Utils.log(this.id + ", " + command + ", " + message);
			
			String result = "";
			boolean status = false;
			switch(command){
				case "connect": 
					if (messages.length >= 2) {
						this.id = messages[0];
						this.room = messages[1];
						if (messages.length == 3)
							this.status = messages[2];
						
						Moss.instance.srv.checkDoubleLogin(this.id);
						Moss.instance.srv.getRoom(this.room).add(this.id, this);
						Moss.instance.userConnected(this);
						invoke("connected", request);
					}
					break;
				case "disconnect": 
					disconnect();
					break;
				case "updateStatus": 
					if (messages.length == 1) {
						setStatus(messages[0]);
						invoke("statusUpdated", messages[0], request);
						Moss.instance.userUpdatedStatus(this, this.status);
					}
					break;
				case "getUser": 
					if (messages.length == 2) {
						User user = Moss.instance.getUserByID(new UserVO(messages[0], messages[1]));
						if(user != null)
							result = user.id + "," + user.room + "," + user.status + "," + "on";
						else
							result = messages[0] + "," + messages[1] + ",," + "off";
					}
					invoke("user", result, request);
					break;
				case "getUsers": 
					List<User> users = null;
					if (messages.length == 1) {
						users = Moss.instance.getUsers(messages[0]);
					}
					else if (messages.length == 3) {
						users = Moss.instance.getUsers(messages[0], Integer.parseInt(messages[1]), Integer.parseInt(messages[2]));
					}
					
					boolean first = true;
					if(users != null) {
						for (User user : users) {
							result += (!first ? Moss.MSG_DELIMITER : "") + user.id + "," + user.room + "," + user.status;
							first = false;
						}
					}
					invoke("users", result, request);
					break;
				case "getUsersCount": 
					if (messages.length == 1) {
						int total = Moss.instance.getUsersCount(messages[0]);
						invoke("usersCount", String.valueOf(total), request);
					}
					break;
				case "invoke":
					String optionalMessage = (messages.length == 4 ? messages[3] : "");
					if (messages.length >= 3) 
						status = Moss.instance.invoke(this, messages[0], messages[1], messages[2], optionalMessage);
					
					invoke("invoke", status ? "ok" : "error", request);
					break;
				case "invokeOnRoom": 
					if (messages.length == 3) {
						Moss.instance.invokeOnRoom(this, messages[0], messages[1], messages[2]);
						status = true;
					}
					invoke("invokeOnRoom", status ? "ok" : "error", request);
					break;
				case "invokeOnAll": 
					if (messages.length == 2) {
						Moss.instance.invokeOnAll(this, messages[0], messages[1]);
						status = true;
					}
					invoke("invokeOnAll", status ? "ok" : "error", request);
					break;
				default: 
					Moss.instance.userMessage(this, command, message);
					break;
			}
		}
	}
	
	public void disconnect()
	{
		if(!isConnected)
			return;
		
		if(!protocolConn)
			Utils.log(this.id + ", " + socket + " has disconnected.");
		
		try
		{
			Moss.instance.srv.getRoom(this.room).remove(this);
			Moss.instance.srv.removeUser(this);
			if(this.id != null)
				Moss.instance.userDisconnected(this);
			
			isConnected = false;
			socket.close();
			
			socket = null;
		}
		catch(IOException e)
		{
			Utils.log("Could not purge "+socket+".");
		}
	}
}

