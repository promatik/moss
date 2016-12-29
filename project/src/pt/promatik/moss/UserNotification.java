package pt.promatik.moss;

public class UserNotification
{
	public static final String MESSAGE = "MESSAGE";
	public static final String DISCONNECTED = "DISCONNECTED";

	public String type;
	public User user;
	public String command;
	public String message;
	public String request;

	public Boolean isMessage() { return type.equals(MESSAGE); }
	public Boolean isDisconnected() { return type.equals(DISCONNECTED); }

	public UserNotification(String type, User user)
	{
		this(type, user, "", "", "");
	}
	
	public UserNotification(String type, User user, String command, String message, String request)
	{
		this.type = type;
		this.user = user;
		this.command = command;
		this.message = message;
		this.request = request;
	}
}
