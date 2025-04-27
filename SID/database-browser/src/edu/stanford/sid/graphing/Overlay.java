package edu.stanford.sid.graphing;

import edu.stanford.sid.database.MonitorInfo;
import java.awt.Graphics;

/**
 * An overlay is any information drawn onto the graph after the files are graphed.  This includes
 * sunruse and sunset indicators and Goes flare indicators.
 */
public abstract class Overlay
{
	private MonitorInfo monitorID;
	
	/**
	 * Creates a new overlay with the MonitorInfo provided.  This allows the calling code
	 * to correctly set the color of the graphics object if the overlay is specific to the
	 * monitor.
	 */
	public Overlay(MonitorInfo monitorID)
	{
		this.monitorID = monitorID;
	}
	
	/**
	 * Gets the monitor that this overlay is for.
	 */
	public MonitorInfo getMonitor()
	{
		return monitorID;
	}
	
	/**
	 * This draws the overlay onto the provided Graphcs object.  The graphcs object should have
	 * the proper bounds set to the edges of the graph area, with the start and end seconds
	 * being the UTC Seconds which anything less or greater than should not appear on the graph.
	 */
	public abstract void drawOverlay(Graphics g, long startSeconds, long endSeconds);
}
