package edu.stanford.sid.util;


import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * A set of static utility methods that allow easy access to GMT time zone
 * calendar objects and simple date format objects.  Because of the
 * unpredictability of the setDefaultTimeZone() method of the TimeZone class
 * it is safer to use these methods to get correctly formatted calendar objects
 * that are guaranteed to have expected time zone behavior.
 * 
 * @author Scott Winegarden, scottw@sun.stanford.edu
 *
 */
public class CalendarUtil
{
	/**
	 * This is the default time zone that is used by the Calendar Factory.
	 */
	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("GMT");
	
	/**
	 * Get a calendar set to the current time.
	 * 
	 * @return a calendar set to the current time, the default time zone of this class.
	 */
	public static Calendar getCalendar()
	{
		return Calendar.getInstance(DEFAULT_TIME_ZONE);
	}
	
	/**
	 * Gets a calendar object set to 12:00:00.00 AM on the same day the given calendar is
	 * set to.  This method does not change the given calendar.
	 * 
	 * @param initial the date the returned calendar should be set to.
	 * @return a calendar set to 12:00:00.00 AM on the given day.
	 */
	public static Calendar getDateResolution(Calendar initial)
	{
		return getCalendar(initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DATE));
	}
	
	public static Calendar getMonthResolution(Calendar initial)
	{
		return getCalendar(initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), 1);
	}
	
	public static Calendar getYearResolution(Calendar initial)
	{
		return getCalendar(initial.get(Calendar.YEAR), 0, 1);
	}
	
	/**
	 * This method returns a collection of calendar objects set to all
	 * the days that the given time range exists in.  For example if
	 * the start time is 2006-07-11 3:23 PM, and the end time is
	 * 2006-07-12 4:14 AM, the returned time would contain 2006-07-11
	 * 12:00:00.00 AM and 2006-07-12 12:00:00.00 AM.
	 * 
	 * @param startTime the start of the time range.
	 * @param endTime the end of the time range.
	 * @return a collection of dates the range spans.
	 */
	public static Collection<Calendar> getDaysInRange(Calendar startTime, Calendar endTime)
	{
		Collection<Calendar> days = new ArrayList<Calendar>();
		Calendar start = getDateResolution(startTime);
		
		while(start.before(endTime))
		{
			days.add((Calendar)start.clone());
			start.add(Calendar.DATE, 1);
		}
		
		return days;
	}
	
	/**
	 * Gets a calendar with the specified year month and day.
	 * 
	 * @param year the year the new calendar should be set to.
	 * @param month the month the new calendar should be set to.
	 * @param day the day the new calendar should be set to.
	 * @return a calendar with the specified day month and year.
	 */
	public static Calendar getCalendar(int year, int month, int day)
	{
		Calendar c = getCalendar();
		
		c.clear();
		
		c.set(year, month, day);
		
		return c;
	}
	
	/**
	 * ThreadLocal instance to cache the DateFormat objects used throughout the
	 * program. 
	 */
	private static ThreadLocal<HashMap<String,SimpleDateFormat>> formatCacheStore =
		new ThreadLocal<HashMap<String,SimpleDateFormat>>()
	{
		protected synchronized HashMap<String,SimpleDateFormat> initialValue()
		{
			return new HashMap<String, SimpleDateFormat>();
		}
	};
	
	/**
	 * Gets a DateFormat set to a Universal time.
	 * 
	 * Since these cannot be modified, but are not thread safe, each
	 * thread will get the same DateFormat each time they call this
	 * method with the same String.
	 * 
	 * @param format a simple date format format.
	 * @return a new simple date format, set to the default time zone of this calendar istance.
	 */
	public static DateFormat getSimpleDateFormat(String format)
	{
		HashMap<String, SimpleDateFormat> formatCache = formatCacheStore.get();
		
		if(!formatCache.containsKey(format))
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat(format);
			dateFormat.setTimeZone(DEFAULT_TIME_ZONE);
			formatCache.put(format, dateFormat);
		}
		
		return formatCache.get(format);
	}
	
	/**
	 * Produces a duplicate of the provided calendar.  Any operations
	 * done on the duplicate will not affect the original calendar.
	 * 
	 * @param original The calendar to duplicate.
	 * @return A separate calendar object set to the same time
	 * and day as the provided calendar.
	 */
	public static Calendar duplicate(Calendar original)
	{
		return (Calendar)original.clone();
	}
	
	/**
	 * A test method for the methods in this class.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		TimeZone.setDefault(DEFAULT_TIME_ZONE);
		
		System.out.println(getCalendar(2006, 07, 01).getTime());
		
		Calendar test = getCalendar(2009, 2, 17);
		Calendar test2 = duplicate(test);
		test2.add(Calendar.DATE, 2);
		
		System.out.format("%s %s%n", test.getTime(), test2.getTime());
		
		System.out.println(getYearResolution(getCalendar(2006, 07, 06)).getTime());
		
		System.out.println(getCalendar(2006, 07, 01).before(getCalendar(2006, 07, 01)));
		
		for(Calendar c : getDaysInRange(getCalendar(2006, 07, 21), getCalendar(2006, 07, 23)))
		{
			System.out.println(c.getTime());
		}
	}
}
