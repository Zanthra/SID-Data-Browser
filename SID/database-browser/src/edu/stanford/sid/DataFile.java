package edu.stanford.sid;

import edu.stanford.sid.database.MonitorInfo;
import edu.stanford.sid.util.CalendarUtil;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.text.DateFormat;
import java.text.ParseException;


/**
 * A class that contains metadata information and get methods for information
 * contained in the file's metadata.
 * 
 * @author Scott Winegarden, scottw@sun.stanford.edu
 * 
 */
public class DataFile implements Serializable
{
	/**
	 * DataFile class version 1.
	 */
	private static final long serialVersionUID = 1L;
	
	// Date format class for the metadata time variables.
	private DateFormat METADATA_DATE_FORMAT = CalendarUtil
			.getSimpleDateFormat("y-M-d H:m:s");
	
	private DateFormat STRING_DATE_FORMAT = CalendarUtil
			.getSimpleDateFormat("yyyy.MM.dd_HH:mm:ss");
	
	// This is a regular expression to retrieve metadata variables from
	// file text lines.
	private static final Pattern METADATA_REGEX = Pattern
			.compile("^#(.*)=(.*)$");
	
	// This is a reference to the file on the filesystem.
	private File file;
	
	// A map of metadata keys to metadata values.
	private Map<String, String> metadata;
	
	private Calendar startTime;
	private Calendar endTime;
	
	/**
	 * A test method for the class. This is not used outside testing.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			new DataFile(new File("07-05_00:00.txt"));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * This constructor creates a DataFile object from the specified File.
	 * 
	 * If the file cannot be read, this will throw an IOException. If the file
	 * is not a valid Data File, it will throw a DataFileFormatException.
	 * 
	 * 
	 */
	public DataFile(java.io.File newFile) throws IOException,
			DataFileFormatException
	{
		file = newFile;
		parseMetadata();
		verifyData();
	}
	
	/**
	 * This constructor is meant to represent a data file that was retrieved
	 * from a JSOC query. This does not parse any information from the file.
	 * 
	 * @param JSOCstring
	 * @deprecated untested in current implementation.
	 */
	public DataFile(String JSOCstring) throws IOException
	{
		parseMetadataJSOC(JSOCstring);
	}
	
	/**
	 * This constructor creates a data file from the provided information.
	 */
	public DataFile(String path, String site, String monitor, String station,
			Calendar startTime, Calendar endTime)
			throws DataFileFormatException
	{
		file = new File(path);
		
		metadata = new HashMap<String, String>();
		metadata.put("Site", site);
		metadata.put("MonitorID", monitor);
		metadata.put("StationID", station);
		this.startTime = startTime;
		this.endTime = endTime;
		
		verifyData();
	}
	
	/**
	 * This constructor creates a data file from the provided information.
	 */
	public DataFile(String path, String site, String monitor, String station,
			Calendar startTime, Calendar endTime, String longitude,
			String latitude) throws DataFileFormatException
	{
		file = new File(path);
		
		metadata = new HashMap<String, String>();
		metadata.put("Site", site);
		metadata.put("MonitorID", monitor);
		metadata.put("StationID", station);
		metadata.put("Longitude", longitude);
		metadata.put("Latitude", latitude);
		this.startTime = startTime;
		this.endTime = endTime;
		
		verifyData();
	}
	
	/**
	 * This constructor creates a data file from the provided information.
	 */
	public DataFile(String path, String site, String monitor, String station,
			Calendar startTime, Calendar endTime, String longitude,
			String latitude, String dataMin, String dataMax)
			throws DataFileFormatException
	{
		file = new File(path);
		
		metadata = new HashMap<String, String>();
		metadata.put("Site", site);
		metadata.put("MonitorID", monitor);
		metadata.put("StationID", station);
		metadata.put("Longitude", longitude);
		metadata.put("Latitude", latitude);
		metadata.put("DataMin", dataMin);
		metadata.put("DataMax", dataMax);
		this.startTime = startTime;
		this.endTime = endTime;
		
		verifyData();
	}
	
	/**
	 * This creates a new data file with the metadata provided in a map.
	 */
	public DataFile(String path, Map<String, String> metadata)
			throws DataFileFormatException
	{
		file = new File(path);
		
		this.metadata = new HashMap<String, String>();
		for (String key : metadata.keySet())
		{
			this.metadata.put(key, metadata.get(key));
		}
		
		verifyData();
	}
	
	
	/**
	 * Private helper method for the constructor.
	 * 
	 * This method reads the metadata from the file and places it in a new
	 * HashMap.
	 * 
	 * @throws IOException
	 *             if the file cannot be read.
	 */
	private void parseMetadata() throws IOException
	{
		metadata = new HashMap<String, String>(); // Create a new hashmap for
													// the metadata.
		try(Scanner in = new Scanner(file))// Opens the data file.
		{
			String nextLine = in.nextLine(); // Read the first line.
		
			while (nextLine.equals("") || nextLine.startsWith("#")) // While we are
																// 	in comments.
			{
				//Use the old method of splitting metadata entries.
				if (nextLine.length() > 0)
				{
					nextLine = nextLine.replaceFirst("#\\s*", "");
					;
					String[] keyValue = nextLine.split("\\s*=\\s*");
					if (keyValue.length == 2)
					{
						metadata.put(keyValue[0], keyValue[1]);
					}
				}
			
				if (!in.hasNextLine()) { return; } // We are done if no more lines
												// in the file.
				nextLine = in.nextLine(); // Read the next line.
			}
		
			if (getMonitor() == null)
			{
				String name = file.getName();
				String[] parts = name.split("(_|\\.)");
				metadata.put("MonitorID", parts[3]);
			}
			
			if (getEndTime() == null)
			{
				while (in.hasNextLine())
				{
					nextLine = in.nextLine();
				}
			
				String[] parts = nextLine.split(",");
				metadata.put("UTC_EndTime", parts[0]);
			}
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * @deprecated untested in current implementations.
	 */
	private void parseMetadataJSOC(String data) throws IOException
	{
		metadata = new HashMap<String, String>(); // Create a new hashmap for
													// the metadata.
		Scanner in = new Scanner(data); // Opens the string.
		
		in.useDelimiter("\n");
		
		while (in.hasNext()) // While we are in comments.
		{
			String nextLine = in.next();
			nextLine = nextLine.replaceAll("\r", "");
			
			System.out.println(nextLine);
			String[] keyValue = nextLine.split("=");
			if (keyValue.length == 2)
			{
				if (keyValue[1].startsWith("\""))
				{
					keyValue[1] = keyValue[1].substring(1,
							keyValue[1].length() - 2);
				}
				if (keyValue[0].equals("file_seg"))
				{
					file = new File(keyValue[1]);
				} else
				{
					metadata.put(keyValue[0], keyValue[1]);
				}
			}
		}
		
		try
		{
			startTime = getTimeString("UTC_StartTime");
			endTime = getTimeString("UTC_EndTime");
		} catch (Exception e)
		{}
	}
	
	/**
	 * This is a private helper method for the Constructor.
	 * 
	 * This checks the three required metadata entries for correct format.
	 * 
	 * @throws DataFileFormatException
	 *             if verification fails.
	 */
	private void verifyData() throws DataFileFormatException
	{
		try
		{
			if (getMonitor() == null) // Make sure we have a monitor ID.
			{ throw new DataFileFormatException("No monitor ID."); }
			
			if (getSite() == null) { throw new DataFileFormatException(
					"No site ID."); }
			
			if (getStation() == null) { throw new DataFileFormatException(
					"No station ID."); }
			
			if (getStartTime() == null) // Make sure we have a start time.
			{ throw new DataFileFormatException("Bad start time format."); }
			
			if (getEndTime() == null) // Make sure we have a end time.
			{ throw new DataFileFormatException("Bad end time format."); }
		} catch (Exception e) // If we have any other error, pass it on as an
								// unknown error.
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * This returns a calendar object containing the time the value of the
	 * specified key of the metadata represents.
	 * 
	 * @param key
	 *            metadata key containing the time to create the calendar for.
	 * @return a calendar object of the time in the value of the key.
	 * @throws ParseException
	 *             if the date is of bad format.
	 */
	private Calendar getTime(String key) throws ParseException
	{
		Calendar startTime = CalendarUtil.getCalendar();
		startTime.setTime(METADATA_DATE_FORMAT.parse(getMetadataValue(key)));
		return startTime;
	}
	
	
	private Calendar getTimeString(String key) throws ParseException
	{
		Calendar startTime = CalendarUtil.getCalendar();
		startTime.setTime(STRING_DATE_FORMAT.parse(getMetadataValue(key)));
		return startTime;
	}
	
	/**
	 * Returns a calendar representing the UTC_StartTime metadata value.
	 * 
	 * @return
	 */
	public Calendar getStartTime()
	{
		try
		{
			if (startTime == null)
			{
				startTime = getTime("UTC_StartTime");
			}
			return startTime;
		} catch (ParseException e)
		{
			return null;
		} catch (NullPointerException e)
		{
			return null;
		}
	}
	
	/**
	 * Returns a calendar representing the UTC_StartTime metadata value.
	 * 
	 * @return
	 */
	public Calendar getEndTime()
	{
		try
		{
			if (endTime == null)
			{
				endTime = getTime("UTC_EndTime");
			}
			return endTime;
		} catch (ParseException e)
		{
			return null;
		} catch (NullPointerException e)
		{
			return null;
		}
	}
	
	/**
	 * @return the value of the metadata key Site.
	 */
	public String getSite()
	{
		return getMetadataValue("Site");
	}
	
	/**
	 * @return the value of the metadata key MonitorID.
	 */
	public String getMonitor()
	{
		return getMetadataValue("MonitorID");
	}
	
	/**
	 * 
	 * @return the value of the metadata key StationID.
	 */
	public String getStation()
	{
		return getMetadataValue("StationID");
	}
	
	/**
	 * 
	 * @param key
	 *            a metadata key.
	 * @return the value of the specified metadata key.
	 */
	public String getMetadataValue(String key)
	{
		return metadata.get(key);
	}
	
	/**
	 * @return the file this DataFile represents.
	 */
	public File getFile()
	{
		return file;
	}
	
	/**
	 * Returns true if the file the given data file was created from is equal to
	 * this data file.
	 */
	public boolean equals(Object obj)
	{
		if (obj instanceof DataFile) { return ((DataFile) obj).file
				.equals(file); }
		return false;
	}
	
	/**
	 * Overrides the hash code method to hash the data file.
	 */
	public int hashCode()
	{
		return file.hashCode();
	}
	
	/**
	 * Returns the MonitorInfo about the monitor that created this data file.
	 */
	public MonitorInfo getMonitorInfo()
	{
		return new MonitorInfo(this);
	}
}
