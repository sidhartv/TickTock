package edu.cmu.ece.sidhartv.ticktock.servlets;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class FriendListServlet
 */
@WebServlet("/friend_list")
public class FriendListServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FriendListServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Connection connection = null;
		PrintWriter responseOut = response.getWriter();
		PrintStream errorOut = System.err;
		try {
			try {
				Class.forName("com.mysql.jdbc.Driver");
			} catch (ClassNotFoundException e) {
				errorOut.println("Error: driver cannot be found");
				response.sendError(500, "Error: driver cannot be found\n");
				return;
			}
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/TickTock", "root", "");
		} catch (SQLException e) {
			String errorMsg = "Failed to connect to MySQL database: \n\t"
					+ e.getMessage();
			errorOut.println(errorMsg);
			response.sendError(500, errorMsg);
			return;
		}
		Statement statement = null;
		try {
			statement = connection.createStatement();
		} catch (SQLException e) {
			errorOut.println("Statement creation failed");
			response.sendError(500, "Statement creation failed");
			return;
		}
		String username = request.getParameter("user");
		int thisID = -1;
		try {
			thisID = SQLHelpers.getUserIDFromName(username);
		} catch (SQLException e) {
			errorOut.println("Malformed username");
			response.sendError(412, "Malformed username");
			return;
		}
		if (thisID == -1) {
			errorOut.println("Could not find user");
			response.sendError(412, "Could not find user");
			return;
		}
		String query1 = "SELECT * FROM friendships WHERE friend1= '" + thisID +"'";
		String query2 = "SELECT * FROM friendships WHERE friend2= '" + thisID + "'";
		ResultSet result1 = null;
		ResultSet result2 = null;
		try {
			result1 = statement.executeQuery(query1);
			result2 = statement.executeQuery(query2);
		} catch (SQLException e) {
			errorOut.println("Friendship query failed");
			response.sendError(500, "Friendship query failed");
			return;
		}
		ArrayList<String> userIDs = new ArrayList<String>();
		try {
			while(result1.next()) {
				userIDs.add(result1.getString("friend2"));
			}
			while(result2.next()) {
				userIDs.add(result2.getString("friend1"));
			}
		} catch (SQLException e) {
			errorOut.println("Something went wrong...very wrong");
			response.sendError(500, "Something went wrong...very wrong");
			return;
		}
		if (userIDs.isEmpty()) {
			response.setStatus(204);
			return;
		}
		ArrayList<String> usernames = new ArrayList<String>();
		for (String userID : usernames) {
			int userID_int = Integer.parseInt(userID);
			try {
				usernames.add(SQLHelpers.getUserFromUserID(userID_int));
			} catch (SQLException e) {
				errorOut.println("Can't find user from id");
				response.sendError(500, "Can't find user from id");
				return;
			}
		}
		responseOut.append("{\n");
		responseOut.append("\"tusernames\": [\n");
		for (int i = 0; i < usernames.size() - 1; i++) {
			responseOut.append("\t\t{\"username\": \"" + usernames.get(i) + "\"},\n");
		}
		responseOut.append("\t\t{\"username\": \"" + usernames.get(usernames.size() - 1) + "\"}\n");
		responseOut.append("\t]");
		responseOut.append("}");
		response.setContentType("application/json");
		response.setStatus(200);
		
	}

}
