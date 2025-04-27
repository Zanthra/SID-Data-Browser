package edu.stanford.sid;

import edu.stanford.sid.util.CalendarUtil;
import edu.stanford.sid.graphing.SunriseSunsetOverlay;
import edu.stanford.sid.util.StationLatitudeLongitude;
import edu.stanford.sid.graphing.GoesOverlay;
import edu.stanford.sid.eds.GoesDataSource;
import edu.stanford.sid.eds.GoesFlareStrength;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.Collection;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.TimerTask;
import edu.stanford.sid.database.*;

import java.util.*;

/**
 * A servlet that graphs and retrieves SID Files.
 * 
 * @author Scott Winegarden, scottw@sun.stanford.edu
 *
 */
public class PlotSid
extends HttpServlet
{
	/**
	 * A default serial version for serialization.  This will probably never be used.
	 */
	private static final long serialVersionUID = 1L;
	
	//These represent the format of the time in the Query string, as well as the time in the data file.
	private static final String QUERY_DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH.mm.ss";
    
    //This is the filesystem's local directory, and the visible address of the public directory.
	private File publicDirectory;
	private String publicDirectoryVisible;
	
	private Timer fileDeletionTimer;
    
    private static DataFileIndex index;
    
    /**
     * Initializes the servlet with the values of the <init-param> entries in web.xml.
     * 
     */
    public void init()
    {
    	publicDirectory = new File(this.getInitParameter("public-directory"));
    	publicDirectoryVisible = this.getInitParameter("public-directory-visible");
        
        fileDeletionTimer = new java.util.Timer();
        
        index = DataFileIndex.getInstance(this.getServletContext());
    }
    
    /**
     * Displays a graph of the data files given by the query string, or
     * copies the data files to an intermediate directory to be retrieved by the
     * user.
     * 
     * A valid query string is of the form:
     * 		?starttime=2006-05-06T11.36.15&stoptime=2006-05-07T9.36.15&res=1400x600&monitor=WSO$NLK$ref
     * 
     * 
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException
    {	
    	try{
    		// Declare default variables.
    		Calendar startTime = CalendarUtil.getCalendar();
    		Calendar endTime = CalendarUtil.getCalendar();
    		
    		int xres = 800;
    		int yres = 300;
    		
    		Collection<MonitorInfo> monitors = new HashSet<MonitorInfo>();
    		
    		
    		java.util.TimeZone tz = CalendarUtil.DEFAULT_TIME_ZONE;
    		
    		//	Indicates whether to display monitor sunrises and sunsets.
    		boolean mss = true;
    		
    		//	Indicates whether to display station sunrises and sunsets.
    		boolean sss = true;
    		
    		boolean goes = false;
    		String flareStrength = "C2.0";
    		
    		String startTimeString = request.getParameter("starttime");
    		String endTimeString = request.getParameter("endtime");
    		
    		try
    		{
    			DateFormat queryFormat = CalendarUtil.getSimpleDateFormat(QUERY_DATE_FORMAT_STRING);
		
    			startTime.setTime(queryFormat.parse(startTimeString));
    			endTime.setTime(queryFormat.parse(endTimeString));
    			
    			//Sanity check, if more than a month of data, don't bother.
    			Calendar temp = (Calendar)startTime.clone();
    			temp.add(Calendar.MONTH, 1);
    			
    			if(endTime.after(temp))
    			{
    				throw new RuntimeException("Time Range Too Long.");
    			}
    		}catch(Exception e)
    		{
    			e.printStackTrace();
    			System.out.println(request.getQueryString());
    			System.out.println(startTimeString);
    			System.out.println(endTimeString);
    			return;		// If we don't know the start and stop times, no point in graphing. //
    		}
    		
    		// Parse query string elements
    		// Graph Resolution
    		if(request.getParameterMap().containsKey("res"))
    		{
    			String[] xyres = request.getParameter("res").split("x");
    			
    			xres = Integer.parseInt(xyres[0]);
    			yres = Integer.parseInt(xyres[1]);
    		}
    		
    		DataFileList files = index.getFilesFromTimeRange(startTime, endTime);
    		
    		// Monitor List
    		if(request.getParameterMap().containsKey("monitor"))
    		{
    			Collection<String> monitorIDs = Arrays.asList(request.getParameterValues("monitor"));
    			for(MonitorInfo mi : files.getMonitorInfo())
    			{
    				if(monitorIDs.contains(mi.getIdentifier())) monitors.add(mi);
    			}
    		}
    		
    		files = files.filterByMonitors(monitors);
    		
    		
    		// TimeZone
    		if(request.getParameterMap().containsKey("TZ"))
    		{
    			tz = java.util.TimeZone.getTimeZone(request.getParameter("TZ"));
    		}
    		
    		// Monitor Sunrise and Sunset overlay.
    		if(request.getParameterMap().containsKey("mss"))
    		{
    			try
    			{
    				mss = Boolean.parseBoolean(request.getParameter("mss"));
    			}catch(Exception e){}
    		}
    		
    		// Station sunrise and sunset overlay.
    		if(request.getParameterMap().containsKey("sss"))
    		{
    			try
    			{
    				sss = Boolean.parseBoolean(request.getParameter("sss"));
    			}catch(Exception e){}
    		}
    		
    		// GOES Overlay
    		if(request.getParameterMap().containsKey("goes"))
    		{
    			try
    			{
    				goes = Boolean.parseBoolean(request.getParameter("goes"));
    			}catch(Exception e){}
    		}
    		
    		// Max flare strength for GOES Overlay
    		if(request.getParameterMap().containsKey("goesFlareStrength"))
    		{
    			try
    			{
    				flareStrength = request.getParameter("goesFlareStrength");
    			}catch(Exception e){}
    		}
    		
    		// Check if they want a plot or download links.
    		if(request.getServletPath().equals("/retrieve"))
    		{
    			response.setContentType("text/html");
    			PrintWriter out = response.getWriter();
    			
    			for(DataFile dataFile : files.getFiles())
    			{
    				out.println(makeLink(copyDataFile(dataFile), dataFile));
    			}
    			
    			out.println("These files will remain avaliable for 5 minutes.");
    		}else if(request.getServletPath().equals("/plot"))
    		{
    			edu.stanford.sid.graphing.Graph grapher = new edu.stanford.sid.graphing.Graph(files,
    					CalendarUtil.duplicate(startTime), CalendarUtil.duplicate(endTime),
    					xres, yres, request.getLocale(), tz);
    			
    			// Add overlays
    			if(mss)
    			{
    				for(MonitorInfo mi : files.getMonitorInfo())
    				{
    					grapher.addOverlay(new SunriseSunsetOverlay(mi, CalendarUtil.duplicate(startTime), CalendarUtil.duplicate(endTime)));
    				}
    			}
    			
    			java.util.HashSet<String> stationIDs = new java.util.HashSet<String>();
    			
    			if(sss)
    			{
    				for(DataFile f : files.getFiles())
    					stationIDs.add(f.getStation());
    				for(String s : stationIDs)
    				{
    					StationLatitudeLongitude sll = index.getStationLatitudeLongitude(s);
    					if(sll != null)
    						grapher.addOverlay(new SunriseSunsetOverlay(sll.longitude, sll.latitude, CalendarUtil.duplicate(startTime), CalendarUtil.duplicate(endTime)));
    				}
    			}
    			
    			if(goes)
    			{
    				try
    				{
    					grapher.addOverlay(new GoesOverlay(CalendarUtil.duplicate(startTime),
    							CalendarUtil.duplicate(endTime),
    							new GoesFlareStrength(flareStrength),
    							GoesDataSource.getInstance(getServletContext())));
    				}catch(Exception e){e.printStackTrace();}
    			}
        	// 	Set the response to PNG.
    			response.setContentType("image/png");
    			response.setHeader("Cache-Control", "max-age=600");
    			
    			javax.imageio.ImageIO.write(grapher.draw(), "PNG", response.getOutputStream());
    			
    		} else
    		{
    			response.setContentType("text/plain");
    			response.getOutputStream().println(request.getServletPath());
    		}
    	}catch(Exception e){
    		//ignored
    	}
    }
    
    /**
     * Gets the string that is to be displayed in the graph's key for a given datafile.
     * 
     * @param file the file to generate the key for.
     * @return the string to be displayed in the graph's key.
     */
    public static String getKey(DataFile file)
    {
    	return String.format("%5.5s %3.3s %16.16s", file.getSite(), file.getStation(), file.getMonitor());
    }
    
    /**
     * 
     * This copies all data, to the end of the given input stream, and writes it to the given output stream.
     * 
     * @param in the input stream to read from.
     * @param out the output stream to write to.
     * @throws IOException if either stream has a problem.
     */
    private static void streamCopy(java.io.InputStream in, java.io.OutputStream out)
    throws IOException
    {
    	// Create a byte buffer for fast copying of the stream.
    	byte[] buffer = new byte[4092];
    	while (true)
    	{
    		// Read in as many bytes as are available.
    		int amountRead = in.read(buffer);
    		
    		// If amountRead is -1 we have reached the end of the stream, so return.
    		if (amountRead == -1) return;
    		
    		// Otherwise write the bytes to the output stream.
    		out.write(buffer, 0, amountRead);
    	}
    }
    
    /**
     * Copies the given data file to a temporary public directory where it can be downloaded.
     * The file will be deleted after 15 minutes to save space on disk, but to give the retriever
     * enough time to download it.  This method returns a string with the name of the file.
     * 
     * @param dataFile the file to be copied.
     * @return the name of the temporary file.
     * @throws IOException if the file cannot be copied.
     */
	private String copyDataFile(DataFile dataFile)
	throws IOException
	{
		File newFile = new File(publicDirectory.getPath() + "/"
				+ String.format("%s_%s_%s_%s.txt", 
						dataFile.getSite(), 
						dataFile.getMonitor(), 
						dataFile.getStation(), 
						CalendarUtil.getSimpleDateFormat(QUERY_DATE_FORMAT_STRING).format(dataFile.getStartTime().getTime())));
		
		if(newFile.createNewFile())
		{
			newFile.deleteOnExit();
			
			try(FileInputStream in = new FileInputStream(dataFile.getFile());
				FileOutputStream out = new FileOutputStream(newFile))
			{
				streamCopy(in, out);
				
				class FileDeletionTask
				extends TimerTask
				{
					private File f;
					public FileDeletionTask(File f)
					{
						this.f = f;
					}
					
					public void run()
					{
						f.delete();
					}
				}
				
				fileDeletionTimer.schedule(new FileDeletionTask(newFile), 15 * 60 * 1000);
				return newFile.getName();
			}
		}
		
		return "";
	}
	
	private String makeLink(String name, DataFile file)
	{
		return String.format("<A href=\"%s/%s\">%s %s</A><br>%n", publicDirectoryVisible, name, getKey(file), CalendarUtil.getSimpleDateFormat(QUERY_DATE_FORMAT_STRING).format(file.getStartTime().getTime()));
	}
}
