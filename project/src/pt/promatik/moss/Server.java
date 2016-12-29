package pt.promatik.moss;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import pt.promatik.moss.utils.Utils;

public class Server extends Thread
{
	public Moss MOSS;
	
	private ServerSocket serverSocket;
	private InetAddress hostAddress;
	private Socket socket;
	private volatile Boolean running = false;
	private int maxErrorLogs = 100;

	private Vector<User> users = new Vector<User>();
	private Vector<User> waiting = new Vector<User>();
	private HashMap<String, Room> rooms = new HashMap<String, Room>();
	
	public Server(Moss instance, int port)
	{
		MOSS = instance;
		
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
		
		MOSS.serverStarted();
	}
	
	public Collection<User> getUsers()
	{
		return users;
	}
	
	public Collection<User> getWaitingUsers()
	{
		return waiting;
	}
	
	public Collection<Room> getRooms()
	{
		return rooms.values();
	}
	
	public synchronized Room getRoom(String id)
	{
		Room r = rooms.get(id);
		if(r == null) {
			r = new Room(id);
			rooms.put(id, r);
		}
		return r;
	}

	public synchronized void removeUser(User user)
	{
		users.remove(user);
		waiting.remove(user);
		
		if(MOSS.CONNECTIONS_MAX > 0 && waiting.size() > 0 && users.size() < MOSS.CONNECTIONS_MAX) {
			waiting.get(0).approveLogin();
			users.add(waiting.get(0));
			waiting.remove(0);
		}
	}
	
	public synchronized void pingUsers()
	{
		Iterator<User> it = users.iterator();
		while (it.hasNext()) {
			User user = it.next();
			user.invoke("ping");
		}
	}
	
	public synchronized void checkDoubleLogin(String id)
	{
		for (Room room : rooms.values()) {
			User user = room.users.get(id);
			if(user != null) {
				user.doubleLogin();
			}
		}
	}
	
	public void userLimitsUpdate()
	{
		while (waiting.size() > 0 && (MOSS.CONNECTIONS_MAX == 0 || MOSS.CONNECTIONS_MAX > users.size())) {
			User waitingUser = waiting.get(0);
			waiting.remove(0);
			waitingUser.approveLogin();
			users.add(waitingUser);
		}
	}
	
	public void run()
	{
		running = true;
		
		while(running)
		{
			try {
				if(MOSS.CONNECTIONS_MAX == 0 || users.size() < (MOSS.CONNECTIONS_MAX + MOSS.CONNECTIONS_WAITING)) {
					socket = serverSocket.accept();
					socket.setSoTimeout(MOSS.socketTimeout);
					
					boolean toWait = (MOSS.CONNECTIONS_MAX > 0 && users.size() >= MOSS.CONNECTIONS_MAX);
					User user = new User(MOSS, socket, toWait);
					
					if(toWait) waiting.add(user);
					else users.add(user);
					
					Utils.log("Client #" + users.size() + " - " + socket + " has connected.");
				} else {
					Thread.sleep(1000);
				}
			} catch(IOException e) {
				maxErrorLogs--;
				if(maxErrorLogs > 0)
					Utils.log("Could not get a client.", e);
			} catch (InterruptedException e) {
				
			}
		}
		
		MOSS.serverStopped();
	}
	
	public void quit()
	{
		running = false;
	}
}

