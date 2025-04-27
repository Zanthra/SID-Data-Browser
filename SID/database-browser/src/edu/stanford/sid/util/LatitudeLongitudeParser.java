package edu.stanford.sid.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.ParseException;

/**
 * Parses latitudes and longitudes into doubles appropriate for
 * doing computations with.
 * 
 * 
 * @author Scott Winegarden (scottw@sun.stanford.edu)
 *
 */
public class LatitudeLongitudeParser
{
	private static final Pattern INTEGER_PATTERN = Pattern.compile("[+\\-]?\\d+");
	private static final Pattern DECIMAL_PATTERN = Pattern.compile("[+\\-]?\\d+\\.\\d+");
	
	private static final Pattern SOUTH_OR_WEST_PATTERN = Pattern.compile("[SsWw]");
	
	
	/**
	 * Provided with a string, will provide a double value with
	 * the decimal representation of that latitude or longitude.
	 * 
	 * 
	 * @param input A string containing a longitude or latitude.
	 * @return A double with that longitude or latitude.
	 * @throws ParseException If the longitude or latitude does not match the format.
	 */
	public static double parseLatitudeLongitude(String input)
	throws ParseException
	{
		int numbers = countNumbers(input);
		if(numbers < 1)
		{
			throw new java.text.ParseException("Empty longitude or latitude.", 0);
		}
		if(numbers == 1)
		{
			Matcher numberMatch = INTEGER_PATTERN.matcher(input);
			numberMatch.find();
			return Double.parseDouble(numberMatch.group());
		}
		if(numbers == 2)
		{
			Matcher decimalMatch = DECIMAL_PATTERN.matcher(input);
			if(decimalMatch.find())
			{
				return Double.parseDouble(decimalMatch.group());
			}
			else
			{
				Matcher integerMatch = INTEGER_PATTERN.matcher(input);
				integerMatch.find();
				int degrees = Integer.parseInt(integerMatch.group());
				integerMatch.find();
				int minutes = Integer.parseInt(integerMatch.group());
				
				if(SOUTH_OR_WEST_PATTERN.matcher(input).find()) degrees *= -1;
				
				return new LatLonConvert(degrees, minutes, 0.0d).getDecimal();
			}
		}
		if(numbers > 2)
		{
			Matcher integerMatcher = INTEGER_PATTERN.matcher(input);
			Matcher decimalMatcher = DECIMAL_PATTERN.matcher(input);
			integerMatcher.find();
			int firstIntegerEnd = integerMatcher.end();
			
			if(decimalMatcher.find() && decimalMatcher.start() > firstIntegerEnd)
			{
				int degrees = Integer.parseInt(integerMatcher.group());
				double minutes = Double.parseDouble(decimalMatcher.group());
				
				if(SOUTH_OR_WEST_PATTERN.matcher(input).find()) degrees *= -1;
				
				return new LatLonConvert(degrees, minutes, 0.0d).getDecimal();
			}
			else
			{
				int degrees = Integer.parseInt(integerMatcher.group());
				integerMatcher.find();
				int minutes = Integer.parseInt(integerMatcher.group());
				integerMatcher.find();
				double seconds;
				if(decimalMatcher.find(integerMatcher.start()))
					seconds = Double.parseDouble(decimalMatcher.group());
				else
					seconds = Integer.parseInt(integerMatcher.group());
				
				if(SOUTH_OR_WEST_PATTERN.matcher(input).find()) degrees *= -1;
				
				return new LatLonConvert(degrees, minutes, seconds).getDecimal();
			}
		}
		return 0.0d;
	}
	
	/**
	 * This counts the number of integers are in the string.  This helps
	 * identify if the latitude and longitude is degrees minutes seconds,
	 * or decimal degrees, or occasionally something inbetween.
	 * 
	 * Example:
	 * 
	 * "N31 21' 15"" would be 3
	 * "-33.5919" would be 2
	 * "12" would be 1
	 * 
	 * @param input The string to test.
	 * @return The number of full integers in the string.
	 */
	private static int countNumbers(String input)
	{
		Matcher countMatch = INTEGER_PATTERN.matcher(input);
		int count = 0;
		while(countMatch.find()) count++;
		
		return count;
	}
	
	/**
	 * Test method.
	 * 
	 * @param args ignored
	 * @throws Throwable
	 */
	public static void main(String[] args)
	throws Throwable
	{
		java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(new java.io.File("LatLon")));
		
		String s;
		
		while(null != (s = in.readLine()))
		{
			System.out.format("%20s -> %f%n", s, parseLatitudeLongitude(s));
		}
	}
}
