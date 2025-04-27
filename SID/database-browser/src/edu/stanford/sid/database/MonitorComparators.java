package edu.stanford.sid.database;

import java.util.Comparator;

/*
 * 
 */

/**
 * This enumerator provides some easy comparators to sort the monitors by.
 * 
 * @author Scott Winegarden, scottw@sun.stanford.edu
 *
 */
public enum MonitorComparators
implements Comparator<MonitorInfo>
{
	/**
	 * Sorts the monitors by the Station ID.
	 */
	STATION("station", "stationID")
	{
		public int compare(MonitorInfo o1, MonitorInfo o2)
		{
			if(o1.getStation().equals("") && o2.getStation().equals("")) return SITE.compare(o1, o2);
			if(o1.getStation().equals("")) return -1;
			if(o2.getStation().equals("")) return 1;
			
			int i;
			
			i = o1.getStation().toLowerCase().compareTo(o2.getStation().toLowerCase());
			if(i != 0) return i;
			
			else return SITE.compare(o1, o2);
		}
	},
	/**
	 * Sorts the monitors by the Monitor ID.
	 */
	MONITOR("monitor", "monitorID")
	{
		public int compare(MonitorInfo o1, MonitorInfo o2)
		{
			if(o1.getMonitor().equals("unknown")) if(!o2.getMonitor().equals("unknown")) return -1;
			if(o2.getMonitor().equals("unknown")) if(!o1.getMonitor().equals("unknown")) return 1;
			return o1.getMonitor().toLowerCase().compareTo(o2.getMonitor().toLowerCase());
		}
	},
	/**
	 * Sorts the monitors by the Site ID.
	 */
	SITE("site", "site")
	{
		public int compare(MonitorInfo o1, MonitorInfo o2)
		{
			if(o1.getSite().equals("unknown") && o2.getSite().equals("unknown")) return MONITOR.compare(o1, o2);
			if(o1.getSite().equals("unknown")) return -1;
			if(o2.getSite().equals("unknown")) return 1;
			
			int i;
			
			i = o1.getSite().toLowerCase().compareTo(o2.getSite().toLowerCase());
			if(i != 0) return i;
			
			else return MONITOR.compare(o1, o2);
		}
	},
	/**
	 * Sorts the monitors by the Country.
	 */
	LOCATION("location", "country")
	{
		public int compare(MonitorInfo o1, MonitorInfo o2)
                {
                        if(o1.getLocation().equals("") && o2.getLocation().equals("")) return SITE.compare(o1, o2);
                        if(o1.getLocation().equals("")) return -1;
                        if(o2.getLocation().equals("")) return 1;

                        int i;

                        i = o1.getLocation().toLowerCase().compareTo(o2.getLocation().toLowerCase());
                        if(i != 0) return i;

                        else return SITE.compare(o1, o2);
                }
	};
	
	/**
	 * A string identifier for the comparator.  This allows you to easially convert
	 * to and from string and MonitorComparators.
	 */
	public String ID;
	
	/**
	 * The name of the SQL column to sort on.
	 */
	public String sqlColumnName;
	
	/**
	 * Compares two MonitorInfo classes, returns 0 if o1 and o2 are equal,
	 * less than 1 if o1 is less than o2, and greater than 1 if o1 is greater
	 * than o2
	 */
	public abstract int compare(MonitorInfo o1, MonitorInfo o2);
	
	/**
	 * Constructor method for the above comparators.
	 * 
	 * @param ID
	 * @param sqlName
	 */
	private MonitorComparators(String ID, String sqlName)
	{
		this.ID = ID;
		this.sqlColumnName = sqlName;
	}
	
	/**
	 * Gets the MonitorComparators for the given id (station, site, monitor, location)
	 */
	public static MonitorComparators getMonitorComparator(String id)
	{
		for(MonitorComparators comparator : MonitorComparators.values())
		{
			if(comparator.ID.equals(id)) return comparator;
		}
		
		return SITE;
	}
}
