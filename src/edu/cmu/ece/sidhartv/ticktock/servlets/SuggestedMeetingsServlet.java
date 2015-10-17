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
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
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
	}

}
