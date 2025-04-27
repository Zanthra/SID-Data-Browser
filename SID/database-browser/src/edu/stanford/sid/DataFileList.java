package edu.stanford.sid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import edu.stanford.sid.database.MonitorInfo;

/**
 * A list of data files.
 * 
 * @author Scott Winegarden, scottw@sun.stanford.edu
 *
 */
public class DataFileList
{
	
    private ArrayList<DataFile> dataFiles;
	
    /**
     * Creates a new data file list with all the files in teh given collection.
     * 
     * @param dataFiles the data files to include in the list.
     */
	public DataFileList(Collection<DataFile> dataFiles)
	{
		this.dataFiles = new ArrayList<DataFile>();
		this.dataFiles.addAll(dataFiles);
	}
	
	/**
	 * This returns an array list of all the files in this list.
	 * 
	 * @return all files in the list.
	 */
	public ArrayList<DataFile> getFiles()
	{
		return dataFiles;
	}
	
	/**
	 * This returns a new data file list, containing only files whose monitor IDs match
	 * one in the array provided.
	 * 
	 * @param monitorIDs a list of monitor IDs.
	 * @return a new data file list containing only files with the given monitor IDs.
	 */
    public DataFileList filterByMonitors(String[] monitorIDs)
    {
    	if(monitorIDs == null)
    	{
    		return this;
    	}
    	
    	HashSet<DataFile> filteredCollection;
    	filteredCollection = new HashSet<DataFile>();
    	
        for(String monitorID : monitorIDs)
            for(DataFile file : dataFiles)
                if(file.getMonitor().toLowerCase().startsWith(monitorID.toLowerCase()))
                    filteredCollection.add(file);
        
        return new DataFileList(filteredCollection);
    }
    
	/**
	 * Returns a DataFileList will only the files in this list that were produced by a monitor in
	 * the provided collection.
	 */
    public DataFileList filterByMonitors(Collection<MonitorInfo> monitors)
    {
    	if(monitors == null || monitors.size() == 0)
    	{
    		return this;
    	}
    	
    	HashSet<DataFile> filteredCollection = new HashSet<DataFile>();
    	
    	for(MonitorInfo monitor : monitors)
    	{
    		for(DataFile file : dataFiles)
    		{
    			if(file.getMonitor().equals(monitor.getMonitor()) &&
    					file.getSite().equals(monitor.getSite()) &&
    					file.getStation().equals(monitor.getStation()))
    			{
    				filteredCollection.add(file);
    			}
    		}
    	}
    	
    	return new DataFileList(filteredCollection);
    }
    
    /**
     * Gets an array containing all the monitors there are data files for
     * in this list.
     * 
     * @return an array of monitor IDs.
     */
    public String[] getMonitors()
    {
        HashSet<String> monitors = new HashSet<String>();
        
        for(DataFile file : dataFiles)
            monitors.add(file.getMonitor());
        
        return monitors.toArray(new String[0]);
    }
    
	/**
	 * Return a collection with one unique MonitorInfo for each that collected
	 * the data files in this collection.
	 */
    public Collection<MonitorInfo> getMonitorInfo()
    {
    	HashSet<MonitorInfo> info = new HashSet<MonitorInfo>();
    	
    	for(DataFile df : dataFiles)
    	{
    		info.add(df.getMonitorInfo());
    	}
    	
    	return info;
    }
    
	/**
	 * Returns true if there are no files in this list.
	 */
    public boolean isEmpty()
    {
    	return dataFiles.isEmpty();
    }
    
    public String toString()
    {
    	return dataFiles.toString();
    }
}
