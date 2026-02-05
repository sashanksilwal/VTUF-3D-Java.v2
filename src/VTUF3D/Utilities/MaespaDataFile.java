package VTUF3D.Utilities;

import java.util.ArrayList;
import java.util.TreeMap;

public class MaespaDataFile
{
	Common common = new Common();
	TreeMap<String,ArrayList<Double>> fileData = new TreeMap<String,ArrayList<Double>>();

	public static void main(String[] args)
	{
		String file;
		MaespaDataFile data;
		
//		file = "/home/kerryn/Documents/Work/VTUF-Runs/PrestonBase/PrestonBase8/1/1/hrflux.dat";
//		data = new MaespaDataFile(file);
//		data.test();
		
		file = "/home/kerryn/Documents/Work/VTUF-Runs/PrestonBase/PrestonBase8/1/1/testflx.dat";
		data = new MaespaDataFile(file, 21);
		data.test2();

	}
	
	public MaespaDataFile(String file)
	{
		super();
		readDataFile(file);
	}
	
	public MaespaDataFile(String file, int skipLines)
	{
		super();
		readDataFile(file, skipLines);
	}
	
	public void test2()
	{
//		System.out.println(getDataArrayForVariable("FBEAM"));
		printArray(getDataArrayForVariable("FBEAM"));
		
	}
	
	public void test()
	{
		System.out.println(fileData.toString());
		System.out.println(getDataArrayForVariable("LECAN"));
		printArray(getDataArrayForVariable("TCAN"));
	}
	
    
//Columns: DOY Tree Spec HOUR hrPAR hrNIR hrTHM hrPs hrRf hrRmW hrLE LECAN Gscan Gbhcan hrH TCAN ALMAX PSIL PSILMIN CI TAIR VPD PAR ZEN AZ
//1    1    1    1            0.00            0.00         3283.89          -67.16           67.16            0.00            0.00            0.00            0.00            0.00            0.00           16.91            0.00           -0.02           -0.02          450.00     
	
	public void readDataFile(String configFileName)
	{
		String[] variables = new String[0];
		boolean atData = false;
		ArrayList<String> namelistFile = common.readTextFileToArray(configFileName);
		for (String line : namelistFile)
		{
			line = line.trim();
//			System.out.println(line);
			if (line.startsWith("Columns:"))
			{
				atData = true;
				variables = line.replace("Columns:", "").trim().split("\\s+");				
				continue;
			}
			if (!atData)
			{
				continue;
			}
			String[] dataItems = line.split("\\s+");
			int count = 0;
			for (String variable : variables)
			{
				ArrayList<Double> variableItem = fileData.get(variable);
				if (variableItem == null)
				{
					variableItem = new ArrayList<Double>();
				}
				String variableValue = dataItems[count];
//				System.out.println(variable + " " + variableValue + " " + count);
				Double varaibleValueDouble = Double.parseDouble(variableValue);
//				if (Double.isNaN(varaibleValueDouble))
//				{
//					System.out.println(variable + " " + variableValue + " " + count);
//				}
				variableItem.add(varaibleValueDouble);
				fileData.put(variable, variableItem);
				
				count ++;
			}
		}		
	}
	
	public void readDataFile(String configFileName, int skipLines)
	{
		String[] variables = new String[0];
		boolean atData = false;
		ArrayList<String> namelistFile = common.readTextFileToArray(configFileName);
		int lineCount = 0;
		for (String line : namelistFile)
		{
			line = line.trim();
//			System.out.println(line);
			
//			if (line.startsWith("Columns:"))
//			{
//				lineCount ++;
//				atData = true;
//				variables = line.replace("Columns:", "").trim().split("\\s+");				
//				continue;
//			}
			
			if (lineCount >= skipLines-1 && !atData)
			{
				lineCount ++;
				atData = true;
				variables = line.replace("Columns:", "").trim().split("\\s+");
				continue;
			}
			
			if (!atData)
			{
				lineCount ++;
				continue;
			}
			String[] dataItems = line.split("\\s+");
			int count = 0;
			for (String variable : variables)
			{
				ArrayList<Double> variableItem = fileData.get(variable);
				if (variableItem == null)
				{
					variableItem = new ArrayList<Double> ();
				}
				String variableValue = dataItems[count];
//				System.out.println(variable + " " + variableValue + " " + count);
				Double varaibleValueDouble = new Double(variableValue).doubleValue();
				variableItem.add(varaibleValueDouble);
				fileData.put(variable, variableItem);
				
				count ++;
				lineCount++;
			}
		}		
	}
	
	public double[] getDataArrayForVariable(String variable)
	{
		ArrayList<Double> variableItem = fileData.get(variable);

		if (variableItem == null) {
			System.err.println("Missing MAESPA variable: " + variable);
			System.err.println("Available variables: " + fileData.keySet());
			throw new RuntimeException("MAESPA variable not found");
		}
		
		double[] returnData = new double[variableItem.size()]; ;
		for (int i=0;i<variableItem.size();i++)
		{
			double item = variableItem.get(i);
//			if (Double.isNaN(item))
//			{
//				System.out.println("Nan");
//			}
			returnData[i]=item;
		}
		
		return returnData;
	}
	
	public void printArray(double[] arrayData)
	{
		for (double value : arrayData)
		{
			System.out.println(value);
		}
	}

}
