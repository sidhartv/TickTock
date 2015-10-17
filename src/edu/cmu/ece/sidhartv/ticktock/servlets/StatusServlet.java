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
@WebServlet("/StatusServlet")
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
		int requestedUserId = -1;
		try {
			requestedUserId = Integer.parseInt(request.getParameter("uid"));
		} catch (NumberFormatException e) {
			errorOut.println("User ID was not a valid number");
			response.sendError(412,"User ID was not a valid number");
			return;
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
		try {
			statement = connection.createStatement();
		} catch (SQLException e) {
			errorOut.println("Statement creation failed");
			response.sendError(500, "Statement creation failed");
			return;
		}
		String dateToUseString = mySQLFormatDateTime.format(dateToUse);
		String query = "SELECT 'status' FROM 'statuses' WHERE 'userID'= " + requestedUserId + " AND 'timeposted' > " + dateToUseString + " ORDER BY 'timeposted' DESC";
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
		try {
			if (queryResultSet.first()) {
				statusToRespond.concat(queryResultSet.getNString("status"));
				timePosted.concat(queryResultSet.getNString("timePosted"));
			} else {
				statusToRespond = null;
				timePosted = null;
			}
		} catch (SQLException e) {
			errorOut.println("Something went wrong...somehow...whoops");
			response.sendError(500,"Something went wrong...somehow...whoops");
			return;
		}
		response.setContentType("text/json");
		responseOut.append("{ \n"
								+ "\t\"status\": " + statusToRespond + "\n"
								+ "\t\"timePosted\": " + timePosted  + "\n"
						+ "}");
		response.setStatus(200); //200 = YAY
		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
