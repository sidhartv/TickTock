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


import edu.cmu.ece.sidhartv.ticktock.servlets.MeetingRequestServlet.MeetingRequest;

/**
 * Servlet implementation class MeetingRequestServlet
 */
@WebServlet("/meeting_request")
public class MeetingRequestServlet extends HttpServlet {
	

	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MeetingRequestServlet() {
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
		boolean isPropositioner = request.getParameter("party").equals("propositioner");
		if (!isPropositioner && !request.getParameter("party").equals("approver")) {
			errorOut.println("Invalid party!");
			response.sendError(412,"Invalid party!");
			return;
		}
		String query = "";
		if (isPropositioner) {
			query = "SELECT * FROM meeting_requests WHERE propositioner= '" + myID +"'";
		} else {
			query = "SELECT * FROM meeting_requests WHERE approver= '" + myID +"'";
		}
		ResultSet result = null;
		try {
			result = statement.executeQuery(query);
		} catch (SQLException e) {
			errorOut.println("SELECT query failed");
			response.sendError(500,"SELECT query failed");
			return;
		}
		ArrayList<MeetingRequest> meetingRequests = new ArrayList<MeetingRequest>();
		try {
			while (result.next()) {
				int id = Integer.parseInt(result.getString("id"));
				int propositioner = Integer.parseInt(result.getString("propositioner"));
				int approver = Integer.parseInt(result.getString("approver"));
				int duration = Integer.parseInt(result.getString("duration"));
				boolean scheduled = Integer.parseInt(result.getString("scheduled")) == 1;
				boolean approved = Integer.parseInt(result.getString("approved")) == 1;
				String expiryDateString = result.getString("expiry");
				SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date expiryDate = null;
				try {
					expiryDate = mySQLFormatDateTime.parse(expiryDateString);
				} catch (ParseException e) {
					errorOut.println("Date unparseable");
					response.sendError(500, "Date unparseable");
					return;
				}
				meetingRequests.add(new MeetingRequest(id,propositioner,approver,duration,expiryDate,approved,scheduled));
			}
		} catch (SQLException e) {
			errorOut.println("Something went terribly wrong with the results");
			response.sendError(500, "Something went terribly wrong with the results");
			return;
		}
		if (meetingRequests.isEmpty()) {
			response.setStatus(204);
			return;
		} else {
			String responseJSON = "{\n" +
										"\t\"meetingRequests\": [\n";
			for (int i = 0; i < meetingRequests.size() - 1; i++) {
				MeetingRequest meetingRequest = meetingRequests.get(i);
				try {
					responseJSON.concat("\t\t{ \"id\": \"" + meetingRequest.getID() + "\", \"initiator\": " + "\"" + meetingRequest.getPropositionerName() + "\"," + "\"target\": \"" + meetingRequest.getApproverName() + ", \"duration\": " + meetingRequest.getDuration() +  "\", \"expiry\": \"" + meetingRequest.getExpiryDateString() + "\", \"approved\": \"" + meetingRequest.isApproved() + "\", \"scheduled\": " + meetingRequest.isScheduled() + "\"},\n");
				} catch (SQLException e) {
					errorOut.println("Error fetching name from ID");
					response.sendError(500, "Error fetching name from ID");
					return;
				}
			}
			MeetingRequest meetingRequest = meetingRequests.get(meetingRequests.size() - 1); 
			try {
				responseJSON.concat("\t\t{ \"id\": \"" + meetingRequest.getID() + "\", \"initiator\": " + "\"" + meetingRequest.getPropositionerName() + "\"," + "\"target\": \"" + meetingRequest.getApproverName() + ", \"duration\": " + meetingRequest.getDuration() +  "\", \"expiry\": \"" + meetingRequest.getExpiryDateString() + "\", \"approved\": \"" + meetingRequest.isApproved() + "\", \"scheduled\": " + meetingRequest.isScheduled() + "\"}\n");
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
		Connection connection = null;
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
		 
		
		boolean makeRequest = request.getParameter("type").equals("request");
		if (!makeRequest && !request.getParameter("type").equals("approval")) {
			errorOut.println("Invalid type parameter");
			response.sendError(412,"Invalid type parameter");
			return;
		}
		if (makeRequest) {
			int me = -1;
			try {
				Integer.parseInt(request.getParameter("me"));
			} catch (NumberFormatException e) {
				errorOut.println("Invalid user ID (me)");
				response.sendError(412, "Invalid user ID (me)");
				return;
			}
			String otherName = request.getParameter("other");
			int other = -1;
			try {
				other = SQLHelpers.getUserIDFromName(otherName);
			} catch (SQLException e) {
				errorOut.println("Something went wrong while querying username");
				response.sendError(412, "Something went wrong while querying username");
				return;
			}
			String validateNotFriendsAlreadyQuery = "SELECT * FROM friendships WHERE (friend1= '" + me + "' AND friend2= '" + other + "') OR (friend1= '" + other + "' AND friend2= '" + me + "')";
			boolean alreadyFriends = true;
			try {
				alreadyFriends = statement.executeQuery(validateNotFriendsAlreadyQuery).first();
			} catch (SQLException e) {
				errorOut.println("Request validation failed");
				response.sendError(500, "Request validation failed");
				return;
			}
			if (!alreadyFriends) {
				errorOut.println("Not friends");
				response.sendError(412, "Not friends");
				return;
			}
			int duration = -1;
			try {
				duration = Integer.parseInt(request.getParameter("duration"));
			} catch (NumberFormatException e) {
				errorOut.println("Invalid duration");
				response.sendError(412, "Invalid duration");
				return;
			}
			/* Set up of current date formatting, etc. */
			SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String expiryDateString = request.getParameter("expiry");
			Date expiryDateTime = null;
			try {
				expiryDateTime = mySQLFormatDateTime.parse(expiryDateString);
			} catch (ParseException e1) {
				errorOut.println("Invalid date parameter");
				response.sendError(412,"Invalid date parameter");
				return;
			}
			String mySQLFormatedExpiryTime = mySQLFormatDateTime.format(expiryDateTime);
			String insertionQuery = "INSERT INTO meeting_requests (propositioner,approver,duration,expiry,approved,scheduled) VALUES ('" + me + "','" + other + "','" + duration + "','" + mySQLFormatedExpiryTime + "','0','0')";
			try {
				statement.execute(insertionQuery);
			} catch (SQLException e) {
				errorOut.println("Insertion failed");
				response.sendError(500, "Insertion failed");
				return;
			}
			response.setStatus(204); //Meeting request sent
		} else {
			int id = -1;
			try {
				id = Integer.parseInt(request.getParameter("id"));
			} catch (NumberFormatException e) {
				errorOut.println("Bad ID");
				response.sendError(412, "Bad ID");
				return;
			}
			String requestedValidationQuery = "SELECT * FROM meeting_requests WHERE id= '" + id +"'";
			boolean alreadyRequested = true;
			try {
				alreadyRequested = statement.executeQuery(requestedValidationQuery).first();
			} catch (SQLException e) {
				errorOut.println("Request validation failed");
				response.sendError(500, "Request validation failed");
				return;
			}
			if (!alreadyRequested) {
				errorOut.println("Request does not exist");
				response.sendError(412, "Request does not exist");
				return;
			}
			String updateQuery = "UPDATE meeting_requests SET approved='1' WHERE id= '" + id +"'";
			try {
				statement.execute(updateQuery);
			} catch (SQLException e) {
				errorOut.println("Update failed");
				response.sendError(500, "Update failed");
				return;
			}
			response.setStatus(204); //Meeting request approved
		}
	}

	public class MeetingRequest {
		private int id;
		private int propositioner;
		private int approver;
		private int duration;
		private boolean approved;
		private boolean scheduled;
		private Date expiryDate;
		public MeetingRequest(int id, int propositioner, int approver, int duration, Date expiryDate, boolean approved, boolean scheduled) {
			this.id = id;
			this.propositioner = propositioner;
			this.approver = approver;
			this.expiryDate = expiryDate;
			this.approved = approved;
			this.scheduled = scheduled;
		}
		public String getID() {
			return Integer.toString(id);
		}
		public String getPropositionerName() throws SQLException {
			return SQLHelpers.getUserFromUserID(propositioner);
		}
		public String getApproverName() throws SQLException {
			return SQLHelpers.getUserFromUserID(approver);
		}
		public String getExpiryDateString() {
			SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return mySQLFormatDateTime.format(expiryDate);
		}
		public String isApproved() {
			if (approved) {
				return "true";
			} else {
				return "false";
			}
		}
		public String isScheduled() {
			if (scheduled) {
				return "true";
			} else {
				return "false";
			}
		}
		public String getDuration() {
			return Integer.toString(duration);
		}
		
	}
}
