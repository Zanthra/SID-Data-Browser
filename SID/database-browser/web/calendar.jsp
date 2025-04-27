<%@ page import="java.text.*,edu.stanford.sid.util.*,edu.stanford.sid.database.*,edu.stanford.sid.*,java.util.*" buffer="32kb" autoFlush="false" %>

<%-- NOTE: If this code is still being maintained in 2020, increase the buffer size
above to prevent the buffer from overfilling with empty years. --%>

<%-- This code displays a page of dates, working backwards from the present all the
way to the end date specified in the static variables below, which contain data files.
The code to generates the calendar should be though of as a backwards timeline, as
long as an iterator is after the end, we do the work for that year or month, and then
step back by one.--%>

<%!//Change these to change the last date the calendar will check for data files.
private static final int END_YEAR = 2003;
private static final int END_MONTH = 6;
private static final int END_DAY = 1;

//Prefix to the browser jsp page.
private static final String BROWSE_URL =   "browse.jsp";

//Date format for the query string.
private static DateFormat DATE_FORMAT = CalendarUtil.getSimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss");

//Date format for the month display.
private static DateFormat MONTH_FORMAT = CalendarUtil.getSimpleDateFormat("MMM");


/**
 * Initialize the JSP page, and get a reference to the DataFileIndex.
 */
public void jspInit()
{
	
}

/**
 * This method creates a link to the browser for a given set of monitors and a time.
 */
private String getBrowseHref(Collection<MonitorInfo> monitors, Calendar time)
{
	//String href = String.format("%s?date=%s&display=superimposed&timeRange=1440&size=1300x300", BROWSE_URL, DATE_FORMAT.format(time.getTime()));
	String href = String.format("%s?date=%s", BROWSE_URL, DATE_FORMAT.format(time.getTime()));

	if(monitors != null)
	{
		for(MonitorInfo monitor : monitors)
		{
			href = String.format("%s&monitor=%s", href, monitor.getIdentifier());
		}
	}
	
	return href;
}%><%
	DataFileIndex index = DataFileIndex.getInstance(this.getServletContext());
//String[] monitors = request.getParameterValues("monitor");

Collection<MonitorInfo> monitors = Collections.emptyList();
Collection<MonitorInfo> monitorsForBrowser = Collections.emptyList();

boolean monitorsFromQueryString = false;

//If someone has linked to a page of the database browser, this code will convert the monitors into
//MonitorInfo objects.  Ohterwise we will load the monitors from the user's session.
if(request.getParameter("monitor") != null)
{
	monitorsFromQueryString = true;
	ArrayList<MonitorInfo> monitorList = new ArrayList<MonitorInfo>();
	Collection<MonitorInfo> monitorsFromDatabase = index.getMonitors(MonitorComparators.MONITOR);
	for(String s : request.getParameterValues("monitor"))
		for(MonitorInfo mi : monitorsFromDatabase)
	if(mi.getIdentifier().equalsIgnoreCase(s) || mi.getMonitor().equalsIgnoreCase(s))
		monitorList.add(mi);
	monitors = monitorList;
	monitorsForBrowser = monitors;
}
else if(request.getSession().getAttribute("monitors") != null)
	monitors = (Collection<MonitorInfo>)request.getSession().getAttribute("monitors");
%>
<HTML>
<head>

<title>SID Data Access</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<META NAME="ROBOTS" CONTENT="NOFOLLOW">

<script type="text/javascript">

  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-19047793-3']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();

</script>
</head>

<body bgcolor="#FFFFFF" text="#000000">
<CENTER>
<A HREF="http://solar-center.stanford.edu/SID">
<IMG SRC="http://solar-center.stanford.edu/images/swtopsmall.jpg" border=0></a>
</CENTER>

<%
	java.util.Collection<Calendar> daysWithData = index.getDaysWithData(monitors);
if(daysWithData.size() == 0)
{
%>
<H1>No DATA for selected monitor.</H1>
<%
	}
%>

<table border width="100%" cellspacing=0 cellpadding=5>

<%
	//Flush the buffer to clear the static header above.
out.flush();

//Get the start and end times for the calendar.
Calendar start = CalendarUtil.getDateResolution(CalendarUtil.getCalendar());
Calendar end = CalendarUtil.getCalendar(END_YEAR, END_MONTH, END_DAY);

int timeout = 1000;
while(!daysWithData.contains(start) && start.after(end) && timeout-- > 0)
{
	start.add(Calendar.DATE, -1);
}

//Create calendars representing the begining of the month, and the end of the month.
Calendar monthIteratorStart = CalendarUtil.getMonthResolution(start);
Calendar monthIteratorEnd = CalendarUtil.duplicate(monthIteratorStart);
monthIteratorEnd.add(Calendar.MONTH, 1);

//Create calendars representing the begining and the end of each year.
Calendar yearIteratorStart = CalendarUtil.getYearResolution(monthIteratorStart);
Calendar yearIteratorEnd = CalendarUtil.duplicate(yearIteratorStart);
yearIteratorEnd.add(Calendar.YEAR, 1);

//While the year is after the end time.
timeout = 100000;
while(yearIteratorEnd.after(end) && timeout-- > 0)
{	//Display the year.
%>
		<tr bgcolor=aqua>
		<th colspan=2 align=center>
		<H2><%=yearIteratorStart.get(Calendar.YEAR)%></H2>
		</th>
		</tr>
	<%
		//Now work through the months.
		//While the month is after the start of the year.
		while(monthIteratorEnd.after(yearIteratorStart))
		{	//Display the month.
	%>
			<tr>
			<td>
			<%=MONTH_FORMAT.format(monthIteratorStart.getTime())%>
			</td>
			<td>
		<%
			//Now we go forward through the dates, so we start with the start of the month.
				Calendar dateIteratorStart = CalendarUtil.duplicate(monthIteratorStart);
				Calendar dateIteratorEnd = CalendarUtil.duplicate(dateIteratorStart);
				dateIteratorEnd.add(Calendar.DATE, 1);
				
				//While it is before the end of the month.
				while(dateIteratorStart.before(monthIteratorEnd))
				{
			//If we have files for the given day.
			if(daysWithData.contains(dateIteratorStart))//index.hasFilesInRange(dateIteratorStart, dateIteratorEnd, monitors))
			{	//Display the day.
		%>
					<A href="<%=getBrowseHref(monitorsForBrowser, dateIteratorStart)%>"><%=dateIteratorStart.get(Calendar.DATE)%></A><%
				//And flush the buffer to display all previously undisplayed text.
				out.flush();
			}
			//Done with the date.
			
			//Step forward one date.
			dateIteratorStart.add(Calendar.DATE, 1);
			dateIteratorEnd.add(Calendar.DATE, 1);
		}
		//Done with month, close the table's block.
		%>
			</td>
			</tr>
		<%
		
		//Step the month backward by one.
		monthIteratorStart.add(Calendar.MONTH, -1);
		monthIteratorEnd.add(Calendar.MONTH, -1);
	}
	//Done with the year.
	//Step the year backward by one.
	yearIteratorStart.add(Calendar.YEAR, -1);
	yearIteratorEnd.add(Calendar.YEAR, -1);
}

//Clear the buffer of any months and years that did not have days associated with them.
out.clearBuffer();

%>

</td>
</tr>
</table>
</body>
</html>
