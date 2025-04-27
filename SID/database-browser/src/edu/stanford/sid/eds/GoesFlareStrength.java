package edu.stanford.sid.eds;

/**
 * This represents the strength of a solar flare in the standard form.
 */
public class GoesFlareStrength
implements Comparable<GoesFlareStrength>
{
	/**
	 * This is the category of flare.  All M flares are stronger than C flares.
	 */
	private enum FlareCategory
	implements Comparable<FlareCategory>
	{
		A("A"),
		B("B"),
		C("C"),
		M("M"),
		X("X");

		String identifier;
		
		FlareCategory(String identifier)
		{
			this.identifier = identifier;
		}
		
		public static FlareCategory getCategory(String category)
		throws IllegalArgumentException
		{
			for(FlareCategory e : values())
				if(e.identifier.equals(category)) return e;
			throw new IllegalArgumentException("No flare category for: " + category);
		}
	}
	
	private FlareCategory category;
	private double relativeStrength;

	/**
	 * Creates a new flare strength from a string.
	 */
	public GoesFlareStrength(String strength)
	throws IllegalArgumentException
	{
		category = FlareCategory.getCategory(strength.substring(0,1));
		relativeStrength = Double.parseDouble(strength.substring(1,4));
	}
	
	/**
	 * Compares two flare strengths.
	 */
	public int compareTo(GoesFlareStrength o)
	{
		if(o == null) return 0;
		if(category.compareTo(o.category) == 0)
			return ((Double)relativeStrength).compareTo((Double)o.relativeStrength);
		else
			return category.compareTo(o.category);
	}
	
	/**
	 * Retruns a string with the flare strength.
	 */
	public String toString()
	{
		return String.format("%s%1.1f", category.identifier, relativeStrength);
	}
}
