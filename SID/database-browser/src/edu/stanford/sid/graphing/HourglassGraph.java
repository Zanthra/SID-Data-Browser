package edu.stanford.sid.graphing;

import edu.stanford.sid.DataFile;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
	
public class HourglassGraph
{
	private Collection<DataFile> _sidFiles;
	private Calendar _startDate;
	private Calendar _endDate;
	private int _width;
	private int _height;
	
	private static final int SECONDS_IN_DAY = 60 * 60 * 24;
	
	public HourglassGraph(Collection<DataFile> sidFiles, Calendar startDate, Calendar endDate, int width, int height)
	{
		_sidFiles = sidFiles;
		_startDate = startDate;
		_endDate = endDate;
		_width = width;
		_height = height;
	}
	
	public BufferedImage draw()
	{
		BufferedImage drawingImage = new BufferedImage(_width, _height, BufferedImage.TYPE_3BYTE_BGR);
		
		long firstDaySeconds = _startDate.getTimeInMillis() / 1000;
		
		for(DataFile f : _sidFiles)
		{
			if(!f.getStartTime().before(_startDate) && !f.getEndTime().after(_endDate))
			{
				try(BufferedReader in = new BufferedReader(new FileReader(f.getFile())))
				{
					
					do
					{
						in.mark(256); //mark(int) lets the input stream be reset() to the location it was marked at.
					}while(in.readLine().startsWith("#")); //Skip all lines with # at the start.
					
					in.reset();
					
					Graphics2D g = drawingImage.createGraphics();
					
					String line;
					
					ArrayList<Double> dataPointsForPixel = new ArrayList<Double>();
					
					int previousX = 0;
					
					while(null != (line = in.readLine()))  // in.readLine() is stored in line, then if it is not null we enter the loop.
					{
						int dataSeconds = (int)(secondsFor(line) - firstDaySeconds);
						
						int day = dataSeconds / SECONDS_IN_DAY;
						int secondsIntoDay = dataSeconds % SECONDS_IN_DAY;
						
						//double dataPoint = (Double.parseDouble(line.substring(21)) - 10000000.000) / 100000000.000;
						double dataPoint = (Double.parseDouble(line.substring(21)) + 5d) / 10d;
						
						
						dataPointsForPixel.add(dataPoint);
						int x = (int)((double)secondsIntoDay / SECONDS_IN_DAY * _width);
						if(x != previousX)
						{
							double average = 0d;
							for(Double d : dataPointsForPixel)
							{
								average += d;
							}
							
							average /= (double)dataPointsForPixel.size();
							dataPointsForPixel.clear();
							
							int y = (int)(day);
							try
							{
								g.setColor(new Color(Color.HSBtoRGB((float)average, 1f, 1f)));
								g.drawRect(x - 1, y, 0, 0);
							}catch(Exception e)
							{
								throw new RuntimeException(String.format("x = %d y = %d color = %d", x, y, (int)(dataPoint * 256)), e);
							}
							previousX = x;
						}
					}
				}catch(Exception e)
				{}
			}
		}
		
		return drawingImage;
	}
	
	private Calendar temporaryCalendar = edu.stanford.sid.util.CalendarUtil.getCalendar();
	private String dateHour;
	private long dateHourSeconds;
	
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
