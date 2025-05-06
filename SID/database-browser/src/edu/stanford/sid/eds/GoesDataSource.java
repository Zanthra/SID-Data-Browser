package edu.stanford.sid.eds;

import edu.stanford.sid.util.CalendarUtil;
import java.util.Calendar;
import java.util.HashMap;
import java.util.ArrayList;
import java.text.*;
import java.util.*;
import java.net.*;

import javax.servlet.ServletContext;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sf.ehcache.*;

/**
 * The GoesDataSource gets data from the noaa goes archive and parses it into lists of solar flares,
 * complete with start, max, end, and strength information.
 */
public class GoesDataSource
{
	private static final String ARCHIVE_FILE_FORMAT = "yyyy'_events/'yyyyMMdd'events.txt'";
	private static final String GOES_URL_DATE_FORMAT = "yyyyMMdd'events.txt'";
	private String goesBaseUrl; //"ftp://ftp.swpc.noaa.gov/pub/indices/events/";
	private String goesUrlFormat;
	
	private File _goesArchiveDirectory;

	private boolean enableGoesLocal = false;
	private boolean enableGoesRemote = false;
	
	private Cache goesFileCache;
	
	public static GoesDataSource getInstance(ServletContext context)
	{
		if(!(context.getAttribute("goes-source") instanceof GoesDataSource))
		{
			context.setAttribute("goes-source", new GoesDataSource(context));
		}
		return (GoesDataSource)context.getAttribute("goes-source");
	}
	
	/**
	 * Creates a new GoesDataSource.  Every effort should be made to reuse a single
	 * GoesDataSource as much as possible.
	 */
	private GoesDataSource(ServletContext context)
	{
		enableGoesLocal = "true".equals(context.getInitParameter("enable-goes-local"));
		enableGoesRemote = "true".equals(context.getInitParameter("enable-goes-remote"));

		if(context.getInitParameter("goes-archive-directory") != null)
		{
			_goesArchiveDirectory = new File(context.getInitParameter("goes-archive-directory"));
		}
		if(context.getInitParameter("goes-remote-url") != null)
		{
			goesBaseUrl = context.getInitParameter("goes-remote-url");
			goesUrlFormat = goesBaseUrl + "/%s";
		}


		CacheManager manager = CacheManager.create();
		if(manager.getCache("goesFileCache") == null)
            manager.addCache(new Cache("goesFileCache", 40, false, false, 3600, 3600));
        goesFileCache = manager.getCache("goesFileCache");
	}
	
	/**
	 * If the goes events file for the given day already exists in the local archive, load the file
	 * from the disk, otherwise open the file from the GOES website.
	 * 
	 * @param date to download the goes data for.
	 * @return an input stream that contains the goes event list.
	 * @throws IOException
	 */
	private InputStream getGoesFileURL(Calendar date)
	throws IOException
	{
		DateFormat archiveDateFormat = CalendarUtil.getSimpleDateFormat(ARCHIVE_FILE_FORMAT);
		File archiveFile = null;

		if(enableGoesLocal)
		{
			archiveFile = new File(_goesArchiveDirectory, archiveDateFormat.format(date.getTime()));
		
		}
		
		//If said file does not exist, connect to the web and download the file.
		if(enableGoesRemote && (archiveFile == null || !archiveFile.exists()))
		{
			DateFormat urlFormat = CalendarUtil.getSimpleDateFormat(GOES_URL_DATE_FORMAT);
			URL goesFileURL = new URL(String.format(goesUrlFormat, urlFormat.format(date.getTime())));
			
			return goesFileURL.openConnection().getInputStream();
		}

		if(archiveFile == null)
		{
			throw new IOException("Cannot load goes file.");
		}
		
		return new FileInputStream(archiveFile);
	}
	
	/**
	 * Gets a collection of GoesEvents for the given time range, at the given flare strength.
	 */
	public Collection<GoesEvent> getListForRange(Calendar startTime, Calendar endTime, GoesFlareStrength minimumStrength)
	{
		//Our array for return values.
		ArrayList<GoesEvent> fullEventList = new ArrayList<GoesEvent>();
		
		for(Calendar date : CalendarUtil.getDaysInRange(startTime, endTime))
		{
			//Try loading it from the cache.
			Element goesFileElement = goesFileCache.get(date);
			if(goesFileElement == null || goesFileElement.isExpired())
			{
				try
				{
					ArrayList<GoesEvent> eventList = new ArrayList<GoesEvent>();
					
					
					try(Scanner goesFileIn = new Scanner(getGoesFileURL(date)))
					{
						while(goesFileIn.hasNextLine())
						{
							String line = goesFileIn.nextLine();
							if(!line.startsWith(":") && !line.startsWith("#") && line.length() > 0 && !line.equals("NO EVENT REPORTS."))
							{
								String strength = line.substring(58,62);
								try
								{
									/*
									 * An event line has fixed line positions, so substring works.
									 * 
									 * Example:
									 * Event    Begin    Max       End  Obs  Q  Type  Loc/Frq   Particulars       Reg#
									 */
									String startString = line.substring(11, 15);
									String maxString = line.substring(18, 22);
									String endString = line.substring(28, 32);
									
									//We duplicate the input date and set hours and minutes form the flare info.
									Calendar flareStartTime = CalendarUtil.duplicate(date);
									flareStartTime.set(Calendar.HOUR, Integer.parseInt(startString.substring(0,2)));
									flareStartTime.set(Calendar.MINUTE, Integer.parseInt(startString.substring(2,4)));
									
									Calendar flareMaxTime = CalendarUtil.duplicate(date);
									flareMaxTime.set(Calendar.HOUR, Integer.parseInt(maxString.substring(0,2)));
									flareMaxTime.set(Calendar.MINUTE, Integer.parseInt(maxString.substring(2,4)));
									
									Calendar flareEndTime = CalendarUtil.duplicate(date);
									flareEndTime.set(Calendar.HOUR, Integer.parseInt(endString.substring(0,2)));
									flareEndTime.set(Calendar.MINUTE, Integer.parseInt(endString.substring(2,4)));
									
									//Add it to the list of events.
									//
									//NOTE: It is here, in the "new GoesFlareStrength(strength)" that valid XRay events only
									//are added to the list.  If the event does not have a valid XRay event strength, the
									//constructor will throw an exception, and it will skip adding that event.
									eventList.add(new GoesEvent(flareStartTime, flareMaxTime, flareEndTime, new GoesFlareStrength(strength)));
								}catch(Exception e)
								{
									//ignored
								}
							}
						}
					}
					
					//Add the results to the cache.
					goesFileElement = new Element(date, eventList);
					goesFileCache.put(goesFileElement);
				}catch(Exception e)
				{
					//ignored
				}
			}
			if(goesFileElement != null) fullEventList.addAll((ArrayList<GoesEvent>)goesFileElement.getObjectValue());
		}
		
		Iterator<GoesEvent> i = fullEventList.iterator();
		while(i.hasNext())
		{
			GoesEvent event = i.next();
			if(event.getStrength().compareTo(minimumStrength) < 0) i.remove();
		}
		
		return fullEventList;
	}
}
