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
	public String server_ip = null;
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
			
			// Default settings
			server_port = getInteger("server_port");
			server_log_level = getInteger("server_log_level");
			server_max_users = getInteger("server_max_users");
			server_waiting_users = getInteger("server_waiting_users");
			mysql_port = getInteger("mysql_port");
			
			server_ip = getProperty("server_ip");
			mysql_host = getProperty("mysql_host");
			mysql_database = getProperty("mysql_database");
			mysql_user = getProperty("mysql_user");
			mysql_pass = getProperty("mysql_pass");
			http_host = getProperty("http_host");
			filelog_path = getProperty("filelog_path");
			filelog_filename = getProperty("filelog_filename");
			
			MOSS.connections_max = server_max_users;
			MOSS.connections_waiting = server_waiting_users;
			
			input.close();
		} catch (Exception e) {
			Utils.error("Config file not found");
		}
	}
	
	public String getString(String key) {
		return super.getProperty(key);
	}
	
	public Integer getInteger(String key) {
		int result = 0;
		try {
			result = Integer.parseInt(super.getProperty(key));
		} catch (Exception e) {
			Utils.error("Error parsing settings " + key);
		}
		return result;
	}
	
	public Boolean getBoolean(String key) {
		Boolean result = null;
		try {
			result = super.getProperty(key).equals("true");
		} catch (Exception e) {
			Utils.error("Error parsing settings " + key);
		}
		return result;
	}
}
