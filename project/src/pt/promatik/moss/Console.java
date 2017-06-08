package pt.promatik.moss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import pt.promatik.moss.utils.Utils;

public class Console extends Thread
{
	private Moss MOSS;
	private List<String> commands;
	private enum Commands { help, log, players_max, players_waiting, users; }
	
	public Console(Moss instance)
	{
		MOSS = instance;
		commands = new ArrayList<String>();
		registerCommands(Commands.class);
	}
	
	public void registerCommand(String command)
	{
		commands.add(command);
	}

	public <E extends Enum<?>> void registerCommands(Class<E> c)
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
				if(input != null && input.length() > 0)
				{
					if(Utils.enumContains(Commands.class, input))
					{
						switch (Commands.valueOf(input))
						{
							case help:
								Utils.error("Command list:");
								Utils.error(String.join(", ", commands));
								break;
							case log:
								Utils.error("Log levels: \n0 None\n1 Default\n2 Errors\n3 Full\n4 Verbose");
								input = br.readLine();
								try {
									int result = Integer.parseInt(input);
									if(result < 0 || result > 4)
										throw new NumberFormatException();
									Utils.log_level = result;
								} catch (NumberFormatException e) {
									Utils.error("Not a valid number.");
								} finally {
									Utils.error("LOG = " + String.valueOf(Utils.log_level));
								}
								break;
							case players_max:
								Utils.error("Input an integer value greather or equal to zero");
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
									Utils.error("CONNECTIONS_MAX = " + String.valueOf(MOSS.connections_max));
								}
								break;
							case players_waiting:
								Utils.error("Input an integer value greather or equal to zero");
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
									Utils.error("CONNECTIONS_WAITING = " + String.valueOf(MOSS.connections_waiting));
								}
								break;
							case users:
								int total = MOSS.server.getUsers().size();
								int totalWaiting = MOSS.server.getWaitingUsers().size();
								Utils.error(total + " online users");
								if(totalWaiting > 0)
									Utils.error(totalWaiting + " waiting to play users");
								break;
							default:
								break;
						}
					} else {
						if(commands.contains(input)) {
							Utils.error("Input the value for '" + input + "':");
							String value = br.readLine();
							MOSS.commandInput(input, value);
						} else {
							Utils.error("The command '" + input + "' was not found.\nType 'help' to get the command list.\nWhile developing, use 'registerCommand()' to register your own commands.");
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
