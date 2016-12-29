package pt.promatik.moss.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import pt.promatik.moss.Moss;

@SuppressWarnings("serial")
public class Settings extends Properties
{
	private Moss MOSS;
	
	// Default settings
	public int server_port = 0;
	public int server_log_level = 0;
	public int server_max_users = 0;
	public int server_waiting_users = 0;
	public String mysql_host;
	public int mysql_port = 0;
	public String mysql_database;
	public String mysql_user;
	public String mysql_pass;
	public String http_host;
	public String filelog_path;
	public String filelog_filename;
	
	public Settings(Moss instance, String filename)
	{
		MOSS = instance;
		InputStream input = null;
		
		if(filename == null)
			filename = "config";

		try {
			input = new FileInputStream(filename);
			load(input);
			
			try {
				if(getProperty("server_port") != null) server_port = Integer.parseInt(getProperty("server_port"));
				if(getProperty("server_log_level") != null) server_log_level = Integer.parseInt(getProperty("server_log_level"));
				if(getProperty("server_max_users") != null) server_max_users = Integer.parseInt(getProperty("server_max_users"));
				if(getProperty("server_waiting_users") != null) server_waiting_users = Integer.parseInt(getProperty("server_waiting_users"));
				if(getProperty("mysql_port") != null) mysql_port = Integer.parseInt(getProperty("mysql_port"));
				
				MOSS.CONNECTIONS_MAX = server_max_users;
				MOSS.CONNECTIONS_WAITING = server_waiting_users;
			} catch (NumberFormatException e) {
				Utils.log("An error ocurred while trying to parse a number from config file", e);
			}
			
			// Default settings
			mysql_host = getProperty("mysql_host");
			mysql_database = getProperty("mysql_database");
			mysql_user = getProperty("mysql_user");
			mysql_pass = getProperty("mysql_pass");
			http_host = getProperty("http_host");
			filelog_path = getProperty("filelog_path");
			filelog_filename = getProperty("filelog_filename");
			
			input.close();
		} catch (Exception e) {
			Utils.log("Config file not found", e);
		}
	}
}
