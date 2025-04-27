package edu.stanford.sid.database;


import java.util.regex.*;

import edu.stanford.sid.DataFile;

/**
 * This class represents a SID monitor, identified by it's MonitorID.
 * 
 * Generally speaking this information is gathered directly or indirectly from the
 * metadata of an nonspecific SID Data File belonging to that monitor.
 * 
 * @author Scott Winegarden, scottw@sun.stanford.edu
 *
 */
public class MonitorInfo
implements java.io.Serializable
{
	private static final long serialVersionUID = 1L;


	//This regular expression breaks an angle of format: N32 16' 21.38"
	// group(1) is the direction, N, S, E or W
	// group(2) is the degrees
	// group(3) is the minutes
	// group(4) is the seconds, and may have a fractional component.
	private Pattern ANGLE_REGEX = Pattern.compile("(N|S|E|W)([0-9]+) ([0-9]+)' ([0-9]*\\.?[0-9]+)\"");
	
	
	//These hold key information about the monitor.
	private String site;
	private String monitor;
	private String station;
	private String location;
	private String website;
	
	//These hold longitude and latitude information.
	private boolean hasLatitudeLongitude;
	private float latitude;
	private float longitude;
	
	/**
	 * Creates a new MonitorInfo from information gathered directly
	 * from the data of the specified SID DataFile.
	 * 
	 * @param file is a sid DataFile to gather monitor info from.
	 */
	public MonitorInfo(DataFile file)
	{
		site = file.getSite();
		monitor = file.getMonitor();
		station = file.getStation();
		
		String latitudeString = file.getMetadataValue("Latitude");
		String longitudeString = file.getMetadataValue("Longitude");
		
		try
		{
			latitude = parseAngle(latitudeString);
			longitude = parseAngle(longitudeString);
			
			if(latitude != 0 || longitude != 0)
			{
				hasLatitudeLongitude = true;
			}
		}catch(Exception ignored)
		{
		}
	}
	
	/**
	 * This method creates MonitorInfo directly, use this if you
	 * gather the data from another source.
	 * 
	 * @param site is the Site ID, eg: WSO
	 * @param monitor is the monitor ID, eg: S-0012
	 * @param station is the station ID that it monitors, eg: NLK
	 * @param longitude is a string containing a floating point, or
	 * 					angle of the monitors longitude on earth.
	 * @param latitude is a string containing a floating point, or
	 * 					angle of the monitors latitude on earth.
	 */
	public MonitorInfo(String site, String monitor, String station, String longitude, String latitude)
	{
		this.site = site;
		this.monitor = monitor;
		this.station = station;
		
		try
		{
			this.latitude = parseAngle(latitude);
			this.longitude = parseAngle(longitude);
		}catch(Exception e)
		{}
		
		if(this.latitude != 0 && this.longitude != 0)
		{
			hasLatitudeLongitude = true;
		}
	}

	/**
	 * This method creates MonitorInfo directly, including a location and a website.
	 */
	public MonitorInfo(String site, String monitor, String station, String longitude, String latitude,
			String location, String website)
	{
		this(site, monitor, station, longitude, latitude);
		this.location = location;
	}
	
	/**
	 * This method turns string angles in either of two forms,
	 * into floating point angles:
	 * 
	 * 1) A floating point number that is readable by Float.parse(String)
	 * 2) A String of the format: N32 21' 18.2" with or without the decimal
	 * 			portion of the seconds.
	 * 
	 * @param angle the angle to be converted.
	 * @return a floating point number containing the angle.
	 * @throws java.text.ParseException if the angle could not be converted.
	 */
	private float parseAngle(String angle)
	throws java.text.ParseException
	{
		float degrees = 0;
		
		if(angle != null)
		{
			Matcher match = ANGLE_REGEX.matcher(angle);
			if(match.matches())
			{
				String direction = match.group(1);
				
				degrees = Float.parseFloat(match.group(2));
				float minutes = Float.parseFloat(match.group(3));
				float seconds = Float.parseFloat(match.group(4));
				
				minutes += seconds/60;
				degrees += minutes/60;
				
				if(direction.equals("S") || direction.equals("W"))
				{
					degrees *= -1;
				}
			}
			else
			{
				degrees = Float.parseFloat(angle);
			}
		}
		
		return degrees;
	}
	
	/**
	 * The hash code is guaranteed to match if
	 * Site ID, Monitor ID, and Station ID all match.
	 */
	public int hashCode()
	{
		return String.format("%s%s%s", site, monitor, station).hashCode();
	}
	
	/**
	 * @return true if the Site ID, Monitor ID and Station ID all match.
	 */
	public boolean equals(Object o)
	{
		if(o instanceof MonitorInfo)
		{
			MonitorInfo m = (MonitorInfo)o;
			return  m.site != null && m.site.equals(site) &&
					m.monitor != null && m.monitor.equals(monitor) &&
					m.station != null && m.station.equals(station);
			
		}else{return false;}
	}
	
	/**
	 * @return the Site Monitor and Station each seperated by a space.
	 */
	public String toString()
	{
		return String.format("%s %s %s", site, monitor, station);
	}
	
	/**
	 * @return the site ID for this monitor.
	 */
	public String getSite()
	{
		return site;
	}
	
	/**
	 * @return the monitor ID for this monitor.
	 */
	public String getMonitor()
	{
		return monitor;
	}
	
	/**
	 * @return the station ID for this monitor.
	 */
	public String getStation()
	{
		return station;
	}

	/**
	 * @return the location of the monitor.
	 */
	public String getLocation()
	{
		if(location != null)return location;
		return "";
	}
	
	/**
	 * Returns the website of the monitor if it has one and it is a valid URL.
	 */
	public String getWebsite()
	{
		try
		{
			new java.net.URL(website);
			return website;
		} catch(Exception e)
		{
			return "";
		}
	}
	
	/**
	 * @return false if the monitor does not have both a longitude
	 * and latitude, or for another reason they cannot be used.
	 */
	public boolean hasLatitudeLongidute()
	{
		return hasLatitudeLongitude;
	}
	
	/**
	 * @return the monitor's latitude.
	 */
	public float getLatitude()
	{
		return latitude;
	}
	
	/**
	 * 
	 * @return the monitor's longitude.
	 */
	public float getLongitude()
	{
		return longitude;
	}
	
	/**
	 * This is a unique string identifier for the MonitorInfo.  If the strings are equal,
	 * the MonitorInfos will be equal.
	 */
	public String getIdentifier()
	{
		return String.format("%s$%s$%s", monitor, site, station);
	}
	
	/**
	 * Checks if this MonitorInfo is equal to the provided string idnetifier.
	 */
	public boolean equalsIdentifier(String identifier)
	{
		String[] parts = identifier.split("$");
		if(parts.length == 3)
			return parts[0].equals(monitor) && parts[1].equals(site) && parts[2].equals(station);
		else if(parts.length == 2)
			return parts[0].equals(monitor) && parts[1].equals(site) && "".equals(station);
		else return false;
	}
}
