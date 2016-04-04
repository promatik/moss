package pt.promatik.moss;

import java.util.HashMap;

public class Room {
	public String name;
	public HashMap<String, User> users = new HashMap<String, User>();
	
	public Room(String name)
	{
		this.name = name;
	}
	
	public synchronized void invoke(User from, String command, String message)
	{
		for (User user : users.values()) {
			user.invoke(from, command, message);
		}
	}
	
	public void add(String id, User user)
	{
		users.put(id, user);
	}
	
	public void remove(String id)
	{
		users.remove(id);
	}
	
	public void remove(User user)
	{
		users.remove(user.id());
	}
	
	public int count()
	{
		return users.size();
	}
	
}
