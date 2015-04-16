package pt.promatik.moss;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class User
{
	private Socket socket;
	private String inputLine;
	private BufferedReader in;
	private BufferedWriter out;
	
	public boolean isConnected;
	
	public User(Socket newSocket)
	{
		socket = newSocket;
		isConnected = true;
		new Inport().start();
	}
	
	public void send(String message)
	{
		try {
			out.write(message);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			
			System.out.println(socket+" has connected input.");
			
			try {
				while((inputLine = in.readLine()) != null) {
					System.out.println(inputLine);
					send(inputLine);
				}
				Thread.sleep( Main.USER_THROTTLE );
			} catch (Exception e) {
				isConnected = false;
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

