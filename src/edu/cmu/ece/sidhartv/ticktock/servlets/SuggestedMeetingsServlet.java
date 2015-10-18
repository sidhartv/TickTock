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
import java.util.HashMap;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SuggestedMeetingsServlet
 */
@WebServlet("/suggested_meetings")
public class SuggestedMeetingsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SuggestedMeetingsServlet() {
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
		
		int me = -1;
		try {
			me = Integer.parseInt(SQLHelpers.translateParameter(request.getParameter("ownID")));
		} catch (NumberFormatException e) {
			errorOut.println("Bad own ID");
			response.sendError(412, "Bad own ID");
			return;
		}
		SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date currentDateTime = new Date (System.currentTimeMillis());
		String mySQLFormatedCurrentTime = mySQLFormatDateTime.format(currentDateTime);
		String query = "SELECT * FROM meeting_requests WHERE propositioner= '" + me +"' OR approver = '" + me + "' AND approved='1' AND expiry < '" + mySQLFormatedCurrentTime + "'";
		ResultSet result = null;
		try {
			result = statement.executeQuery(query);
		} catch (SQLException e) {
			errorOut.println("SELECT query failed");
			response.sendError(500,"SELECT query failed");
			return;
		}
		ArrayList<MeetingRequest> requests = new ArrayList<MeetingRequest>();
		try {
			while(result.next()) {
				int prop = Integer.parseInt(result.getString("propositioner"));
				int app = Integer.parseInt(result.getString("approver"));
				int other = -1;
				if (prop == me) {
					other = app;
				} else {
					other = prop;
				}
				int duration = -1;
				try {
					duration = Integer.parseInt(result.getString("duration"));
				} catch (NumberFormatException | SQLException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				String expiryStr = null;
				try {
					expiryStr = result.getString("expiry");
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				Date expiry = null;
				try {
					expiry = mySQLFormatDateTime.parse(expiryStr);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				requests.add(new MeetingRequest(me, other, duration, expiry));
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<String> jsonArray = new ArrayList<String>();
		
		for (MeetingRequest meetingRequest : requests) {
			TimeSegmentCollection myTimeSegments = new TimeSegmentCollection(meetingRequest.getMe());
			TimeSegmentCollection otherTimeSegments = new TimeSegmentCollection(meetingRequest.getOther());
			int duration = meetingRequest.getDuration();
			HashMap<ListClass, LinkedList<TimeSegment>> map = myTimeSegments.getSegregatedFreeList();
			for (ListClass lc : map.keySet()) {
				if (lc.isInBounds(duration)) {
					LinkedList<TimeSegment> listOfInterest = map.get(lc);
					for (TimeSegment timeSegment : listOfInterest) {
						TimeSegment match = otherTimeSegments.findSegment(timeSegment.getStart(), duration);
						if (match == null) {
							continue;
						} else {
							jsonArray.add("{ \"start\": \"" + timeSegment.getStart() + ", \"duration\": \"" + duration + "\"}");
							break;
						}
					}
					break;
				}
			}
			break;
		}
		if (jsonArray.isEmpty()) {
			response.setStatus(204);
			return;
		}
		
		String jsonStr = "{\n" +
							"\t \"suggestions\": [\n";
		for (int i = 0; i < jsonArray.size() - 1; i++) {
			jsonStr.concat(jsonArray.get(i) + ",\n");
		}
		jsonStr.concat(jsonArray.get(jsonArray.size() - 1) + "\n");
		jsonStr.concat("\t]");
		jsonStr.concat("}");
		responseOut.append(jsonStr);
		response.setStatus(200);
		return;
		
		
	}
	public class MeetingRequest {
		private int me;
		private int other;
		private int duration;
		private Date expiry;
		
		public MeetingRequest(int me,int other,int duration,Date expiry) {
			this.me = me;
			this.other = other;
			this.duration = duration;
			this.expiry = expiry;
		}

		public int getMe() {
			return me;
		}

		public int getOther() {
			return other;
		}

		public int getDuration() {
			return duration;
		}

		public Date getExpiry() {
			return expiry;
		}
		
	}
	
	
	public class TimeSegment {
		private Date start;
		private Date end;
		private int user;
		private int state;
		public TimeSegment(int user, Date start, Date end, int state) {
			this.start = start;
			this.end = end;
			this.state = state;
			this.user = user;
		}
		public Date getStart() {
			return start;
		}
		public Date getEnd() {
			return end;
		}
		public int getState() {
			return state;
		}
		public int getUser() {
			return user;
		}
		public int getDifference() {
			long endMillis = end.getTime();
			long startMillis = start.getTime();
			long diffMillis = endMillis - startMillis;
			long diffMinutes = diffMillis / 60000;
			return (int)diffMinutes;
		}
		public boolean isInRange(Date start, int duration) {
			long myStart = this.start.getTime();
			long myEnd = this.end.getTime();
			long thatStart = start.getTime();
			return (thatStart > myStart && thatStart + duration < myEnd);
		}
	}
	public class TimeSegmentCollection {
		private HashMap<ListClass, LinkedList<TimeSegment>> segregatedFreeList;
		private int user;
		public TimeSegmentCollection(int user) {
			ListClass class0 = new ListClass (0,15);
			ListClass class1 = new ListClass (15,30);
			ListClass class2 = new ListClass (30,60);
			ListClass class3 = new ListClass (60,120);
			ListClass class4 = new ListClass (120,300);
			ListClass class5 = new ListClass (300,Integer.MAX_VALUE);
			
			this.user = user;
			segregatedFreeList.put(class0, null);
			segregatedFreeList.put(class1, null);
			segregatedFreeList.put(class2, null);
			segregatedFreeList.put(class3, null);
			segregatedFreeList.put(class4, null);
			segregatedFreeList.put(class5, null);
			try {
				calculateFreeList();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public void calculateFreeList() throws SQLException, ClassNotFoundException, ParseException {
			Connection connection = null;
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/TickTock", "root", "");
			Statement statement = connection.createStatement();
			SimpleDateFormat mySQLFormatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date currentTime = new Date(System.currentTimeMillis());
			String currentMySQLFormatted = mySQLFormatDateTime.format(currentTime); 
			
			String query = "SELECT * FROM user_states WHERE user_id= '" + user + "' AND endTime > '" + currentMySQLFormatted + "'";
			ResultSet result = statement.executeQuery(query);
			LinkedList<TimeSegment> allSegments = new LinkedList<TimeSegment>();
			while(result.next()) {
				String startDate = result.getString("state_start");
				String endDate = result.getString("state_end");
				Date start = mySQLFormatDateTime.parse(startDate);
				Date end = mySQLFormatDateTime.parse(endDate);
				int userID = Integer.parseInt(result.getString("user_id"));
				int state = Integer.parseInt(result.getString("state"));
				TimeSegment seg = new TimeSegment(userID,start,end,state);
				allSegments.add(seg);
			}
			for (ListClass listClass : segregatedFreeList.keySet()) {
				segregatedFreeList.replace(listClass, new LinkedList<TimeSegment>());
			}
			for (TimeSegment timeSegment : allSegments) {
				if (timeSegment.getState() == 2) {
					addSegment(timeSegment);
				}
			}
		}
		public HashMap<ListClass,LinkedList<TimeSegment>> getSegregatedFreeList() {
			return this.segregatedFreeList;
		}
		
		private void addSegment(TimeSegment t) {
			for (ListClass listClass : segregatedFreeList.keySet()) {
				if (listClass.isInBounds(t)) {
					addToList(listClass,t);
				}
			}
		}
		private void addToList(ListClass lc, TimeSegment t) {
			LinkedList<TimeSegment> list = segregatedFreeList.get(lc);
			list.addLast(t);
		}
		public TimeSegment findSegment(Date start,int duration) {
			for (ListClass listClass : segregatedFreeList.keySet()) {
				if (listClass.isInBounds(duration)) {
					LinkedList<TimeSegment> listOfInterest = segregatedFreeList.get(listClass);
					for (TimeSegment timeSegment : listOfInterest) {
						if (timeSegment.isInRange(start, duration)) {
							return timeSegment;
						}
					}
				}
			}
			return null;
		}
		
	}
	public class ListClass {
		private int lowerBound;
		private int upperBound;
		public ListClass(int lowerBound, int upperBound) {
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}
		public int getLowerBound() {
			return lowerBound;
		}
		public int getUpperBound() {
			return upperBound;
		}
		public boolean isInBounds(TimeSegment t) {
			return lowerBound <= t.getDifference() && t.getDifference() < upperBound;
		}
		public boolean isInBounds (int duration) {
			return lowerBound <= duration && duration < upperBound;
		}
	}

}
