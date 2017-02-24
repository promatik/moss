package pt.promatik.moss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.promatik.moss.utils.Utils;

public class Console extends Thread
{
	private Moss MOSS;
	private List<String> commands;
	
	private final static String HELP = "help";
	private final static String LOG = "log";
	private final static String MAX_PLAYERS = "players_max";
	private final static String MAX_WAITING = "players_waiting";
	private final static String USER_COUNT = "users";
	
	public Console(Moss instance)
	{
		MOSS = instance;
		commands = new ArrayList<String>(Arrays.asList(LOG, MAX_PLAYERS, MAX_WAITING, USER_COUNT));
	}
	
	public void registCommand(String command)
	{
		commands.add(command);
	}

	public <E extends Enum<?>> void registCommands(Class<E> c)
	{
		for (E o: c.getEnumConstants())
			commands.add(o.name());
	}
	
	public List<String> getCommandList()
	{
		return commands;
	}
	
	public void run()
	{
		String input;
		while(true)
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				input = br.readLine();
				if(input != null && input.length() > 0) {
					switch (input) {
						case HELP:
							Utils.log("Command list:", true);
							Utils.log(String.join(", ", commands), true);
							break;
						case LOG:
							Utils.log("Log levels: \n0 None\n1 Default\n2 Errors\n3 Full", true);
							input = br.readLine();
							try {
								int result = Integer.parseInt(input);
								if(result < 0 || result > 3)
									throw new NumberFormatException();
								Utils.log_level = result;
							} catch (NumberFormatException e) {
								Utils.error("Not a valid number.");
							} finally {
								Utils.log("LOG = " + String.valueOf(Utils.log_level), true);
							}
							break;
						case MAX_PLAYERS:
							Utils.log("Input an integer value greather or equal to zero", true);
							input = br.readLine();
							try {
								int result = Integer.parseInt(input);
								if(result < 0)
									throw new NumberFormatException();
								MOSS.connections_max = result;
								MOSS.server.userLimitsUpdate();
							} catch (NumberFormatException e) {
								Utils.error("Not a valid number.");
							} finally {
								Utils.log("CONNECTIONS_MAX = " + String.valueOf(MOSS.connections_max), true);
							}
							break;
						case MAX_WAITING:
							Utils.log("Input an integer value greather or equal to zero", true);
							input = br.readLine();
							try {
								int result = Integer.parseInt(input);
								if(result < 0)
									throw new NumberFormatException();
								MOSS.connections_waiting = result;
								MOSS.server.userLimitsUpdate();
							} catch (NumberFormatException e) {
								Utils.error("Not a valid number.");
							} finally {
								Utils.log("CONNECTIONS_WAITING = " + String.valueOf(MOSS.connections_waiting), true);
							}
							break;
						case USER_COUNT:
							int total = MOSS.server.getUsers().size();
							int totalWaiting = MOSS.server.getWaitingUsers().size();
							Utils.log(total + " online users", true);
							if(totalWaiting > 0)
								Utils.log(totalWaiting + " waiting to play users", true);
							
							break;
						default:
							if(commands.contains(input)) {
								Utils.log("Input the value for '" + input + "':", true);
								String value = br.readLine();
								MOSS.commandInput(input, value);
							} else {
								Utils.error("The command '" + input + "' was not found.\nType 'help' to get the command list.\nWhile developing, use 'registCommand()' to regist your own commands.");
							}
							break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
