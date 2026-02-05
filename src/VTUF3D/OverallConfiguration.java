package VTUF3D;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;

import VTUF3D.Utilities.Common;
import VTUF3D.Utilities.MaespaDataFile;
import VTUF3D.Utilities.Namelist;

public class OverallConfiguration
{
	String rootDirectory ;
	Common common = new Common();
	
	public int[][] treeXYMap;
	public int[][] treeXYTreeMap;
	

	

//	public static void main(String[] args)
//	{
//		String rootDirectory = "/home/kerryn/git/2018-03-MasterITProject/VTUF3DTesting/PrestonBaseSmall/";
//		OverallConfiguration overall = new OverallConfiguration(rootDirectory);
//		
//		overall.readParametersDat();
//		
//		MaespaConfigTreeMapState treeMapFromConfig = overall.readMaespaTreeMapFromConfig(overall.rootDirectory);
//		treeMapFromConfig.rootDirectory=rootDirectory;
//		TreeMap<String,ArrayList<MaespaDataResults>> returnValues = overall.mapTrees(treeMapFromConfig);
//
//		
//
//		
//		overall.readMaespaTestData(treeMapFromConfig, overall.treeXYMap, rootDirectory);
//		
//		int x=1;
//		int y=3;
//		int tree = overall.treeXYMap[x][y];
//		
//		TreeMap<String,TreeMap<String,Namelist>> namelists = overall.readNamelists(treeMapFromConfig);
//
//		TreeMap<String,Namelist> namelistForTree = namelists.get(tree + "_1");
//		Namelist treedatnamelist = namelistForTree.get("treesdatfilestrnamelist");
//		
//		double crownHeight = treedatnamelist.getDoubleValue("allhtcrown", "values");
//		double trunkHeight = treedatnamelist.getDoubleValue("allhttrunk", "values");
//		double treeHeight = crownHeight + trunkHeight;
//
//
//		
//
//	}
	
	public OverallConfiguration(String rootDirectory)
	{
		super();
		this.rootDirectory = rootDirectory;
	}
	
	public static int getVegHeight(HashMap<String,HashMap<String,Namelist>> namelists, int x, int y, int[][] treeXYMap)
	{
		int tree = treeXYMap[x][y];
		HashMap<String,Namelist> namelistForTree = namelists.get(tree + "_1");
		if (namelistForTree == null)
		{
			return 0;
		}
		Namelist treedatnamelist = namelistForTree.get("treesdatfilestrnamelist");
		
		double crownHeight = treedatnamelist.getDoubleValue("allhtcrown", "values");
		double trunkHeight = treedatnamelist.getDoubleValue("allhttrunk", "values");
		double treeHeight = crownHeight + trunkHeight;
		int treeHeightInt = (int)Math.round(treeHeight);
		
		return treeHeightInt;
	}
	
	
	public HashMap<String,HashMap<String,Namelist>> readNamelists(MaespaConfigTreeMapState treeMapFromConfig)
	{
//		MaespaTreeConfiguration treeConfig = new MaespaTreeConfiguration();
		HashMap<String,HashMap<String,Namelist>> returnValues = new HashMap<String,HashMap<String,Namelist>>();
		String rootDirectory = treeMapFromConfig.rootDirectory;
		treeXYMap = new int[treeMapFromConfig.width][treeMapFromConfig.length];
		treeXYTreeMap = new int[treeMapFromConfig.width][treeMapFromConfig.length];
		int x,y;
        int numberOfTreePlots;

        int treeFilesNumber;
        int treeNumber;
     
        numberOfTreePlots = treeMapFromConfig.numberTreePlots;
 
        for (int loopCount= 0;loopCount < numberOfTreePlots;loopCount++)
        {
                x=treeMapFromConfig.xLocation[loopCount];
                y=treeMapFromConfig.yLocation[loopCount];
                treeFilesNumber=treeMapFromConfig.treesfileNumber[loopCount];
                treeNumber=treeMapFromConfig.trees[loopCount];
                treeXYMap[x][y]=treeFilesNumber  ;
                treeXYTreeMap[x][y]=treeNumber;
          
                for (int diffShadingType =1;diffShadingType < 3;diffShadingType++)
                {
                	HashMap<String,Namelist> maespaNamelists= returnValues.get(treeFilesNumber+"_"+diffShadingType);
                	if (maespaNamelists == null)
                	{
                		maespaNamelists = readMaespaNamelistFiles(treeFilesNumber, diffShadingType, rootDirectory);
                	}
                	returnValues.put(treeFilesNumber+"_"+diffShadingType, maespaNamelists);
                }     
        }
        return returnValues;
	}
	
	  public HashMap<String,Namelist> readMaespaNamelistFiles(int treeConfigLocation, int diffShadingType, String rootDirectory)
	    {
		  HashMap<String,Namelist> returnValues = new HashMap<String,Namelist>();
	      String  treeConfigLocationStr;
	      String  diffShadingTypeStr;
	      String  treesdatfilestr;  
	      String  strdatfilestr  ;
	      String  phydatfilestr  ;
	     treeConfigLocationStr = treeConfigLocation+"";

	     diffShadingTypeStr = diffShadingType+"";
	    
	    treesdatfilestr = rootDirectory + treeConfigLocationStr + "/" + diffShadingTypeStr + "/trees.dat";    
	    strdatfilestr = rootDirectory + treeConfigLocationStr + "/" + diffShadingTypeStr + "/str1.dat";
	    phydatfilestr = rootDirectory + treeConfigLocationStr + "/" + diffShadingTypeStr + "/phy1.dat";

		Namelist strdatfilestrnamelist = new Namelist(strdatfilestr);
	    Namelist treesdatfilestrnamelist = new Namelist(treesdatfilestr);
	    Namelist phydatfilestrnamelist = new Namelist(phydatfilestr);

	    returnValues.put("strdatfilestrnamelist", strdatfilestrnamelist);
	    returnValues.put("treesdatfilestrnamelist", treesdatfilestrnamelist);
	    returnValues.put("phydatfilestrnamelist", phydatfilestrnamelist);
	    return  returnValues;


	    }
	
	public ParametersDat readParametersDat()
	{
		ParametersDat parameters = new ParametersDat();
		
		String line;
//		String[] lineSplit;
		double[] lineSplitDouble ;
		boolean[] lineSplitBoolean;
		
		String file = rootDirectory + "parameters.dat";
		ArrayList<String> namelistFile = common.readTextFileToArray(file);
		int count = 0;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.vfcalc = (int)lineSplitDouble[0];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.yd = (int) Math.round(lineSplitDouble[0]);
		parameters.deltat = lineSplitDouble[1];
		parameters.outpt_tm = lineSplitDouble[2];
		
		parameters.restartedRun = false;
		parameters.restartedRunStartTimestep = 0;
//		parameters.restartedRunNumber = 0;
		if (lineSplitDouble.length > 5)
		{
			double restart = lineSplitDouble[4];
			if (restart==1)
			{
				parameters.restartedRun = true;
			}
			parameters.restartedRunStartTimestep = (int) Math.round(lineSplitDouble[5]);
//			parameters.restartedRunNumber = (int) Math.round(lineSplitDouble[6]);
			parameters.year = (int) Math.round(lineSplitDouble[3]);
		}
		else if (lineSplitDouble.length > 3)
		{
			parameters.year = (int) Math.round(lineSplitDouble[3]);
		}
		else
		{
//			parameters.year = 2004;
			System.out.println("missing year from parameters.dat");
			System.exit(1);
		}
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.Tthreshold = lineSplitDouble[0];
		count++;
		
		
		
		line = namelistFile.get(count);
		lineSplitBoolean = common.splitBooleanString(line);
		parameters.facet_out = lineSplitBoolean[0];
		parameters.matlab_out = lineSplitBoolean[1];
		parameters.sum_out = lineSplitBoolean[2];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.dalb = lineSplitDouble[0];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.albr = lineSplitDouble[0];
		parameters.albs = lineSplitDouble[1];
		parameters.albw = lineSplitDouble[2];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.emisr = lineSplitDouble[0];
		parameters.emiss = lineSplitDouble[1];
		parameters.emisw = lineSplitDouble[2];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.cloudtype = (int)lineSplitDouble[0];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.IntCond = lineSplitDouble[0];
		parameters.Intresist = lineSplitDouble[1];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.uc = lineSplitDouble[0];
		parameters.numlayers = (int)lineSplitDouble[1];
		count++;
		
		parameters.thickr = new double[parameters.numlayers];
		parameters.lambdar = new double[parameters.numlayers];
		parameters.htcapr = new double[parameters.numlayers];
		for (int i=0;i<parameters.numlayers;i++)
		{
			line = namelistFile.get(count);
			lineSplitDouble = common.splitDoubleString(line);
			parameters.thickr[i]= lineSplitDouble[0];
			parameters.lambdar[i]= lineSplitDouble[1];
			parameters.htcapr[i]= lineSplitDouble[2];
			count++;
		}
		
		parameters.thicks = new double[parameters.numlayers];
		parameters.lambdas = new double[parameters.numlayers];
		parameters.htcaps = new double[parameters.numlayers];
		for (int i=0;i<parameters.numlayers;i++)
		{
			line = namelistFile.get(count);
			lineSplitDouble = common.splitDoubleString(line);
			parameters.thicks[i]= lineSplitDouble[0];
			parameters.lambdas[i]= lineSplitDouble[1];
			parameters.htcaps[i]= lineSplitDouble[2];
			count++;
		}
		
		parameters.thickw = new double[parameters.numlayers];
		parameters.lambdaw = new double[parameters.numlayers];
		parameters.htcapw = new double[parameters.numlayers];
		for (int i=0;i<parameters.numlayers;i++)
		{
			line = namelistFile.get(count);
			lineSplitDouble = common.splitDoubleString(line);
			parameters.thickw[i]= lineSplitDouble[0];
			parameters.lambdaw[i]= lineSplitDouble[1];
			parameters.htcapw[i]= lineSplitDouble[2];
			count++;
		}
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.z0 = lineSplitDouble[0];
		parameters.lambdaf = lineSplitDouble[1];
		parameters.zrooffrc = lineSplitDouble[2];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.z0roofm = lineSplitDouble[0];
		parameters.z0roadm = lineSplitDouble[1];
		parameters.z0roofh = lineSplitDouble[2];
		parameters.z0roadh = lineSplitDouble[3];
		parameters.moh = lineSplitDouble[4];
		parameters.rw = lineSplitDouble[5];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.buildht_m = lineSplitDouble[0];
		parameters.zref = lineSplitDouble[1];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.minres = (int)lineSplitDouble[0];
		count++;

		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.Tsfcr = lineSplitDouble[0];
		parameters.Tsfcs = lineSplitDouble[1];
		parameters.Tsfcw = lineSplitDouble[2];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.Tintw = lineSplitDouble[0];
		parameters.Tints = lineSplitDouble[1];
		parameters.Tfloor = lineSplitDouble[2];
		parameters.Tbuild_min = lineSplitDouble[3];
		count++;

		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.stror_in = lineSplitDouble[0];
		parameters.strorint = lineSplitDouble[1];
		parameters.strormax = lineSplitDouble[2];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.xlat_in = lineSplitDouble[0];
		parameters.xlatint = lineSplitDouble[1];
		parameters.xlatmax = lineSplitDouble[2];
		count++;
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.numlp = (int)lineSplitDouble[0];
		count++;
		
		parameters.lpin = new double[parameters.numlp];
		for (int i=0;i<parameters.numlp;i++)
		{
			line = namelistFile.get(count);
			lineSplitDouble = common.splitDoubleString(line);
			parameters.lpin[i]= lineSplitDouble[0];
			count++;
		}
		
		line = namelistFile.get(count);
		lineSplitDouble = common.splitDoubleString(line);
		parameters.numbhbl = (int)lineSplitDouble[0];
		count++;
		
		parameters.bh_o_bl = new double[parameters.numbhbl];
		for (int i=0;i<parameters.numbhbl;i++)
		{
			line = namelistFile.get(count);
			lineSplitDouble = common.splitDoubleString(line);
			parameters.bh_o_bl[i]= lineSplitDouble[0];
			count++;
		}
		
		return parameters;
	}
	
	public HashMap<String,MaespaDataFile> readMaespaTestflxData(HashMap<String,ArrayList<MaespaDataResults>> maespaResults)
	{
		HashMap<String,MaespaDataFile> testflxResults = new HashMap<String,MaespaDataFile>();		
        String treeConfigLocation;
        String treeConfigLocationStr;      
		
		Set<String> keys = maespaResults.keySet();
		for (String key : keys)
		{
			String[] splitKey = key.split("_");		
	        treeConfigLocation=splitKey[0]      ; 
	        treeConfigLocationStr=treeConfigLocation+"";
        	MaespaDataFile data = readMaespaTestflxDataFiles(rootDirectory + treeConfigLocationStr + "/" + splitKey[1]  , rootDirectory);
        	testflxResults.put(key, data);
		}
		return testflxResults;
	}
	
    public MaespaDataFile readMaespaTestflxDataFiles(String testflxLocation, String rootDirectory)
    {
	     int linesToSkip;
	     String testflxfilestr;  
	     testflxfilestr = testflxLocation + "/testflx.dat"; 
	     linesToSkip = 21; 
		 MaespaDataFile data = new MaespaDataFile(testflxfilestr, linesToSkip);
	     return data;
    }
     
	
    public void readMaespaTestData(MaespaConfigTreeMapState treeMapFromConfig, int[][] treeXYMap, String rootDirectory)
    {
        int  nr_points;
        int treeConfigLocation;
        String treeConfigLocationStr;
        String pointsfilestr;   
        treeConfigLocation=1; 
        treeConfigLocationStr=treeConfigLocation+"";
        pointsfilestr = rootDirectory + treeConfigLocationStr + "/" + "1" + "/points.dat";    	
		Namelist pointsnamelist = new Namelist(pointsfilestr);      
        nr_points = pointsnamelist.getIntValue("CONTROL", "NOPOINTS");      
        for (int loopCount= 1;loopCount < treeMapFromConfig.numberTreePlots; loopCount ++)
        {
        	MaespaDataFile data = readMaespaTestDataFiles(nr_points, treeConfigLocation, rootDirectory);

        }
//        System.out.println(nr_points);

    }
	
    public MaespaDataFile readMaespaTestDataFiles(int nr_points, int treeConfigLocation, String rootDirectory)
    {

	     int linesToSkip;	
	//     int  n,i, nr_lines, nr_elements;
	//     int  hr_nr_lines;
	
	     String   treeConfigLocationStr;
	     String  testflxfilestr;    
	     treeConfigLocationStr=treeConfigLocation+"";
	     testflxfilestr = rootDirectory + treeConfigLocationStr+ "/1" + "/testflx.dat";         
	     linesToSkip = 21;         
	     MaespaDataFile data = new MaespaDataFile(testflxfilestr, linesToSkip); 
	     return data;
    }
     
	
	public HashMap<String,ArrayList<MaespaDataResults>> mapTrees(MaespaConfigTreeMapState treeMapFromConfig)
	{
		MaespaTreeConfiguration treeConfig = new MaespaTreeConfiguration();
		HashMap<String,ArrayList<MaespaDataResults>> returnValues = new HashMap<String,ArrayList<MaespaDataResults>>();
		String rootDirectory = treeMapFromConfig.rootDirectory;
		treeXYMap = new int[treeMapFromConfig.width][treeMapFromConfig.length];
		treeXYTreeMap = new int[treeMapFromConfig.width][treeMapFromConfig.length];
		int x,y;
//        int[] treeLocationMap; // only load each tree config once
//        boolean dataLoaded = false;
        int numberOfTreePlots;
//        int width,length;
        double gridSize;
        int treeFilesNumber;
        int treeNumber;
//        int diffShadingType = 1;
        
        numberOfTreePlots = treeMapFromConfig.numberTreePlots;
//        treeLocationMap = new int[numberOfTreePlots];
//        width = treeMapFromConfig.width;
//        length = treeMapFromConfig.length;
        gridSize = treeMapFromConfig.configTreeMapGridSize;
        		
//		System.out.println("numberOfTreePlots " + numberOfTreePlots);
        for (int loopCount= 0;loopCount < numberOfTreePlots;loopCount++)
        {
                x=treeMapFromConfig.xLocation[loopCount];
                y=treeMapFromConfig.yLocation[loopCount];
                treeFilesNumber=treeMapFromConfig.treesfileNumber[loopCount];
                int treeHeight= treeMapFromConfig.treesHeight[loopCount];
                treeNumber=treeMapFromConfig.trees[loopCount];
                treeXYMap[x][y]=treeFilesNumber  ;
                treeXYTreeMap[x][y]=treeNumber;
//                System.out.println(loopCount + " " + x + " " + y + " " + treeFilesNumber + " " + treeNumber + " " + numberOfTreePlots + " " + treeHeight);
                
                for (int diffShadingType =1;diffShadingType < 3;diffShadingType++)
                {
                	ArrayList<MaespaDataResults> maespaHRWatData= returnValues.get(treeFilesNumber+"_"+diffShadingType);
                	if (maespaHRWatData == null)
                	{
                		maespaHRWatData = treeConfig.readMaespaHRWatDataFiles(treeFilesNumber, gridSize, diffShadingType, rootDirectory);
                	}
                	returnValues.put(treeFilesNumber+"_"+diffShadingType, maespaHRWatData);
                }
                
                
                
        }
//        System.out.println(returnValues.toString());
        return returnValues;
	}
	

	
	 public MaespaConfigTreeMapState readMaespaTreeMapFromConfig(String rootDirectory)
	 {
		MaespaConfigTreeMapState state = new MaespaConfigTreeMapState();
	    int numberTreePlots;
	    int numberBuildingPlots;
	    int width,length;
	    int[] xLocation, yLocation;
	    int[] xBuildingLocation, yBuildingLocation;
	    int[] phyfileNumber,strfileNumber,treesfileNumber, treesHeight,trees;
	    int[] buildingsHeight;
//	    int[]  partitioningMethod;
//	    int[]  usingDiffShading;
	    
	    int configTreeMapCentralArrayLength;
	    int configTreeMapCentralWidth;
	    int configTreeMapCentralLength;
	    int configTreeMapX;
	    int configTreeMapY;
	    int configTreeMapX1;
	    int configTreeMapX2;
	    int configTreeMapY1;
	    int configTreeMapY2;
	    int configTreeMapNumsfcab;
	    double configTreeMapGridSize;
	    int configTreeMapHighestBuildingHeight;
	    
	    state.rootDirectory = rootDirectory;
	    String treemapfilename = rootDirectory
	    		+ "treemap.dat";
	    Namelist treemapNamelist = new Namelist(treemapfilename);	    
	    
	    numberTreePlots = treemapNamelist.getIntValue("count", "numberTreePlots");
	    state.numberTreePlots=numberTreePlots;
	    	    
	    numberBuildingPlots = treemapNamelist.getIntValue("buildingcount", "numberBuildingPlots");
	    state.numberBuildingPlots=numberBuildingPlots;
	     
	    xBuildingLocation = new int[numberBuildingPlots];
	    yBuildingLocation = new int[numberBuildingPlots];
	    buildingsHeight = new int[numberBuildingPlots];  

	    try
	    {
	    	 xLocation = treemapNamelist.getIntArrayValue("location", "xLocation");
	    }
	    catch(Exception e)
	    {
	    	xLocation = new int[0];
	    }
	    state.xLocation=xLocation;
	    
	    try
	    {
	    	yLocation = treemapNamelist.getIntArrayValue("location", "yLocation");
	    }
	    catch(Exception e)
	    {
	    	yLocation = new int[0];
	    }	    
	    state.yLocation=yLocation;
	    
	    try
	    {
	    	 phyfileNumber = treemapNamelist.getIntArrayValue("location", "phyfileNumber");
	    	 System.out.println("phyfileNumber size=" + phyfileNumber.length);
	    }
	    catch(Exception e)
	    {
	    	phyfileNumber = new int[0];
	    }	   
	    state.phyfileNumber=phyfileNumber;
	    try
	    {
	    	 strfileNumber = treemapNamelist.getIntArrayValue("location", "strfileNumber");
	    	 System.out.println("strfileNumber size=" + strfileNumber.length);
	    }
	    catch(Exception e)
	    {
	    	strfileNumber = new int[0];
	    }		   
	    state.strfileNumber=strfileNumber;
	    try
	    {
	    	  treesfileNumber = treemapNamelist.getIntArrayValue("location", "treesfileNumber");
	    	  System.out.println("treesfileNumber size=" + treesfileNumber.length);
	    }
	    catch(Exception e)
	    {
	    	treesfileNumber = new int[0];
	    }		  
	    state.treesfileNumber=treesfileNumber;
	    try
	    {
	    	treesHeight = treemapNamelist.getIntArrayValue("location", "treesHeight");
	    	System.out.println("treesHeight size=" + treesHeight.length);
	    }
	    catch(Exception e)
	    {
	    	treesHeight = new int[0];
	    }	
	    
	    state.treesHeight=treesHeight;
	    try
	    {
	    	trees = treemapNamelist.getIntArrayValue("location", "trees");
	    	System.out.println("trees size=" + trees.length);
	    }
	    catch(Exception e)
	    {
	    	trees = new int[0];
	    }
	    state.trees=trees;	      

	    try
	    {
	    	 xBuildingLocation = treemapNamelist.getIntArrayValue("buildinglocation", "xBuildingLocation");
	    }
	    catch(Exception e)
	    {
	    	xBuildingLocation = new int[0];
	    }
	    state.xBuildingLocation=xBuildingLocation;
	    try
	    {
	    	yBuildingLocation = treemapNamelist.getIntArrayValue("buildinglocation", "yBuildingLocation");
	    }
	    catch(Exception e)
	    {
	    	yBuildingLocation = new int[0];
	    }	    
	    state.yBuildingLocation=yBuildingLocation;
	    try
	    {
	    	buildingsHeight = treemapNamelist.getIntArrayValue("buildinglocation", "buildingsHeight");
	    }
	    catch(Exception e)
	    {
	    	buildingsHeight = new int[0];
	    }		    
	    state.buildingsHeight=buildingsHeight   ; 

	    width = treemapNamelist.getIntValue("domain", "width");
	    state.width=width;
	    length = treemapNamelist.getIntValue("domain", "length");
	    state.length=length ;
	    configTreeMapCentralArrayLength = treemapNamelist.getIntValue("domain", "configTreeMapCentralArrayLength");
	    state.configTreeMapCentralArrayLength=configTreeMapCentralArrayLength;
	    configTreeMapCentralWidth = treemapNamelist.getIntValue("domain", "configTreeMapCentralWidth");
	    state.configTreeMapCentralWidth=configTreeMapCentralWidth;
	    configTreeMapCentralLength = treemapNamelist.getIntValue("domain", "configTreeMapCentralLength");
	    state.configTreeMapCentralLength=configTreeMapCentralLength;
	    configTreeMapX = treemapNamelist.getIntValue("domain", "configTreeMapX");
	    state.configTreeMapX=configTreeMapX;
	    configTreeMapY = treemapNamelist.getIntValue("domain", "configTreeMapY");
	    state.configTreeMapY=configTreeMapY;
	    configTreeMapX1 = treemapNamelist.getIntValue("domain", "configTreeMapX1");
	    state.configTreeMapX1=configTreeMapX1;
	    configTreeMapX2 = treemapNamelist.getIntValue("domain", "configTreeMapX2");
	    state.configTreeMapX2=configTreeMapX2;
	    configTreeMapY1 = treemapNamelist.getIntValue("domain", "configTreeMapY1");
	    state.configTreeMapY1=configTreeMapY1;
	    configTreeMapY2 = treemapNamelist.getIntValue("domain", "configTreeMapY2");
	    state.configTreeMapY2=configTreeMapY2;
	    configTreeMapGridSize = treemapNamelist.getDoubleValue("domain", "configTreeMapGridSize");
	    state.configTreeMapGridSize=configTreeMapGridSize;
	    configTreeMapNumsfcab = treemapNamelist.getIntValue("domain", "configTreeMapNumsfcab");
	    state.configTreeMapNumsfcab=configTreeMapNumsfcab;
	    configTreeMapHighestBuildingHeight = treemapNamelist.getIntValue("domain", "configTreeMapHighestBuildingHeight");
	    state.configTreeMapHighestBuildingHeight=configTreeMapHighestBuildingHeight;

	    state.configPartitioningMethod=treemapNamelist.getIntValue("runSwitches", "partitioningMethod");
	    state.usingDiffShading=treemapNamelist.getIntValue("runSwitches", "usingDiffShading");

	    // Read per-cell surface properties (optional section)
	    try
	    {
	    	double[] cellAlbedo = treemapNamelist.getDoubleArrayValue("cellProperties", "cellAlbedo");
	    	double[] cellEmissivity = treemapNamelist.getDoubleArrayValue("cellProperties", "cellEmissivity");
	    	state.cellAlbedo = cellAlbedo;
	    	state.cellEmissivity = cellEmissivity;
	    	state.hasCellProperties = true;
	    	System.out.println("Per-cell surface properties loaded: " + cellAlbedo.length + " cells");
	    }
	    catch(Exception e)
	    {
	    	// Per-cell properties not specified - use defaults
	    	state.hasCellProperties = false;
	    	System.out.println("No per-cell surface properties found, using uniform values from parameters.dat");
	    }

//	    end subroutine readMaespaTreeMapFromConfig
	    return state;
	 }

	public int[][] getTreeXYMap()
	{
		return treeXYMap;
	}

	public void setTreeXYMap(int[][] treeXYMap)
	{
		this.treeXYMap = treeXYMap;
	}

	public int[][] getTreeXYTreeMap()
	{
		return treeXYTreeMap;
	}

	public void setTreeXYTreeMap(int[][] treeXYTreeMap)
	{
		this.treeXYTreeMap = treeXYTreeMap;
	}
	
	public String getFileNameFromFileNumber(int file)
	{
		String filename="";
		if (file == Constants.inputs_store_out)
		{
			filename = rootDirectory + "Inputs_Store.out";
		}
		if (file == Constants.FLUXES_OUT)
		{
			filename = rootDirectory + "Fluxes.out";
		}
		if (file == Constants.energybalancetsfctimeaverage_out)
		{
			filename = rootDirectory + "EnergyBalance_Tsfc_TimeAverage.out";
		}
		if (file == Constants.tsfcfacetssunshade_out)
		{
			filename = rootDirectory + "Tsfc_Facets_SunShade.out";
		}
		if (file == Constants.tsfcfacets_out)
		{
			filename = rootDirectory + "Tsfc_Facets.out";
		}
		if (file == Constants.energybalancefacets_out)
		{
			filename = rootDirectory + "EnergyBalance_Facets.out";
		}
		if (file == Constants.EnergyBalanceOverallOut)
		{
			filename = rootDirectory + "EnergyBalance_Overall.out";
		}
		if (file == Constants.forcing_out)
		{
			filename = rootDirectory + "Forcing.out";
		}
		if (file == Constants.RadiationBalanceFacetsOut)
		{
			filename = rootDirectory + "RadiationBalance_Facets.out";
		}
		if (file == Constants.TsfcSolarSVF_Patch_yd)
		{
			filename = rootDirectory + "TsfcSolarSVF_Patch_yd.out";
		}

		return filename;
	}
	
	public void writeOutput(int file, String text, boolean deleteExisting)
	{
		String filename = getFileNameFromFileNumber(file);		
		if (deleteExisting)
		{
//			System.out.println("deleting " + file);
//			String filename = getFileNameFromFileNumber(file);
			File fileToDelete = new File(filename);
			fileToDelete.delete();
		}		
		common.writeFile(text, filename);
//		writeOutput(file, text);
	}
	
	public void writeOutput(int file, String text)
	{		
		String filename = getFileNameFromFileNumber(file);		
		common.appendFile(text, filename);
	}
	
	public void writeOutput(String file, String text)
	{
		String filename= rootDirectory + file;
		common.appendFile(text, filename);
	}
	
	public void deleteOldOutput(String file)
	{
		String filename= rootDirectory + file;
		File fileToDelete = new File(filename);
		fileToDelete.delete();
	}
	
	public void writeOutput(String file, String text, boolean deleteExisting)
	{
		String filename= rootDirectory + file;
		if (deleteExisting)
		{
			File fileToDelete = new File(filename);
			fileToDelete.delete();
		}
//		System.out.println("output=" + filename);
//		System.out.println(text);
		common.appendFile(text, filename);
	}
	
    public static double getTransmissionForTree(int treeLocation, HashMap<String,MaespaDataFile> maespaTestflxData  )
    {
      //This is 1 because TD in Maespa is always the same throughout the day
      MaespaDataFile data = maespaTestflxData.get(treeLocation + "_1");
      double[] tdData = data.getDataArrayForVariable("TD");
      return tdData[0];
    }
    
    public static int getBuildingHeightFromConfig(int x, int y, MaespaConfigTreeMapState treeState)
	   {
    	//if the tree location isn't found, then it will be 0 high
    	int buildingHeight = 0 ;
	    //first check if the x,y is in the location list
	    for (int loopCount = 0;loopCount < treeState.numberBuildingPlots;loopCount++)
		{
	        if ( (x-1)==treeState.xBuildingLocation[loopCount] && (y-1)==treeState.yBuildingLocation[loopCount] ) 
	        {
	            buildingHeight = treeState.buildingsHeight[loopCount];
	        }
		}
	    return buildingHeight;
	   
	   }

	    
}
