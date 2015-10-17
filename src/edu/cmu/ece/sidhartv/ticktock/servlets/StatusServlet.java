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
				response.getWriter().append("Error: driver cannot be found\n");
			}
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/TickTock", "root", "");
			errorOut.append("Connection successful");
		} catch (SQLException e) {
			errorOut.append("Failed to connect to MySQL database: \n\t"
					+ e.getMessage()
					+ "\n");
			response.setStatus(500);
		}
		Statement statement = null;
		int requestedUserId = -1;
		try {
			requestedUserId = Integer.parseInt(request.getParameter("uid"));
		} catch (NumberFormatException e) {
			errorOut.append("User ID was not a valid number");
			response.setStatus(500);
		}
		
		String dateTimeRequested = (request.getParameter("time"));
		SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Date dateToUse = null;
		if (dateTimeRequested.equals("current")) {
			dateToUse = new Date(System.currentTimeMillis());
		} else {
			try {
				dateToUse = mySQLFormatDateTime.parse(dateTimeRequested);
			} catch (ParseException e) {
				errorOut.append("Could not parse the provided time\n");
				response.setStatus(500);
			}
		}
		try {
			statement = connection.createStatement();
		} catch (SQLException e) {
			errorOut.append("Statement creation failed\n");
			response.setStatus(500);
		}
		String dateToUseString = mySQLFormatDateTime.format(dateToUse);
		String query = "SELECT 'status' FROM 'statuses' WHERE 'userID'= " + requestedUserId + " AND 'timeposted' > " + dateToUseString + " ORDER BY 'timeposted' DESC";
		ResultSet queryResultSet = null;
		try {
			queryResultSet = statement.executeQuery(query);
		} catch (SQLException e) {
			errorOut.append("Query failed\n");
			response.setStatus(500);
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
			errorOut.append("Something went wrong...somehow...whoops");
			response.setStatus(500);
		}
		response.setContentType("text/json");
		responseOut.append("{ \n"
								+ "\t\"status\": " + statusToRespond + "\n"
								+ "\t\"timePosted\": " + timePosted  + "\n"
						+ "}");
		response.setStatus(200);
		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
