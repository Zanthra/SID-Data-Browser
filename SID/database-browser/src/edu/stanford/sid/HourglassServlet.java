package edu.stanford.sid;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.stanford.sid.database.DataFileIndex;
import edu.stanford.sid.database.MonitorInfo;
import edu.stanford.sid.graphing.HourglassGraph;
import edu.stanford.sid.util.CalendarUtil;

public class HourglassServlet
extends HttpServlet
{	
	private static final String QUERY_DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH.mm.ss";
    
	private DataFileIndex index;
	
	/**
     * Initializes the servlet with the values of the <init-param> entries in web.xml.
     * 
     */
    public void init()
    {
        index = DataFileIndex.getInstance(this.getServletContext());
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException
    {
    	Calendar endTime = CalendarUtil.getDateResolution(CalendarUtil.getCalendar());
    	Calendar startTime = CalendarUtil.duplicate(endTime);
    	startTime.add(Calendar.YEAR, -1);
    	
    	HashSet<DataFile> files = new HashSet<DataFile>();
    	
    	for(int i = 0;i < 52;i++)
    	{
    		Calendar cStart = CalendarUtil.duplicate(startTime);
    		cStart.add(Calendar.WEEK_OF_YEAR, i);
    		Calendar cEnd = CalendarUtil.duplicate(cStart);
    		cEnd.add(Calendar.WEEK_OF_YEAR, 1);
    		
    		//files.addAll(index.getFilesFromTimeRange(cStart, cEnd).filterByMonitors(Collections.singleton(new MonitorInfo("UN-VIENNA", "016", "HWV", "", ""))).getFiles());
    		files.addAll(index.getFilesFromTimeRange(cStart, cEnd).filterByMonitors(Collections.singleton(new MonitorInfo("Jaap", "S-0031-FB-0031", "NAA", "", ""))).getFiles());
    	}
    	
    	response.setContentType("image/png");
    	
    	javax.imageio.ImageIO.write(new HourglassGraph(files, startTime, endTime, 600, 800).draw(), "PNG", response.getOutputStream());
    }
}
