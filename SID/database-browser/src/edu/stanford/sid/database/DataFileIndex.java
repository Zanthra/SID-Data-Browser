package edu.stanford.sid.database;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;

import javax.servlet.ServletContext;

import edu.stanford.sid.*;
import edu.stanford.sid.util.*;

/**
 * 
 * This class is the base class that all forms of data file indexes derive.
 * 
 * To retrieve a DataFileIndex call the DataFileIndex.getInstance(ServletContext)
 * method to retrieve the DataFileIndex for your servlet context.  Once you get
 * an instance, you can use the getFilesFromTimeRange(startTime, endTime) method
 * to request data files from the index.
 * 
 * @author Scott Winegarden (scottw@sun.stanford.edu)
 *
 */
public abstract class DataFileIndex
{
	/**
	 * This provides you with a DataFileIndex.
	 *
	 * If you have the index-type context parameter in your ServletContext set to filesystem
	 * you will get a FilesystemDataFileIndex with the provided root-data-directory,
	 * index-directory, index-log-directory, and update-interval.
	 *
	 * If you do not have the index-type, or it is set to anything else you will get a
	 * ShowInfoDataFileIndex.  It is not recommended to keep a reference to the DataFileIndex
	 * around after use.
	 * 
	 * @param context the context to get the instance for.
	 * @return a DataFileIndex for the given context.
	 */
	public static DataFileIndex getInstance(ServletContext context)
	{
		//Check without synchronization for performance.
		if(context.getInitParameter("index-type") != null && context.getInitParameter("index-type").equals("filesystem")){
			System.out.println("Filesystem index");
			if(!(context.getAttribute("data-index") instanceof DataFileIndex))
			{
				//Synchronize to prevent multiple instances.
				synchronized(context)
				{
					//Check with synchronization in case an instance was made after blocking.
					if(!(context.getAttribute("data-index") instanceof DataFileIndex))
					{
						if(context.getInitParameter("filesystem-index") != null && context.getInitParameter("filesystem-index").equals("true"))
						{
							System.out.println("created index");
							//Create the DataFileIndex.
							context.setAttribute("data-index", new FilesystemDataFileIndex(
									new File(context.getInitParameter("root-data-directory")),
									new File(context.getInitParameter("index-directory")),
									new File(context.getInitParameter("index-log-directory")),
									Integer.parseInt(context.getInitParameter("update-interval"))));
						}
						else
						{
							return new ShowInfoDataFileIndex(context);
						}
					}
				}
			}
		
			//Return the DataFileIndex.
			return (DataFileIndex)context.getAttribute("data-index");
		}
		return new ShowInfoDataFileIndex(context);
	}
	
	/**
	 * Gets the latitude and longitude for the given station identifier.
	 */
	public abstract StationLatitudeLongitude getStationLatitudeLongitude(String identifier);
	

	/**
	 * This is the primary method of retrieving data files from the database.
	 * 
	 * This method will return all SID data files within the start and stop
	 * times specified.
	 * 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public abstract DataFileList getFilesFromTimeRange(Calendar startTime, Calendar endTime);
	
	/**
	 * This retrieves the list of SID monitoring sites that there is data in
	 * the database for.
	 * 
	 * @param comparator Provide a sorting comparator to sort the monitor collection.
	 * @return a collection of all monitors sorted by the given comparator.
	 */
	public abstract Collection<MonitorInfo> getMonitors(Comparator<MonitorInfo> comparator);
	
	/**
	 * This retrieves a list of calendars containing the dates that there is data
	 * available for the given monitors.
	 * 
	 * @param monitors
	 * @return
	 */
	
	public abstract Collection<Calendar> getDaysWithData(Collection<MonitorInfo> monitors);
	
	/**
	 * 
	 * @return true if there are data files for the given monitors during the given
	 * time range.
	 */
	public abstract boolean hasFilesInRange(Calendar startTime, Calendar endTime, Collection<MonitorInfo> monitors);
	
	/**
	 * Returns true if all data files have been indexed.
	 * @return
	 */
	public abstract boolean isUpdateComplete();
	
	/**
	 * This frees up any resources that the index may be using.  This should be called after
	 * you have finished making use of the index.  Once this has been called one should assume
	 * that the index is no longer usable.  If you need to use the index again, get another
	 * by calling getInstance().
	 */
	public abstract void close();
}
