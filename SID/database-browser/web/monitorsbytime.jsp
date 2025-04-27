<%@ page import="java.util.*,edu.stanford.sid.database.*,edu.stanford.sid.util.*,java.text.DateFormat,edu.stanford.sid.graphing.SunriseSunset" %>

<%!private static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH.mm.ss";
private static final String MONITORS_BY_TIME_SRC = "monitorsbytime.jsp";

public void jspInit()
{

}

private String createBrowseLink(MonitorInfo info, Calendar date)
{
	return String.format("browse.jsp?monitor=%s&date=%s", info.getIdentifier(), CalendarUtil.getSimpleDateFormat(DATE_FORMAT_STRING).format(date.getTime()));
}%>

<%
	DateFormat dateFormat = CalendarUtil.getSimpleDateFormat(DATE_FORMAT_STRING);
DataFileIndex index = DataFileIndex.getInstance(this.getServletContext());

Calendar date = CalendarUtil.getCalendar();

//Parse the query string for variables the user provides.
if(request.getParameter("date") != null)
{
	try
	{
		date.setTime(dateFormat.parse(request.getParameter("date")));
	}catch(Exception e)
	{
		//If we have any problems parsing the date, reset to current.
		date = CalendarUtil.getCalendar();
	}
}
Calendar nextDay = CalendarUtil.duplicate(date);
nextDay.add(Calendar.DATE, 1);
Calendar previousDay = CalendarUtil.duplicate(date);
previousDay.add(Calendar.DATE, -1);

Calendar dateStart = CalendarUtil.getDateResolution(date);
Calendar dateEnd = CalendarUtil.duplicate(dateStart);
dateEnd.add(Calendar.DATE, 1);

Collection<MonitorInfo> monitors = index.getFilesFromTimeRange(date, nextDay).getMonitorInfo();
ArrayList<MonitorInfo> monitorsInDaytime = new ArrayList<MonitorInfo>();

for(MonitorInfo mi : monitors)
{
	StationLatitudeLongitude sll = index.getStationLatitudeLongitude(mi.getStation());
	
	if(sll == null || new SunriseSunset(sll.latitude, sll.longitude, date, 0).isDaytime())
	{
		if(new SunriseSunset(mi.getLatitude(), mi.getLongitude(), date, 0).isDaytime())
		{
	monitorsInDaytime.add(mi);
		}
	}
}
%>

<head>
<title>SID Data Access</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>

<body bgcolor="#FFFFFF" text="#000000">
<CENTER>
<A HREF="http://solar-center.stanford.edu/SID">
<IMG SRC="http://solar-center.stanford.edu/images/swtopsmall.jpg" border=0/></a>
</CENTER>

<H1>Site Finder</H1><br/>
Find a site in daytime by entering the time of an event in the box below and selecting "Change Date".
The monitors listed below will be monitors that are in daytime during the given time.
<P/>
<form action="monitorsbytime.jsp" method="get">
<input type="submit" value="Change Date"/>

<input type="text" name="date" value="<%=dateFormat.format(date.getTime()) %>"/>

</form><p/>

<form action="browse.jsp" method="get">
<input type="submit" value="Select Monitors"/>
<input type="hidden" name="date" value="<%=dateFormat.format(dateStart.getTime()) %>"/>
<table rules="all" border="box" cellpadding=2>
<tr><td>Select</td><td>Site</td><td>Monitor</td><td>Station</td><td></td></tr>

<%

for(MonitorInfo info : monitorsInDaytime)
{
%><tr><td><input type="checkbox" name="monitor" value="<%=info.getIdentifier()%>"></td><td><%=info.getSite()%></td><td><%=info.getMonitor()%></td><td><%=info.getStation()%></td><td><A href="<%=createBrowseLink(info, dateStart)%>">Data</A></td></tr><%
}%>

</table>
</form>
</body>

