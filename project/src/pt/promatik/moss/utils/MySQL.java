package pt.promatik.moss.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class MySQL
{
	public boolean connected = false;
	private int port = 3306;
	private Connection conn;
	private final String tag = "SQL ";
	
	public MySQL()
	{
		
	}
	
	public int port()
	{
		return port;
	}
	
	public void connect(String host, String database, String user, String password)
	{
		connect(host, port, database, user, password);
	}
	
	public void connect(String host, int _port, String database, String user, String password)
	{
		connect(host, port, database, user, password, true, StandardCharsets.UTF_8);
	}
	
	public void connect(String host, int _port, String database, String user, String password, boolean useUnicode, Charset charset)
	{
		if(_port > 0)
			port = _port;
		
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + String.valueOf(port) + "/" + database + "?useUnicode=" + (useUnicode ? "yes" : "no") + "&characterEncoding=" + charset.name(), user, password);
			connected = true;
		} catch (Exception e) {
			Utils.log("Connect exception " + e.toString() + " - " + e.getMessage(), tag, e);
		}
	}
	
	public void disconnect()
	{
		try {
			connected = false;
			conn.close();
		} catch (SQLException e) {
			Utils.log(e);
		}
	}

	public Connection getConnection()
	{
		return conn;
	}

	public Statement getStatement()
	{
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
		} catch (SQLException e) {
			Utils.log("Statement exception " + e.toString(), tag);
		}
		return stmt;
	}
	
	public HashMap<String, String> queryFirst(String query, String... fields)
	{
		ArrayList<HashMap<String, String>> result = query(query, fields);
		return result.size() > 0 ? result.get(0) : null;
	}
	
	public ArrayList<HashMap<String, String>> query(String query, String... fields)
	{
		if(Utils.log_level >= Utils.LOG_VERBOSE)
			Utils.log(query, tag);
		
		Statement stmt = null;
		ArrayList<HashMap<String, String>> result = new ArrayList<>();
		try {
			stmt = conn.createStatement();
			ResultSet queryResult = stmt.executeQuery(query);
			
			while (queryResult.next()) {
				HashMap<String, String> row = new HashMap<String, String>();
				for (String field : fields) {
					row.put(field, queryResult.getString(field));
				}
				result.add(row);
			}
			stmt.close();
		} catch (SQLException e) {
			Utils.error("Statement exception " + e.toString(), tag);
			Utils.error("Query: " + query, tag);
		}
		return result;
	}
	
	public long query(String query)
	{
		if(Utils.log_level >= Utils.LOG_VERBOSE)
			Utils.log(query, tag);
		
		long id = -1;
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			if(query.indexOf("UPDATE") == 0 || query.indexOf("INSERT") == 0)
				stmt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
			else
				stmt.executeQuery(query);
			
			try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					id = generatedKeys.getLong(1);
				}
			}
			
			stmt.close();
		} catch (SQLException e) {
			Utils.error("Statement exception " + e.toString(), tag);
			Utils.error("Query: " + query, tag);
		}
		return id;
	}
}
