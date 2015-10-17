package edu.cmu.ece.sidhartv.ticktock.servlets;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Servlet implementation class StatusServlet
 */
@WebServlet("/status")
public class StatusServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @throws SQLException 
     * @see HttpServlet#HttpServlet()
     */
    public StatusServlet() throws SQLException {
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
		
		
		String usernameRequested = (request.getParameter("user"));
		try {
			usernameRequested.length();
		} catch (NullPointerException e) {
			errorOut.println("User parameter not passed in...or something went wrong and I don't know why.");
			response.sendError(412, "User parameter not passed in...or something went wrong and I don't know why.");
			return;
		}
		
		
		int requestedUserId = -1; 
		try {
			requestedUserId = SQLHelpers.getUserIDFromName(usernameRequested);
		} catch (SQLException e) {
			errorOut.println("Something went wrong while querying username");
			response.sendError(500, "Something went wrong while querying username");
			return;
		}
		if (requestedUserId < 0) {
			errorOut.println("Invalid userID");
			response.sendError(412, "Invalid username");
		}
		
		String dateTimeRequested = (request.getParameter("time"));
		try {
			dateTimeRequested.length();
		} catch (NullPointerException e) {
			errorOut.println("Time parameter was not passed in...or something went wrong and I don't know why.");
			response.sendError(412, "Time parameter was not passed in...or something went wrong and I don't know why.");
			return;
		}
		
		SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Date dateToUse = null;
		if (dateTimeRequested.equals("current")) {
			dateToUse = new Date(System.currentTimeMillis());
		} else {
			try {
				dateToUse = mySQLFormatDateTime.parse(dateTimeRequested);
			} catch (ParseException e) {
				errorOut.println("Could not parse the provided time");
				response.sendError(500, "Could not parse the provided time");
				return;
			}
		}
		String dateToUseString = mySQLFormatDateTime.format(dateToUse);
		String query = "SELECT * FROM 'statuses' WHERE 'userID'= " + requestedUserId + " AND 'timeposted' <= " + dateToUseString + " AND " + dateToUseString + " < 'expiry' ORDER BY 'timeposted' DESC";
		ResultSet queryResultSet = null;
		try {
			queryResultSet = statement.executeQuery(query);
		} catch (SQLException e) {
			errorOut.println("Query failed");
			response.sendError(500,"Query failed");
			return;
		}
		String statusToRespond = "";
		String timePosted = "";
		String userIDStr = "";
		try {
			if (queryResultSet.first()) {
				statusToRespond.concat(queryResultSet.getNString("status"));
				timePosted.concat(queryResultSet.getNString("timePosted"));
				userIDStr.concat(queryResultSet.getNString("userID"));
				int userID = Integer.parseInt(userIDStr);
				String username = SQLHelpers.getUserFromUserID(userID);
				
				response.setContentType("application/json");
				responseOut.append("{ \n"
										+ "\t\"username\": " + username + "\n"
										+ "\t\"status\": " + statusToRespond + "\n"
										+ "\t\"timePosted\": " + timePosted  + "\n"
								+ "}");
				response.setStatus(200); //200 = YAY
			} else {
				statusToRespond = null;
				timePosted = null;
				userIDStr = null;
				
				response.setStatus(204); //Empty response
			}
		} catch (SQLException e) {
			errorOut.println("Something went wrong...somehow...whoops");
			response.sendError(500,"Something went wrong...somehow...whoops");
			return;
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintStream errorOut = System.err; //Prints to console, or wherever stderr points, when something goes wrong
		
		/* MySQL set up */
		Connection connection = null;
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
		
		
		
		
		
		String status = request.getParameter("status");
		
		/* Parse and validate the userID out of the parameters */
		int userID = -1;
		try {
			userID = Integer.parseInt(request.getParameter("uid"));
		} catch (NumberFormatException e) {
			errorOut.println("User ID must be an integer");
			response.sendError(412, "User ID must be an integer");
			return;
		}
		
		/* Validate username */
		String username = null;
		try {
			username = SQLHelpers.getUserFromUserID(userID);
		} catch (SQLException e) {
			errorOut.println("User verification failed");
			response.sendError(500, "User verification failed");
			return;
		}
		if (username == null) {
			errorOut.println("User ID does not exist!");
			response.sendError(412, "User ID does not exist");
			return;
		}
		
		/* Set up of current date formatting, etc. */
		SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date currentDateTime = new Date (System.currentTimeMillis());
		String mySQLFormatedCurrentTime = mySQLFormatDateTime.format(currentDateTime);

		String expiry = request.getParameter("expiry");
		
		/* Update old statuses to be expired */
		String updateExpiryQuery = "UPDATE 'statuses' SET 'expiry'= " + mySQLFormatedCurrentTime + " WHERE 'userID'=" + Integer.toString(userID) + " AND 'expiry' > " + mySQLFormatedCurrentTime; 
		try {
			statement.execute(updateExpiryQuery);
		} catch (SQLException e) {
			errorOut.println("UPDATE of old, non expired statuses failed");
			response.sendError(500, "UPDATE of old, non expired statuses failed");
			return;
		}
		
		/* Format the new status' expiry date */
		Date expiryDate = null;
		try {
			expiryDate = mySQLFormatDateTime.parse(expiry);
		} catch (ParseException e1) {
			errorOut.println("Invalid expiry date");
			response.sendError(412, "Invalid expiry date");
			return;
		}
		String mySQLFormattedExpiryDate = mySQLFormatDateTime.format(expiryDate);
		
		/* Insert the new status into the database */
		String insertQuery = "INSERT INTO 'statuses' ('userID','status','timeposted','expiry') VALUES (" + Integer.toString(userID) + "," + status + "," + mySQLFormatedCurrentTime + "," + mySQLFormattedExpiryDate + ")"; 
		try {
			statement.executeQuery(insertQuery);
		} catch (SQLException e) {
			errorOut.println("INSERTion of new status failed");
			response.sendError(500, "INSERTion of new status failed");
			return;
		}
		response.setStatus(204); //All good, won't receive any data.
		
		
	}

}
