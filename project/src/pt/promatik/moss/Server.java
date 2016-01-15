package pt.promatik.moss;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import pt.promatik.moss.utils.Utils;

public class Server extends Thread
{
	private ServerSocket serverSocket;
	private InetAddress hostAddress;
	private Socket socket;
	private volatile Boolean running = false;
	
	private Vector<User> users = new Vector<User>();
	private HashMap<String, Room> rooms = new HashMap<String, Room>();
	
	public Server(int port)
	{
		try
		{
			hostAddress = InetAddress.getLocalHost();
			Utils.log("Server host address is: " + hostAddress + ", port: " + String.valueOf(port));
		}
		catch(UnknownHostException e)
		{
			Utils.log("Could not get the host address.", e);
			return;
		}
		
		try
		{
			serverSocket = new ServerSocket(port, 0, hostAddress);
		}
		catch(IOException e)
		{
			Utils.log("Could not open server socket.", e);
			return;
		}
		
		Moss.instance.serverStarted();
	}
	
	public Collection<Room> getRooms() {
		return rooms.values();
	}
	
	public synchronized Room getRoom(String id) {
		Room r = rooms.get(id);
		if(r == null) {
			r = new Room(id);
			rooms.put(id, r);
		}
		return r;
	}

	public synchronized void removeUser(User user) {
		users.remove(user);
	}
	
	public synchronized void pingUsers() {
		for (User user : users) {
			user.invoke("ping");
		}
	}
	
	public synchronized void checkDoubleLogin(String id) {
		for (Room room : rooms.values()) {
			User user = room.users.get(id);
			if(user != null) {
				user.doubleLogin();
			}
		}
	}
	
	public void run()
	{
		running = true;
		
		while(running)
		{
			try {
				socket = serverSocket.accept();
				socket.setSoTimeout(Moss.instance.socketTimeout);
				
				users.add(new User(socket));
				Utils.log("Client " + socket + " has connected.");
			} catch(IOException e) {
				Utils.log("Could not get a client.", e);
			}
		}
		
		Moss.instance.serverStopped();
	}
	
	public void quit() {
		running = false;
	}
}

