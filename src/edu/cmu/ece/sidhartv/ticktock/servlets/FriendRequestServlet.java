package edu.cmu.ece.sidhartv.ticktock.servlets;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class RequestFriend
 */
@WebServlet("/friend_request")
public class FriendRequestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FriendRequestServlet() {
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
		int myID = -1;
		try {
			myID = Integer.parseInt(request.getParameter("id"));
		} catch (NumberFormatException e) {
			errorOut.println("Invalid id number given");
			response.sendError(412, "Invalid id number given");
			return;
		}
		boolean isInitiator = request.getParameter("party").equals("initiator");
		if (!isInitiator && !request.getParameter("party").equals("target")) {
			errorOut.println("Invalid party!");
			response.sendError(412,"Invalid party!");
			return;
		}
		String query = "";
		if (isInitiator) {
			query = "SELECT * FROM 'friend_requests' WHERE 'initiator_id'=" + myID;
		} else {
			query = "SELECT * FROM 'friend_requests' WHERE 'target_id'=" + myID;
		}
		ResultSet result = null;
		try {
			result = statement.executeQuery(query);
		} catch (SQLException e) {
			errorOut.println("SELECT query failed");
			response.sendError(500,"SELECT query failed");
			return;
		}
		ArrayList<FriendRequest> friendRequests = new ArrayList<FriendRequest>();
		try {
			while (result.next()) {
				int initiator = Integer.parseInt(result.getNString("initiator_id"));
				int target = Integer.parseInt(result.getNString("target_id"));
				String requestDateString = result.getNString("request_time");
				SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date requestDate = null;
				try {
					requestDate = mySQLFormatDateTime.parse(requestDateString);
				} catch (ParseException e) {
					errorOut.println("Date unparseable");
					response.sendError(500, "Date unparseable");
					return;
				}
				friendRequests.add(new FriendRequest(initiator,target,requestDate));
			}
		} catch (SQLException e) {
			errorOut.println("Something went terribly wrong with the results");
			response.sendError(500, "Something went terribly wrong with the results");
			return;
		}
		if (friendRequests.isEmpty()) {
			response.setStatus(204);
			return;
		} else {
			String responseJSON = "{\n" +
										"\t\"friendRequests\": [\n";
			for (int i = 0; i < friendRequests.size() - 1; i++) {
				FriendRequest friendRequest = friendRequests.get(i);
				try {
					responseJSON.concat("\t\t{ \"initiator\": " + "\"" + friendRequest.getInitiatorName() + "\"," + "\"target\": \"" + friendRequest.getTargetName() + "\"},\n");
				} catch (SQLException e) {
					errorOut.println("Error fetching name from ID");
					response.sendError(500, "Error fetching name from ID");
					return;
				}
			}
			FriendRequest friendRequest = friendRequests.get(friendRequests.size() - 1); 
			try {
				responseJSON.concat("\t\t{ \"initiator\": " + "\"" + friendRequest.getInitiatorName() + "\"," + "\"target\": \"" + friendRequest.getTargetName() + "\", \"request_time\": \"" + friendRequest.getRequestDateString() + "\"}\n");
			} catch (SQLException e) {
				errorOut.println("Error fetching name from ID");
				response.sendError(500, "Error fetching name from ID");
				return;
			}
			responseJSON.concat("\t]\n}");
			response.setContentType("application/json");
			responseOut.append(responseJSON);
			response.setStatus(200); //A-ok! :) 
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
	}
	private class FriendRequest {
		private int initiator;
		private int target;
		private Date requestDate;
		public FriendRequest(int initiator, int target, Date requestDate) {
			this.initiator = initiator;
			this.target = target;
			this.requestDate = requestDate;
		}
		public int getInitiator() {
			return initiator;
		}
		public void setInitiator(int initiator) {
			this.initiator = initiator;
		}
		public int getTarget() {
			return target;
		}
		public void setTarget(int target) {
			this.target = target;
		}
		public String getTargetName() throws SQLException {
			return SQLHelpers.getUserFromUserID(target);
		}
		public String getInitiatorName() throws SQLException {
			return SQLHelpers.getUserFromUserID(initiator);
		}
		public String getRequestDateString() {
			SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return mySQLFormatDateTime.format(requestDate);
		}
			
	}
}
