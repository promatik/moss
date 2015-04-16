package pt.promatik.moss;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.ArrayList;

public class Server extends Thread
{
	private ServerSocket serverSocket;
	private InetAddress hostAddress;
	private Socket socket;
	private volatile Boolean running = false;
	private ArrayList<User> users = new ArrayList<User>();
	
	public Server(int port)
	{
		try
		{
			hostAddress = InetAddress.getLocalHost();
			System.out.println("Server host address is: "+hostAddress);
		}
		catch(UnknownHostException e)
		{
			System.out.println("Could not get the host address.");
			return;
		}
		
		try
		{
			serverSocket = new ServerSocket(port, 0, hostAddress);
		}
		catch(IOException e)
		{
			System.out.println("Could not open server socket.");
			return;
		}
		
		System.out.println("Socket "+serverSocket+" created.");
	}
	
	public void run()
	{
		System.out.println("Room has been started.");
		running = true;
		
		while(running)
		{
			for(int i = 0;i < users.size();i++) {
				if(!users.get(i).isConnected) {
					System.out.println(users.get(i)+" removed due to lack of connection.");
					users.remove(i);
				}
			}
			
			try {
				socket = serverSocket.accept();
				System.out.println("Client "+socket+" has connected.");
				users.add(new User(socket));
			} catch(IOException e) {
				System.out.println("Could not get a client.");
			}
			
			try {
				Thread.sleep(Main.ROOM_THROTTLE);
			} catch(InterruptedException e) {
				running = false;
				System.out.println("Room has been interrupted.");
			}
		}
	}
	
	public void quit() {
		running = false;
	}
}

