package pt.promatik.moss;

import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import pt.promatik.moss.socket.net.WebServerSocket;
import pt.promatik.moss.utils.Utils;

public class Server extends Thread
{
	public Moss MOSS;
	
	private WebServerSocket serverSocket;
	private volatile Boolean running = false;
	private int maxErrorLogs = 100;

	private Vector<User> users = new Vector<User>();
	private Vector<User> waiting = new Vector<User>();
	private HashMap<String, Room> rooms = new HashMap<String, Room>();

	public Server(Moss instance, int port)
	{
		this(instance, null, port);
	}
	
	public Server(Moss instance, String ip, int port)
	{
		MOSS = instance;
		
		try {
			InetAddress hostAddress = (ip == null || ip.trim().isEmpty()) ? InetAddress.getLocalHost() : InetAddress.getByName(ip);
			Utils.log("Server host address is: " + hostAddress + ", port: " + String.valueOf(port));
			
			serverSocket = new WebServerSocket(port, hostAddress);
			MOSS.serverStarted();
		}
		catch(UnknownHostException e) {
			Utils.log("Could not get the host address.", e);
		}
		catch(IOException e) {
			Utils.log("Could not open server socket.", e);
		}
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
		
		if(MOSS.connections_max > 0 && waiting.size() > 0 && users.size() < MOSS.connections_max) {
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
			user.invoke(User.PING);
		}
	}
	
	public synchronized User checkDoubleLogin(String id)
	{
		for (Room room : rooms.values()) {
			User user = room.users.get(id);
			if(user != null) {
				if(MOSS.autoLogoutOnDoubleLogin)
					user.doubleLogin();
				return user;
			}
		}
		return null;
	}
	
	public void userLimitsUpdate()
	{
		while (waiting.size() > 0 && (MOSS.connections_max == 0 || MOSS.connections_max > users.size())) {
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
				if(MOSS.connections_max == 0 || users.size() < (MOSS.connections_max + MOSS.connections_waiting)) {
					Socket socket = serverSocket.accept();
					socket.setSoTimeout(MOSS.socketTimeout);
					
					boolean toWait = (MOSS.connections_max > 0 && users.size() >= MOSS.connections_max);
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

