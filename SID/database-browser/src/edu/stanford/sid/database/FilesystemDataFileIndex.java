package edu.stanford.sid.database;


import edu.stanford.sid.DataFile;
import edu.stanford.sid.DataFileList;
import edu.stanford.sid.util.CalendarUtil;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;

import java.util.logging.*;

import javax.servlet.ServletContext;


/**
 * DataFileIndex is a singleton class tied to a ServletContext, that can be
 * queried to get datafiles for a given range.  Calling getInstance will get
 * you one and only one instance of the DataFileIndex for the given 
 * ServletContext.  After you have an instance you can call getFilesInTimeRange
 * to retrieve a DataFileList of files in the given time range.<p>
 * 
 * The DataFileIndex class creates numerous temporary files, one for each day
 * in UTC, and stores the information for each data file that has data in that
 * day.  The index will first be initialized when the first call to getInstance
 * is made.  This indexing procedure can take a long time, but once it is
 * completed, the index will keep itself updated by traversing the entire data
 * Directory to find added files.<p>
 * 
 * The following is the proper method to get the instance of this class:
 * 
 * DataFileIndex.getInstance(this.getServletContext());
 * 
 * @author Scott Winegarden, scottw@sun.stanford.edu
 *
 */
public class FilesystemDataFileIndex
extends DataFileIndex
{
	/**
	 * A singleton instance generator.<p>
	 * 
	 * Get an instance of the DataFileIndex for a given ServletContext.  If an
	 * instance does not exist it will create one with a root directory defined
	 * by the servlet context's init-parameter defined by the key
	 * "root-data-directory".<p>
	 * 
	 * This method throws a NullPointerException if the DataFileIndex is null,
	 * or the context initParameter of root-data-directory, index-directory, or
	 * index-log-directory is null.
	 * 
	 * @param context the context to get the instance for.
	 * @return a DataFileIndex for the given context.
	 */
	public static DataFileIndex getInstance(ServletContext context)
	{
		//Check without synchronization for performance.
		if(!(context.getAttribute("data-index") instanceof DataFileIndex))
		{
			//Synchronize to prevent multiple instances.
			synchronized(context)
			{
				//Check with synchronization in case an instance was made after blocking.
				if(!(context.getAttribute("data-index") instanceof DataFileIndex))
				{
					//Create the DataFileIndex.
					context.setAttribute("data-index", new FilesystemDataFileIndex(
							new File(context.getInitParameter("root-data-directory")),
							new File(context.getInitParameter("index-directory")),
							new File(context.getInitParameter("index-log-directory")),
							600000));
				}
			}
		}
		
		//Return the DataFileIndex.
		return (DataFileIndex)context.getAttribute("data-index");
	}
	
	/**
	 * 
	 * This class stores a single index file for the Data Files.  The information
	 * contained in the index file is not referenced by the DataIndexFile class,
	 * so it does not take space in memory.
	 * 
	 * @author Scott Winegarden, scottw@sun.stanford.edu
	 *
	 */
	private class DataIndexFile
	{
		private File indexFile;
		private int numberOfFiles = 0;
		private Calendar date;
		
		/**
		 * Constructs a new empty DataIndexFile.  If there is any problem
		 * creating the indexFile a IOException will be thrown.
		 * 
		 * @param date the date this DataIndexFile represents.
		 * @throws IOException if there is an error writing the index file.
		 */
		public DataIndexFile(Calendar date)
		throws IOException
		{
			this.date = date;
			createFile();
		}
		
		/**
		 * This method writes the given collection of files to disk.
		 * 
		 * @param files the collection of files to write to disk.
		 * @throws IOException if there is an error writing the files.
		 */
		private void writeFiles(Collection<DataFile> files)
		throws IOException
		{
			try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(indexFile)))
			{
				numberOfFiles = files.size();
				
				for(DataFile file : files)
				{
					out.writeObject(file);
				}
			}
		}
		
		/**
		 * This method reads the collection of data files from the disk.
		 * 
		 * @return a collection of DataFile objects in this index file.
		 * @throws IOException if the file cannot be read.
		 * @throws ClassNotFoundException if it cannot identify a serialized class.
		 */
		private Collection<DataFile> readFiles()
		throws IOException, ClassNotFoundException
		{
			Collection<DataFile> files = new HashSet<DataFile>();
			
			if(numberOfFiles != 0)
			{
				try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(indexFile)))
				{
					for(int i = 0;i < numberOfFiles;i++)
					{
						files.add((DataFile)in.readObject());
					}
				}
			}
			
			return files;
		}
		
		/**
		 * Reads the collection of data files from disk.
		 * 
		 * @return a collection containing all the data files from the
		 * file the class represents.
		 * @throws IOException if there is an error reading files.
		 * @throws ClassNotFoundException if the DataFile class cannot
		 * be found.
		 */
		public synchronized Collection<DataFile> getFiles()
		throws IOException, ClassNotFoundException
		{
			Collection<DataFile> files = null;
			
			if(cache.cacheHit(this))
			{
				files = cache.getCachedList(this);
			}
			else
			{
				files = readFiles();
			}
			
			cache.cacheAdd(this, files);
			return files;
		}
		
		/**
		 * Adds a file to the collection of data files on disk.
		 * 
		 * @param file is the file to be added.
		 * @throws IOException if there is an error reading or writing the
		 * index file.
		 * @throws ClassNotFoundException if the DataFile class cannot be found.
		 */
		public synchronized void addFile(DataFile file)
		throws IOException, ClassNotFoundException
		{
			cache.cacheRemove(this);
			
			Collection<DataFile> files = readFiles();
			files.add(file);
			writeFiles(files);
		}
		
		private void createFile()
		throws IOException
		{
			indexFile = File.createTempFile(String.format("%d.", date.getTimeInMillis()), ".temp-index-file", indexDirectory);
			indexFile.deleteOnExit();
		}
		
		/**
		 * Removes all nonexistant files from the collection.  This
		 * makes sure that all files in the collection actually exsist.
		 *
		 */
		public synchronized void verify()
		{
			try
			{
				//Read the list in.
				Collection<DataFile> files = getFiles();
				DataFile f = null;
				//Iterate over the list of files, if you find one that no longer
				//exists, remove it from the list.
				for(java.util.Iterator<DataFile> i = files.iterator();i.hasNext();f = i.next())
				{
					if(!f.getFile().exists())
					{
						i.remove();
					}
				}
				//Write the list out.
				writeFiles(files);
			}catch(Exception e)
			{
				try
				{
					if(!indexFile.exists())
					{
						createFile();
					}
					writeFiles(new HashSet<DataFile>());
				}catch(IOException ex){throw new RuntimeException("Cannot write index file: " + indexFile.toString());}
			}
		}
		
		/**
		 * Returns true if this index file has any files of the given monitor.
		 * If the monitor array is null, that will return true if there are any
		 * monitors in this index file.
		 * 
		 * @param monitors
		 * @return
		 */
		public synchronized boolean hasFiles(Collection<MonitorInfo> monitors)
		{
			if(numberOfFiles == 0)
			{
				//If there are no files, it cannot have files for
				//the given monitors.
				return false;
			}else if(monitors == null || monitors.size() == 0)
			{
				//If there are files, and you are not looking for
				//a specific one, you have found one.
				return true;
			}else
			{
				try
				{
					//Otherwise lets go hunting.
					for(DataFile d : getFiles())
					{
						for(MonitorInfo monitor : monitors)
						{
							//For each file, check its monitor against each monitor
							//passed as an argument, if we find a match, we are done.
							if(new MonitorInfo(d).equals(monitor))
							{
								return true;
							}
						}
					}
					//No such luck, none found.
					return false;
				}catch(Exception e){
					//If anything goes wrong ignore it, and return false.
					return false;
				}
			}
		}
		
		/**
		 * Cleans up the resources used by this index file.
		 */
		protected void finalize()
		throws Throwable
		{
			super.finalize();
			indexFile.delete();
		}
	}
	
	/**
	 * A timer task that updates the index every time it is run.
	 * 
	 * @author Scott Winegarden, scottw@sun.stanford.edu
	 *
	 */
	private class UpdateIndexTask
	extends TimerTask
	{
		/**
		 * When this method is run, if the number of files in the
		 * root directory is different than the last time it was called
		 * will clear all index files of deleted files, and add all new
		 * files to the index.
		 * 
		 */
		public void run()
		{
			//Set this to minimum priority so that the requests can get processed faster.
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			
			//Variables for the file handler and temporary file.
			FileHandler fileHandler = null;
			File tempFile = null;
			try
			{
				//Create a temporary file in the log directory.
				tempFile = File.createTempFile(String.format("%s-", java.text.DateFormat.getDateTimeInstance().format(new java.util.Date())),".log", indexLogDirectory);
				
				//Create the log file handler.
				fileHandler = new FileHandler(tempFile.getAbsolutePath(), false);
				fileHandler.setFormatter(new java.util.logging.SimpleFormatter());
			}catch(Exception e){}
			
			//If we managed to create a handler properly, add it to the logger.
			if(fileHandler != null)
			{
				indexingLogger.addHandler(fileHandler);
			}
			
			indexingLogger.fine("Indexing Started");
			
			verifyFiles(); //Verify files in the index exist.
			processFilesInDirectory(rootDataDirectory); //Process all the files in the index directory.
			
			//At least one complete update has finished.
			updateComplete = true;
			
			//If we managed to create a new log file, we are done with it and should clean up the old one.
			if(fileHandler != null)
			{
				indexingLogger.removeHandler(fileHandler);
				fileHandler.close();
			}
		}
		
		/**
		 * Called by the run action to remove all deleted files from all
		 * index files.
		 *
		 */
		private void verifyFiles()
		{
			for(DataIndexFile indexFile : indexFiles.values())
			{
				indexFile.verify();
			}
		}
		
		/**
		 * Called by the run action to recursively process all files in
		 *  the directory.
		 *  
		 *  Simply, for each file in the directory, if it is a directory
		 *  process all the files in it, if it is a file, process it.
		 * 
		 * @param rootDirectory the directory to process.
		 */
		private void processFilesInDirectory(File rootDirectory)
		{
			indexingLogger.finer(String.format("Processing Directory: %s", rootDirectory.getAbsolutePath()));
			for(File file : rootDirectory.listFiles())
			{
				if(file.isDirectory())
				{
					processFilesInDirectory(file);
				}else
				{
					try
					{
						indexingLogger.fine(String.format("Found File: %s", file.getAbsolutePath()));
						processFile(file);
					}catch(Exception e)
					{
						indexingLogger.log(Level.WARNING, String.format("Could not index file: %s", file.getAbsolutePath()), e);
					}
				}
			}
		}
		
		/**
		 * Processes a single file, and if it is a valid data file, will
		 * add it to the index.
		 * 
		 * @param file the file to process.
		 */
		private void processFile(File file)
		throws Exception
		{
			//If the file does not match a standard naming convention, we are done.
			if(!file.getName().matches(".*(dat|csv|txt)")) return;
			
			indexingLogger.fine(String.format("Indexing file: %s", file.getAbsolutePath()));
			
			DataFile df = new DataFile(file);		//Construct the data file.
			monitorSet.add(new MonitorInfo(df));	//Add its monitor info to the set.
				
			//For each day in UTC the monitor has data for, add it to the monitor map.
			for(Calendar date : CalendarUtil.getDaysInRange(df.getStartTime(), df.getEndTime()))
			{
				if(indexFiles.get(date) == null) 
				{
					//If we don't have a date for this entry, create one.
					indexFiles.put(date, new DataIndexFile(date));
				}
				//Add it to this date's entry.
				indexFiles.get(date).addFile(df);
			}
		}
	}
	
	/**
	 * This is a <b>very</b> simple implementation of a cache.
	 * 
	 * It stores two lists, one of keys and one of values.  When data files
	 * are requested, you can check to see if they are in the cache with cacheHit
	 * method.  If it returns true, you can use the getChachedList method to retrieve
	 * the cached data.  Each time you read the data from an index file due to a
	 * cache miss, call the cacheAdd method to speed up the next request for that data.
	 * 
	 * The cacheAdd method automatically removes the first object from the cache if
	 * the cache has more objects than cacheSize;
	 * 
	 * @author Scott Winegarden
	 *
	 */
	private class IndexFileCache
	{
		public IndexFileCache()
		{
			cacheData = new ArrayList<Collection<DataFile>>();
			cacheKeys = new ArrayList<DataIndexFile>();
		}
		
		private ArrayList<Collection<DataFile>> cacheData;
		private ArrayList<DataIndexFile> cacheKeys;
		
		public synchronized Collection<DataFile> getCachedList(DataIndexFile indexFile)
		{
			return cacheData.get(cacheKeys.indexOf(indexFile));
		}
		
		public synchronized boolean cacheHit(DataIndexFile indexFile)
		{
			return cacheKeys.contains(indexFile);
		}
		
		public synchronized void cacheAdd(DataIndexFile indexFile, Collection<DataFile> data)
		{
			cacheRemove(indexFile);
			
			if(cacheKeys.size() >= cacheSize)
			{
				cacheKeys.remove(0);
				cacheData.remove(0);
			}
			
			cacheKeys.add(indexFile);
			cacheData.add(data);
		}
		
		public synchronized void cacheRemove(DataIndexFile indexFile)
		{
			while(cacheKeys.contains(indexFile))
			{
				cacheData.remove(cacheKeys.indexOf(indexFile));
				cacheKeys.remove(indexFile);
			}
		}
	}
	
	
	private File rootDataDirectory;
	private File indexDirectory;
	
	private File indexLogDirectory;
	
	private IndexFileCache cache;
	private int cacheSize = 31;
	
	/**
	 * Map of index files to calendar days.
	 */
	private Map<Calendar, DataIndexFile> indexFiles;
	
	/**
	 * Information on the various monitors the index has data for.
	 */
	private Set<MonitorInfo> monitorSet;
	
	
	private Timer updateTimer = new Timer();
	
	/**
	 * This keeps track of whether the index has completed its initial
	 * update.
	 */
	private boolean updateComplete = false;
	
	private Logger indexingLogger;
	
	/**
	 * Create a DataFileIndex for the given directory.
	 * 
	 * @param rootDataDirectory the root directory.
	 */
	FilesystemDataFileIndex(File rootDataDirectory, File indexDirectory, File indexLogDirectory, int updateInterval)
	{
		System.out.format("%s  %s  %s  %d%n", rootDataDirectory, indexDirectory, indexLogDirectory, updateInterval);
		
		this.rootDataDirectory = rootDataDirectory;
		this.indexDirectory = indexDirectory;
		this.indexLogDirectory = indexLogDirectory;
		
		indexingLogger = Logger.getAnonymousLogger();
		indexingLogger.setLevel(Level.FINEST);
		cache = new IndexFileCache();
		
		for(File f : indexDirectory.listFiles())
		{
			if(f.getName().endsWith(".temp-index-file"))
				f.delete();
		}
		
		indexFiles = new HashMap<Calendar, DataIndexFile>();
		monitorSet = new HashSet<MonitorInfo>();
		
		updateTimer.schedule(new UpdateIndexTask(), 0, updateInterval);
	}
	
	public edu.stanford.sid.util.StationLatitudeLongitude getStationLatitudeLongitude(String identifier)
	{return null;}
	
	
	/**
	 * Retrieve a list of data files from the time span starting at
	 * startTime, and ending at endTime.  This method checks to be
	 * sure that all files returned start before the end time, and
	 * end after the start time.
	 * 
	 * @param startTime the start of the time range.
	 * @param endTime the end of the time range.
	 * @return a DataFileList full of data files from the given time range.
	 */
	public DataFileList getFilesFromTimeRange(Calendar startTime, Calendar endTime)
	{
		Collection<DataFile> dataFiles = new HashSet<DataFile>();
		
		//Check if we are requested for more than a month worth of data.  This could take up
		//more memory than is available.
		Calendar timeTest = (Calendar)startTime.clone();
		timeTest.add(Calendar.MONTH, 1);
		if(timeTest.before(endTime))
		{
			throw new RuntimeException("Time range cannot be greater than one month.");
		}
		
		for(Calendar date : CalendarUtil.getDaysInRange(startTime, endTime))
		{
			if(indexFiles.containsKey(date))
			{
				try
				{
					for(DataFile d : indexFiles.get(date).getFiles())
					{
						if(d.getFile().exists())
						{
							if(d.getStartTime().before(endTime) && d.getEndTime().after(startTime))
							{
								dataFiles.add(d);
							}
						}
					}
				}catch(IOException e)
				{}catch(ClassNotFoundException e)
				{}
			}
		}
		
		return new DataFileList(dataFiles);
	}
	
	/**
	 * Gets a sorted list of all the monitors in the index.
	 * 
	 * null is an acceptable value for the comparator, in this case it will
	 * not sort the monitors.
	 * 
	 * @param comp
	 * @return
	 */
	public Collection<MonitorInfo> getMonitors(Comparator<MonitorInfo> comp)
	{
		ArrayList<MonitorInfo> list = new ArrayList<MonitorInfo>();
		list.addAll(monitorSet);
		
		if(comp != null)
		{
			java.util.Collections.sort(list, comp);
		}
		
		return list;
	}
	
	/**
	 * Gets a list of days that the provided monitors have data for since 2003.
	 */
	public Collection<Calendar> getDaysWithData(Collection<MonitorInfo> monitors)
	{
		Calendar start = CalendarUtil.getCalendar(2003, 1, 1);
		Calendar end = CalendarUtil.getCalendar();
		
		ArrayList<Calendar> daysWithData = new ArrayList<Calendar>();
		
		for(Calendar c : CalendarUtil.getDaysInRange(start, end))
		{
			Calendar next = CalendarUtil.duplicate(c);
			next.add(Calendar.DATE, 1);
			if(hasFilesInRange(c, next, monitors)) daysWithData.add(c);
		}
		
		return daysWithData;
	}
	
	/**
	 * Returns true if there are datafiles in the given range for a given set of monitors.
	 * If monitors is null, this will assume all monitors.
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public boolean hasFilesInRange(Calendar startTime, Calendar endTime, Collection<MonitorInfo> monitors)
	{
		for(Calendar date : CalendarUtil.getDaysInRange(startTime, endTime))
		{
			if(indexFiles.containsKey(date))
			{
				if(indexFiles.get(date).hasFiles(monitors))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns true if the index has indexed all file in the root directory
	 * at least once.  If this is false, any requests for data may not reflect
	 * the Sid Data.
	 * 
	 * @return true if the index has updated once already.
	 */
	public boolean isUpdateComplete()
	{
		return updateComplete;
	}
	
	/**
	 * There is no need to close any resources after the index is done being accessed.
	 */
	public void close()
	{
		
	}
}
