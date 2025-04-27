package edu.stanford.sid.graphing;

import edu.stanford.sid.DataFile;
import edu.stanford.sid.util.*;
import edu.stanford.sid.database.MonitorInfo;
import java.util.Collection;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.util.Calendar;
import java.awt.Color;
import java.awt.geom.Rectangle2D;

import edu.stanford.sid.eds.*;

/**
 * The GoesOverlay is available to display flares retrieved from the Goes archive onto the graphs.
 *
 * @author Scott Winegarden, scottw@sun.stanford.edu
 */
public class GoesOverlay
extends Overlay
{
	private Collection<GoesEvent> events;
	
	/**
	 * This creates a new GoesOverlay with the given start and end times, with a minimu flare strength.
	 * It will not display any flares weaker than specificed by the strength parameter.
	 */
	public GoesOverlay(Calendar startTime, Calendar endTime, GoesFlareStrength strength, GoesDataSource source)
	throws Exception
	{
		super(null);
		
		events = source.getListForRange(startTime, endTime, strength);
	}
	
	/**
	 * This creates a new goes oerlay with the given start and stop times, but will only display
	 * flares stronger than C2.0.
	 */
	public GoesOverlay(Calendar startTIme, Calendar endTime, GoesDataSource source)
	throws Exception
	{
		this(startTIme, endTime, new GoesFlareStrength("C2.0"), source);
	}
	
	/**
	 * This draws the overlay onto the provided Graphcs object.
	 */
	public void drawOverlay(Graphics g, long xStartSeconds, long xEndSeconds)
	{
		Font overlayFont = new Font("courier", Font.PLAIN, 10);
		g.setFont(overlayFont);
		
		g.setColor(Color.RED);
		int xWidthSeconds = (int) (xEndSeconds - xStartSeconds);
		
		float xMod = g.getClipBounds().width / (float)xWidthSeconds;
		
		for(GoesEvent e : events)
		{
			int xLocStart = (int)((e.getStartTime().getTimeInMillis() / 1000 - xStartSeconds) * xMod);
			int xLocEnd = (int)((e.getEndTime().getTimeInMillis() / 1000 - xStartSeconds) * xMod);
			
			
			g.fillRect(xLocStart, 4, xLocEnd - xLocStart, 4);
			g.drawString(e.getStrength().toString(), xLocStart - 10, 25);
		}
	}
}
