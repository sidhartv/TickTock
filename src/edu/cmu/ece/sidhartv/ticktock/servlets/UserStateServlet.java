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
 * Servlet implementation class ScheduledEventServlet
 */
@WebServlet("/user_state")
public class UserStateServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public UserStateServlet() {
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
		boolean userAllStates = request.getParameter("type").equals("user_all_states");
		boolean userStateAtTime = request.getParameter("type").equals("user_state_at_time");
		boolean usersWithStateAtTime = request.getParameter("type").equals("users_with_state_at_time");
		if (!(userAllStates || userStateAtTime || usersWithStateAtTime)) {
			errorOut.println("Invalid type parameter");
			response.sendError(412, "Invalid type parameter");
			return;
		}
		if (userAllStates) {
			String username = request.getParameter("user");
			int user_id;
			try {
				user_id = SQLHelpers.getUserIDFromName(username);
			} catch (SQLException e3) {
				errorOut.println("Cannot translate to user id");
				response.sendError(412, "Cannot translate to user id");
				return;
			}
			if (user_id < 0) {
				errorOut.println("Malformed user ID");
				response.sendError(412,"Malformed user ID");
				return;
			}
			String query = "SELECT * FROM user_states WHERE user_id= '" + user_id + "'";
			ResultSet result = null;
			try {
				result = statement.executeQuery(query);
			} catch (SQLException e) {
				errorOut.println("SELECT error");
				response.sendError(500,"SELECT error");
				return;
			}
			try {
				if (!result.first()) {
					response.setStatus(204);
					return;
				}
			} catch (SQLException e2) {
				errorOut.println("Wat");
				response.sendError(500,"Wat");
				return;
			}
			try {
				result.beforeFirst();
			} catch (SQLException e1) {
				errorOut.println("Wat");
				response.sendError(500,"Wat");
				return;
			}
			String JSON = "{\n" +
					"\t \"events\": [\n";
			ArrayList<String> rows = new ArrayList<String>();
			try {
				while(result.next()) {
					int user_id_rcvd = -1;
					try {
						user_id_rcvd = Integer.parseInt(result.getString("user_id"));
					} catch (NumberFormatException e) {
						errorOut.println("Error in user_id parsing");
						response.sendError(500, "Error in user_id parsing");
						return;
					} catch (SQLException e) {
						errorOut.println("MySQL error - user_id");
						response.sendError(500, "MySQL error - user_id");
						return;
					}
					int state = -1;
					try {
						state = Integer.parseInt(result.getString("state"));
					} catch (SQLException e) {
						errorOut.println("MySQL error - event_start or event_end");
						response.sendError(500, "MySQL error - event_start or event_end");
						return;
					}
					SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String startDateStr = null;
					String endDateStr = null;
					try {
						startDateStr = result.getString("state_start");
						endDateStr = result.getString("state_end");
					} catch (SQLException e) {
						errorOut.println("MySQL error - state_start or state_end");
						response.sendError(500, "MySQL error - state_start or state_end");
						return;
					}
					Date startDate = null;
					Date endDate = null;
					try {
						startDate = mySQLFormatDateTime.parse(startDateStr);
						endDate = mySQLFormatDateTime.parse(endDateStr);
					} catch (ParseException e) {
						errorOut.println("Date parsing error - start or end date from DB");
						response.sendError(500, "Date parsing error - start or end date from DB");
						return;
					}
					String start = mySQLFormatDateTime.format(startDate);
					String end = mySQLFormatDateTime.format(endDate);
					rows.add("\t\t {\"user_id\": \"" + user_id_rcvd + "\", \"state\": \"" + state + "\", \"startTime\": \"" + start + "\", \"endTime\": \"" + end + "\"}");
				}
			} catch (SQLException e) {
				errorOut.println("How did this mess up...");
				response.sendError(500, "How did this mess up...");
				return;
			}
			for (int i = 0; i < rows.size() - 1; i++) {
				JSON.concat(rows.get(i) + ",\n");
			}
			JSON.concat(rows.get(rows.size() - 1) + "\n");
			JSON.concat("\t]\n");
			JSON.concat("}");
			responseOut.append(JSON);
			response.setStatus(200); //AYYYYY 
		} else if (userStateAtTime) {
			String username = request.getParameter("user");
			int user_id;
			try {
				user_id = SQLHelpers.getUserIDFromName(username);
			} catch (SQLException e3) {
				errorOut.println("Cannot translate to user id");
				response.sendError(412, "Cannot translate to user id");
				return;
			}
			if (user_id < 0) {
				errorOut.println("Malformed user ID");
				response.sendError(412,"Malformed user ID");
				return;
			}

			String timeRequested = request.getParameter("time");
			SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date timeRequestedDate = null;
			try {
				timeRequestedDate = mySQLFormatDateTime.parse(timeRequested);
			} catch (ParseException e) {
				//TODO auto-gen
				e.printStackTrace();
			}
			String strDateRequest = mySQLFormatDateTime.format(timeRequestedDate);

			String query = "SELECT * FROM user_states WHERE user_id= '" + user_id + "' AND state_start < '" + strDateRequest + "' AND '" + strDateRequest + "' < state_end";
			ResultSet result = null;
			try {
				result = statement.executeQuery(query);
			} catch (SQLException e) {
				e.printStackTrace();;
			}
			try {
				if (result.first()) {
					response.setContentType("text/text");
					responseOut.append(result.getString("state"));
					response.setStatus(200);
					return;
				} else {
					response.setContentType("text/text");
					responseOut.append("0");
					response.setStatus(200);
					return;
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			int stateRequested = -1;
			try {
				stateRequested = Integer.parseInt(request.getParameter("state")); 
			} catch (NumberFormatException e) {
				//TODO auto-gen
				e.printStackTrace();
			}
			if (stateRequested < 0 || stateRequested >= 4) {
				//TODO fix
				response.sendError(412, "Bad state");
			}


			String timeRequested = request.getParameter("time");
			SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date timeRequestedDate = null;
			try {
				timeRequestedDate = mySQLFormatDateTime.parse(timeRequested);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			String strDateRequest = mySQLFormatDateTime.format(timeRequestedDate);
			String query = "SELECT * FROM user_states WHERE state= '" + stateRequested + "' AND state_start < '" + strDateRequest + "' AND '" + strDateRequest + "' < state_end";
			ResultSet result = null;
			try {
				result = statement.executeQuery(query);
			} catch (SQLException e) {
				e.printStackTrace();;
			}
			ArrayList<Integer> userIDs = new ArrayList<Integer>();
			try {
				while(result.next()) {
					int id = Integer.parseInt(result.getString("user_id"));
					userIDs.add(id);
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (userIDs.isEmpty()) {
				response.setStatus(204);
				return;
			}
			ArrayList<String> usernames = new ArrayList<String>();
			for (Integer id : userIDs) {
				String username = null;
				try {
					username = SQLHelpers.getUserFromUserID(id);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (username == null) {
					response.sendError(500, "ID match error");
					return;
					//TODO fix
				}
				usernames.add(username);
			}
			response.setContentType("text/text");
			for (String username : usernames) {
				responseOut.append(username + "\n");
			}
			response.setStatus(200);
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
		int user_id = -1;
		try {
			user_id = Integer.parseInt(request.getParameter("userID"));
		} catch (NumberFormatException e) {
			errorOut.println("Invalid user ID");
			response.sendError(412, "Invalid userID");
			return;
		}
		int state = -1;
		try {
			state = Integer.parseInt(request.getParameter("state"));
		} catch (NumberFormatException e) {
			errorOut.println("State is not valid");
			response.sendError(412,"State is not valid");
			return;
		}
		if (state < 0 || state >= 4) {
			errorOut.println("State is not valid");
			response.sendError(412,"State is not valid");
			return;
		}
		String startDate = request.getParameter("start");
		String endDate = request.getParameter("end");

		SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date startDateTime;
		try {
			startDateTime = mySQLFormatDateTime.parse(startDate);
		} catch (ParseException e) {
			errorOut.println("Error parsing start time");
			response.sendError(412,"Error parsing start time");
			return;
		}
		Date endDateTime;
		try {
			endDateTime = mySQLFormatDateTime.parse(endDate);
		} catch (ParseException e) {
			errorOut.println("Error parsing end time");
			response.sendError(412,"Error parsing end time");
			return;
		}
		String startInsert = mySQLFormatDateTime.format(startDateTime);
		String endInsert = mySQLFormatDateTime.format(endDateTime);

		String insertQuery = "INSERT INTO user_states (user_id,event_start,event_end,state) VALUES ('" + user_id + "','" + startInsert + "','" + endInsert + "','" + state + "')";
		try {
			statement.execute(insertQuery);
		} catch (SQLException e) {
			errorOut.println("Error inserting query");
			response.sendError(500,"Internal server error - insert");
			return;
		}
		response.setStatus(204);
	}

}
