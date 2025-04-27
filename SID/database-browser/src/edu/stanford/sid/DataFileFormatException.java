package edu.stanford.sid;

/**
 * 
 * An exception that can be thrown to notify the calling program that
 * a data file that is being constructed is not in the proper data
 * file format.
 * 
 * @author Scott Winegarden, scottw@sun.stanford.edu
 *
 */
public class DataFileFormatException
extends Exception
{
	/**
	 * Default serial version for serialization.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * This makes a new DataFileFormatException.
	 * 
	 * @param error the error.
	 */
	public DataFileFormatException(String error)
	{
		super(error);
	}
}
