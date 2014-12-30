package pt.promatik.moss;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class User
{
	private Socket socket;
	private String inputLine;
	public boolean isConnected;
	
	public User(Socket newSocket)
	{
		socket = newSocket;
		isConnected = true;
		new Inport().start();
	}
	
	private class Inport extends Thread
	{
		private BufferedReader in;
		public void run()
		{
			try { in = new BufferedReader(new InputStreamReader(socket.getInputStream())); }
			catch(IOException e) { return; }
			
			System.out.println(socket+" has connected input.");
			
			try {
				while((inputLine = in.readLine()) != null)
					System.out.println(inputLine);
				Thread.sleep( Main.USER_THROTTLE );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void disconnect()
	{
		try
		{
			isConnected = false;
			socket.close();
		}
		catch(IOException e)
		{
			System.out.println("Could not purge "+socket+".");
		}
	}
}

