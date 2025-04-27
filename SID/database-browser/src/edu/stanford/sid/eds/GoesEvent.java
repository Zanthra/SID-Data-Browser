
package edu.stanford.sid.eds;

import java.util.Calendar;

/**
 * A GoesEvent is a solar flare event as measured by GOES.  It
 * includes a start time, and end time, a maximum time (the time
 * the flare was at it's strongest), and the strength of the flare.
 */
public class GoesEvent
{
	private Calendar startTime;
	private Calendar maxTime;
	private Calendar endTime;
	
	private GoesFlareStrength strength;
	
	/**
	 * Creates a new GoesEvent with the provided information.
	 */
	public GoesEvent(Calendar startTime, Calendar endTime,
		Calendar maxTime, GoesFlareStrength strength)
	{
		this.startTime = startTime;
		this.endTime = endTime;
		this.maxTime = maxTime;
		this.strength = strength;
	}
	
	/**
	 * Returns the start time of the flare.
	 */
	public Calendar getStartTime()
	{
		return startTime;
	}
	
	/**
	 * Returns the end time of the flare.
	 */
	public Calendar getEndTime()
	{
		return endTime;
	}

	/**
	 * Returns the max time of the flare.
	 */
	public Calendar getMax()
	{
		return maxTime;
	}
	
	/**
	 * Returns the strength of the flare.
	 */
	public GoesFlareStrength getStrength()
	{
		return strength;
	}
}
