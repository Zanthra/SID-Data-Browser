package edu.stanford.sid.database;

import edu.stanford.sid.*;
import edu.stanford.sid.util.CalendarUtil;

import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Collections;

import javax.servlet.ServletContext;

import edu.stanford.sid.util.*;

import net.sf.ehcache.*;

/**
 * This class is a implementation of the DataFileIndex that is backed by the show_info interface to DRMS.
 * 
 * @author Scott Winegarden scottw@sun.stanford.edu
 *
 */
public class ShowInfoDataFileIndex
extends DataFileIndex
{
	//Date format used by the time-convert program for converting between TAI and UTC (used by showinfo)
	private static final String TIME_CONVERT_DATE_STRING = "yyyy.MM.dd_HH:mm:ss.SSS_Z";
	private static final String SHOW_INFO_DATE_STRING = "yyyy.MM.dd_HH:mm:ss_Z";
		

	private static String DATA_SERIES_ID = "sid_awe.sid";
	private static String MONITOR_SERIES_ID = "sid_awe.monitors";
	private static String SITE_SERIES_ID = "sid_awe.sites";
	
	private static final String SHOW_INFO_PARAMETER_1 = "-a";
	private static final String SHOW_INFO_PARAMETER_2 = "-P";
	private static final String SHOW_INFO_PARAMETER_3 = "seg=file_seg";
	private static final String SHOW_INFO_DATA_FORMAT_STRING = DATA_SERIES_ID + "[? UTC_EndTime > $(%s) and UTC_StartTime < $(%s) ?]";
	
	private static final int DEFAULT_SHOW_INFO_TIMEOUT = 30000;

	
	private String showInfoExecutable;
	
	// These are the Ehcache caches used to provide performance improvements.
	private Cache transmitterCache;
	private Cache monitorCache;
	private Cache dataFileCache;
	
	/**
	 * If this class is constructed with the first line of a ShowInfo response, it can then be
	 * called with the key of a requested value to tell what index a split string of values that
	 * key is located in.
	 */
	private class ShowInfoLinePositions
	{
		private java.util.List<String> linePositions;
		public ShowInfoLinePositions(String headerLine)
		{
			//Show Info provides a like like:
			//Frequency	Latitude	Longitude	SampleRate	StationID	TimeZone	UTC_EndTime	UTC_Offset	MonitorID	Site	UTC_StartTime
			linePositions = java.util.Arrays.asList(headerLine.split("\\t"));
		}
		
		int getIndexOf(String id)
		{
			return linePositions.indexOf(id);
		}
	}
	
	/**
	 * This creates all of the caches needed for performance.
	 */
	private void createCaches()
	{
		CacheManager manager = CacheManager.create();
		if(manager.getCache("transmitterCache") == null)
			manager.addCache(new Cache("transmitterCache", 1000, false, true, 600, 600));
		transmitterCache = manager.getCache("transmitterCache");
		
		if(manager.getCache("monitorCache") == null)
			manager.addCache(new Cache("monitorCache", 5, false, false, 15, 30));
		monitorCache = manager.getCache("monitorCache");

		if(manager.getCache("dataFileCache") == null)
			manager.addCache(new Cache("dataFileCache", 500, false, false, 60 * 15, 60 * 15));
		dataFileCache = manager.getCache("dataFileCache");
	}
	
	/**
	 * Default constructor without a servlet context.
	 */
	private ShowInfoDataFileIndex()
	{
		createCaches();
	}
	
	/**
	 * Creates a new JSOCDataFileIndex, pass the ServletContext of the calling servlet.
	 *
	 */
	public ShowInfoDataFileIndex(ServletContext context)
	{
		if(context.getInitParameter("data-series-id") != null)
		{
			DATA_SERIES_ID = context.getInitParameter("data-series-id");
		}
		
		showInfoExecutable = context.getInitParameter("show_info-executable");

		if(showInfoExecutable == null)
		{
			throw new RuntimeException("When using the show-info database you must include a show_info-executable path.");
		}

		createCaches();
	}
	
	/**
	 * This method is used to retrieve the Longitude and Latitude for a given transmitter.
	 *
	 */
	public StationLatitudeLongitude getStationLatitudeLongitude(String id)
	{
		if(id == null) return null;
		
		StationLatitudeLongitude result = null;
		
		Element transmitterElement = transmitterCache.get(id);
		
		// Check the cache.
		if(transmitterElement == null || transmitterElement.isExpired())
		{
			try
			{
				String[] parameters = {showInfoExecutable, String.format("sid_awe.transmitters[%s]", id), "-a"};
                final Process showInfo = Runtime.getRuntime().exec(parameters);
                setTimeout(showInfo, DEFAULT_SHOW_INFO_TIMEOUT);
				
				try(Scanner showInfoScanner = new Scanner(showInfo.getInputStream()))
				{
					// 	Get the value positions from the first line.
					ShowInfoLinePositions positions = new ShowInfoLinePositions( showInfoScanner.nextLine());
					
					while(showInfoScanner.hasNextLine())
					{
						try
						{
							String[] line = showInfoScanner.nextLine().split("\\t");
							
							result = new StationLatitudeLongitude(
									line[positions.getIndexOf("StationID")],
									Double.parseDouble(line[positions.getIndexOf("Latitude")]),
									Double.parseDouble(line[positions.getIndexOf("Longitude")]));
							
							transmitterElement = new Element(id, result);
							transmitterCache.put(transmitterElement);
						}catch(Exception e){}
					}
				}finally{
					showInfo.destroy();
				}
			}catch(Exception e){}
		}
		if(transmitterElement != null) result = (StationLatitudeLongitude)transmitterElement.getObjectValue();
		else transmitterCache.put(new Element(id, null));
		return result;
	}
	
	/**
	 * This method requests the data files from the JSOC database.
	 * 
	 * This method returns all the data files within the given time range.
	 */
	public DataFileList getFilesFromTimeRange(Calendar startTime, Calendar endTime)
	{
		DateFormat timeConvertFormat = CalendarUtil.getSimpleDateFormat(TIME_CONVERT_DATE_STRING);
		
		//Check if we are requested for more than a week worth of data.  This could take up
		//more memory than is available.
		Calendar timeTest = (Calendar)startTime.clone();
		timeTest.add(Calendar.DATE, 7);
		
		if(timeTest.before(endTime))
		{
			throw new RuntimeException("Time range cannot be greater than one week.");
		}
		
		ArrayList<DataFile> files = new ArrayList<DataFile>();
		Element dataFileElement = dataFileCache.get(timeConvertFormat.format(startTime.getTime()) + timeConvertFormat.format(endTime.getTime()));
		if(dataFileElement == null || dataFileElement.isExpired())
		{
			try
			{
				String[] parameters = {showInfoExecutable, String.format(SHOW_INFO_DATA_FORMAT_STRING, timeConvertFormat.format(startTime.getTime()), timeConvertFormat.format(endTime.getTime())), SHOW_INFO_PARAMETER_1, SHOW_INFO_PARAMETER_2, SHOW_INFO_PARAMETER_3};
				
				final Process showInfo = Runtime.getRuntime().exec(parameters);
				setTimeout(showInfo, DEFAULT_SHOW_INFO_TIMEOUT);
				
				try(Scanner showInfoScanner = new Scanner(showInfo.getInputStream()))
				{
					ShowInfoLinePositions positions = new ShowInfoLinePositions(showInfoScanner.nextLine());
					
					DateFormat showInfoFormat = CalendarUtil.getSimpleDateFormat(SHOW_INFO_DATE_STRING);
					
					while(showInfoScanner.hasNextLine())
					{
						try
						{
							String line = showInfoScanner.nextLine();
							String[] parts = line.split("\\t");
							
							String dataMin = parts[positions.getIndexOf("DataMin")];
							String dataMax = parts[positions.getIndexOf("DataMax")];
							
							String longitude = parts[positions.getIndexOf("Longitude")];
							String latitude = parts[positions.getIndexOf("Latitude")];
							
							longitude = longitude.replace("degrees", "");
							latitude = latitude.replace("degrees", "");

							String monitor = parts[positions.getIndexOf("MonitorID")];
							String site = parts[positions.getIndexOf("Site")];
							String station = parts[positions.getIndexOf("StationID")];
							
							String endTimeString = parts[positions.getIndexOf("UTC_EndTime")];
							String startTimeString = parts[positions.getIndexOf("UTC_StartTime")];
							
							String path = parts[positions.getIndexOf("file_seg")];
							
							Calendar fileStartTime = CalendarUtil.getCalendar();
							Calendar fileEndTime = CalendarUtil.getCalendar();
							fileStartTime.setTime(showInfoFormat.parse(startTimeString));
							fileEndTime.setTime(showInfoFormat.parse(endTimeString));
							
							files.add(new DataFile(path, site, monitor, station, fileStartTime, fileEndTime, longitude, latitude, dataMin, dataMax));
						}catch(Exception e){}
					}
				}finally{
					showInfo.destroy();
				}
				
				dataFileElement = new Element(timeConvertFormat.format(startTime.getTime()) + timeConvertFormat.format(endTime.getTime()), files);
			}catch(Exception e)
			{
				e.printStackTrace();
			}
			if(dataFileElement != null) dataFileCache.put(dataFileElement);
		}
		
		if(dataFileElement != null)
		{
			return new DataFileList((ArrayList<DataFile>)dataFileElement.getObjectValue());
		}
		
		return new DataFileList(new ArrayList<DataFile>());
	}
	
	public boolean isUpdateComplete()
	{
		//JSOC database is always updated.
		return true;
	}
	
	/**
	 * Retrieve unique monitor info from the database.
	 * 
	 * This method returns a collection with exactly one of each unique Site Monitor Station ID set.
	 */
	public Collection<MonitorInfo> getMonitors(Comparator<MonitorInfo> comparator)
	{
		ArrayList<MonitorInfo> monitors = new ArrayList<MonitorInfo>();

		String[] monitorParameters = {showInfoExecutable, "-a", MONITOR_SERIES_ID + "[? DataAvailable = $$1$$ ?]"};		
		String[] siteParameters = {showInfoExecutable, "-a", SITE_SERIES_ID + "[]"};
		
		Element monitorElement = monitorCache.get("monitors");
		
		if(monitorElement == null || monitorElement.isExpired())
		{
        	try
        	{
        		final Process monitorProcess = Runtime.getRuntime().exec(monitorParameters);
        		setTimeout(monitorProcess, DEFAULT_SHOW_INFO_TIMEOUT);
        		final Process siteProcess = Runtime.getRuntime().exec(siteParameters);
    			setTimeout(siteProcess, DEFAULT_SHOW_INFO_TIMEOUT);

				String[] stationParameters = {showInfoExecutable, "-a", DATA_SERIES_ID + "[? recnum in (select distinct on (MonitorID, Site, StationID) recnum from " + DATA_SERIES_ID + ") ?]"};
				
				final Process stationProcess = Runtime.getRuntime().exec(stationParameters);
				setTimeout(stationProcess, DEFAULT_SHOW_INFO_TIMEOUT);
				
        		try(Scanner monitorIn = new Scanner(monitorProcess.getInputStream());
        			Scanner siteIn = new Scanner(siteProcess.getInputStream());
        			Scanner stationIn = new Scanner(stationProcess.getInputStream()))
        		{
        			ShowInfoLinePositions positions = new ShowInfoLinePositions(monitorIn.nextLine());
        			ShowInfoLinePositions sitePositions = new ShowInfoLinePositions(siteIn.nextLine());
        			
        			HashMap<String, String> siteMap = new HashMap<String, String>();
        				
        			while(siteIn.hasNextLine())
        			{	
        				String line = siteIn.nextLine();
        				siteMap.put(line.split("\\t")[sitePositions.getIndexOf("Site")], line);
        			}
        				
        			ShowInfoLinePositions stationPositions = new ShowInfoLinePositions(stationIn.nextLine());
        			HashMap<String, ArrayList<String>> stationMap = new HashMap<String, ArrayList<String>>();
        					
        			while(stationIn.hasNextLine())
        			{
        				String[] line = stationIn.nextLine().split("\\t");
        				String key = String.format("%s$%s", line[stationPositions.getIndexOf("MonitorID")], line[stationPositions.getIndexOf("Site")]);
        				
        				if(!stationMap.containsKey(key))
        					stationMap.put(key, new ArrayList<String>());
        				
        				stationMap.get(key).add(line[stationPositions.getIndexOf("StationID")]);
        			}
        				
        			while(monitorIn.hasNextLine())
        			{
        				try
        				{
        					String line = monitorIn.nextLine();
        					String[] parts = line.split("\\t");
        					String site = parts[positions.getIndexOf("Site")];
        					String monitor = parts[positions.getIndexOf("MonitorID")];
        					String longitude = parts[positions.getIndexOf("Longitude")];
        					String latitude = parts[positions.getIndexOf("Latitude")];
        						
        					//String dataType = parts[positions.getIndexOf("DataType")];
        						
        					
        					String location = "";
        					String website = "";
        					
        					String station = parts[positions.getIndexOf("StationID")];
        					String[] stations = station.split(",");
        					
        					if(siteMap.containsKey(site))
        					{
        						location = siteMap.get(site).split("\\t")[sitePositions.getIndexOf("Country")];
        						website = siteMap.get(site).split("\\t")[sitePositions.getIndexOf("Website")];
        					}
        					
        					for(String s : stations)
        						monitors.add(new MonitorInfo(site,monitor, s, longitude, latitude, location, website));
        				}catch(Exception e)
        				{
        					System.out.println("Error parsing Monitor Entry.");
        					e.printStackTrace();
        				}
        			}
        		}finally
        		{
        			siteProcess.destroy();
        			monitorProcess.destroy();
        			stationProcess.destroy();
        		}
        		
        		monitorElement = new Element("monitors", monitors);
        		monitorCache.put(monitorElement);
        	}catch(Exception e)
        	{
        		e.printStackTrace();
        	}
		}

		if(monitorElement != null)
		{
			ArrayList<MonitorInfo> mi = (ArrayList<MonitorInfo>)monitorElement.getObjectValue();
			Collections.sort(mi, comparator);
			return mi;
		}

        return new java.util.ArrayList<MonitorInfo>();
	}
	
	/**
	 * This provides all the days that the given monitors have data for.
	 */
	public Collection<Calendar> getDaysWithData(Collection<MonitorInfo> monitors)
	{
		java.util.TreeSet<Calendar> daysWithData = new java.util.TreeSet<Calendar>();
		
		String[] calendarParameters = {showInfoExecutable, "-a", DATA_SERIES_ID + "[? recnum in (select distinct on (UTC_StartTime,UTC_EndTime) recnum from " + DATA_SERIES_ID + " " + generateMonitorFilter(monitors, true) + ") ?]"};
		
		DateFormat showInfoFormat = CalendarUtil.getSimpleDateFormat(SHOW_INFO_DATE_STRING);
		
		try
		{
			final Process showInfo = Runtime.getRuntime().exec(calendarParameters);
			setTimeout(showInfo, 30000);
			
			
			try(Scanner calendarIn = new Scanner(showInfo.getInputStream()))
			{
				ShowInfoLinePositions positions = new ShowInfoLinePositions(calendarIn.nextLine());
				
				while(calendarIn.hasNextLine())
				{
					try
					{
						String line = calendarIn.nextLine();
						String[] parts = line.split("\\t");
						
						String fileStartTimeString = parts[positions.getIndexOf("UTC_StartTime")];
						String fileEndTimeString = parts[positions.getIndexOf("UTC_EndTime")];
						
						Calendar fileStartTime = CalendarUtil.getCalendar();
						fileStartTime.setTime(showInfoFormat.parse(fileStartTimeString));
						
						Calendar fileEndTime = CalendarUtil.getCalendar();
						fileEndTime.setTime(showInfoFormat.parse(fileEndTimeString));
						
						daysWithData.addAll(CalendarUtil.getDaysInRange(fileStartTime, fileEndTime));
					}
					catch(Exception e)
					{
						//	ignored
					}
				}
			}finally
			{
				showInfo.destroy();
			}
		}
		catch(Exception e){e.printStackTrace();}
		return daysWithData;
	}
	
	/**
	 * Check if the given MonitorInfo collections contains a monitor that matches the monitorIndentifier.
	 */
	private static boolean monitorCollectionContains(Collection<MonitorInfo> monitors, String monitorIdentifier)
	{
		for(MonitorInfo mi : monitors)
		{
			if(mi.equalsIdentifier(monitorIdentifier))
				return true;
		}
		return false;
	}
	
	/**
	 * This method returs true if and only if there is one or more data files of the given monitor
	 * in the specified time range.
	 */
	public boolean hasFilesInRange(Calendar startTime, Calendar endTime, Collection<MonitorInfo> monitors)
	{
		//Check if we are requested for more than a weeks worth of data.  This could take up
		//more memory than is available.
		Calendar timeTest = (Calendar)startTime.clone();
		timeTest.add(Calendar.DATE, 7);
		if(timeTest.before(endTime))
		{
			throw new RuntimeException("Time range cannot be greater than one week.");
		}


		try
		{
			DateFormat timeConvertFormat = CalendarUtil.getSimpleDateFormat(TIME_CONVERT_DATE_STRING);
			String[] parameters = {showInfoExecutable, String.format(SHOW_INFO_DATA_FORMAT_STRING, timeConvertFormat.format(startTime.getTime()), timeConvertFormat.format(endTime.getTime())), SHOW_INFO_PARAMETER_1};
			
			final Process showInfo = Runtime.getRuntime().exec(parameters);
			setTimeout(showInfo, 30000);
			
			try(Scanner showInfoScanner = new Scanner(showInfo.getInputStream()))
			{
				ShowInfoLinePositions positions = new ShowInfoLinePositions(showInfoScanner.nextLine());
			

				while(showInfoScanner.hasNextLine())
				{
					try
					{
						String line = showInfoScanner.nextLine();
						String[] parts = line.split("\\t");
						String monitor = String.format("%s$%s$%s",
								parts[positions.getIndexOf("MonitorID")],
								parts[positions.getIndexOf("Site")],
								parts[positions.getIndexOf("StationID")]);
						if(monitorCollectionContains(monitors, monitor)) return true;
						
					}catch(Exception e){
					}
					
				}
			}finally
			{
				showInfo.destroy();
			}
		}
		catch(Exception e){e.printStackTrace();}

		return false;
	}
	
	private static String generateMonitorFilter(Collection<MonitorInfo> monitorCollection, boolean solo)
	{
		//Find a safe SQL string separator to protect against code injection.
		//Parameterized queries would be nice, but with ShowInfo being a command
		//line program, it's not possible.
		String ss = "string";
		boolean safeStringSeparatorFound = false;
		while(!safeStringSeparatorFound)
		{
			boolean foundMatch = false;
			String escapedSS = "$" + ss + "$";
			for(MonitorInfo mi : monitorCollection)
			{
				if(mi.getSite().contains(escapedSS) ||
						mi.getStation().contains(escapedSS) ||
						mi.getMonitor().contains(escapedSS))
					foundMatch = true;
			}
			
			if(foundMatch)
			{
				ss = ss + "string";
			} else
			{
				safeStringSeparatorFound = true;
			}
		}
		
		//Put the dollar signs around the string seperator.
		ss = "$" + ss + "$";
		
		//Process the monitor filter.
		String monitorFilter = "";
		
		if(monitorCollection.size() > 0)
		{
			MonitorInfo[] monitors = monitorCollection.toArray(new MonitorInfo[0]);

			if(!solo)
			{
				monitorFilter = monitorFilter + " AND";
			}else
			{
				monitorFilter = monitorFilter + " WHERE";
			}
			
			monitorFilter = monitorFilter + " (";
			
			for(int i = 0;i < monitors.length - 1;i++)
			{
				monitorFilter = monitorFilter + "(MonitorID=" + ss +
					monitors[i].getMonitor() + ss +
					" AND Site=" + ss +
					monitors[i].getSite();
				
				//If there are not multiple stations this monitor is looking for, add
				//the station to the selection.
				if(!monitors[i].getStation().equalsIgnoreCase("multiple"))
					monitorFilter = monitorFilter + ss + " AND StationID=" + ss +
						monitors[i].getStation();
				monitorFilter = monitorFilter + ss + ") OR ";
			}
			
			monitorFilter = monitorFilter + "MonitorID=" + ss +
				monitors[monitors.length - 1].getMonitor() + ss +
				" AND Site="+ ss +
				monitors[monitors.length - 1].getSite();
			if(!monitors[monitors.length - 1].getStation().equalsIgnoreCase("multiple"))
				monitorFilter = monitorFilter + ss + " AND StationID=" + ss +
					monitors[monitors.length - 1].getStation();
			monitorFilter = monitorFilter + ss + ")";
		}
		
		return monitorFilter;
	}
	
	private java.util.Timer timeoutTimer = new java.util.Timer();
	
	/**
	 * This is a timer task for killing the ShowInfo processes after a given
	 * amount of time.
	 * 
	 * @author Scott Winegarden (scottw@sun.stanford.edu)
	 *
	 */
	private class TimeoutTimerTask
	extends java.util.TimerTask
	{
		private Process processToKill;
		public TimeoutTimerTask(Process process)
		{
			this.processToKill = process;
		}
		
		public void run()
		{
			processToKill.destroy();
		}
	}
	
	/**
	 * Sets a timeout for the given process, at which point it will be
	 * killed.
	 * 
	 * @param process process to kill.
	 * @param milliseconds how long to wait to kill it.
	 */
	private void setTimeout(Process process, int milliseconds)
	{
		timeoutTimer.schedule(new TimeoutTimerTask(process), milliseconds);
	}
	
	public void close()
	{
		//Close connections.  Show Info has no connections, so nothing to close.
	}
}
