package pt.promatik.moss;

public class UserNotification {
	public static final String MESSAGE = "MESSAGE";
	public static final String DISCONNECTED = "DISCONNECTED";

	public String command;
	public User user;
	public Object[] args;

	public Boolean isMessage() { return command.equals(MESSAGE); }
	public Boolean isDisconnected() { return command.equals(DISCONNECTED); }
	
	public UserNotification(String command, User user, Object[] args) {
		this.command = command;
		this.user = user;
		this.args = args;
	}
}
