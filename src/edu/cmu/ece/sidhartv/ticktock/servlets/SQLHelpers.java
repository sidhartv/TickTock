package edu.cmu.ece.sidhartv.ticktock.servlets;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLHelpers {
	public static String getUserFromUserID(int id) throws SQLException {
		Connection connection = null;
		PrintStream errorOut = System.err;
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			errorOut.println("Error: driver cannot be found");
			throw new SQLException();
		}
		connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/TickTock", "root", "");
		
		String query = "SELECT username FROM users WHERE id= '" + Integer.toString(id) + "'";
		Statement statement = connection.createStatement();
		ResultSet result = statement.executeQuery(query);
		if (result.first()) {
			String username = result.getString("username");
			return username;
		}
		return null;	
	}
	public static int getUserIDFromName(String username) throws SQLException {
		Connection connection = null;
		PrintStream errorOut = System.err;
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			errorOut.println("Error: driver cannot be found");
			throw new SQLException();
		}
		connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/TickTock", "root", "");
		
		String query = "SELECT id FROM users WHERE username= '" + username + "'";
		Statement statement = connection.createStatement();
		ResultSet result = statement.executeQuery(query);
		if (result.first()) {
			int userID = Integer.parseInt(result.getString("id"));
			return userID;
		}
		return -1;
	
	}
	
	
}
