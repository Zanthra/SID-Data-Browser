package edu.stanford.sid.database.ajax;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Calendar;

import javax.servlet.http.*;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import edu.stanford.sid.DataFileList;
import edu.stanford.sid.database.DataFileIndex;
import edu.stanford.sid.database.MonitorInfo;
import edu.stanford.sid.util.CalendarUtil;

/**
 * This returns a JSON containing the monitors that have data between the provided
 * starttime and endttime.
 * 
 * @author Scott Winegarden (scottw@sun.stanford.edu)
 *
 */
public class AjaxMonitorsForTimeRange
extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	//These represent the format of the time in the Query string.
	private static final String QUERY_DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH.mm.ss";
	
	private DataFileIndex index;
	
	public void init()
	{
		index = DataFileIndex.getInstance(getServletContext());
	}
	
	/**
	 * Converts MonitorInfo objects into their identifiers for Ajax requests.
	 *
	 */
	private class MonitorInfoSerializer implements JsonSerializer<MonitorInfo>
	{
		public JsonElement serialize(MonitorInfo arg0, Type arg1,
				JsonSerializationContext arg2)
		{
			return new JsonPrimitive(arg0.getIdentifier());
		}
		
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws IOException
	{
		//Prepare calendars.
		DateFormat dateFormat = CalendarUtil.getSimpleDateFormat(QUERY_DATE_FORMAT_STRING);
		
		Calendar startTime = CalendarUtil.getCalendar();
		Calendar endTime = CalendarUtil.getCalendar();
		
		//If we cannot determine the calendar data, pass the error to the AJAX request.
		try
		{
			startTime.setTime(dateFormat.parse(request.getParameter("starttime")));
			endTime.setTime(dateFormat.parse(request.getParameter("endtime")));
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid starttime or endtime.");
			return;
		}
		
		//Get the files for the time range.
		DataFileList files = index.getFilesFromTimeRange(startTime, endTime);
		
		//Prepare the JSON builder.
		GsonBuilder gson = new GsonBuilder()
						.registerTypeAdapter(MonitorInfo.class, new MonitorInfoSerializer())
						.setPrettyPrinting();
		
		response.setContentType("application/json");
		
		//Get the MonitorInfo and convert to JSON.
		response.getWriter().print(gson.create().toJson(files.getMonitorInfo()));
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws IOException
	{
		doGet(request, response);
	}
	
	public void doPut(HttpServletRequest request, HttpServletResponse response)
	throws IOException
	{
		doGet(request, response);
	}
}
