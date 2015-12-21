package pt.promatik.moss;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class MySQL {
	public boolean connected = false;
	private Connection conn;
	
	public MySQL(){
		
	}
	
	public void connect(String host, String database, String user, String password){
		connect(host, 3306, database, user, password);
	}
	
	public void connect(String host, int port, String database, String user, String password){
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + String.valueOf(port) + "/" + database, user, password);
			connected = true;
		} catch (Exception e) {
			Utils.log("Connect exception " + e.toString() + " - " + e.getMessage(), "MySQL", e);
		}
	}
	
	public void disconnect(){
		try {
			connected = false;
			conn.close();
		} catch (SQLException e) {
			Utils.log(e);
		}
	}

	public Connection getConnection(){
		return conn;
	}

	public Statement getStatement(){
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
		} catch (SQLException e) {
			Utils.log("Statement exception " + e.toString(), "MySQL");
		}
		return stmt;
	}
	
	public HashMap<String, String> queryFirst(String query, String... fields){
		return query(query, fields).get(0);
	}
	
	public ArrayList<HashMap<String, String>> query(String query, String... fields){
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
			Utils.log("Statement exception " + e.toString(), "MySQL");
		}
		return result;
	}
	
	public void query(String query){
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			if(query.indexOf("UPDATE") == 0 || query.indexOf("INSERT") == 0)
				stmt.executeUpdate(query);
			else
				stmt.executeQuery(query);
			stmt.close();
		} catch (SQLException e) {
			Utils.log("Statement exception " + e.toString(), "MySQL");
		}
	}
}
