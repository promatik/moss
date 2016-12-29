package pt.promatik.moss.vo;

import pt.promatik.moss.User;

public class UserVO
{
	public String id;
	public String room;
	
	public UserVO(String id, String room)
	{
		this.id = id;
		this.room = room;
	}
	
	public String toString()
	{
		String[] user = {id, room, "", User.OFF, User.UNAVAILABLE, "{}"};
		return String.join(User.MSG_USER_DELIMITER, user);
	}
}
