package edu.stanford.sid.graphing;

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.Locale;
import java.text.DateFormat;
import java.util.TimeZone;

import edu.stanford.sid.*;
import edu.stanford.sid.database.MonitorInfo;
import edu.stanford.sid.util.*;

/**
 * The graph class draws the SID data files, the constructor returns very quickly, but the draw
 * method includes the graphics processing to create the SID graph.
 */
public class Graph
{
	private static final int TOP_BORDER_WIDTH = 0;
	private static final int BOTTOM_BORDER_WIDTH = 12;
	private static final int LEFT_BORDER_WIDTH = 0;
	private static final int RIGHT_BORDER_WIDTH = 200; //This doubles as the key width.
	private static final int RIGHT_BORDER_WIDTH_NO_KEY = 0;
	
	private static final int DEFAULT_WIDTH = 1000;
	private static final int DEFAULT_HEIGHT = 300;
	
	private static final int DEFAULT_VERTICAL_DIVISIONS = 4;
	private static final int DEFAULT_HORIZONTAL_DIVISIONS = 4;

	private static final double DEFAULT_Y_MIN = -5;
	private static final double DEFAULT_Y_MAX = 5;
	private static final double SS_LIMIT = 0.1; //The amount above or below the default min and max before assuming supersid data.
	
	private static final Color[] DEFAULT_COLOR_MAP = parseColors();

	private static Color[] parseColors()
	{
		String[] s = "0x336699 0x99CCFF 0x999933 0x666699 0xCC9933 0x006666 0x3399FF 0x993300 0xCCCC99 0x666666 0xFFCC66 0x6699CC 0x663366 0x9999CC 0xCCCCCC 0x669999 0xCCCC66 0xCC6600 0x9999FF 0x0066CC 0x99CCCC 0x999999 0xFFCC00 0x009999 0x99CC33 0xFF9900 0x999966 0x66CCCC 0x339966 0xCCCC33".split(" ");
		Color[] temp = new Color[s.length];
		for(int i = 0;i < s.length;i++)
			temp[i] = Color.decode(s[i]);
		return temp;
	}
	


	private static final BasicStroke GUIDES_STROKE = new BasicStroke(1, 
																	 BasicStroke.CAP_SQUARE,
																	 BasicStroke.JOIN_BEVEL,
																	 1.0f,
																	 new float[] {5.0f, 4.0f, 2.0f, 4.0f},
																	 2);
	
	private int width;
	private int height;
	
	private Calendar startTime;
	private Calendar endTime;
	
	private DataFileList files;
	private Map<MonitorInfo, Color> colorMap = new HashMap<MonitorInfo, Color>();
	private Map<MonitorInfo, Double> dataMinMap = new HashMap<MonitorInfo, Double>();
	private Map<MonitorInfo, Double> dataMaxMap = new HashMap<MonitorInfo, Double>();
	private Locale userLocale;
	private DateFormat userDateFormat;
	private TimeZone userTimeZone;
	
	/**
	 * Creates a grapher for the given data files, with a start and end time to emcompass all files.
	 */
	public Graph(DataFileList files)
	{
		this(files, null, null);
		
		Calendar startTime = null;
		Calendar endTime = null;
		
		for(DataFile file : files.getFiles())
		{
			if(startTime == null)
				startTime = file.getStartTime();
			if(endTime == null)
				endTime = file.getEndTime();
			
			if(startTime.after(file.getStartTime()))
				startTime = file.getStartTime();
			if(endTime.before(file.getEndTime()))
				endTime = file.getEndTime();
		}
		
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	/**
	 * Creates a grapher to produce a graph for the given data files over the given time range.
	 */
	public Graph(DataFileList files, Calendar startTime, Calendar endTime)
	{
		this(files, startTime, endTime, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}
	
	/**
	 * Creates a grapher to make a graph for the given data files over the given time range with the given dimensions.
	 */
	public Graph(DataFileList files, Calendar startTime, Calendar endTime, int width, int height)
	{
		this(files, startTime, endTime, width, height, Locale.US, edu.stanford.sid.util.CalendarUtil.DEFAULT_TIME_ZONE);
	}
	
	/**
	 * Creates a grapher to make a graph fro the given data files, over the given time range, with the given dimensions, displaying
	 * the times in the provided locale and time zone.
	 */
	public Graph(DataFileList files, Calendar startTime, Calendar endTime, int width, int height, Locale userLocale, TimeZone userTimeZone)
	{
		this.files = files;
		this.width = width;
		this.height = height;
		this.userLocale = userLocale;
		this.userTimeZone = userTimeZone;
		
		userDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, userLocale);
		userDateFormat.setTimeZone(userTimeZone);
		
		userDateFormat = CalendarUtil.getSimpleDateFormat("yyyy-MM-dd HH:mm");
		
		Collection<MonitorInfo> monitors = files.getMonitorInfo();
		for(int i = 0;i < monitors.size();i++)
		{
			colorMap.put(monitors.toArray(new MonitorInfo[0])[i], DEFAULT_COLOR_MAP[i % DEFAULT_COLOR_MAP.length]);
		}
		
		for(DataFile f : files.getFiles())
		{
			try
			{
				Double dataMin = Double.parseDouble(f.getMetadataValue("DataMin"));
				Double dataMax = Double.parseDouble(f.getMetadataValue("DataMax"));
				
				if(dataMinMap.containsKey(f.getMonitorInfo()) && dataMinMap.get(f.getMonitorInfo()).compareTo(dataMin) < 0)
				{}else
				{
					dataMinMap.put(f.getMonitorInfo(), dataMin);
				}
				
				if(dataMaxMap.containsKey(f.getMonitorInfo()) && dataMaxMap.get(f.getMonitorInfo()).compareTo(dataMax) > 0)
				{}else
				{
					dataMaxMap.put(f.getMonitorInfo(), dataMax);
				}
			}catch(Exception e){ /*ignored*/ }
		}
		
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public BufferedImage draw()
	{
		//Create the image and the Graphics object to draw to it with.
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = image.createGraphics();
		try{
		
			//Antialiasing does not work with the fast drawing method I use, and slows the graphing down unnecessarily.
			g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
			
			//Clear the buffer.
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			
			//Draw the background.
			drawBackground((Graphics2D)g.create());
			
			//For each file, draw the file.
			for(DataFile file : files.getFiles())
			{
				//Set the bounds while drawing the file.
				Graphics plot = g.create(LEFT_BORDER_WIDTH + 1, TOP_BORDER_WIDTH + 1,
						width - LEFT_BORDER_WIDTH - RIGHT_BORDER_WIDTH - 2,
						height - TOP_BORDER_WIDTH - BOTTOM_BORDER_WIDTH - 2);
				try
				{
					plotFile(plot, file, colorMap.get(file.getMonitorInfo()));
				}catch(Exception e)
				{
					//If anything goes wrong, log it and continue.
					plot.dispose();
					e.printStackTrace();
				}
			}
			
			//Draw our overlays with bounds.
			drawOverlays((Graphics2D)g.create(LEFT_BORDER_WIDTH + 1, TOP_BORDER_WIDTH + 1,
											width - LEFT_BORDER_WIDTH - RIGHT_BORDER_WIDTH - 2,
                                        	height - TOP_BORDER_WIDTH - BOTTOM_BORDER_WIDTH - 2));
		
			//Finish up.
			return image;
		}finally
		{
			g.dispose();
		}
	}
	
	/**
	 * This draws the background, including the key.  This is the only method which draws outside
	 * of the graph's bounding box.
	 */
	private void drawBackground(Graphics2D g)
	{try{
		//Draw border.
		g.setColor(Color.BLACK);
		Font guideFont = new Font("courier", Font.PLAIN, 11);
		g.setFont(guideFont);
		
		//A 0 width rectangle is 1 pixel across.  If we have an X width graph, we want to draw the rectangle
		//X - 2 pixels across.  This comes out to X - 1 pixels in width.
		int rectangleWidth = width - LEFT_BORDER_WIDTH - RIGHT_BORDER_WIDTH - 1;
		int rectangleHeight = height - TOP_BORDER_WIDTH - BOTTOM_BORDER_WIDTH - 1;
		
		g.drawRect(LEFT_BORDER_WIDTH, TOP_BORDER_WIDTH, rectangleWidth, rectangleHeight);
		
		//Draw key
		
		Collection<MonitorInfo> monitors = files.getMonitorInfo();
		
		for(int i = 0; i < monitors.size(); i++)
		{
			//For each monitor draw a short 10 pixel line, then draw a string identifying the monitor.
			int lineY = i * 12 + 8;
			int lineStartX = width - RIGHT_BORDER_WIDTH + 1;
			int lineStopX = lineStartX + 15;
			
			g.setColor(colorMap.get(monitors.toArray(new MonitorInfo[0])[i]));
			
			g.fillRect(lineStartX, lineY-2, 15, 5);
	
			g.setColor(Color.BLACK);
			for(DataFile d : files.getFiles())
			{
				if(d.getMonitorInfo().equals(monitors.toArray(new MonitorInfo[0])[i]))
				{
					g.drawString(String.format("%s %s %s", d.getStation(), d.getSite(), d.getMonitor()), lineStopX + 2, lineY + 4);
					break;
				}
			}
		}

		
		g.setStroke(GUIDES_STROKE);
		
		//Draw vertical divisions
		for(int i = 1;i < DEFAULT_VERTICAL_DIVISIONS;i++)
		{
			g.setColor(Color.LIGHT_GRAY);
			
			//If we are on division 1 of 4, we want to be 1/4 of the way through the border rectangle.
			int xpos = LEFT_BORDER_WIDTH + rectangleWidth * i / DEFAULT_VERTICAL_DIVISIONS;
			//We start the pixel past the top rectangle line.
			int ystart = TOP_BORDER_WIDTH + 1;
			//We end one pixel above the bottom rectangle line.
			int ystop = TOP_BORDER_WIDTH + rectangleHeight - 1;
			g.drawLine(xpos, ystart, xpos, ystop);
			
			
			//For each guide we label it with the users local time.
			g.setColor(Color.BLACK);
			
			long startMillis = startTime.getTimeInMillis();
			long stopMillis = endTime.getTimeInMillis();
			
			Calendar guidePosition = edu.stanford.sid.util.CalendarUtil.getCalendar();
			//This is a slightly different version of the equation above to get the xpos
			guidePosition.setTimeInMillis(startMillis + ((stopMillis - startMillis) / DEFAULT_VERTICAL_DIVISIONS) * i);
			
			//Generate the text, then produce a rectangle that the text fits in.
			String text = userDateFormat.format(guidePosition.getTime());
			Rectangle2D textBounds = guideFont.getStringBounds(text, g.getFontRenderContext());
			
			//Put the middle of the text just under the guide line.
			int stringXStart = xpos - (int)(textBounds.getWidth() / 2);
			//Put it 2 pixels under the border rectangle.
			int stringYStart = ystop + 2 + (int)textBounds.getHeight();
			
			
			g.drawString(text, stringXStart, stringYStart);
		}
		
		//Draw horizontal divisions
		for(int i = 1;i < DEFAULT_HORIZONTAL_DIVISIONS;i++)
		{
			g.setColor(Color.LIGHT_GRAY);
			//Identical calculations to those to determine vertical divisions.
			int ypos = TOP_BORDER_WIDTH + rectangleHeight * i / DEFAULT_HORIZONTAL_DIVISIONS;
			int xstart = LEFT_BORDER_WIDTH + 1;
			int xstop = LEFT_BORDER_WIDTH + rectangleWidth - 1;
			g.drawLine(xstart, ypos, xstop, ypos);
		}
		
		g.setStroke(new BasicStroke(1));
		
		//Get the start seconds end seconds and width for the graph.
		long xStartSeconds = startTime.getTimeInMillis() / 1000;
		long xEndSeconds = endTime.getTimeInMillis() / 1000;
		int xWidthSeconds = (int) (xEndSeconds - xStartSeconds);
		
		//This provides precision for the width and height.
		float xMod = rectangleWidth / (float)xWidthSeconds;
		g.setColor(Color.BLACK);
		
		Calendar hourTicks = edu.stanford.sid.util.CalendarUtil.duplicate(startTime);
		
		while(endTime.after(hourTicks))
		{
			int position = (int)((hourTicks.getTimeInMillis() / 1000 - xStartSeconds) * xMod);
			g.drawLine(position, TOP_BORDER_WIDTH + rectangleHeight - 1, position, TOP_BORDER_WIDTH + rectangleHeight - 6);
			hourTicks.add(Calendar.HOUR, 1);
		}
	}finally{			
		g.dispose();
	}}
	
	/**
	 * This graphs the given data file in the given color.
	 */
	private void plotFile(Graphics g, DataFile f, Color c)
	throws IOException, java.text.ParseException
	{try{
		g.setColor(c);
		
		double yMin = DEFAULT_Y_MIN;
        double yMax = DEFAULT_Y_MAX;
                
		//Predetect SuperSID and normalize data.
		try
        {
			double dataMin = dataMinMap.get(f.getMonitorInfo());
			double dataMax = dataMaxMap.get(f.getMonitorInfo());
			if(dataMin + SS_LIMIT < yMin || dataMax - SS_LIMIT > yMax)
			{
				yMin = dataMin;
				yMax = dataMax;
			}
        }catch(Exception e){//If there is any problem with this step assume Normal SID.
		}
        
        try(BufferedReader in = new BufferedReader(new FileReader(f.getFile())))
        {
		
        	//Get the start seconds end seconds and width for the graph.
        	long xStartSeconds = startTime.getTimeInMillis() / 1000;
        	long xEndSeconds = endTime.getTimeInMillis() / 1000;
        	int xWidthSeconds = (int) (xEndSeconds - xStartSeconds);
		
        	//This provides precision for the width and height.
        	double xMod = g.getClipBounds().width / (double)xWidthSeconds;
        	double yMod = (double)g.getClipBounds().height;
		
        	//Set up some starting variables.
        	int previousDataX = -1;
        	int previousMin = Integer.MAX_VALUE;
        	int previousMax = Integer.MIN_VALUE;
        	int previousDataY = -1;
		
        	String line;
		
        	do
        	{
        		in.mark(256); //mark(int) lets the input stream be reset() to the location it was marked at.
        	}while(in.readLine().startsWith("#")); //Skip all lines with # at the start.
		
        	in.reset();
		
        	while(null != (line = in.readLine()))  // in.readLine() is stored in line, then if it is not null we enter the loop.
        	{	
        		//Parse the line into integers assuming -5 to 5 is the range.
        		int dataSeconds = (int)(secondsFor(line) - xStartSeconds);
			
        		double dataPoint = (Double.parseDouble(line.substring(21)) - yMin) / (yMax - yMin);
        		//Multiplying these integers wtih the X and Y modifiers gets us pixels.
        		int x = (int)(dataSeconds * xMod);
        		int y = (int)(g.getClipBounds().height - dataPoint * yMod);
			
        		//If our previous X coordinate is different than our current, then we have moved right one pixel on the graph.
        		if(previousDataX != x)
        		{
        			//If our previous Y datapoint is not -1 (the first), then we need to draw the range the previous X location,
        			//then draw the line connecting the previous X to the current X.
        			if(previousDataY != -1)
        			{
        				g.drawLine(previousDataX, previousMin, previousDataX, previousMax);
        				g.drawLine(previousDataX, previousDataY, x, y);
        			}
				
        			//We now have a new previous min and max.
        			previousMin = y;
        			previousMax = y;
        		}else
        		{
        			//Otherwise, update the min and max to reflect the new point.
        			previousMin = Math.min(previousMin, y);
        			previousMax = Math.max(previousMax, y);
        		}			
			
        		//Finally update the previousDataX and previousDataY.
        		previousDataX = x;
        		previousDataY = y;
        	}
		
        	//When we are done, finish the graph by plotting the range of the last X location.
        	g.drawLine(previousDataX, previousMin, previousDataX, previousMax);
        }
	}finally{
		g.dispose();
	}}
	
	/**
	 * This cycles through the overlays and draws them to the graph.
	 */
	private void drawOverlays(Graphics g)
	{try{
		//Calculate the start and end seconds.
		long xStartSeconds = startTime.getTimeInMillis() / 1000;
		long xEndSeconds = endTime.getTimeInMillis() / 1000;
		
		for(Overlay o : overlays)
		{
			//If the overlay should be colored to match a monitor, set the color, otherwise
			//set it black or let it deal with the color.
			if(o.getMonitor() != null)
			{
				g.setColor(colorMap.get(o.getMonitor()));
				o.drawOverlay(g, xStartSeconds, xEndSeconds);
			}
			else
			{
				g.setColor(Color.BLACK);
				o.drawOverlay(g, xStartSeconds, xEndSeconds);
			}
		}
	}finally{
		g.dispose();
	}}
	
	/**
	 * This method adds an overlay to be drawn after the graph is complete.
	 */
	public void addOverlay(Overlay overlayToAdd)
	{
		overlays.add(overlayToAdd);
	}
	
	private Calendar temporaryCalendar = edu.stanford.sid.util.CalendarUtil.getCalendar();
	private String dateHour;
	private long dateHourSeconds;
	private java.util.ArrayList<Overlay> overlays = new java.util.ArrayList<Overlay>();
	
	//Converts a valid SID file element's time into seconds UTC.
	private long secondsFor(String s)
	{
		//If the year, month, day, hour, and minute don't equal the previous request, update the previous request.
		//In 5 second data, this cuts the number of full integer parses, and calendar calculations down to 1/12.
		if(!s.substring(0, 16).equals(dateHour))
		{
			dateHour = s.substring(0, 16);
			temporaryCalendar.clear();
			temporaryCalendar.set(Integer.parseInt(s.substring(0, 4)),
								  Integer.parseInt(s.substring(5, 7)) - 1,
								  Integer.parseInt(s.substring(8, 10)),
								  Integer.parseInt(s.substring(11, 13)),
								  Integer.parseInt(s.substring(14, 16)));
			dateHourSeconds = temporaryCalendar.getTimeInMillis() / 1000;
		}
		
		//Then parse the seconds and add it to the previous ymdh&m.
		return dateHourSeconds +
			   Integer.parseInt(s.substring(17, 19));
	}
}
