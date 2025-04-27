<%@ page import="java.text.*,java.util.*,edu.stanford.sid.util.*,edu.stanford.sid.database.*,edu.stanford.sid.*" %>
<%!//private static final DateFormat DATE_FORMAT = CalendarFactory.getSimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss");
private static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH.mm.ss";

//
// These static variables can be changed in order to make different options available.
// The text in variables below can contain html such as img tags, this lets you display
// images instead of text as the labels.

//Prefixes for links.
private static final String PLOT_URL =     "plot";
private static final String RETRIEVE_URL = "retrieve";
private static final String BROWSE_URL =   "browse.jsp";

//Constants for time durations.
private static final int MINUTE = 1;
private static final int HOUR =   60 * MINUTE;
private static final int DAY =    24 * HOUR;

//Values and captions for the forward and reverse links.
private static final int[] rev =        { -3 * DAY, -1 * DAY, -6 * HOUR, -1 * HOUR, -15 * MINUTE };
private static final String[] revText = { "3 Days", "1 Day",  "6 Hours", "1 Hour",  "15 Minutes" };

private static final int[] fwd =        { 15 * MINUTE,  1 * HOUR, 6 * HOUR,  1 * DAY, 3 * DAY };
private static final String[] fwdText = { "15 Minutes", "1 Hour", "6 Hours", "1 Day", "3 Days" };

//Graph sizees and text.
private static final int[] xresolutions =       { 1000,     1300,     1600 };
private static final int[] yresolutions =       { 200,     300,      375 };
private static final String[] resolutionNames = { "Small", "Medium", "Large" };

//Graph time ranges and text.
private static final int[] graphLengths =        { 1 * HOUR, 6 * HOUR,  1 * DAY, 3 * DAY };
private static final String[] graphLengthNames = { "1 Hour", "6 Hours", "1 Day", "3 Days" };

private static final int END_YEAR = 2003;
private static final int END_MONTH = 6;
private static final int END_DAY = 1;

//private DataFileIndex index;

public void jspInit()
{
	//index = DataFileIndex.getInstance(this.getServletContext());
}

/**
 * Returns a link to the plot servlet for displaying plot images, if the plot parameter
 * is false, instead this will return an href to retrieve the data files.
 */
private static String getPlotSrc(Calendar startTime, Calendar endTime, int xres, int yres, Collection<MonitorInfo> monitors, boolean plot, String tz, boolean mss, boolean sss, boolean goes)
{
	StringBuffer string = new StringBuffer();
	
	if(plot)
	{
		string.append(PLOT_URL);
	}else
	{
		string.append(RETRIEVE_URL);
	}
	
	DateFormat dateFormat =  CalendarUtil.getSimpleDateFormat(DATE_FORMAT_STRING);
	
	string.append(String.format("?starttime=%s&endtime=%s&res=%dx%d&TZ=%s&mss=%b&sss=%b&goes=%b", dateFormat.format(startTime.getTime()), dateFormat.format(endTime.getTime()), xres, yres, tz, !mss, !sss, !goes));
	
	for(MonitorInfo mi : monitors)
	{
		string.append("&monitor=" + mi.getIdentifier());
	}
	
	return string.toString();
}

/**
 * Creates a link to another browser offset by the given number of minutes.
 */
private static String createNavigationLink(Calendar date, int offset, String queryString)
{
	if(queryString == null)
	{
		queryString = "date=";
	} else if(!queryString.matches(".*date=.*"))
	{
		queryString = queryString + "&date=";
	}
	
	 DateFormat dateFormat =  CalendarUtil.getSimpleDateFormat(DATE_FORMAT_STRING);
	
	Calendar temp = CalendarUtil.duplicate(date);
	temp.add(Calendar.MINUTE, offset);
	return String.format("%s?%s", BROWSE_URL, queryString.replaceFirst("date=[^&]*", "date=" + dateFormat.format(temp.getTime())));
}%><%
	//Begin by initializing default variables.
DataFileIndex index = DataFileIndex.getInstance(this.getServletContext());
Calendar startTime = null;
//String[] monitors = {"all"};
Collection<MonitorInfo> monitors = Collections.emptyList();
boolean superimposed = false;
int xres = 1000;
int yres = 200;
TimeZone localTimeZone = TimeZone.getTimeZone("GMT");
DateFormat formatForLocalTimeZone = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, request.getLocale());
formatForLocalTimeZone.setTimeZone(TimeZone.getTimeZone("GMT"));
int minutes = 1 * DAY;
boolean mss = false;
boolean sss = false;
boolean goes = false;
DateFormat dateFormat =  CalendarUtil.getSimpleDateFormat(DATE_FORMAT_STRING);


//Parse the query string for variables the user provides.
if(request.getParameter("date") != null)
{
	startTime = CalendarUtil.getCalendar();
	startTime.setTime(dateFormat.parse(request.getParameter("date")));
}

if(request.getParameter("monitor") != null)
{
	ArrayList<MonitorInfo> monitorList = new ArrayList<MonitorInfo>();
	Collection<MonitorInfo> monitorsFromDatabase = index.getMonitors(MonitorComparators.MONITOR);
	for(String s : request.getParameterValues("monitor"))
		for(MonitorInfo mi : monitorsFromDatabase)
	if(mi.getIdentifier().equalsIgnoreCase(s) || mi.getMonitor().equalsIgnoreCase(s))
		monitorList.add(mi);
	monitors = monitorList;
}
if(request.getSession().getAttribute("monitors") != null)
	monitors = (Collection<MonitorInfo>)request.getSession().getAttribute("monitors");


if(request.getParameter("display") != null)
{
	superimposed = request.getParameter("display").equals("superimposed");
}

if(request.getParameter("time") != null)
{
	localTimeZone = TimeZone.getTimeZone(request.getParameter("time"));
	formatForLocalTimeZone.setTimeZone(TimeZone.getTimeZone(request.getParameter("time")));
}

if(request.getParameter("size") != null)
{
	try
	{
                String[] xyres = request.getParameter("size").split("x");

		xres = Integer.parseInt(xyres[0]);
		yres = Integer.parseInt(xyres[1]);
	}catch(Exception e){xres = 1000;yres=200;}
}

if(request.getParameter("timeRange") != null)
{
	try
	{
		minutes = Integer.parseInt(request.getParameter("timeRange"));
	}catch(Exception e){}
}

if(request.getParameter("mss") != null)
{
	try
	{
		mss = Boolean.parseBoolean(request.getParameter("mss"));
	}catch(Exception e){}
}

if(request.getParameter("sss") != null)
{
        try
        {
                sss = Boolean.parseBoolean(request.getParameter("sss"));
        }catch(Exception e){}
}

if(request.getParameter("goes") != null)
{
	try
	{
		goes = Boolean.parseBoolean(request.getParameter("goes"));
	}catch(Exception e){}
}
%><%
	/*
if(startTime == null)
{
	String[] temp = null;
	if(!monitors[0].equals("all"))
	{
		temp = monitors;
	}
	
	startTime = CalendarFactory.getDateResolution(CalendarFactory.getCalendar());
	Calendar end = CalendarFactory.getCalendar(END_YEAR, END_MONTH, END_DAY);
	
	Calendar stop;
	
	do{
		stop = CalendarFactory.duplicate(startTime);
		startTime.add(Calendar.MINUTE, -1 * minutes);
	}while(end.before(startTime) && !index.hasFilesInRange(startTime, stop, temp));
}
*/

if(startTime == null)
{
	java.util.Collection<Calendar> c = index.getDaysWithData(monitors);
	if(c.size() == 0)
		startTime = CalendarUtil.getDateResolution(CalendarUtil.getCalendar());
	else if(c instanceof java.util.TreeSet)
		startTime = ((java.util.TreeSet<Calendar>)c).last();
	else
		for(Calendar cal : c)
	startTime = cal;
}

//Set up the information needed to create the page.
Calendar endTime = CalendarUtil.duplicate(startTime);
endTime.add(Calendar.MINUTE, minutes);
DataFileList list = index.getFilesFromTimeRange(startTime, endTime);

ArrayList<String> images = new ArrayList<String>();

//Generate the image srcs.
if(superimposed)
{
	images.add(getPlotSrc(startTime, endTime, xres, yres, monitors, true, localTimeZone.getID(), mss, sss, goes));
}else
{
	DataFileList selectedList = list;
	if(monitors.size() > 0)
	{
		selectedList = list.filterByMonitors(monitors);
	}
	
	for(MonitorInfo monitor : selectedList.getMonitorInfo())
	{
		Collection<MonitorInfo> temp = Collections.singleton(monitor);
		images.add(getPlotSrc(startTime, endTime, xres, yres, temp, true, localTimeZone.getID(), mss, sss, goes));
	}
}

try
{
	getServletConfig().getServletContext().getRequestDispatcher("/" + images.get(0)).forward(request, response);
}
catch(Exception e)
{throw new RuntimeException(e);}

//Create the timezone list.
TreeMap<String, String> timeZones = new TreeMap<String, String>();
for(String id : TimeZone.getAvailableIDs())
{
	timeZones.put(TimeZone.getTimeZone(id).getDisplayName(request.getLocale()), id);
}
%>

<%-- This begins the portion of code that represents the view, and it is safe to edit
this HTML to make changes to the page. --%>
<html>
<head>
<title>SID Data Access</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">

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

<BR><A HREF="http://solar-center.stanford.edu/SID/data/data-access.html">About SID DATA</A>

</CENTER>

<%-- Display an informative method if the index is not done updating. --%>
<% if(!index.isUpdateComplete())
{%><font color="#A00000">Not all data may be currently available.  Try again in a few minutes.</font><br>
<%}%>


<form name="Config" method="GET" action="<%=BROWSE_URL%>">
<input type="hidden" name="date" value="<%=dateFormat.format(startTime.getTime())%>">

<%--
<center>
<A href="#" onClick="document.getElementById('optionstable').style.display='none'">Hide Options</A>
&nbsp;&nbsp;&nbsp;<A href="#" onClick="document.getElementById('optionstable').style.display=''">Show Options</A><br/>
</center>
--%>
<table id="optionstable" width=100% border=1>
<tr>
<td>

<%-- Display the time zone list. --%>
<select name="time">
<option value="<%=localTimeZone.getID()%>"><%=localTimeZone.getDisplayName(request.getLocale())%></option>
<% for(String s : timeZones.keySet())
{%><option value="<%=timeZones.get(s)%>"><%=s%></option>
<%}%>
</select><br>
<input type="radio" name="display" value="superimposed"<%if(superimposed) {%> CHECKED<%}%>>Superimposed<br>
<input type="radio" name="display" value="vertical"<%if(!superimposed) {%> CHECKED<%}%>>Vertical<p>
<input type="checkbox" name="mss" value="true"<%if(mss) {%> CHECKED<%}%>>Don't Display Monitor Sunrise and Sunsets (Colored Arrows)<br>
<input type="checkbox" name="sss" value="true"<%if(sss) {%> CHECKED<%}%>>Don't Display Station Sunrise and Sunsets (Black Arrows)<p>
<input type="checkbox" name="goes" value="true"<%if(goes) {%> CHECKED<%}%>><b>Don't</b> show solar flares stronger than C2.0 (From GOES)
<P/>
<A href="<%=getPlotSrc(startTime, endTime, xres, yres, monitors, false, localTimeZone.getID(), false, false, false)%>">Download Data Files</A>

<h1 align=right><input type="submit" value="Update Graph">&nbsp;&nbsp;</h1>

</td>
<td>
<B><A href="calendar.jsp">View Data by Date</A></b>
<br/><B><A href="http://solar-center.stanford.edu/SID/map">Map of Monitors</A></b>
<P/><B><a
  href="monitor_session_popup.jsp"
  target="_blank"
  onClick="window.open(href, 'Add Monitors', 'width=600,height=400,scrollbars=yes'); return false">View Data by Monitor</a><br/>
<a href="AddMonitorToSession?clear"
 target="_blank"
 onClick="window.open(href, 'Clear Monitors', 'width=0,height=0,scrollbars=no'); return false">Clear Monitor Selection</a></B><br/>
Selected Monitors:<br/>
<div style="height:140px; overflow:auto;">
<table>
<% for(MonitorInfo m : monitors)
{%><tr><%if(list.getMonitorInfo().contains(m)) {%>
<td><font color="blue"><%=m.getSite()%></font></td>
<td><font color="blue"><%=m.getMonitor()%></font></td>
<td><font color="blue"><%=m.getStation()%></font></td>
<%} else {%>
<td><%=m.getSite()%></td>
<td><%=m.getMonitor()%></td>
<td><%=m.getStation()%></td>
<%}%></tr>
<%}%>
</table>
</div>
<%--
   <B>&lt; Use Add to the left to select monitors</b>
   <%--<B><A href="monitors.jsp">View Data by Site</A></b>--%>
<%--
--%>

<%-- Display the monitors available for the time range. --%>
<%-- <select name="monitor" align=right multiple size=7> --%>
<%-- <option value="all" <%if(monitors[0].equalsIgnoreCase("all")) {%>selected<%}%>>all</option> --%>
<%-- <% for(String s : list.getMonitors()) --%>
<%--{%><option value="<%=s%>" <%if(Arrays.asList(monitors).contains(s)) {%>selected<%}%>><%=s%></option> --%>
<%-- <%}%> --%>
<%--</select> --%>


<%--<% for(MonitorInfo mi : monitors)
{%><input type="hidden" name="monitor" value="<%=mi.getMonitor()%>">
<%}%>--%>


</td>
<td valign=top>
<B>Graph Length:</B><br>
<%-- Display the graph length selector. --%>
<% for(int i = 0;i < graphLengths.length;i++)
{%><input type="radio" name="timeRange" value="<%=graphLengths[i]%>"<% if(minutes == graphLengths[i])
{%> CHECKED<%}%>><%=graphLengthNames[i]%><br>
<%}%>
<P/>
<B>Graph Size:</B><br>
<%-- Display the graph size selection. --%>
<% for(int i = 0;i < xresolutions.length;i++)
{%><input type="radio" name="size" value="<%=xresolutions[i]%>x<%=yresolutions[i]%>"<%if(xres == xresolutions[i] && yres == yresolutions[i])
{ %> CHECKED<%}%>><%=resolutionNames[i]%><br>
<%}%>

</td>
</tr>
</table>

</form>

<center>
<%-- Display the forward and back links. --%>
<B>Back:</b> <% for(int i = 0;i < rev.length;i++)
{%><A href="<%=createNavigationLink(startTime, rev[i], request.getQueryString())%>"><%=revText[i]%></A> |
<%}%>
 &nbsp;&nbsp; <B>Forward:</B> <% for(int i = 0;i < fwd.length;i++)
{%><A href="<%=createNavigationLink(startTime, fwd[i], request.getQueryString())%>"><%=fwdText[i]%></A> |
<%}%><br>

<B><a href="browse.jsp">Go to Latest Data</a><br/></B>
<%-- Display the start time of the graph in the user's local time zone and locale. --%>
<H2>Graph starts at: <%= formatForLocalTimeZone.format(startTime.getTime()) %></H2><p>
</center>
<table border=1>

<%-- Display the graphs. --%>
<% if(list.filterByMonitors(monitors).isEmpty())
{ %> <tr>
		<td bgcolor="#FF0000"><H1>No data available for the given time range</H1></td>
	 </tr>
<%} else {
	for (String image : images) { %>
		<tr>
			<td>
				<img width=<%=xres%> height=<%=yres%> src="<%=image%>">
			</td>
		</tr>
<%	}
}%>

</table>

<%-- Display a link to retrieve the data files. --%>
<P><A href="<%=getPlotSrc(startTime, endTime, xres, yres, monitors, false, localTimeZone.getID(), false, false, false)%>"><Font size=+1><B>Retrieve Files</b></A></font>


<P>


<TABLE> <TR> <TD valign=top> <form name="GOES" target="new"  method="get"
      action="http://www.sec.noaa.gov/rt_plots/xray_5m.html">
      <input type="submit" name="submit"
            value="What is happening right now on the Sun?">
	    <BR>&nbsp; &nbsp; <i><small>(Real-time X-ray flare satellite data)</i></small>
	    </form>
	    </TD>

	 <TD valign=top>
	    <form name="Sunview" target="new"  method="get"
	          action="http://sunearth.gsfc.nasa.gov/sunearthday/media_viewer/flash.html">
		  <input type="submit" name="submit"
		        value="Current Solar Images">
			</form>
       <TD valign=top> <form name="GOES" target="new"  method="get" action="http://www.lmsal.com/SXT/plot_goes.html">
    <input type="submit" name="goes" value="Access GOES Data Graphs">
    </form></TD>

    <TD valign=top> <form name="GOES" target="new"  method="get"
    action="http://www.sec.noaa.gov/ftpmenu/indices/events.html">
        <input type="submit" name="goes" value="Access GOES Flare Catalog">
	</form>

	</TD> </TR> </TABLE>

</body>
</html>
