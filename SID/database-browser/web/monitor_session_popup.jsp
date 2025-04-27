<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="edu.stanford.sid.database.*,java.util.*" %>


<%!

//DataFileIndex index;

private static final String LIST_MONITORS_SRC = "AddMonitorToSession";
private static final String THIS_SRC = "monitor_session_popup.jsp";

public void jspInit()
{
	//index = DataFileIndex.getInstance(this.getServletContext());
}

private String createAddLink(MonitorInfo info)
{
	return String.format("AddMonitorToSession?submit=add&monitor=%s", info.getIdentifier());
}

%>

<%


DataFileIndex index = DataFileIndex.getInstance(this.getServletContext());


Collection<MonitorInfo> sessionContains = Collections.EMPTY_LIST;
if(request.getSession().getAttribute("monitors") != null)
	sessionContains = (Collection<MonitorInfo>)request.getSession().getAttribute("monitors");

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Add Monitor</title>
<style type="text/css" title="currentStyle">
	@import "script/DataTables/css/demo_table.css";
</style>
<script src="script/DataTables/js/jquery.js"></script>
<script src="script/DataTables/js/jquery.dataTables.js"></script>
<script type="text/javascript">
var monitorTable;

$(document).ready(function () {
	/*
	 * Function: fnGetDisplayNodes
	 * Purpose:  Return an array with the TR nodes used for displaying the table
	 * Returns:  array node: TR elements
	 *           or
	 *           node (if iRow specified)
	 * Inputs:   object:oSettings - automatically added by DataTables
	 *           int:iRow - optional - if present then the array returned will be the node for
	 *             the row with the index 'iRow'
	 */
	$.fn.dataTableExt.oApi.fnGetDisplayNodes = function ( oSettings, iRow )
	{
		var anRows = [];
		if ( oSettings.aiDisplay.length !== 0 )
		{
			if ( typeof iRow != 'undefined' )
			{
				return oSettings.aoData[ oSettings.aiDisplay[iRow] ].nTr;
			}
			else
			{
				for ( var j=oSettings._iDisplayStart ; j<oSettings._iDisplayEnd ; j++ )
				{
					var nRow = oSettings.aoData[ oSettings.aiDisplay[j] ].nTr;
					anRows.push( nRow );
				}
			}
		}
		return anRows;
	};
	
	$('#selectMon').click( function() {
		var sData = $('input', monitorTable.fnGetNodes()).serialize();
		document.location.href= "<%=LIST_MONITORS_SRC%>?" + sData;
		return false;
	} );
	 
	monitorTable = $('#monitorTable').dataTable();
	
	$('#checkall').click( function() {
		$('input', monitorTable.fnGetDisplayNodes()).attr('checked','checked');
		return false; // to avoid refreshing the page
	} );
});
</script>
</head>
<body>

<BLOCKQUOTE>
<form id="selection" method="GET" action="<%=LIST_MONITORS_SRC%>">
<button type="button" id="selectMon">Select Monitors</button><button type="button" id="checkall">Check All On Page</button><br>
<p/>
<div>
<table id="monitorTable" rules="all" border="box" cellpadding=2 width="100%">
<thead>
<tr><th>Select</th><th><A href="#">Site</A></th>
<th><A href="#">Location</A></th>
<th><A href="#">Monitor</A></th>
<th><A href="#">Station</A></th>
<!--<td></td>--><!--<td>Calendar</td>--></tr>
</thead>
<tbody>

<%

for(MonitorInfo info : index.getMonitors(MonitorComparators.getMonitorComparator(request.getParameter("sort"))))
{
%>
<tr><td><input type="checkbox" name="monitor" value="<%=info.getIdentifier()%>" <%if(sessionContains.contains(info)) {%> CHECKED<%}%>></td>
<td><%=info.getSite()%></td><td><%=info.getLocation()%></td>
<td><%=info.getMonitor()%></td><td><%=info.getStation()%></td>
<%--<td><A href="<%=createAddLink(info)%>">Add</A></td>--%></tr><%}%>
</tbody>
</table>
</div>
</form>
</BLOCKQUOTE>

</body>
</html>
