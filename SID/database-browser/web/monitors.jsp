<%@ page import="edu.stanford.sid.database.*" %>

<%!

//DataFileIndex index;

private static final String LIST_MONITORS_SRC = "monitors.jsp";

public void jspInit()
{
	//index = DataFileIndex.getInstance(this.getServletContext());
}

private String createBrowseLink(MonitorInfo info)
{
	return String.format("browse.jsp?monitor=%s", info.getMonitor());
}

private String createCalendarLink(MonitorInfo info)
{
	return String.format("calendar.jsp?monitor=%s", info.getMonitor());
}

%>

<%

if("Show Calendar".equals(request.getParameter("submit")))
{
	getServletContext().getRequestDispatcher("/calendar.jsp").forward(request, response);
}
if("Show Data".equals(request.getParameter("submit")))
{
	getServletContext().getRequestDispatcher("/index.jsp").forward(request, response);
}

DataFileIndex index = DataFileIndex.getInstance(this.getServletContext());

String[] monitors = request.getParameterValues("monitor");

java.util.List monitorList = new java.util.ArrayList<String>();
if(monitors != null)
{
	monitorList = java.util.Arrays.asList(monitors);
}

%>

<head>
<title>SID Data Access</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>

<body bgcolor="#FFFFFF" text="#000000">
<CENTER>
<A HREF="http://solar-center.stanford.edu/SID">
<IMG SRC="http://solar-center.stanford.edu/images/swtopsmall.jpg" border=0></a>
</CENTER>

<P>
<BLOCKQUOTE>
<form method="GET" action="<%=LIST_MONITORS_SRC%>">
<input type="submit" name="submit" value="Show Data">
<input type="submit" name="submit" value="Show Calendar"><br>
<table rules="all" border="box" cellpadding=2>
<tr><td>Select</td><td><A href="<%=LIST_MONITORS_SRC%>?sort=site">Site</A></td><td><A href="<%=LIST_MONITORS_SRC%>?sort=location">Location</A></td><td><A href="<%=LIST_MONITORS_SRC%>?sort=monitor">Monitor</A></td><td><A href="<%=LIST_MONITORS_SRC%>?sort=station">Station</A></td><td></td><td></td><!--<td>Calendar</td>--></tr>

<%

for(MonitorInfo info : index.getMonitors(MonitorComparators.getMonitorComparator(request.getParameter("sort"))))
{ if(monitors == null || monitorList.contains(info.getMonitor()))
{
%>
<tr><td><input type="checkbox" name="monitor" value="<%=info.getMonitor()%>"></td><td><%=info.getSite()%></td><td><%=info.getLocation()%></td><td><%=info.getMonitor()%></td><td><%=info.getStation()%></td><td><A href="<%=createCalendarLink(info)%>">Calendar</A></td><td><A href="<%=createBrowseLink(info)%>">Data</A></td></tr><%}}%>

</table>

<P>
<input type="submit" name="submit" value="Show Data">
<input type="submit" name="submit" value="Show Calendar"><br>
</form>
</BLOCKQUOTE>
</body>
