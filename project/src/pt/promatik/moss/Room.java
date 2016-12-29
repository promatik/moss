package pt.promatik.moss;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;

import pt.promatik.moss.utils.Utils;

public class Room
{
	public String name;
	public HashMap<String, User> users = new HashMap<String, User>();
	
	public Room(String name)
	{
		this.name = name;
	}
	
	public synchronized void invoke(User from, String command, String message)
	{
		try {
			Iterator<User> it = users.values().iterator();
			while (it.hasNext()) {
				User user = it.next();
				user.invoke(from, command, message);
			}
		} catch(ConcurrentModificationException e) {
			Utils.log("Concurrent modification on room users", e);
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
