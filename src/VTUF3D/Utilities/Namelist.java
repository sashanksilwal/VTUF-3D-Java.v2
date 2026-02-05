package VTUF3D.Utilities;

import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TreeMap;

public class Namelist
{
	
	private Common common = new Common();
//	private TreeMap<String,String> namelistData = new TreeMap<String,String>();
	
	TreeMap<String,TreeMap<String,String>> namelistData = new TreeMap<String,TreeMap<String,String>> ();

	public static void main(String[] args)
	{
//		String filename = "/home/kerryn/Documents/Work/VTUF-Runs/PrestonBase/PrestonBase8/treemap.dat";
//		Namelist namelist = new Namelist(filename);
//		//System.out.println( cfm.getValue("date1"));
//		namelist.test();
		
		String filename = "/home/kerryn/Documents/Work/VTUF-Runs/PrestonBase/PrestonBase8/1/1/str1.dat";
		Namelist namelist = new Namelist(filename);
		namelist.test2();
		
		filename = "/home/kerryn/Documents/Work/VTUF-Runs/PrestonBase/PrestonBase8/1/1/points.dat";
		namelist = new Namelist(filename);
		namelist.test3();
		
	}
	
	public void test3()
	{
		System.out.println(getValue("CONTROL", "NOPOINTS"));
	}
	
	public void test2()
	{
		
		System.out.println(getValue("allom", "coefft"));
		System.out.println(getValue("allom", "expont"));
		System.out.println(getValue("allom", "winterc"));
		
		System.out.println(getValue("ladd", "jleaf"));
		
		
	}
	
	public void test()
	{
		System.out.println(getValue("buildingcount", "numberBuildingPlots"));
		System.out.println(getValue("buildinglocation", "buildingsHeight"));
//		System.out.println(namelistData.get("location"));
		int[] xlocation= getIntArrayValue("location", "xLocation");
		System.out.println(xlocation);
		System.out.println("");
		
		System.out.println(getIntValue("runSwitches", "partitioningMethod"));
		System.out.println(getIntValue("runSwitches", "usingDiffShading"));
	}

	public Namelist(String configFileName)
	{
		super();
		readNamelist(configFileName);
		
		
		
	}
	
//	&count
//	numberTreePlots=343
//	/
//
//	&location
//	xLocation=0 0
	
	public void readNamelist(String configFileName)
	{
		TreeMap<String, String> group;
		String groupname=null;
		
		ArrayList<String> namelistFile = common.readTextFileToArray(configFileName);
		int count = 0;
		for (String line : namelistFile)
		{
			count ++;
			line = line.trim();
//			System.out.println(line);
			if (line.length() < 1)
			{
				groupname = null;
				continue;
			}
			if (line.startsWith("/"))
			{
				groupname = null;
				continue;
			}
			//the start of a group
			if (line.startsWith("&"))
			{
				groupname = line.replace("&", "");
				//group = new TreeMap<String, String>();				
				continue;
			}
			//skip comments and labels before data starts
			if (groupname == null)
			{
				continue;
			}
			String[] removeComments = line.split("!");
			line = removeComments[0];
			String[] splitLine = line.split("=");
			TreeMap<String,String> item = new TreeMap<String,String>();
			String splitLine0 = splitLine[0].trim();
			String splitLine1;
			if (splitLine.length < 2)
			{
				splitLine1 = namelistFile.get(count).trim();
			}
			else
			{
				splitLine1 = splitLine[1].trim();
			}
			
			item.put(splitLine0, splitLine1);
			group = namelistData.get(groupname);
			if (group == null)
			{
				group = new TreeMap<String, String>();
			}
			group.put(splitLine0, splitLine1);
//			System.out.println(group);
			namelistData.put(groupname, group);
//			System.out.println(namelistData);
			
			
		}
//		System.out.println(namelistData.toString());
		
	}
	public String getValue(String group, String key)
	{
		TreeMap<String,String> aGroup = namelistData.get(group);
		String item = aGroup.get(key);
		return item;
	}
	
	public String getValueTrimQuotes(String group, String key)
	{
		TreeMap<String,String> aGroup = namelistData.get(group);
		String item = aGroup.get(key);
		item = item.replaceAll("'", "");
		return item;
	}
	
	public int getIntValue(String group, String key)
	{
		TreeMap<String,String> aGroup = namelistData.get(group);
		String item = aGroup.get(key);
		int intValue = new Integer(item).intValue();
		return intValue;
	}
	public double getDoubleValue(String group, String key)
	{
		TreeMap<String,String> aGroup = namelistData.get(group);
		String item = aGroup.get(key);
		double intValue = new Double(item).doubleValue();
		return intValue;
	}
	
	public int[] getIntArrayValue(String group, String key)
	{
		TreeMap<String,String> aGroup = namelistData.get(group);
		String item = aGroup.get(key);
		String[] splitItem = item.split(" ");

		int[] intValue =new int[splitItem.length];
		int count = 0;
		for (String aValue : splitItem)
		{
			intValue[count]= new Integer(aValue).intValue();
			count ++;
		}

		return intValue;
	}

	public double[] getDoubleArrayValue(String group, String key)
	{
		TreeMap<String,String> aGroup = namelistData.get(group);
		String item = aGroup.get(key);
		String[] splitItem = item.split(" ");

		double[] doubleValue = new double[splitItem.length];
		int count = 0;
		for (String aValue : splitItem)
		{
			doubleValue[count] = new Double(aValue).doubleValue();
			count++;
		}

		return doubleValue;
	}
	
//	public String getValue(String key)
//	{
//		return namelistData.get(key);
//	}
//	public String[] getValues(String key)
//	{
//		String keyValue = namelistData.get(key);
//		if (keyValue == null)
//		{
//			return new String[]{""};
//		}
//		String[] keyValueSplit = keyValue.split(",");
//		return keyValueSplit;
//	}
//	public double getDoubleValue(String key)
//	{
//		String value = namelistData.get(key);
//		return new Double(value).doubleValue();
//	}
//	
//	public Date getDateValue(String key)
//	{
//		String dateStr = namelistData.get(key);
//		//System.out.println(key + " " + dateStr) ;
//		//String formatStr = cfmData.get("date_fmt");
//		String[] dateStrSplit = dateStr.split(",");
//		//  year,month,day,hour
//		Integer year = new Integer( dateStrSplit[0]).intValue();
//		Integer month = new Integer( dateStrSplit[1]).intValue();
//		Integer day = new Integer( dateStrSplit[2]).intValue();
//		Integer hour = new Integer( dateStrSplit[3]).intValue();
//		
//		Calendar cal = Calendar.getInstance();
//		cal.set(Calendar.YEAR,year);
//		cal.set(Calendar.MONTH,month-1);
//		cal.set(Calendar.DAY_OF_MONTH,day);		 
//		cal.set(Calendar.HOUR_OF_DAY,hour);
//		
//		cal.set(Calendar.MINUTE,0);
//		cal.set(Calendar.SECOND,0);
//		cal.set(Calendar.MILLISECOND,0);
//
//		Date d = cal.getTime();	
//		return d;
//	}

	
	
	
	
	
}
