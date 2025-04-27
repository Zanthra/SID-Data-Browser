package edu.stanford.sid.graphing;

import edu.stanford.sid.DataFile;
import edu.stanford.sid.util.*;
import edu.stanford.sid.database.MonitorInfo;
import java.util.Collection;

import java.awt.Graphics2D;
import java.awt.Graphics;
import java.util.Calendar;
import java.awt.Color;


/**
 * The sunrise sunset overlay displays a upwards arrow for sunrise at the given
 * longitude and latitude, and a downwards arrow for sunset at the given longitude
 * and latitude.
 *
 * If you have more than one location that you want to draw the sunrise and sunset for
 * you need to create multiple SunriseSunsetOverlays.
 */
public class SunriseSunsetOverlay
extends Overlay
{
	//These are the longitude and latitude.
	private double latitude;
	private double longitude;
	
	//This says whether to display the overlay.
	private boolean visible = false;
	
	//These are the full days we want sunrise and sunsets for.
	private Collection<Calendar> dates;
	
	//These are all the sunrises and sunsets in the given time.
	private Collection<Calendar> sunrises;
	private Collection<Calendar> sunsets;
	
	/**
	 * Create a SunriseSunsetOverlay that will draw all the Sunrises and Sunsets between the
	 * startTime and endTime.
	 */
	public SunriseSunsetOverlay(MonitorInfo mi, Calendar startTime, Calendar endTime)
	{
		super(mi);
		latitude = mi.getLatitude();
		longitude = mi.getLongitude();
			
		sunrises = new java.util.ArrayList<Calendar>();
                        sunsets = new java.util.ArrayList<Calendar>();

		for(Calendar date : CalendarUtil.getDaysInRange(startTime, endTime))
		{
			SunriseSunset ss = new SunriseSunset(latitude, longitude, date, 0);

			if(ss.isSunrise()) sunrises.add(ss.getSunrise());
			if(ss.isSunset()) sunsets.add(ss.getSunset());
		}
		visible = true;
	}
	
	/**
	 * Creates a SunriseSunsetOverlay that will draw all the Sunrises and Sunsets between the
	 * provided file's start and end times.
	 */
	public SunriseSunsetOverlay(DataFile dataFile)
	{
		super(dataFile.getMonitorInfo());
		String longitudeString = dataFile.getMetadataValue("Longitude");
		String latitudeString = dataFile.getMetadataValue("Latitude");
		
		try
		{
			latitude = LatitudeLongitudeParser.parseLatitudeLongitude(latitudeString);
			longitude = LatitudeLongitudeParser.parseLatitudeLongitude(longitudeString);
			
			sunrises = new java.util.ArrayList<Calendar>();
			sunsets = new java.util.ArrayList<Calendar>();
			
			dates = CalendarUtil.getDaysInRange(dataFile.getStartTime(), dataFile.getEndTime());
			
			for(Calendar date : dates)
			{
				SunriseSunset ss = new SunriseSunset(latitude, longitude, date, 0);
				
				if(ss.isSunrise()) sunrises.add(ss.getSunrise());
				if(ss.isSunset()) sunsets.add(ss.getSunset());
			}
			
			visible = true;
		}
		catch(Exception e){}
	}
	
	/**
	 * Creates a new SunriseSunsetOverlay for the given longitude and latitude that will draw
	 * the Sunrises and Sunsets between the startTime and the endTime.
	 */
	public SunriseSunsetOverlay(double longitude, double latitude, Calendar startTime, Calendar endTime)
	{
		super(null);
		sunrises = new java.util.ArrayList<Calendar>();
		sunsets = new java.util.ArrayList<Calendar>();
		
		for(Calendar date : CalendarUtil.getDaysInRange(startTime, endTime))
		{
			SunriseSunset ss = new SunriseSunset(latitude, longitude, date, 0);
			
			if(ss.isSunrise()) sunrises.add(ss.getSunrise());
			if(ss.isSunset()) sunsets.add(ss.getSunset());
		}
		visible = true;
	}
	
	/**
	 * Draws the overlay onto the graph.
	 */
	public void drawOverlay(Graphics g, long xStartSeconds, long xEndSeconds)
	{
		if(visible)
		{
			int xWidthSeconds = (int) (xEndSeconds - xStartSeconds);
			
			float xMod = g.getClipBounds().width / (float)xWidthSeconds;
			
			int height = g.getClipBounds().height;
			
			for(Calendar sunrise : sunrises)
			{
				int xLoc = (int)((sunrise.getTimeInMillis() / 1000 - xStartSeconds) * xMod);
				
				g.drawLine(xLoc, 0, xLoc, height);
				
				int[] xPoints = { xLoc, xLoc - 5, xLoc + 5 };
				int[] yPoints = { 0, 10, 10 };
				g.fillPolygon(xPoints, yPoints, 3);
			}
			
			for(Calendar sunset : sunsets)
			{
				int xLoc = (int)((sunset.getTimeInMillis() / 1000 - xStartSeconds) * xMod);
				
				g.drawLine(xLoc, 0, xLoc, height);
				
				int[] xPoints = { xLoc, xLoc - 5, xLoc + 5 };
				int[] yPoints = { height, height - 10, height - 10 };
				g.fillPolygon(xPoints, yPoints, 3);
			}
		}
	}
}
