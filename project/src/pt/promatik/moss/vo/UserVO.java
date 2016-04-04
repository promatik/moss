package pt.promatik.moss.vo;

import pt.promatik.moss.Moss;

public class UserVO {
	public String id;
	public String room;
	
	public UserVO(String id, String room) {
		this.id = id;
		this.room = room;
	}
	
	public String toString(){
		String[] user = {id, room, "", "off", "0", "{}"};
		return String.join(Moss.MSG_DELIMITER, user);
	}
}
