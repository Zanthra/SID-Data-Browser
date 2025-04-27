<%@ page import="java.text.*,java.util.*,edu.stanford.sid.util.*,edu.stanford.sid.database.*,edu.stanford.sid.*,edu.stanford.sid.eds.GoesFlareStrength" %>
<%!private static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH.mm.ss";

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

//Graph sizes and text.
private static final int[] xresolutions =       { 1000,    1300,     1600 };
private static final int[] yresolutions =       { 200,     300,      375 };
private static final String[] resolutionNames = { "Small", "Medium", "Large" };

//Graph time ranges and text.
private static final int[] graphLengths =        { 1 * HOUR, 6 * HOUR,  1 * DAY, 3 * DAY };
private static final String[] graphLengthNames = { "1 Hour", "6 Hours", "1 Day", "3 Days" };

//
private static final String[] goesStrengths =     {  "A1.0",              "C1.0",           "M1.0",            "X1.0" };
private static final String[] goesStrengthNames = { "(A1.0) All Flares", "(C1.0) Visible", "(M1.0) Distinct", "(X1.0) Strong" };


private static final int END_YEAR = 2003;
private static final int END_MONTH = 6;
private static final int END_DAY = 1;

public void jspInit()
{}

/**
 * Returns a link to the plot servlet for displaying plot images, if the plot parameter
 * is false, instead this will return an href to retrieve the data files.
 */
private static String getPlotSrc(Calendar startTime, Calendar endTime,
		int xres, int yres, Collection<MonitorInfo> monitors, boolean plot,
		String tz, boolean mss, boolean sss, boolean goes, String goesFlareStrength)
{
	StringBuilder string = new StringBuilder();
	
	if(plot)
	{
		string.append(PLOT_URL);
	}else
	{
		string.append(RETRIEVE_URL);
	}
	
	DateFormat dateFormat =  CalendarUtil.getSimpleDateFormat(DATE_FORMAT_STRING);
	
	string.append(String.format("?starttime=%s&endtime=%s&res=%dx%d&TZ=%s&mss=%b&sss=%b&goes=%b&goesFlareStrength=%s",
			dateFormat.format(startTime.getTime()), dateFormat.format(endTime.getTime()), xres, yres, tz, !mss, !sss, !goes, goesFlareStrength));
	
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
}

/**
 *
 */
private String clearMonitorsLink(String queryString)
{
	if(queryString == null)
	{
		queryString = "";
	}
	
	return String.format("%s?%s", BROWSE_URL, queryString.replaceAll("&?monitor=[^&]*", ""));
}

private String getPageLink(Collection<MonitorInfo> monitors, String queryString)
{
	if(queryString == null)
	{
		queryString = "";
	}
	
	queryString = queryString.replaceAll("&?monitor=[^&]*", "");
	
	for(MonitorInfo mi : monitors)
	{
		queryString = queryString.concat("&monitor=" + mi.getIdentifier());
	}
	
	return String.format("%s?%s", BROWSE_URL, queryString);
}

private String getCalendarLink(Collection<MonitorInfo> monitors)
{
	String value = "calendar.jsp?";
	for(MonitorInfo mi : monitors)
	{
		value += String.format("&monitor=%s", mi.getIdentifier());
	}
	
	return value;
}%><%

Locale locale = request.getLocale();

if(request.getParameter("language") != null)
{
	locale = Locale.forLanguageTag(request.getParameter("language"));
}

response.setContentType("text/html; charset=UTF-8");
response.setCharacterEncoding("UTF-8");

ResourceBundle localBundle = ResourceBundle.getBundle("browse", locale);

String[] revTextLocal = { "3 " + localBundle.getString("message.days"),
		"1 "+localBundle.getString("message.day"),
		"6 "+localBundle.getString("message.hours"),
		"1 "+localBundle.getString("message.hour"),
		"15 "+localBundle.getString("message.minutes") };
String[] fwdTextLocal = { "15 "+localBundle.getString("message.minutes"),
		"1 "+localBundle.getString("message.hour"),
		"6 "+localBundle.getString("message.hours"),
		"1 "+localBundle.getString("message.day"),
		"3 "+localBundle.getString("message.days") };
String[] resolutionNamesLocal = { localBundle.getString("message.small"),
		localBundle.getString("message.medium"),
		localBundle.getString("message.large") };
String[] graphLengthNamesLocal = { "1 "+localBundle.getString("message.hour"),
		"6 "+localBundle.getString("message.hours"),
		"1 "+localBundle.getString("message.day"),
		"3 "+localBundle.getString("message.days") };
String[] goesStrengthNamesLocal = { localBundle.getString("message.A10"),
		localBundle.getString("message.C10"),
		localBundle.getString("message.M10"),
		localBundle.getString("message.X10") };

	//Begin by initializing default variables.
DataFileIndex index = DataFileIndex.getInstance(this.getServletContext());
Calendar startTime = null;
Collection<MonitorInfo> monitors = Collections.emptyList();
boolean superimposed = false;
int xres = 1000;
int yres = 200;
TimeZone localTimeZone = TimeZone.getTimeZone("GMT");
DateFormat formatForLocalTimeZone = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
formatForLocalTimeZone.setTimeZone(TimeZone.getTimeZone("GMT"));
int minutes = 1 * DAY;
boolean mss = false;
boolean sss = false;
boolean goes = false;
boolean monitorsFromQueryString = false;
String goesFlareStrength = "C1.0";
DateFormat dateFormat =  CalendarUtil.getSimpleDateFormat(DATE_FORMAT_STRING);


//Parse the query string for variables the user provides.
if(request.getParameter("date") != null)
{
	startTime = CalendarUtil.getCalendar();
	startTime.setTime(dateFormat.parse(request.getParameter("date")));
}

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
}
else if(request.getSession().getAttribute("monitors") != null)
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

if(request.getParameter("goesFlareStrength") != null)
{
	try
	{
		goesFlareStrength = new GoesFlareStrength(request.getParameter("goesFlareStrength")).toString();
	}catch(Exception e){}
}
%><%
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

ArrayList<MonitorInfo> newMonitorInfo = new ArrayList<MonitorInfo>();

for(MonitorInfo mi : monitors)
{
	if(mi.getStation().equalsIgnoreCase("multiple"))
		for(MonitorInfo mi2 : list.getMonitorInfo())
		{
	if(mi.getSite().equalsIgnoreCase(mi2.getSite())
			&& mi.getMonitor().equalsIgnoreCase(mi2.getMonitor()))
		newMonitorInfo.add(mi2);
		}
	else
		newMonitorInfo.add(mi);
}

monitors = newMonitorInfo;

ArrayList<String> images = new ArrayList<String>();
ArrayList<String> identifiers = new ArrayList<String>();

//Generate the image srcs.
if(superimposed)
{
	images.add(getPlotSrc(startTime, endTime, xres, yres, monitors, true, localTimeZone.getID(), mss, sss, goes, goesFlareStrength));
	identifiers.add("superimposed");
}else
{
	DataFileList selectedList = list;
	if(monitors.size() > 0)
	{
		selectedList = list.filterByMonitors(monitors);
	}
	
	ArrayList<MonitorInfo> sortedMonitors = new ArrayList<MonitorInfo>();
	sortedMonitors.addAll(selectedList.getMonitorInfo());
	Collections.sort(sortedMonitors, MonitorComparators.MONITOR);
	
	for(MonitorInfo monitor : sortedMonitors)
	{
		Collection<MonitorInfo> temp = Collections.singleton(monitor);
		images.add(getPlotSrc(startTime, endTime, xres, yres, temp, true, localTimeZone.getID(), mss, sss, goes, goesFlareStrength));
		identifiers.add(monitor.toString());
	}
}

//Create the timezone list.
TreeMap<String, String> timeZones = new TreeMap<String, String>();
for(String id : TimeZone.getAvailableIDs())
{
	timeZones.put(TimeZone.getTimeZone(id).getDisplayName(request.getLocale()), id);
}
%>


<%-- This begins the portion of code that represents the view, and it is safe to edit
this HTML to make changes to the page.  Before making said changes, make sure you look
at how the localization is done on some of the strings. Consider how your change will
affect the look of any localized versions of the page.  --%>
<html>
<head>
<title>SID Data Access</title>
<!--<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">-->

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

<A HREF="http://solar-center.stanford.edu/SID"><IMG SRC="http://solar-center.stanford.edu/images/swtopsmall.jpg" border=0/></a><br/>
<font size="small"><A href="browse.jsp?language=en">English</A>&nbsp;&nbsp;<A href="browse.jsp?language=es"><%=localBundle.getString("message.spanish")%></A></font><br/>


<A HREF="http://solar-center.stanford.edu/SID/data/data-access.html"><%=localBundle.getString("message.about")%></A>&nbsp;&nbsp;&nbsp;<A href="http://sid.stanford.edu/database-browser/linking.jsp">Linking to this Page</A>


</CENTER>

<%-- Display an informative method if the index is not done updating. --%>
<% if(!index.isUpdateComplete())
{%><font color="#A00000">Not all data may be currently available.  Try again in a few minutes.</font><br>
<%}%>


<form name="Config" method="GET" action="<%=BROWSE_URL%>">
<%if(monitorsFromQueryString){for(MonitorInfo mi : monitors) {%>
<input type="hidden" name="monitor" value="<%=mi.getIdentifier()%>"/>
<%}} %>
<input type="hidden" name="date" value="<%=dateFormat.format(startTime.getTime())%>"/>
<%if(request.getParameter("language") != null) {%><input type="hidden" name="language" value="<%=request.getParameter("language")%>"/><%}%>

<table id="optionstable" width=100% border=1>
<tr>
<td>

<input type="radio" name="display" value="superimposed"<%if(superimposed) {%> CHECKED<%}%>><%=localBundle.getString("message.superimposed") %><br>
<input type="radio" name="display" value="vertical"<%if(!superimposed) {%> CHECKED<%}%>><%=localBundle.getString("message.vertical") %><p>
<input type="checkbox" name="mss" value="true"<%if(mss) {%> CHECKED<%}%>><%=localBundle.getString("message.monitorsunrisesunset")%><br>
<input type="checkbox" name="sss" value="true"<%if(sss) {%> CHECKED<%}%>><%=localBundle.getString("message.stationsunrisesunset")%><p>
<input type="checkbox" name="goes" value="true"<%if(goes) {%> CHECKED<%}%>><%=localBundle.getString("message.solarflares") %><br/>

<select name="goesFlareStrength">
<%if(!Arrays.asList(goesFlareStrength).contains(goesFlareStrength)){ %>
<option value="<%=goesFlareStrength%>"><%=goesFlareStrength%></option>
<%} %>
<% for(int i = 0;i < goesStrengths.length;i++)
{%><option value="<%=goesStrengths[i]%>" <%if(goesStrengths[i].equals(goesFlareStrength)) { %>SELECTED<%} %>><%=goesStrengthNames[i]%></option>
<%}%>
</select><%=localBundle.getString("message.minflare") %>
<P/>
<A href="<%=getPlotSrc(startTime, endTime, xres, yres, monitors, false, localTimeZone.getID(), false, false, false, goesFlareStrength)%>"><%=localBundle.getString("message.download") %></A><p/>



<h1 align=right><input type="submit" value="<%=localBundle.getString("message.update")%>">&nbsp;&nbsp;</h1>

</td>
<td>
<B><A href="<%if(monitorsFromQueryString) { %><%=getCalendarLink(monitors) %><%} else { %>calendar.jsp<%} %>"><%=localBundle.getString("message.calendar") %></A></b>
<br/><B><A href="monitorsbytime.jsp"><%=localBundle.getString("message.eventsearch") %></A></B>
<br/><B><A href="http://solar-center.stanford.edu/SID/map"><%=localBundle.getString("message.map") %></A></b>
<P/>
<%if(monitorsFromQueryString) { %>
<a href="<%=clearMonitorsLink(request.getQueryString()) %>"><font color="red"><%=localBundle.getString("message.override") %></font></a><br/>
<%} else {%>
<B><a
  href="monitor_session_popup.jsp"
  target="_blank"
  onClick="window.open(href, 'Add Monitors', 'width=750,height=600,scrollbars=yes'); return false"><%=localBundle.getString("message.monitors") %></a><br/>
<a href="AddMonitorToSession?clear"
 target="_blank"
 onClick="window.open(href, 'Clear Monitors', 'width=0,height=0,scrollbars=no'); return false"><%=localBundle.getString("message.clear") %></a></B><br/>
<%} %>
<%=localBundle.getString("message.selected") %><br/>
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


</td>
<td valign=top>
<B><%=localBundle.getString("message.graphlength") %></B><br>
<%-- Display the graph length selector. --%>
<% for(int i = 0;i < graphLengths.length;i++)
{%><input type="radio" name="timeRange" value="<%=graphLengths[i]%>"<% if(minutes == graphLengths[i])
{%> CHECKED<%}%>><%=graphLengthNamesLocal[i]%><br>
<%}%>
<P/>
<B><%=localBundle.getString("message.graphsize") %></B><br>
<%-- Display the graph size selection. --%>
<% for(int i = 0;i < xresolutions.length;i++)
{%><input type="radio" name="size" value="<%=xresolutions[i]%>x<%=yresolutions[i]%>"<%if(xres == xresolutions[i] && yres == yresolutions[i])
{ %> CHECKED<%}%>><%=resolutionNamesLocal[i]%><br>
<%}%>

</td>
</tr>


<tr>
<td colspan="3">
<TABLE> <TR> <TD valign=top>
      <input type="button" name="button" value="What is happening right now on the Sun?" onclick="window.open('http://www.sec.noaa.gov/rt_plots/xray_5m.html')">

	    <BR>&nbsp; &nbsp; <small><i>(Real-time X-ray flare satellite data)</i></small>
	    </TD>

	 <TD valign=top>
	    <input type="button" name="button" value="Current Solar Images" onclick="window.open('http://sdo.gsfc.nasa.gov/data/')">

    <TD valign=top>
        <input type="button" name="button" value="Access GOES Flare Catalog" onclick="window.open('http://www.sec.noaa.gov/ftpmenu/indices/events.html')">

	</TD> </TR> </TABLE>
</td>
</tr>

</table>

</form>



<center>
<%-- Display the forward and back links. --%>
<B><%=localBundle.getString("message.back") %>:</b> <% for(int i = 0;i < rev.length;i++)
{%><A href="<%=createNavigationLink(startTime, rev[i], request.getQueryString())%>"><%=revTextLocal[i]%></A> |
<%}%>
 &nbsp;&nbsp; <B><%=localBundle.getString("message.forward") %>:</B> <% for(int i = 0;i < fwd.length;i++)
{%><A href="<%=createNavigationLink(startTime, fwd[i], request.getQueryString())%>"><%=fwdTextLocal[i]%></A> |
<%}%><br>

<B><a href="browse.jsp"><%=localBundle.getString("message.latest") %></a><br/></B>
<%-- Display the start time of the graph in the user's local time zone and locale. --%>
<H2><%=localBundle.getString("message.start") %> <%= formatForLocalTimeZone.format(startTime.getTime()) %><% if (request.getParameter("date") == null) {%> (Latest) <%} %></H2><p/>
</center>
<table border=1>

<%-- Display the graphs. --%>
<% if(list.filterByMonitors(monitors).isEmpty())
{ %> <tr>
		<td bgcolor="#FF0000"><H1><%=localBundle.getString("message.nodata") %></H1></td>
	 </tr>
<%} else {
	for (int i = 0;i < images.size();i++)
	{
		String image = images.get(i);
		String identifier = identifiers.get(i);%>
		<tr>
			<td>
			<div style="width:<%=xres%>;height:<%=yres%>;overflow:hidden;">
			<font style="position:relative;width:<%=xres%>;height:<%=yres%>;"><span style="position:absolute;width:<%=xres%>;height:<%=yres%>;"><img src="<%=image%>"/></span><%=identifier%></font>
			</div>
			</td>
		</tr>
<%	}
}%>


</table>

<%-- Display a link to retrieve the data files. --%>
<P><A href="<%=getPlotSrc(startTime, endTime, xres, yres, monitors, false, localTimeZone.getID(), false, false, false, goesFlareStrength)%>"><Font size=+1><B><%=localBundle.getString("message.retrieve")%></b></A></font><br/>
<A href="<%=getPageLink(monitors, request.getQueryString()) %>"><%=localBundle.getString("message.createlink")%></A><P/>


<P>




</body>
</html>
