		// !
		// _____________________________________________________________________________________
		// !
		// ! VTUF-3D model
		// !
		// !
		// -------------------------------------------------------------------------------------
		//
		// ! This model is written primarily in Fortran 77 but uses some Fortran
		// 2003. Therefore
		// ! a Fortran 2003 compiler is required.
		// !
		// !
		// !
		// !
		// -------------------------------------------------------------------------------------
		// ! Original references:
		// !
		// ! Krayenhoff ES, Voogt JA (2007) A microscale three-dimensional urban
		// energy balance
		// ! model for studying surface temperatures. Boundary-Layer Meteorol
		// 123:433-461
		// !
		// ! Krayenhoff ES (2005) A micro-2scale 3-D urban energy balance model
		// for studying
		// ! surface temperatures. M.Sc. Thesis, University of Western Ontario,
		// London, Canada
		// !
		// -------------------------------------------------------------------------------------
		// !
		// ! *** This model is for research and teaching purposes only ***
		// !
		// !
		// -------------------------------------------------------------------------------------
		// !
		// ! Last updated:
		// ! September 2011 by Scott Krayenhoff
		// ! 2013-2016, modified heavily by Kerry Nice
		// !
		// !
		// _____________________________________________________________________________________



package VTUF3D;

import VTUF3D.Utilities.MaespaDataFile;
import VTUF3D.Utilities.Namelist;
import java.util.ArrayList;
import java.util.HashMap;

public class TUFreg3D
{
	//added these to restart crashed runs at a certain point. 
	// and number the output with the new values
	protected static boolean restartedRun = false;
	protected static int restartedRunStartTimestep = 0;
//	protected static int restartedRunNumber = 0;
	
	VTUF3DUtil util = new VTUF3DUtil();
	private String[] args;
	
	public static long startTime = System.currentTimeMillis();
	
	//to shift one indexed arrays to zero
	public final static int ONE = 0;
	public final static int TWO = 1;
	public final static int THREE = 2;
	public final static int FOUR = 3;
	public final static int FIVE = 4;
	public final static int SIX = 5;
	
	//TODO, try to shift these to zero based, but is probably a bit painful 
	public final static int FACE_ONE = 1;
	public final static int FACE_TWO = 2;
	public final static int FACE_THREE = 3;
	public final static int FACE_FOUR = 4;
	public final static int FACE_FIVE = 5;
	public final static int FACE_SIX = 6;

	public static void main(String[] args)
	{
		TUFreg3D vtuf = new TUFreg3D();
		vtuf.args = args;
		vtuf.run();
	}

	public void run()
	{
		boolean calcKdn;
		boolean calcz0;		
		boolean frcwrite;
		boolean facet_out;
		boolean matlab_out;
		boolean sum_out;
		boolean calcLdn;
		boolean calclf;
		double zrooffrc;
		double Tbuild_min;

		int numlayers, numfrc,badKdn;
		int numbhbl;
		int minres;
		int numlp;
		int vfcalc;
		int yd, numout;
		int year;
		int cloudtype;
		double Ldn;
		double stror_in, strorint, strormax, xlatint, xlatmax, xlat_in;
		double outpt_tm, uc, Ldn_fact, zref;
		double Tintw, Tints, IntCond, Tthreshold, starttime, httc = 0;
		double deltatfrc;
		double buildht_m;
		double Ktotfrc, Udir,Ta, Ua,dta_starttime, dta_timeend,Td;
		double press;
		
		double lambdaf, Tcan, moh,cpair;

		double z0, z0roofm, z0roofh, z0roadm, z0roadh;
		double zH, Kdn_diff;
		double nKdndiff, vK;
//		double maespaTimeChecked;
		double sigma,Tfloor,rw, ea, Intresist, Kdn_ae_store, timeend, deltat;

		double timeis;
		double Tsfcr, Tsfcw, Tsfcs,albr, albw, albs, emisr, emisw, emiss, dalb, svfe_store;
		double[] lambdaav;
//		double[] lambda;
		double[] htcap;
		double[] thick;
		double[] tlayer;
		double[] tlayerp;

		double[] gam;
		double[] denom;
		double[] A;
		double[] B;
		double[] D;
		double[] R;
		
		double[] Pressfrc;
		double[] Udirfrc;
		double[] Kdnfrc;
		double[] Ldnfrc;
		double[] Tafrc;
		double[] eafrc;
		double[] Uafrc;
		double[] lambdar;
		double[] timefrc;
		double[] htcapr;
		double[] thickr;
		double[] htcaps;
		double[] thicks;
		double[] lpin;
		double[] bh_o_bl;
		double[] lambdas;
		double[] lambdaw;
		double[] thickw;
		
		double[] lambdaavr;	
		double[] depthr;
		double[] lambdaavs;
		double[] depths;
		double[] lambdaavw;
		double[] htcapw;
		double[] depthw;
		MaespaConfigTreeMapState treeMapFromConfig; 
		int numberOfExtraTthresholdLoops;
		int tthresholdLoops;

		boolean writeKabs;
		boolean writeKl;
		boolean writeLabs;
		boolean writeKrefl;
		boolean writeLrefl;
		boolean writeTsfc;
		boolean writeEnergyBalances;

		boolean writeTmrt;
		boolean writeUtci;
		boolean writeLdown;

		int DIFFERENTIALSHADINGDIFFUSE;

		// ! constants:
		sigma = 5.67e-8;
		cpair = 1004.67;
		vK = 0.4;
		// PI=ACOS(-1.0);
//		double PI = Math.PI;

		//TODO disabling some output for Urban Plumber
		writeKabs = false;
		writeKl = false;
		writeLabs = false;
		writeKrefl = false;
		writeLrefl = false;
		writeTsfc = true;
		writeEnergyBalances = true;
		writeLdown = false;
		writeTmrt = true;
		writeUtci = true;

		// ! initialization
		tthresholdLoops = 0;
		numberOfExtraTthresholdLoops = 3;
		Kdn_diff = 0.;
		nKdndiff = 0;
		badKdn = 0;
		svfe_store = 0.;
		Kdn_ae_store = 0.;
//		maespaTimeChecked = -1.;
//		maespaWatQe = 0.;
		// !!KN, initializing it because it gets used below before any value is set
		zH = 0; 
		// !!KN, initializing it because it gets used below before any value is set if ldn is not calculated
		Ldn_fact = 1.0; 

        String rootDirectory = args[0];
        if (rootDirectory == null || rootDirectory.trim().equals(""))
        {
        	System.out.println("no root directory");
        	System.exit(1);
        }
		OverallConfiguration overall = new OverallConfiguration(rootDirectory);
		// print the root directory
		System.out.println("Root directory: " + rootDirectory);
		treeMapFromConfig = overall.readMaespaTreeMapFromConfig(rootDirectory);
		treeMapFromConfig.rootDirectory = rootDirectory;
		DIFFERENTIALSHADINGDIFFUSE = treeMapFromConfig.usingDiffShading;

		if (treeMapFromConfig.usingDiffShading == 0)
		{
			System.out.println("DIFFERENTIALSHADING100PERCENT");
		}

		HashMap<String, ArrayList<MaespaDataResults>> maespaDataArray = overall.mapTrees(treeMapFromConfig);
		int[][] treeXYMap = overall.getTreeXYMap();
		int[][] treeXYTreeMap = overall.getTreeXYTreeMap();

		//TODO replace these with online versions
		HashMap<String, MaespaDataFile> maespaTestflxData = overall.readMaespaTestflxData(maespaDataArray);
		HashMap<String, HashMap<String, Namelist>> namelists = overall.readNamelists(treeMapFromConfig);

		double[][] treeXYMapSunlightPercentageTotal = new double[treeMapFromConfig.width][treeMapFromConfig.length];

		ParametersDat parameters = overall.readParametersDat();
		// //! MAIN PARAMETER AND INITIAL CONDITION INPUT FILE
		// //! read in the input file values
		// //! output file recording inputs:

		// ! model/integration parameters
		vfcalc = parameters.vfcalc;
		yd = parameters.yd;
		year = parameters.year;
		deltat = parameters.deltat;
		outpt_tm = parameters.outpt_tm;
		Tthreshold = parameters.Tthreshold;
		facet_out = parameters.facet_out;
		matlab_out = parameters.matlab_out;
		sum_out = parameters.sum_out;
		
		//now you can restart a crashed run
		restartedRun = parameters.restartedRun;
		restartedRunStartTimestep = parameters.restartedRunStartTimestep;
//		restartedRunNumber = parameters.restartedRunNumber;
		
		// ! radiative parameters
		dalb = parameters.dalb;
		albr = parameters.albr;
		albs = parameters.albs;
		albw = parameters.albw;
		emisr = parameters.emisr;
		emiss = parameters.emiss;
		emisw = parameters.emisw;
		cloudtype = parameters.cloudtype;

		// ! conduction parameters
		IntCond = parameters.IntCond;
		Intresist = parameters.Intresist;
		uc = parameters.uc;
		numlayers = parameters.numlayers;

//		lambda = new double[numlayers];
		lambdaav = new double[numlayers];
		htcap = new double[numlayers];
		thick = new double[numlayers];
		lambdar = new double[numlayers];
		lambdaavr = new double[numlayers];
		htcapr = new double[numlayers];
		thickr = new double[numlayers];
		lambdas = new double[numlayers];
		lambdaavs = new double[numlayers];
		htcaps = new double[numlayers];
		thicks = new double[numlayers];
		lambdaw = new double[numlayers];
		lambdaavw = new double[numlayers];
		htcapw = new double[numlayers];
		thickw = new double[numlayers];
		tlayer = new double[numlayers];
		tlayerp = new double[numlayers];
		gam = new double[numlayers];
		denom = new double[numlayers];
		A = new double[numlayers];
		B = new double[numlayers];
		D = new double[numlayers];
		R = new double[numlayers];
		depthr = new double[numlayers];
		depths = new double[numlayers];
		depthw = new double[numlayers];
	
		thickr = parameters.thickr;
		lambdar = parameters.lambdar;
		htcapr = parameters.htcapr;

		thicks = parameters.thicks;
		lambdas = parameters.lambdas;
		htcaps = parameters.htcaps;

		thickw = parameters.thickw;
		lambdaw = parameters.lambdaw;
		htcapw = parameters.htcapw;

		// ! convection parameters
		z0 = parameters.z0;
		lambdaf = parameters.lambdaf;
		zrooffrc = parameters.zrooffrc;
		z0roofm = parameters.z0roofm;
		z0roadm = parameters.z0roadm;
		z0roofh = parameters.z0roofh;
		z0roadh = parameters.z0roadh;
		moh = parameters.moh;
		rw = parameters.rw;
		
		// ! domain geometry
		//this is obsolete now, calculated from the domains later on now
		buildht_m = parameters.buildht_m;
		zref = parameters.zref;
		minres = parameters.minres;

		// ! initial temperatures
		Tsfcr = parameters.Tsfcr;
		Tsfcs = parameters.Tsfcs;
		Tsfcw = parameters.Tsfcw;
		Tintw = parameters.Tintw;
		Tints = parameters.Tints;
		Tfloor = parameters.Tfloor;
		Tbuild_min = parameters.Tbuild_min;

		// ! loop parameters all
		stror_in = parameters.stror_in;
		strorint = parameters.strorint;
		strormax = parameters.strormax;

		xlat_in = parameters.xlat_in;
		xlatint = parameters.xlatint;
		xlatmax = parameters.xlatmax;
		numlp = parameters.numlp;
		lpin = parameters.lpin;
		numbhbl = parameters.numbhbl;
		bh_o_bl = parameters.bh_o_bl;

		if (z0 < 0.)
		{
			calcz0 = true;
		}
		else
		{
			calcz0 = false;
		}

		if (vfcalc == 0 && (numlp > 1 || numbhbl > 1))
		{
			System.out.println(
					"must turn on calculation of view factors (i.e.vfcalc=1)if more than one lambdap or H/L ratio is chosen");
			// (i.e.vfcalc=1)if more than one lambdap or H/L ratio is chosen'
			System.out.println("vfcalc, numlp =" + " " + vfcalc + " " + numlp);
			System.exit(1);
		}

		if (Math.abs(xlat_in) > 90.0 || Math.abs(xlatmax) > 90.0)
		{
			System.out.println("one of xlat_in or xlatmax is greater than 90 or lessthan -90, xlat_in, xlatmax =" + " "
					+ xlat_in + " " + xlatmax);
			System.exit(1);
		}
		if (xlatint < 1e-9)
		{
			System.out.println("xlatint must be greater than 0, xlatint=" + " " + xlatint);
			System.out.println("if you do not want to simulate more than one latitude,set xlat_in=xlatmax");
			System.exit(1);
		}

		Tsfcr = Tsfcr + 273.15;
		Tsfcs = Tsfcs + 273.15;
		Tsfcw = Tsfcw + 273.15;
		Tintw = Tintw + 273.15;
		Tints = Tints + 273.15;
		Tfloor = Tfloor + 273.15;

		// ! write to output file that records the inputs
		// ! model/integration parameters
		overall.writeOutput(Constants.inputs_store_out, "vfcalc,yd,deltat,outpt_tm,Tthreshold",true);
		overall.writeOutput(Constants.inputs_store_out,
				vfcalc + " " + yd + " " + deltat + " " + outpt_tm + " " + Tthreshold);
		overall.writeOutput(Constants.inputs_store_out, "facet_out,matlab_out,sum_out");
		overall.writeOutput(Constants.inputs_store_out, facet_out + " " + matlab_out + " " + sum_out);
	
		// ! radiative parameters
		overall.writeOutput(Constants.inputs_store_out, "dalb");
		overall.writeOutput(Constants.inputs_store_out, dalb + "");
		overall.writeOutput(Constants.inputs_store_out, "albr,albs,albw,emisr,emiss,emisw");
		overall.writeOutput(Constants.inputs_store_out,
				albr + " " + albs + " " + albw + " " + emisr + " " + emiss + " " + emisw);
		overall.writeOutput(Constants.inputs_store_out, "cloudtype");
		overall.writeOutput(Constants.inputs_store_out, cloudtype + "");

		// ! conduction parameters
		overall.writeOutput(Constants.inputs_store_out, "IntCond,Intresist,uc,numlayers");
		overall.writeOutput(Constants.inputs_store_out, IntCond + " " + Intresist + " " + uc + " " + numlayers);
		overall.writeOutput(Constants.inputs_store_out, "thickr(k),lambdar(k),htcapr(k)");
		for (int k = 0; k < numlayers; k++)
		{
			overall.writeOutput(Constants.inputs_store_out, thickr[k] + " " + lambdar[k] + " " + htcapr[k]);
		}
		overall.writeOutput(Constants.inputs_store_out, "thicks(k),lambdas(k),htcaps(k)");
		for (int k = 0; k < numlayers; k++)
		{
			overall.writeOutput(Constants.inputs_store_out, thicks[k] + " " + lambdas[k] + " " + htcaps[k]);
		}
		overall.writeOutput(Constants.inputs_store_out, "thickw(k),lambdaw(k),htcapw(k)");
		for (int k = 0; k < numlayers; k++)
		{
			overall.writeOutput(Constants.inputs_store_out, thickw[k] + " " + lambdaw[k] + " " + lambdaw[k]);
		}

		// ! convection parameters
		overall.writeOutput(Constants.inputs_store_out, "z0,lambdaf,zrooffrc");
		overall.writeOutput(Constants.inputs_store_out, z0 + " " + lambdaf + " " + zrooffrc);
		overall.writeOutput(Constants.inputs_store_out, "z0roofm,z0roadm,z0roofh,z0roadh,moh,rw");
		overall.writeOutput(Constants.inputs_store_out,
				z0roofm + " " + z0roadm + " " + z0roofh + " " + z0roadh + " " + moh + " " + rw);

		// ! domain geometry
		overall.writeOutput(Constants.inputs_store_out, "buildht_m,zref,minres");
		overall.writeOutput(Constants.inputs_store_out, buildht_m + " " + zref + " " + minres);

		// ! initial temperatures
		overall.writeOutput(Constants.inputs_store_out, "Tsfcr,Tsfcs,Tsfcw,Tintw,Tints,Tfloor,Tbuild_min");
		overall.writeOutput(Constants.inputs_store_out,
				Tsfcr + " " + Tsfcs + " " + Tsfcw + " " + Tintw + " " + Tints + " " + Tfloor + " " + Tbuild_min);

		// ! loop parameters
		overall.writeOutput(Constants.inputs_store_out, "stror_in,strorint,strormax");
		overall.writeOutput(Constants.inputs_store_out, stror_in + " " + strorint + " " + strormax);
		overall.writeOutput(Constants.inputs_store_out, "xlat_in,xlatint,xlatmax");
		overall.writeOutput(Constants.inputs_store_out, xlat_in + " " + xlatint + " " + xlatmax);
		overall.writeOutput(Constants.inputs_store_out, "numlp");
		overall.writeOutput(Constants.inputs_store_out, numlp + "");
		overall.writeOutput(Constants.inputs_store_out, "lpin(k)");
		for (int k = 0; k < numlp; k++)
		{
			overall.writeOutput(Constants.inputs_store_out, lpin[k] + "");
		}
		overall.writeOutput(Constants.inputs_store_out, "bh_o_bl(k)");
		for (int l = 0; l < numbhbl; l++)
		{
			overall.writeOutput(Constants.inputs_store_out, bh_o_bl[l] + "");
		}

		// ! ATMOSPHERIC FORCING
		ForcingData forcing = new ForcingData(rootDirectory);
		forcing.readForcingData();
		numfrc = forcing.getNumfrc();
		starttime = forcing.getStarttime();
		deltatfrc = forcing.getDeltatfrc();
		dta_starttime = forcing.getStarttime();

		Pressfrc = forcing.getPressfrc();
		Udirfrc = forcing.getUdirfrc();
		Kdnfrc = forcing.getKdnfrc();
		Ldnfrc = forcing.getLdnfrc();
		Tafrc = forcing.getTafrc();
		eafrc = forcing.getEafrc();
		Uafrc = forcing.getUafrc();
		timefrc = forcing.getTimefrc();

		// ! Initial values:
		press = Pressfrc[restartedRunStartTimestep];
		Udir = Udirfrc[restartedRunStartTimestep];
		Ktotfrc = Kdnfrc[restartedRunStartTimestep];
		Ldn = Ldnfrc[restartedRunStartTimestep];
		Ta = Tafrc[restartedRunStartTimestep] + 273.15;
		ea = eafrc[restartedRunStartTimestep];
		Ua = Math.max(0.1, Uafrc[restartedRunStartTimestep]);
		System.out.println("initial forcing data:");
		System.out.println("temperature (C), vapour pressure (mb) = " + " " + Ta + " " + ea);
		System.out.println("wind speed (m/s), wind direction (degrees) = " + " " + Ua + " " + Udir);
		System.out.println("pressure (mb) = " + " " + press);
		System.out.println("initial forcing data:");
		System.out.println("temperature (C), vapour pressure (mb) = " + " " + Ta + " " + ea);
		System.out.println("wind speed (m/s), wind direction (degrees) = " + " " + Ua + " " + Udir);
		System.out.println("pressure (mb) = " + " " + press);

		calcKdn = false;
		if (Ktotfrc < -90.)
		{
			calcKdn = true;
		}
		// this fixes a bug. Before, it was always calculating since calcLdn wasn't initialized
		calcLdn = false; 
		if (Ldnfrc[restartedRunStartTimestep] < 0.)
		{
			calcLdn = true;
			// ! Prata's clear sky formula (QJRMS 1996)
			Ldn = (1. - (1. + 46.5 * ea / Ta) * Math.exp(-(Math.pow((1.2 + 3. * 46.5 * ea / Ta), (0.5))))) * sigma
					* Math.pow(Ta, 4);

			// ! Sellers (1965) modification of Ldown based on cloud type (cloud base height) in Oke (1987)
			if (cloudtype == 0)
			{
				// ! clear:
				Ldn_fact = 1.00;
			}
			else if (cloudtype == 1)
			{
				// ! cirrus:
				Ldn_fact = 1.04;
			}
			else if (cloudtype == 2)
			{
				// ! cirrostratus:
				Ldn_fact = 1.08;
			}
			else if (cloudtype == 3)
			{
				// ! altocumulus:
				Ldn_fact = 1.17;
			}
			else if (cloudtype == 4)
			{
				// ! altostratus:
				Ldn_fact = 1.20;
			}
			else if (cloudtype == 7)
			{
				// ! cumulonimbus:
				Ldn_fact = 1.21;
			}
			else if (cloudtype == 5)
			{
				// ! stratocumulus/cumulus:
				Ldn_fact = 1.22;
			}
			else if (cloudtype == 6)
			{
				// ! thick stratus (Ns?):
				Ldn_fact = 1.24;
			}
			else
			{
				System.out.println("cloudtype must be between 0 and 7, cloudtype = " + " " + cloudtype);
			}

			Ldn = Ldn * Ldn_fact;
			System.out.println("Ldown calc Prata & Sellers, Ldown (W/m2) = " + " " + Ldn);
		}
		Td = (4880.357 - 29.66 * Math.log(ea)) / (19.48 - Math.log(ea));

		if (!calcLdn)
		{
			System.out.println("Ldown (W/m2) = " + " " + Ldn);
			System.out.println("Ldown (W/m2) = " + " " + Ldn);
		}

		if (calcKdn)
		{
			System.out.println("Kdown (W/m2) = to be calculated");
			System.out.println("Kdown (W/m2) = to be calculated");
		}
		else
		{
			System.out.println("Kdown (W/m2) = " + " " + Ktotfrc);
			System.out.println("Kdown (W/m2) = " + " " + Ktotfrc);
		}

		// !print *,'Ldn,Ldn_fact,calcKdn,calcLdn',Ldn,Ldn_fact,calcKdn,calcLdn
		// ! assume initial Tcan!!!
		Tcan = Ta + 0.5;

		timeis = starttime;
		timeend = starttime + deltatfrc * (1.0*numfrc-1);
		dta_timeend = timeend;

		// ! number of times output will be written in Matlab output section:
		numout = (int) ((timeend - starttime) / outpt_tm) + 1;

		overall.writeOutput(Constants.inputs_store_out, "numfrc,starttime,deltatfrc");
		overall.writeOutput(Constants.inputs_store_out, numfrc + " " + starttime + " " + deltatfrc);

		calclf = false;
		frcwrite = true;

		// ! OPEN OUTPUT FILES
	
		overall.writeOutput(Constants.FLUXES_OUT,
				"veg,i,timeis,maespaRnet,Rnet,sfc(i_Constants.sfc_emiss)*sigma*Math.pow(Tsfc[iab],4),(httc*(Tsfc[iab]-Tconv)),maespaQh,leFromEt,(lambda_sfc[iab]*(Tsfc[iab]-sfc_ab(iab_sfc_ab_layer_temp))*2./sfc_ab(iab_6+3*numlayers)),maespaQg,maespaAbsorbedThermal,QGBiomass,Tsfc,Tconv,httc,sfc_emis,MaespaRnetGround,deltaQVeg,absbl[iab],absbs[iab],tots[iab],totl[iab],reflts[iab],refltl[iab],kup,lup,kdn_grid,kdir,kdif,ktotfrc,kbeam,ldn,leFromEt2,leFromEt4"
				,true);
		overall.writeOutput(Constants.energybalancetsfctimeaverage_out,
				"lambdap,H/L,H/W,latitude,streetdir,julian_day,time_of_day(centre),time(continuous&centre),time_of_day(end),time(continuous&end),Kuptot_avg,Luptot_avg,Rntot_avg,Qhtot_avg,Qgtot_avg,Qanthro_avg,Qac_avg,Qdeep_avg,Qtau,TR_avg,TT_avg,TN_avg,TS_avg,TE_avg,TW_avg"
				,true);
		overall.writeOutput(Constants.tsfcfacetssunshade_out,
				"lambdap,H/L,H/W,latitude,streetdir,julian_day,time_of_day,time(continuous),TTsun,TTsh,TNsun,TNsh,TSsun,TSsh,TEsun,TEsh,TWsun,TWsh"
				,true);
		overall.writeOutput(Constants.tsfcfacets_out,
				"lambdap,H/L,H/W,latitude,streetdir,julian_day,time_of_day,time(continuous),Tcomplete,Tbirdeye,Troof,Troad,Tnorth,Tsouth,Teast,Twest,Tcan,Ta,Tint,httcR,httcT,httcW,TbrightR,TbrightT,TbrightN,TbrightS,TbrightE,TbrightW"
				,true);
		overall.writeOutput(Constants.energybalancefacets_out,
				"lambdap,H/L,H/W,latitude,streetdir,julian_day,time_of_day,time(continuous),QR,HR,GR,QT,HT,GT,QN,HN,GN,QS,HS,GS,QE,HE,GE,QW,HW,GW",
				true);
		overall.writeOutput(Constants.EnergyBalanceOverallOut,
				"lambdap,H/L,H/W,latitude,streetdir,julian_day,time_of_day,time(continuous),Rnet_tot,Qh_SumSfc,Qh_Vol,Qg_SumSfc,Qg_SfcCanAir,Rnet_can,Qh_CanTop,Qh_SumCanSfc,Qg_Can_CanAir,Ucan,Utop,Uroad,wstar,Kdn,Kup,Ldn,Lup,Kdir_Calc,Kdif_Calc,Kdir,Kdif,Kup_can,Lup_can,az,zen,Kdn(NoAtm),Kdn_grid,Qe_tot"
				,true);
	
		if (frcwrite)
		{
			overall.writeOutput(Constants.forcing_out,
					"lambdap,H/L,H/W,latitude,streetdir,time,Kdir,Kdif,Ldn,Ta,ea,Ua,Udir,Press,az,zen"
					,true);
		}
		overall.writeOutput(Constants.RadiationBalanceFacetsOut,
				"lambdap,H/L,H/W,latitude,streetdir,julian_day,time_of_day,time(continuous),SKd,SKup,SLd,SLup,EKd,EKup,ELd,ELup,NKd,NKup,NLd,NLup,WKd,WKup,WLd,WLup,RfKd,RfKup,RfLd,RfLup,FKd,FKup,FLd,FLup"
				,true);
	
//System.out.println("++++++++++++++++++++++++start loop=" + (System.currentTimeMillis() - startTime)/1000. );		
		
		VTUF3DLoop loop = new VTUF3DLoop();
		loop.loop(numlp, numbhbl, minres, vfcalc, lpin, treeMapFromConfig, buildht_m, zref, overall, Pressfrc, Udirfrc, Kdnfrc, Ldnfrc, Tafrc, 
				eafrc, Uafrc, press, Udir, Ktotfrc, Ldn, Ta, ea, Ua, numlayers,lambdaavr, lambdaavs, lambdaavw, thickr, thicks, thickw,
				lambdar, lambdas, lambdaw, Intresist, htcapr, htcaps, htcapw, deltat, uc, util, timeend, namelists, treeXYMap,
				zH, z0roofh, z0roofm, z0roadh, z0roadm, moh, depthr, depths, depthw, albs, emiss, treeXYTreeMap, Tsfcs, 
				albr, emisr, Tsfcr, albw, emisw, Tsfcw,
				lambdaf, calclf, calcz0, z0, xlat_in, xlatmax, stror_in, strormax, facet_out, bh_o_bl, timeis,
				yd, dta_starttime, starttime, numout, outpt_tm, Tintw, Tints,
				Tcan, gam, tlayer, denom, A, B, D,
				dta_timeend, lambdaav, thick, R, IntCond,
				cpair, numfrc, deltatfrc, timefrc, calcLdn,
				Td, Ldn_fact, sigma, cloudtype, calcKdn, treeXYMapSunlightPercentageTotal, maespaTestflxData,
				vK, Kdn_diff, nKdndiff, Kdn_ae_store, dalb,
				svfe_store, httc, rw, zrooffrc,
				DIFFERENTIALSHADINGDIFFUSE, maespaDataArray, Tfloor,
				Tthreshold, tthresholdLoops, numberOfExtraTthresholdLoops, Tbuild_min, frcwrite,
				tlayerp, htcap,
				sum_out, matlab_out, writeTsfc, writeKl, writeKabs, writeKrefl, writeLabs, writeLrefl, writeLdown,  writeTmrt, writeUtci, 
				writeEnergyBalances,  strorint,  xlatint,  badKdn, year);

	}

}








