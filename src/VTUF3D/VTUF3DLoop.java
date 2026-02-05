package VTUF3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.IntStream;

import VTUF3D.Utilities.Common;
import VTUF3D.Utilities.MaespaDataFile;
import VTUF3D.Utilities.Namelist;

public class VTUF3DLoop
{
//	//to shift one indexed arrays to zero
//	public final static int ONE = 0;
//	public final static int TWO = 1;
//	public final static int THREE = 2;
//	public final static int FOUR = 3;
//	public final static int FIVE = 4;
//	public final static int SIX = 5;

	// ====== PARALLEL PROCESSING CONFIGURATION ======
	// Set to true to enable parallel processing (recommended for large grids)
	public static boolean PARALLEL_ENABLED = true;

	// Number of threads to use (0 = use all available processors)
	public static int NUM_THREADS = 0;

	// ====== VIEW FACTOR CACHE CONFIGURATION ======
	// Set to true to cache view factors to disk for faster subsequent runs
	// The cache is automatically invalidated when geometry changes (al2, aw2, bh, numsfc2)
	public static boolean VIEW_FACTOR_CACHE_ENABLED = true;

	// ForkJoinPool for parallel processing
	private static ForkJoinPool parallelPool = null;

	// Progress tracker for ETA reporting
	private static ProgressTracker progressTracker = null;

	// Array pool for reducing GC pressure
	private static ArrayPool arrayPool = null;

	/**
	 * Initialize the parallel processing pool and other performance components.
	 * Call this once before running simulations.
	 */
	public static void initParallelPool() {
		if (PARALLEL_ENABLED && parallelPool == null) {
			int threads = NUM_THREADS > 0 ? NUM_THREADS : Runtime.getRuntime().availableProcessors();
			parallelPool = new ForkJoinPool(threads);
			System.out.println("Parallel processing enabled with " + threads + " threads");
		}

		// Initialize array pool
		if (arrayPool == null) {
			arrayPool = ArrayPool.getInstance();
		}

		// Initialize progress tracker
		if (progressTracker == null) {
			progressTracker = new ProgressTracker();
		}
	}

	/**
	 * Shutdown the parallel pool and cleanup resources.
	 */
	public static void shutdownParallelPool() {
		if (parallelPool != null) {
			parallelPool.shutdown();
			parallelPool = null;
		}

		// Print array pool stats and cleanup
		if (arrayPool != null) {
			System.out.println(arrayPool.getStats());
			arrayPool.clear();
		}

		// Finish progress tracking
		if (progressTracker != null) {
			progressTracker.finish();
			progressTracker = null;
		}
	}

	/**
	 * Newton's method solver for surface temperature.
	 * This is extracted to allow parallel processing.
	 */
	private static double solveNewtonTsfc(
			double Tsfc_init, double emiss, double sigma,
			double httc, double lambda_sfc, double thickness,
			double Rnet, double Tconv, double layerTemp) {

		double Tnew = Tsfc_init;
		double Told = Tnew + 999.0;
		double Fold, Fold_prime;
		int patchItrCount = 0;
		int httcRetries = 0;
		double httcLocal = httc;

		while (Math.abs(Tnew - Told) > Constants.NEWTON_CONVERGENCE_TOLERANCE) {
			Told = Tnew;

			double conduction = lambda_sfc * 2.0 / thickness;

			Fold = emiss * sigma * Math.pow(Told, 4)
					+ (httcLocal + conduction) * Told
					- Rnet - httcLocal * Tconv
					- lambda_sfc * layerTemp * 2.0 / thickness;

			Fold_prime = 4.0 * emiss * sigma * Math.pow(Told, 3)
					+ httcLocal + conduction;

			Tnew = -Fold / Fold_prime + Told;

			if (Double.isNaN(Tnew)) {
				return Tsfc_init;
			}

			patchItrCount++;
			if (patchItrCount > Constants.NEWTON_MAX_ITERATIONS) {
				if (httcLocal < 0) {
					httcLocal += Constants.NEWTON_HTTC_ADJUSTMENT;
				} else {
					httcLocal -= Constants.NEWTON_HTTC_ADJUSTMENT;
				}
				patchItrCount = 0;
				httcRetries++;

				if (httcRetries > Constants.NEWTON_MAX_HTTC_RETRIES) {
					return Tsfc_init;
				}
			}
		}

		return Tnew;
	}
	// ====== END PARALLEL PROCESSING CONFIGURATION ======

	public static void main(String[] args)
	{
		
	}
	
	public void loop(int numlp, int numbhbl, int minres, int vfcalc, double[] lpin, MaespaConfigTreeMapState treeMapFromConfig, double buildht_m, 
			double zref, OverallConfiguration overall, double[] Pressfrc, double[] Udirfrc, double[] Kdnfrc, double[] Ldnfrc, double[] Tafrc, 
			double[] eafrc, double[] Uafrc, double press, double Udir, double Ktotfrc, double Ldn, double Ta, double ea, double Ua, int numlayers, 
			double[] lambdaavr, double[] lambdaavs, double[] lambdaavw, double[] thickr, double[] thicks, double[] thickw, 
			double[] lambdar, double[] lambdas, double[] lambdaw, double Intresist, double[] htcapr, double[] htcaps, double[] htcapw, 
			double deltat, double uc, VTUF3DUtil util, double timeend, HashMap<String, HashMap<String, Namelist>> namelists, int[][] treeXYMap, 
			double zH, double z0roofh, double z0roofm, double z0roadh, double z0roadm, double moh, double[] depthr, double[] depths, double[] depthw, 
			double albs, double emiss, int[][] treeXYTreeMap, double Tsfcs, double albr, double emisr, double Tsfcr, double albw, double emisw, double Tsfcw, 
			double lambdaf, boolean calclf, boolean calcz0, double z0, double xlat_in, double xlatmax, double stror_in, double strormax, boolean facet_out, 
			double[] bh_o_bl,
			double timeis, int yd, double dta_starttime, double starttime, int numout, double outpt_tm, double Tintw, double Tints, double Tcan, 
			double[] gam, double[] tlayer, double[] denom, double[] A, double[] B, double[] D, 
			double dta_timeend, double[] lambdaav, double[] thick, double[] R, double IntCond, 
			double cpair, int numfrc, double deltatfrc, double[] timefrc, boolean calcLdn, 
			double Td, double Ldn_fact, double sigma, int cloudtype, boolean calcKdn, double[][] treeXYMapSunlightPercentageTotal, 
			HashMap<String, MaespaDataFile> maespaTestflxData, 
			double vK, double Kdn_diff, double nKdndiff, double Kdn_ae_store, double dalb, double svfe_store, double httc, double rw, double zrooffrc, 
			int DIFFERENTIALSHADINGDIFFUSE, HashMap<String, ArrayList<MaespaDataResults>> maespaDataArray, double Tfloor, 
			double Tthreshold, int tthresholdLoops, int numberOfExtraTthresholdLoops, double Tbuild_min, boolean frcwrite, double[] tlayerp, double[] htcap, 
			boolean sum_out, boolean matlab_out, boolean writeTsfc, boolean writeKl, boolean writeKabs, boolean writeKrefl, boolean writeLabs, 
			boolean writeLrefl, boolean writeLdown, boolean writeTmrt, boolean writeUtci, boolean writeEnergyBalances, double strorint, double xlatint, int badKdn, int year)
	{
		// Initialize parallel processing pool
		initParallelPool();

		int minres_bh;
		int par_ab, numsfc_ab, numsfc2, jab;
		double hwactual;
		int time_out;
		int j = 0, numvf = 0, vfiend, a1, a2, b1, b2;
		double avg_cnt;
		int numvertex, timewrite = 0;
		int numcany;
		int numNwall2, numSwall2, numEwall2, numWwall2, numstreet2, numtrees2 = 0, numtreetops2 = 0;
		int numroof2, numwall2, numabovezH, counter2;
		int lptowrite;
		int bhbltowrite, nbuildx, nbuildy;
		boolean ray;
		boolean first_write, last_write, newlp, newbhbl;		
		boolean solar_refl_done;
		boolean ywrite = false;
		String numc, lpwrite1, strorwrite1, latwrite1, ydwrite1, bhblwrite1;
		String numc2, lpwrite, strorwrite, latwrite, ydwrite2, bhblwrite2;
		String numc3, time1, ydwrite, bhblwrite, latwrite2;
		String time2;
		String time3;
//		int i;
		int numsfc;
		int par, iij;
		int nKgrid;
		int timefrc_index, maxbh;
		int tim ;
		int numTsun = 0, numTsh = 0, numNsun = 0, numNsh = 0, numSsun = 0, numSsh = 0, numEsun = 0;
		int numEsh = 0, numWsun = 0, numWsh = 0;
		int al, aw, bh, numfiles;
		int counter = 0;
		int p = 0;
		int yd_actual, bl, bw, sw, sw2;
		int numroof, al2, aw2;
		double[] fx = new double[5], fy = new double[5], fz = new double[5], fxx = new double[5], fyy = new double[5],
				fzz = new double[5];
		double pi, vx, vy, vz, vxx, vyy, vzz, xpinc, ypinc, vftot;
		double stror, az, ralt, ypos = 0, Kdir = 0;
		double solarin, vfsum2;
		double[] dx = new double[3];
		double y1, z1, z2 = 0, x2, yyy, Lup_refl, Lup_refl_old;
		double Lemit5, vftot5, Kup_refl, Kup_refl_old, aaaa;
		double CZ, INOT, Ktot = 0, alb_sfc, abs_aero = 0;
		double LAT, TM, zeni, AZIM, angdif, HWactual;
		double HW_avg2, wavelenx, waveleny;
		double lpactual, Kdir_NoAtm;
		double Kdir_Calc, Kdif_Calc, Kbeam, bhblactual;
		double patchlen;		
		double  deltat2, dTcan_old, zd, Qhcan;
		double  Udirdom, rhoa = 0, rhocan = 0, Cairavg, Tzd;
		double Tsfc_R, Ri, cdtown, Fm = 0, ustar = 0, Qhcan_kin, wstar = 0, httc_top;
		double Fh, Tlog_fact, bp, bm, bn, bq, CL, CR, FR, Cmid, Fmid, Ccan = 0;
		double Bcan = 0, Acan = 0, zzz, Ucantst, Ucan = 0, Qh_tot = 0, Rnet_tot = 0, Tsfc_cplt = 0;
		double Tsfc_bird = 0, Tsfc_N = 0, Tsfc_S = 0, Tsfc_E = 0, Tsfc_W = 0, Tsfc_T = 0, zwall;
		double Ueff, Thorz = 0, zhorz = 0, Uhorz = 0, rhohorz = 0, Rnet, Qg_tot = 0, Tconv;
		double Tnew, Told, Fold, Fold_prime, Tdiffmax, Qhtop, Aplan;
		double Tint, lambd_o_thick, lambd_o_thick2;		
		double canyair, Qhcantmp = 0, lambdapR;
		double Rnet_R = 0, Qh_R = 0, Qg_R = 0, Rnet_T = 0, Qh_T = 0, Qg_T = 0, Rnet_N = 0, Qh_N = 0, Qg_N = 0;
		double Rnet_S = 0, Qh_S = 0, Qg_S = 0, Rnet_E = 0, Qh_E = 0, Qg_E = 0, Rnet_W = 0, Qh_W = 0, Qg_W = 0;		
		double Qecantmp, Qe_tot = 0, Qetop, Qecan;
		double Qetot_avg = 0;
		double leFromEt5;
		double maespaWatQh;
		double maespaWatQn, maespaWatQc;
		double maespaPar, maespaTcan, maespaOutPar, maespaLw;
		int diffShadingValueUsed = 0;
		double diffShadingCalculatedValue;
		double Ucanpy, CA;
		double Kdn_R = 0, Kup_R = 0, Ldn_R = 0, Lup_R = 0, Kdn_T = 0, Kup_T = 0, Ldn_T = 0, Lup_T = 0;
		double Kdn_N = 0, Kup_N = 0, Ldn_N = 0, Lup_N = 0, Kdn_S = 0, Kup_S = 0, Ldn_S = 0, Lup_S = 0;
		double Kdn_E = 0, Kup_E = 0, Ldn_E = 0, Lup_E = 0, Kdn_W = 0, Kup_W = 0, Ldn_W = 0, Lup_W = 0;
		double thick_totr, thick_tots, thick_totw;
		double deltat_cond, Ta_sol, Td_sol, Emit_W, Absbl_W, Absbs_W;
		double Qh_abovezH = 0, httcR = 0, httcW = 0, httcT = 0;
		double Qanthro = 0, Qanthro_avg = 0, Qtau_avg = 0, Qac = 0, Qac_avg = 0, Qdeep = 0, Qdeep_avg = 0;
		double Rntot_avg = 0, Qhtot_avg = 0, Qgtot_avg = 0, TR_avg = 0, TT_avg = 0;
		double Trad_R = 0, Trad_T = 0, Trad_N = 0, Trad_S = 0, Trad_E = 0, Trad_W = 0;
		double TN_avg = 0, TS_avg = 0, TE_avg = 0, TW_avg = 0, Lroof,  Tp = 0;
		double TTsun = 0, TTsh = 0, TNsun = 0, TNsh = 0, TSsun = 0, TSsh = 0, TEsun = 0, TEsh = 0, TWsun = 0, TWsh = 0;
		double vf = 0;
		double xt, yt, zt, zen = 0, bldht_tot;
		double  Kup = 0, Lup = 0, xlat, Kdn_re_store = 0, separat, lambdac;
		double Kdn_grid = 0, Kdif = 0, DR1F = 0, Kuptot_avg = 0, Luptot_avg = 0;
		double  refldiff, svferror;
		double[][] v = new double[4][3];
		double[] vmag = new double[4];
		double[] vangle = new double[4];
		double[] vns = new double[3];
		double[] cp = new double[3];
		double[] vtemp1 = new double[3];
		double[] vtemp2 = new double[3];
		double vfsum;
		double magvns, mcp, dp, g;
		double[] vecti = new double[3];
		double[] vectj = new double[3];
		double pll, F3, F5, F7, F9;
		double[] angsun = new double[3];
		double[] angsfc = new double[3];
		double[][] corner = new double[4][3];		
		int[][] bldht;
		int[][] veght;
		int[][] veghti;
		
		boolean[][][] surf_shade;
		boolean[][][] veg_shade;
		boolean[][][][] surf;
		int[][] bldhti;
		double[][] sfc;
		int[] ind_ab;
		double[][] sfc_ab;
		int[] sfc_ab_map_x;
		int[] sfc_ab_map_y;
		int[] sfc_ab_map_z;
		int[] sfc_ab_map_f;
		int[] vffile;
		int[] vfppos;
		int[] vfipos;
		int[] mend;
		double[] refl_emist;
		double[] absbs;
		double[] absbl;
		double[] tots;
		double[] totl;
		double[] refls;
		double[] refll;
		double[] reflts;
		double[] refltl;
		double[] reflps;
		double[] reflpl;
		double[] vf2;
		double[] vf3 = null;
		int[] vf3j = null;
		int[] vf2j;
		double[][] vertex;
		double[][] face;
		double[] lambda_sfc;
		double[] Tsfc;
		double[] Trad;
		double[] Qh;
		double[] Qe;
		double[] Uwrite;
		double[] Twrite;
		
		// ! now keep track of the energy balance components so they can be output each timestep
		double[] currentRnet;
		double[] currentQe;
		double[] currentQh;
		double[] currentQg;
		
		int tempTimeis;
//		int nval = 8;
//		int[] ival = new int[nval];
//		double factR;
//		double random_number_value;
		String outputDebugStr;
		int timefrc_index_for_ldown;
		double gridTmrt;
		double gridUtci;
		double ldownForTmrtCalc, lupForTmrtCalc, maespaTcanForTmrtCalc, maespasoiltForTmrtCalc, taForUtciCalc,solarForTmrtCalc;
		Common common = new Common();

		int numlayersMinus1 = numlayers-1;
		int numlayersMinus2 = numlayers-2;
		int sixPlusThreeTimesNumlayers = 6 + (3 * numlayers)-1;
		int fivePlusNumlayers = 5 + numlayers-1;

//System.out.println("++++++++++++++++++++++++in loop=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000. );		
		//  MAIN LOOP THROUGH BUILDING GEOMETRIES (lp and bhbl)
		for (int lpiter = 0; lpiter < numlp; lpiter++)
		{
			for (int bhiter = 0; bhiter < numbhbl; bhiter++)
			{
				// ! KN, taking out resolution stuff, since the buildings are built from the config files
				minres_bh = minres; 
								
				newlp = true;
				newbhbl = true;
				vfcalc = 1;
				//  NOTE that these formulae assume that bl=bw (i.e. buildings with square footprints)
				//  AND that sw=sw2 (street widths are equal in both directions)

				if (lpin[lpiter] > 0.25) // ! KN taking out because specified in config now
				{
					sw = minres_bh;
					bl = (int) Math.round(
							(1.0*sw) * Math.sqrt(lpin[lpiter]) * (Math.sqrt(lpin[lpiter]) + 1.) / (1. - lpin[lpiter]));
				}
				else
				{
					bl = minres_bh;
					sw = (int) Math.round((1.0*bl) * (1. / Math.sqrt(lpin[lpiter]) - 1.));
				}

				bw = bl;
				bh = treeMapFromConfig.configTreeMapHighestBuildingHeight;
				//this used be from parameters.dat, but now is calculated from the domain
				buildht_m = bh;

				patchlen = treeMapFromConfig.configTreeMapGridSize;
				System.out.println("------------------------------------------");
				System.out.println("patch length (m) = " + " " + patchlen);
				System.out.println("building height (m) = " + " " + buildht_m);
				System.out.println("building height (patches) = " + " " + bh);
				System.out.println("reference or forcing height (m) = " + " " + zref);
				overall.writeOutput(Constants.inputs_store_out, "patchlen,buildht_m,bh,zref");
				overall.writeOutput(Constants.inputs_store_out, patchlen + " " + buildht_m + " " + bh + " " + zref);
			
				sw2 = sw;

				//  The following expressions control the size of the domain, which must be large
				//  enough so that the radiation is properly calculated (i.e., so that
				//  building walls and street in the central urban unit don't  see past the
				//  edge of the domain below roof level) - the expression currently used was
				//  arrived at by educated guess (essentially, either large sw or large bh
				//  relative to bl or bw is a problem, and requires a larger domain)
				nbuildx = (int) Math.round(2. * (1.0*bh) / ((1.0*bl) + (sw)) * 5. / Math.sqrt((1.0*bh) / (sw)));
				nbuildy = (int) Math.round(2. * (1.0*bh) / ((1.0*bw) + (sw2)) * 5. / Math.sqrt((1.0*bh) / (sw2)));
				if ((nbuildx % 2) == 0)
				{
					nbuildx = nbuildx + 1;
				}
				if ((nbuildy % 2) == 0)
				{
					nbuildy = nbuildy + 1;
				}

				overall.writeOutput(Constants.inputs_store_out, "nbuildx,nbuildy");
				overall.writeOutput(Constants.inputs_store_out, nbuildx + " " + nbuildy);
			
				//  Dimensions of the domain
				aw = Math.max(bw * 5 + sw2 * 4, nbuildy * bw + (nbuildy - 1) * sw2);
				al = Math.max(bl * 5 + sw * 4, nbuildx * bl + (nbuildx - 1) * sw);

				System.out.println("________________________________");
				System.out.println("number of buildings across domain in x, y directions:" + " " + Math.max(5, nbuildx)
						+ " " + Math.max(5, nbuildy));
			
				//  Defining the central 'urban unit' from which output is derived
				b1 = (int) Math.round(Math.max(2., ((1.0*nbuildy) - 1.) / 2.) * (1.0*bw))
						+ (int) Math.round((Math.max(2., ((1.0*nbuildy) - 1.) / 2.) - 0.4999) * (1.0*sw2)) + 1;
				b2 = b1 + bw + sw2 - 1;
				a1 = (int) Math.round(Math.max(2., ((1.0*nbuildx) - 1.) / 2.) * (1.0*bl))
						+ (int) Math.round((Math.max(2., ((1.0*nbuildx) - 1.) / 2.) - 0.4999) * (1.0*sw)) + 1;
				a2 = a1 + bl + sw - 1;

				aw = treeMapFromConfig.configTreeMapX;
				al = treeMapFromConfig.configTreeMapY;
				a1 = treeMapFromConfig.configTreeMapX1;
				a2 = treeMapFromConfig.configTreeMapX2;
				b1 = treeMapFromConfig.configTreeMapY1;
				b2 = treeMapFromConfig.configTreeMapY2;
				patchlen = treeMapFromConfig.configTreeMapGridSize;

				System.out.println("aw,al,a1,a2,b1,b2,patchlen,buildht_m,sw,bh,bl" + " " + aw + " " + al + " " + a1
						+ " " + a2 + " " + b1 + " " + b2 + " " + patchlen + " " + buildht_m + " " + sw + " " + bh + " " + bl);
		
				//  Geometric ratios of the central 'urban unit'
				lpactual = 1.0 * bl * bw / (bl + sw) / (bw + sw2);
				hwactual = 1.0 * bh * 2. / (sw + sw2);
				bhblactual = 1.0 * bh * 2. / (bl + bw);
				System.out.println("building height, length, street width (patches) = " + " " + bh + " " + bl + " " + sw);
				System.out.println("domain dimension in x, y (patches) = " + " " + al + " " + aw);
				System.out.println("urban unit start & end in x (patches) = " + " " + a1 + " " + a2);
				System.out.println("urban unit start & end in y (patches) = " + " " + b1 + " " + b2);
				overall.writeOutput(Constants.inputs_store_out, "bh,bl,sw,lpactual,bhblactual,hwactual,aw,al,a1,a2");
				overall.writeOutput(Constants.inputs_store_out, bh + " " + bl + " " + sw + " " + lpactual + " "
						+ bhblactual + " " + hwactual + " " + aw + " " + al + " " + a1 + " " + a2);

				//  Initial atmospheric values:
				press = Pressfrc[TUFreg3D.restartedRunStartTimestep];
				Udir = Udirfrc[TUFreg3D.restartedRunStartTimestep];
				Ktotfrc = Kdnfrc[TUFreg3D.restartedRunStartTimestep];
				Ldn = Ldnfrc[TUFreg3D.restartedRunStartTimestep];
				Ta = Tafrc[TUFreg3D.restartedRunStartTimestep] + 273.15;
				ea = eafrc[TUFreg3D.restartedRunStartTimestep];
				Ua = Math.max(0.1, Uafrc[TUFreg3D.restartedRunStartTimestep]);

				int numlayersMinusOne = numlayers - 1 ;
				// Determine inter-layer thermal conductivities
				for (int k = 0; k < numlayersMinusOne; k++)
				{
					lambdaavr[k] = (thickr[k] + thickr[k + 1])
							/ (thickr[k] / lambdar[k] + thickr[k + 1] / lambdar[k + 1]);
				}
				// adding additional resistance (0.123 W/m2/K) at building interiors
				lambdaavr[numlayersMinusOne] = thickr[numlayersMinusOne] / 2.
						/ (Intresist + thickr[numlayersMinusOne] / 2. / lambdar[numlayersMinusOne]);
				for (int k = 0; k < numlayersMinusOne; k++)
				{
					lambdaavs[k] = (thicks[k] + thicks[k + 1])
							/ (thicks[k] / lambdas[k] + thicks[k + 1] / lambdas[k + 1]);
				}
				lambdaavs[numlayersMinusOne] = lambdas[numlayersMinusOne];
				for (int k = 0; k < numlayersMinusOne; k++)
				{
					lambdaavw[k] = (thickw[k] + thickw[k + 1])
							/ (thickw[k] / lambdaw[k] + thickw[k + 1] / lambdaw[k + 1]);
				}
				// adding additional resistance (0.123 W/m2/K) at building interiors
				lambdaavw[numlayersMinusOne] = thickw[numlayersMinusOne] / 2.
						/ (Intresist + thickw[numlayersMinusOne] / 2. / lambdaw[numlayersMinusOne]);

				deltat = util.calcUC(uc, numlayers, thickr, lambdaavr, htcapr, thicks, lambdaavs, htcaps, thickw,
						lambdaavw, htcapw, lambdar, lambdas, lambdaw, deltat);
				

				// Various settings and calculations
				solar_refl_done = false;

				par = 12;
				par_ab = 5 + 4 * numlayers;

				ralt = 90. - zen;

				// so that output will be written at the final timestep
				timeend = timeend + 1.5 * deltat / 3600.;

				// for Matlab visualization output
				first_write = true;

				// Create the domain (call barray_cube)

				// allocate(bldhti(0:al+1,0:aw+1))
				// allocate(veghti(0:al+1,0:aw+1))
				bldhti = new int[al + 2][aw + 2];
				veghti = new int[al + 2][aw + 2];

				for (int x = 0; x < al + 2; x++)
				{
					for (int y = 0; y < aw + 2; y++)
					{
						bldhti[x][y] = 0;
						veghti[x][y] = 0;
					}
				}

				HashMap<String, int[][]> barray_returnValues = Barray_Cube.barray_cube(bw, bl, sw, sw2, al, aw, bh,
						bldhti, veghti, namelists, treeXYMap, treeMapFromConfig);
				bldhti = barray_returnValues.get("bldht");
				veghti = barray_returnValues.get("veght");
	
				maxbh = 0;
				numroof = 0;
				bldht_tot = 0.;
				int count2 =0;
				for (int y = 1; y < aw+1; y++)
				{
					for (int x = 1; x < al+1; x++)
					{
						count2  = count2 + 1;
						
						if (bldhti[x][y] > 0)
						{
							bldht_tot = bldht_tot + bldhti[x][y];
							if (bldhti[x][y] > maxbh)
							{
								maxbh = bldhti[x][y];
							}
							numroof = numroof + 1;
						}
					}
				}
				System.out.println("maxbh,zref,zh" + " " + maxbh + " " + zref + " " + zH);
				if ((1.0*maxbh) * patchlen > zref - 0.1 * zH)
				{
					System.out.println("zref must be at least 0.1*zH above highest roof");
					System.out.println(
							"maxbh, zref, 0.1*zH (all in m) = " + " " + maxbh * patchlen + " " + zref + " " + 0.1 * zH);
					System.exit(1);
				}

				zH = (bldht_tot) / (1.0*numroof);

				//  in metres:
				zH = zH * patchlen;

				//  thermal roughness lengths if not specified:
				if (z0roofh < 0.)
				{
					z0roofh = z0roofm / moh;
				}
				if (z0roadh < 0.)
				{
					z0roadh = z0roadm / moh;
				}

				if (z0roofh / z0roofm < (1. / 210.) || z0roadh / z0roadm < (1. / 210.))
				{
					System.out.println("'Problem; ratio too small: z0roof(h/m), z0road(h/m) = " + " "
							+ z0roofh / z0roofm + " " + z0roadh / z0roadm);
					System.exit(1);
				}

				dTcan_old = 0.;

				lambdapR = 0.;
				canyair = 0.;
				for (int y = 0; y < aw; y++)
				{
					for (int x = 0; x < al; x++)
					{
						//  determine the total air volume below zH in the central urban unit
						if (x >= a1 && x <= a2 && y >= b1 && y <= b2)
						{
							canyair = canyair + Math.max(0., zH - (bldhti[x][y]) * patchlen);
							if ((1.0*bldhti[x][y]) * patchlen >= zH - 0.01)
							{
								lambdapR = lambdapR + 1.;
							}
						}
					}
				}
				lambdapR = lambdapR / ((1.0*a2 - a1 + 1) * (b2 - b1 + 1));

				al2 = al;
				aw2 = aw;

				//  now declare:
				veght = new int[al2 + 2][aw2 + 2];
				bldht = new int[al2 + 2][aw2 + 2];
				surf_shade = new boolean[al2 + 2][aw2 + 2][bh + 2];
				veg_shade = new boolean[al2 + 1][aw2 + 1][bh + 2];
				surf = new boolean[al2+1][aw2+1][bh+1][5+1];
				Uwrite = new double[(int) Math.round(zref - 0.5)];
				Twrite = new double[(int) Math.round(zref - 0.5)];

				for (int x = 0; x < al + 2; x++)
				{
					for (int y = 0; y < aw + 2; y++)
					{
						bldht[x][y] = 0;
						veght[x][y] = 0;
					}
				}

				//  here, copy the bldhti array to bldht then deallocate bldhti array
				for (int y = 0; y < aw2+1; y++)
				{
					for (int x = 0; x < al2+1; x++)
					{
						bldht[x][y] = bldhti[x][y];
						veght[x][y] = veghti[x][y];
						// ! also add up the number of tree surfaces (4 walls * tree height) + 1 roof
						if (veght[x][y] > 0)
						{
							numtrees2 = numtrees2 + (4 * veght[x][y]);
							numtreetops2 = numtreetops2 + 1;
						}
					}
				}

				numsfc = 0;

				//  steps:
				//  make surf_shade array from bldht array

				//  faces: 1=up, 2=north, 3=east, 4=south, 5=west (subject to rotation up to 90 degrees, of course)
				// ! KN, added 6=vegetation

				//  general conversion from building height array to shading
				//  array of cells; true=street&building interior; false=ambient air:
				// if z height =0, could be street or building
				// z > 1 will be a building
				int count = 0;
				for (int z = 0; z <= bh + 1; z++)
				{
					for (int y = 0; y <= aw2 + 1; y++)
					{
						for (int x = 0; x <= al2 + 1; x++)
						{
							surf_shade[x][y][z] = false;
							count ++;
							if (bldht[x][y] >= z)
							{
								surf_shade[x][y][z] = true;
//								if (   (x-treeMapFromConfig.configTreeMapX1) >0 &&
//										(x-treeMapFromConfig.configTreeMapX1) <=3 &&
//										(y-treeMapFromConfig.configTreeMapY1) >0 &&
//										(y-treeMapFromConfig.configTreeMapY1) <=3
//									)
//								{
////								System.out.println("surf_shade " + (x-treeMapFromConfig.configTreeMapX1) + " " + (y-treeMapFromConfig.configTreeMapY1) + " " + z);
//								}
//								System.out.println("surf_shade " + (x) + " " + (y) + " " + z);
							}
							if (veght[x][y] > z)
							{
								veg_shade[x][y][z] = true;
//								System.out.println("veg_shade " + x + " " + y + " " + z);
							}
						}
					}
				}
				System.out.println("general conversion count "+ count);

				// general conversion from shading array to surface
				// array (no parameter values yet though):

				// initialize array to contain no faces; surf=T means it is a surface patch,
				// surf=F means it is nothing (e.g. border between 2 building interior
				// cells or border between 2 ambient air cells)
				for (int f = TUFreg3D.FACE_ONE; f <= TUFreg3D.FACE_FIVE; f++) 
				{
					for (int z = 0; z <= bh; z++)
					{
						for (int y = 1; y <= aw2; y++)
						{
							for (int x = 1; x <= al2; x++)
							{
								surf[x][y][z][f] = false;
							}
						}
					}
				}
				// streets
				{
					int z = 0; // only in scope in this section
					for (int y = 1; y <= aw2; y++)
					{
						for (int x = 1; x <= al2; x++)
						{
							count ++;							
							surf[x][y][z][TUFreg3D.FACE_ONE] = true;  
							if (bldht[x][y] > 0)
							{
								surf[x][y][z][TUFreg3D.FACE_ONE] = false;  
							}
							else
							{
//								if (   (x-treeMapFromConfig.configTreeMapX1) >0 &&
//										(x-treeMapFromConfig.configTreeMapX1) <=3 &&
//										(y-treeMapFromConfig.configTreeMapY1) >0 &&
//										(y-treeMapFromConfig.configTreeMapY1) <=3
//									)
//								{
//									System.out.println("Street " + x + " " + y + " " + z + " " + TUFreg3D.FACE_ONE);
//								}
								numsfc = numsfc + 1;
							}
						} 
					}
				}
				// roofs and walls
				{
					//these are the faces
					// faces: 1=up, 2=north, 3=east, 4=south, 5=west
					int f = TUFreg3D.FACE_ONE; // only in scope in this section  
					for (int z = 1; z <= bh; z++)
					{
						for (int y = 1; y < aw2; y++)
						{
							for (int x = 1; x < al2; x++)
							{
//								System.out.println(surf_shade[x][y][z] + " " + x + " " + y + " " + z + " " + f );
								if (surf_shade[x][y][z])
								{
									f = TUFreg3D.FACE_ONE;  
									if (!surf_shade[x][y][z + 1] || z == bh)
									{
										surf[x][y][z][f] = true;
										numsfc = numsfc + 1;
//										if (   (x-treeMapFromConfig.configTreeMapX1) >0 &&
//												(x-treeMapFromConfig.configTreeMapX1) <=3 &&
//												(y-treeMapFromConfig.configTreeMapY1) >0 &&
//												(y-treeMapFromConfig.configTreeMapY1) <=3
//											)
//										{
//											System.out.println("Roof/wall " + x + " " + y + " " + z + " " + f);
//										}
									}
									f = TUFreg3D.FACE_TWO;  
									if ((y != aw) && (!surf_shade[x][y + 1][z]) )
									{
										surf[x][y][z][f] = true;
										numsfc = numsfc + 1;
//										if (   (x-treeMapFromConfig.configTreeMapX1) >0 &&
//												(x-treeMapFromConfig.configTreeMapX1) <=3 &&
//												(y-treeMapFromConfig.configTreeMapY1) >0 &&
//												(y-treeMapFromConfig.configTreeMapY1) <=3
//											)
//										{
//											System.out.println("Roof/wall " + x + " " + y + " " + z + " " + f);
//										}
									}
									f = TUFreg3D.FACE_THREE; 
									if ((x != al)  && !(surf_shade[x + 1][y][z]) )
									{
										surf[x][y][z][f] = true;
										numsfc = numsfc + 1;
//										if (   (x-treeMapFromConfig.configTreeMapX1) >0 &&
//												(x-treeMapFromConfig.configTreeMapX1) <=3 &&
//												(y-treeMapFromConfig.configTreeMapY1) >0 &&
//												(y-treeMapFromConfig.configTreeMapY1) <=3
//											)
//										{
//											System.out.println("Roof/wall " + x + " " + y + " " + z + " " + f);
//										}
									}
									f = TUFreg3D.FACE_FOUR; 
									if ( (y != 1) && (!surf_shade[x][y - 1][z]) )
									{
										surf[x][y][z][f] = true;
										numsfc = numsfc + 1;
//										if (   (x-treeMapFromConfig.configTreeMapX1) >0 &&
//												(x-treeMapFromConfig.configTreeMapX1) <=3 &&
//												(y-treeMapFromConfig.configTreeMapY1) >0 &&
//												(y-treeMapFromConfig.configTreeMapY1) <=3
//											)
//										{
//											System.out.println("Roof/wall " + x + " " + y + " " + z + " " + f);
//										}
									}
									f = TUFreg3D.FACE_FIVE;  
									if ((x != 1) && (!surf_shade[x - 1][y][z]) )
									{
										surf[x][y][z][f] = true;
										numsfc = numsfc + 1;
//										if (   (x-treeMapFromConfig.configTreeMapX1) >0 &&
//												(x-treeMapFromConfig.configTreeMapX1) <=3 &&
//												(y-treeMapFromConfig.configTreeMapY1) >0 &&
//												(y-treeMapFromConfig.configTreeMapY1) <=3
//											)
//										{
//											System.out.println("Roof/wall " + x + " " + y + " " + z + " " + f);
//										}
									}
								}
							}
						}
					}
				}

				System.out.println("------------------------------------------");
				System.out.println("number of patches (domain) = " + " " + numsfc);
				System.out.println(a2 + " " + a1 + " " + b2 + " " + b1 + " " + bh + " " + bl + " " + bw);
				numsfc_ab = (a2 - a1 + 1) * (b2 - b1 + 1) + bh * 2 * (bl + bw);
				System.out.println("numsfc_ab2" + " " + numsfc_ab);
				// ! KN replace formula with value from config file
				numsfc_ab = treeMapFromConfig.configTreeMapNumsfcab; 
				System.out.println("number of patches (central urban unit) = " + " " + numsfc_ab);
	
				// ! KN haven't found a good way to count inner surfaces during the config process, so recount them here
				{
					int iIndex16=0; //only in htis scope
					int iabCount = 0; // only declare in this scope
					for (int f = TUFreg3D.FACE_ONE; f <= TUFreg3D.FACE_FIVE; f++)  
					{
						for (int z = 0; z <= bh; z++)
						{
							for (int y = 0; y <= aw2; y++)
							{
								for (int x = 0; x <= al2; x++)
								{
									if (surf[x][y][z][f])
									{

										iIndex16 = iIndex16 + 1;
										// ! print *,i
										if (x >= a1 && x <= a2 && y >= b1 && y <= b2)
										{
											// !print
											// *,'x,y,z,f,a1,a2,b1,b2,i,surf(x,y,z,f)',x,y,z,f,a1,a2,b1,b2,i,surf(x,y,z,f)

											iabCount = iabCount + 1;
											// ! print *,'iab',iab
										}
									}
								}
							}
						}
					}
					// ! stop
					if (iabCount > 0)
					{
						numsfc_ab = iabCount;
					}
					System.out.println("fixed number of patches (central urban unit) = " + " " + numsfc_ab);
				}

				sfc_ab = new double[numsfc_ab][par_ab];
				sfc_ab_map_x = new int[numsfc_ab];
				sfc_ab_map_y = new int[numsfc_ab];
				sfc_ab_map_z = new int[numsfc_ab];
				sfc_ab_map_f = new int[numsfc_ab];
				sfc = new double[numsfc][par];
				ind_ab = new int[numsfc];
				vffile = new int[numsfc_ab];
				vfppos = new int[numsfc_ab + 1];
				vfipos = new int[numsfc_ab + 1];
				mend = new int[numsfc_ab];
				// Use ArrayPool for frequently reallocated arrays to reduce GC pressure
				refl_emist = arrayPool.acquireDouble(numsfc_ab);
				absbs = arrayPool.acquireDouble(numsfc_ab);
				absbl = arrayPool.acquireDouble(numsfc_ab);
				tots = arrayPool.acquireDouble(numsfc_ab);
				totl = arrayPool.acquireDouble(numsfc_ab);
				refls = arrayPool.acquireDouble(numsfc_ab);
				refll = arrayPool.acquireDouble(numsfc_ab);
				reflts = arrayPool.acquireDouble(numsfc_ab);
				refltl = arrayPool.acquireDouble(numsfc_ab);
				reflps = arrayPool.acquireDouble(numsfc_ab);
				reflpl = arrayPool.acquireDouble(numsfc_ab);
				Tsfc = arrayPool.acquireDouble(numsfc_ab);
				Trad = arrayPool.acquireDouble(numsfc_ab);
				lambda_sfc = arrayPool.acquireDouble(numsfc_ab);
				Qh = arrayPool.acquireDouble(numsfc_ab);
				Qe = arrayPool.acquireDouble(numsfc_ab);

				currentRnet = arrayPool.acquireDouble(numsfc_ab);
				currentQe = arrayPool.acquireDouble(numsfc_ab);
				currentQh = arrayPool.acquireDouble(numsfc_ab);
				currentQg = arrayPool.acquireDouble(numsfc_ab);

				// Arrays for parallel processing - store per-patch intermediate values
				double[] httc_patch = arrayPool.acquireDouble(numsfc_ab);
				double[] Rnet_patch = arrayPool.acquireDouble(numsfc_ab);
				double[] Tconv_patch = arrayPool.acquireDouble(numsfc_ab);

				// Per-cell surface properties for spatial variation (if specified in treemap.dat)
				double[][] cellAlbedoMap = null;
				double[][] cellEmissivityMap = null;
				boolean hasCellProperties = treeMapFromConfig.hasCellProperties;
				if (hasCellProperties)
				{
					// Initialize per-cell property maps
					int domainWidth = treeMapFromConfig.width;
					int domainLength = treeMapFromConfig.length;
					cellAlbedoMap = new double[domainWidth][domainLength];
					cellEmissivityMap = new double[domainWidth][domainLength];

					// Fill with default values
					for (int xi = 0; xi < domainWidth; xi++)
					{
						for (int yi = 0; yi < domainLength; yi++)
						{
							cellAlbedoMap[xi][yi] = albs;  // Default to global street albedo
							cellEmissivityMap[xi][yi] = emiss;  // Default to global street emissivity
						}
					}

					// Populate from treemap config (1D array indexed as x * length + y)
					int numCells = treeMapFromConfig.cellAlbedo.length;
					System.out.println("Loading per-cell surface properties for " + numCells + " cells");
					for (int idx = 0; idx < numCells; idx++)
					{
						int xi = idx / domainLength;
						int yi = idx % domainLength;
						if (xi < domainWidth && yi < domainLength)
						{
							cellAlbedoMap[xi][yi] = treeMapFromConfig.cellAlbedo[idx];
							cellEmissivityMap[xi][yi] = treeMapFromConfig.cellEmissivity[idx];
						}
					}
					System.out.println("Per-cell properties loaded: albedo range [" +
						treeMapFromConfig.cellAlbedo[0] + " - " +
						treeMapFromConfig.cellAlbedo[numCells-1] + "]");
				}

				//  SFC_AB ARRAY (second dimension) - central urban unit; only patches to have 'history'
				//  1: i (sfc array)
				//  2: f (sfc array)
				//  3: z (sfc array)
				//  4: y (sfc array)
				//  5: x (sfc array)
				//  6 to 5+numlayers: layer temperatures (starting with layer closest to surface)
				//  5+numlayers+1 to 5+2*numlayers: layer thermal conductivities (avg)
				//  5+2*numlayers+1 to 5+3*numlayers: layer heat capacities
				//  5+3*numlayers+1 to 5+4*numlayers: layer thicknesses

				//  SFC ARRAY (second dimension) - all patches in the domain
				//  1: surface type (1=roof,2=street,3=wall)
				//  2: sunlit fraction (0 to 4 out of 4)
				//  3: albedo
				//  4: emissivity
				//  5: environment view factor (1-SVF)
				//  6: component of surface's normal vector pointing in x-direction
				//  7: component of surface's normal vector pointing in y-direction
				//  8: component of surface's normal vector pointing in z-direction
				//  9: 0-not in initial array, 1-in initial input array, 2-in area
				//  of interest for calculations (generally where output will come from
				//  10: x-value of patch center
				//  11: y-value of patch center
				//  12: z-value of patch center

				//  Layer depths for three sfcs
				depthr[TUFreg3D.ONE] = thickr[TUFreg3D.ONE] / 2.;
				thick_totr = thickr[TUFreg3D.ONE];
				for (int k = 1; k < numlayers; k++)
				{
					depthr[k] = depthr[k - 1] + (thickr[k - 1] + thickr[k]) / 2.;
					thick_totr = thick_totr + thickr[k];
				}
				depths[TUFreg3D.ONE] = thicks[TUFreg3D.ONE] / 2.;
				thick_tots = thicks[TUFreg3D.ONE];
				for (int k = 1; k < numlayers; k++)
				{
					depths[k] = depths[k - 1] + (thicks[k - 1] + thicks[k]) / 2.;
					thick_tots = thick_tots + thicks[k];
				}
				depthw[TUFreg3D.ONE] = thickw[TUFreg3D.ONE] / 2.;
				thick_totw = thickw[TUFreg3D.ONE];
				for (int k = 1; k < numlayers; k++)
				{
					depthw[k] = depthw[k - 1] + (thickw[k - 1] + thickw[k]) / 2.;
					thick_totw = thick_totw + thickw[k];
				}

				iij = 1;

				//  POPULATE THE MAIN PARAMETER ARRAY (SFC)
				//  ideally this would be up with the initial surf array assignment
				//  to reduce looping, but the sfc array is not defined yet at that point
				numroof2 = 0;
				numstreet2 = 0;
				numwall2 = 0;
				numNwall2 = 0;
				numSwall2 = 0;
				numEwall2 = 0;
				numWwall2 = 0;
				numtrees2 = 0;
				numtreetops2 = 0;
				int iIndex12 = 0-1; //start with -1 for 0 index arrays
				int iab = 0-1; //start with -1 for 0 index arrays
				sfc[1][9] = 0.;
				avg_cnt = ((a2 - a1 + 1.) * (b2 - b1 + 1.));
				canyair = canyair / (1.0*avg_cnt) / (1. - lambdapR);
				System.out.println("bh,aw2,al2" + " " + bh + " " + aw2 + " " + al2);
				// print *,'bh,aw2,al2',bh,aw2,al2;
				for (int f = TUFreg3D.FACE_ONE; f <= TUFreg3D.FACE_FIVE; f++)  
				{
					for (int z = 0; z <= bh; z++)
					{
						for (int y = 0; y <= aw2; y++)
						{
							for (int x = 0; x <= al2; x++)
							{
								if (surf[x][y][z][f])
								{
									iIndex12 = iIndex12 + 1;
									sfc[iIndex12][Constants.sfc_in_array] = 1.;

									//  if the patch is in the central urban
									// unit, sfc[i][sfc_in_array]=2.
									if (x >= a1 && x <= a2 && y >= b1 && y <= b2)
									{
										iab = iab + 1;
										// ! print *,'central urban unit
										// x,y,z,f,a1,a2,b1,b2,i,iab',x,y,z,f,a1,a2,b1,b2,i,iab
										sfc[iIndex12][Constants.sfc_in_array] = 2.;
										sfc_ab[iab][Constants.sfc_ab_i] = iIndex12;
										sfc_ab[iab][Constants.sfc_ab_f] = f;
										sfc_ab[iab][Constants.sfc_ab_z] = z;
										sfc_ab[iab][Constants.sfc_ab_y] = y;
										sfc_ab[iab][Constants.sfc_ab_x] = x;
										sfc_ab_map_x[iab] = x;
										sfc_ab_map_y[iab] = y;
										sfc_ab_map_z[iab] = z;
										sfc_ab_map_f[iab] = f;
									}

									//  set the roof, wall, and road albedos and emissivities
									//  and temperatures, and thermal properties and thicknesses
									if (f == TUFreg3D.FACE_ONE && z == 0)
									{
										sfc[iIndex12][Constants.sfc_surface_type] = 2.;
										// Use per-cell properties if available, otherwise global values
										if (hasCellProperties && cellAlbedoMap != null && x > 0 && y > 0 &&
											x <= cellAlbedoMap.length && y <= cellAlbedoMap[0].length)
										{
											sfc[iIndex12][Constants.sfc_albedo] = cellAlbedoMap[x-1][y-1];
											sfc[iIndex12][Constants.sfc_emiss] = cellEmissivityMap[x-1][y-1];
										}
										else
										{
											sfc[iIndex12][Constants.sfc_albedo] = albs;
											sfc[iIndex12][Constants.sfc_emiss] = emiss;
										}
										// if this is a Maespa vegetation surface, override with vegetation properties
										if (treeXYTreeMap[x-1][y-1] > 0)
										{
											// !print *,'surface i=',i,' is vegetation'
											sfc[iIndex12][Constants.sfc_albedo] = Constants.vegetationAlbedo;
											sfc[iIndex12][Constants.sfc_emiss] = Constants.vegetationEmissivity;
										}
										sfc[iIndex12][Constants.sfc_x_vector] = 0.;
										sfc[iIndex12][Constants.sfc_y_vector] = 0.;
										sfc[iIndex12][Constants.sfc_z_vector] = 1.;
										if (sfc[iIndex12][Constants.sfc_in_array] > 1.5)
										{
											numstreet2 = numstreet2 + 1;
											for (int k = 0; k < numlayers; k++)
											{
												sfc_ab[iab][k + 3 * numlayers + 5] = thicks[k];
												sfc_ab[iab][k + numlayers + 5] = lambdaavs[k];
												sfc_ab[iab][k + 2 * numlayers + 5] = htcaps[k];
											}
											lambda_sfc[iab] = lambdas[TUFreg3D.ONE];
											Tsfc[iab] = Tsfcs;
										}
									}
									else if (f == TUFreg3D.FACE_ONE && z > 0) 
									{
										sfc[iIndex12][Constants.sfc_surface_type] = 1.;
										sfc[iIndex12][Constants.sfc_albedo] = albr;
										sfc[iIndex12][Constants.sfc_emiss] = emisr;
										sfc[iIndex12][Constants.sfc_x_vector] = 0.;
										sfc[iIndex12][Constants.sfc_y_vector] = 0.;
										sfc[iIndex12][Constants.sfc_z_vector] = 1.;
										if (sfc[iIndex12][Constants.sfc_in_array] > 1.5)
										{
											numroof2 = numroof2 + 1;
											for (int k = 0; k < numlayers; k++)
											{
												sfc_ab[iab][k + 3 * numlayers + 5] = thickr[k];
												sfc_ab[iab][k + numlayers + 5] = lambdaavr[k];
												sfc_ab[iab][k + 2 * numlayers + 5] = htcapr[k];
											}
											lambda_sfc[iab] = lambdar[TUFreg3D.ONE];
											Tsfc[iab] = Tsfcr;
										}
									}
										else
										{
											sfc[iIndex12][Constants.sfc_surface_type] = 3.;
										sfc[iIndex12][Constants.sfc_albedo] = albw;
										sfc[iIndex12][Constants.sfc_emiss] = emisw;
										sfc[iIndex12][Constants.sfc_z_vector] = 0.;
										if (sfc[iIndex12][Constants.sfc_in_array] > 1.5)
										{
											numwall2 = numwall2 + 1;
											for (int k = 0; k < numlayers; k++)
											{
												sfc_ab[iab][k + 3 * numlayers + 5] = thickw[k];
												sfc_ab[iab][k + numlayers + 5] = lambdaavw[k];
												sfc_ab[iab][k + 2 * numlayers + 5] = htcapw[k];
											}
											Tsfc[iab] = Tsfcw;
											lambda_sfc[iab] = lambdaw[TUFreg3D.ONE];
										}
										if (f == TUFreg3D.FACE_TWO) 
										{
											if (sfc[iIndex12][Constants.sfc_in_array] > 1.5)
											{
												numNwall2 = numNwall2 + 1;
											}
											sfc[iIndex12][Constants.sfc_x_vector] = 0.;
											sfc[iIndex12][Constants.sfc_y_vector] = 1.;
										}
										else if (f == TUFreg3D.FACE_THREE) 
										{
											if (sfc[iIndex12][Constants.sfc_in_array] > 1.5)
											{
												numEwall2 = numEwall2 + 1;
											}
											sfc[iIndex12][Constants.sfc_x_vector] = 1.;
											sfc[iIndex12][Constants.sfc_y_vector] = 0.;
										}
										else if (f == TUFreg3D.FACE_FOUR) 
										{
											if (sfc[iIndex12][Constants.sfc_in_array] > 1.5)
											{
												numSwall2 = numSwall2 + 1;
											}
											sfc[iIndex12][Constants.sfc_x_vector] = 0.;
											sfc[iIndex12][Constants.sfc_y_vector] = -1.;
										}
										else if (f == TUFreg3D.FACE_FIVE)
										{
											if (sfc[iIndex12][Constants.sfc_in_array] > 1.5)
											{
												numWwall2 = numWwall2 + 1;
											}
											sfc[iIndex12][Constants.sfc_x_vector] = -1.;
											sfc[iIndex12][Constants.sfc_y_vector] = 0.;
										}
										else
										{
											System.out.println("PROBL w/ sfc(i, ) assignment");
											System.exit(1);
										}
										
									}
								}
							}
						}
					}
				}

				//started index with -1
				numsfc2 = iab+1;
				if (numsfc2 != numsfc_ab)
				{
					System.out.println("number of patches in the central urban unit incorrect");
					System.out.println("numsfc2!=numsfc_ab" + " " + numsfc2 + " " + numsfc_ab);
					System.exit(1);
				}

				// building + street widths in each horizontal dimension
				wavelenx = (1.0*bl + sw);
				waveleny = (1.0*bw + sw2);
				//  KN domain is not so regular now
				wavelenx = (1.0*treeMapFromConfig.configTreeMapCentralWidth / 2); 
				waveleny = (1.0*treeMapFromConfig.configTreeMapCentralLength / 2);

				//  Match each patch not in the central urban unit with its corresponding patch
				//  in the central urban unit. Its temperature will then evolve  according to that
				//  patch in the central urban unit. This is the optimization that allows this version 
				//  of the model to run much more quickly.
				boolean found = false;
				int iIndex = 0-1;//so can index by 0
				
//				for (int f = TUFreg3D.FACE_ONE; f <= TUFreg3D.FACE_FIVE; f++) 
//				{
//					for (int z = 0; z <= bh; z++)
//					{
//						for (int y = treeMapFromConfig.configTreeMapY1; y <= treeMapFromConfig.configTreeMapY1 + treeMapFromConfig.configTreeMapCentralWidth; y++)
//						{
//							for (int x = treeMapFromConfig.configTreeMapX1; x <= treeMapFromConfig.configTreeMapX1 + treeMapFromConfig.configTreeMapCentralWidth; x++)
//							{
//								if (surf[x][y][z][f])
//								{
//									System.out.println(surf[x][y][z][f] + " " + x + " " + y + " " + z + " " + f);
//								}
//								
//							}
//						}
//					}
//				}
//				System.out.println("-------------------------------------");
				
				for (int f = TUFreg3D.FACE_ONE; f <= TUFreg3D.FACE_FIVE; f++) 
				{
					for (int z = 0; z <= bh; z++)
					{
						for (int y = 1; y <= aw2; y++)
						{
							for (int x = 1; x <= al2; x++)
							{
//								System.out.println(surf[x][y][z][f] + " " + x + " " + y + " " + z + " " + f);
								if (surf[x][y][z][f])
								{
									iIndex = iIndex + 1;									
									for (int iabCount = 0; iabCount < numsfc2; iabCount++)
									{
//										System.out.println(surf[x][y][z][f] + " " + treeMapFromConfig.configTreeMapX1 + " " + sfc_ab[iabCount][Constants.sfc_ab_x] +
//												" " + treeMapFromConfig.configTreeMapCentralWidth + " " + (x - 1) + " " + (x - 1) % treeMapFromConfig.configTreeMapCentralWidth);
										if (Math.abs((int) (treeMapFromConfig.configTreeMapX1 - sfc_ab[iabCount][Constants.sfc_ab_x])) 
												== ( (x - 1) % treeMapFromConfig.configTreeMapCentralWidth))
										{
//											System.out.println(treeMapFromConfig.configTreeMapY1 + " " +  sfc_ab[iabCount][Constants.sfc_ab_y] 
//													+ " " + treeMapFromConfig.configTreeMapCentralLength + " " + (y - 1) + " " + (y - 1) % treeMapFromConfig.configTreeMapCentralLength);
											if (Math.abs((int) (treeMapFromConfig.configTreeMapY1 - sfc_ab[iabCount][Constants.sfc_ab_y])) 
													== ( (y - 1) % treeMapFromConfig.configTreeMapCentralLength))
											{
												// KN match these up by the grid instead now
												ind_ab[iIndex] = iabCount;
												// goto 329;
												//iabCount = numsfc2;
												found = true;
												break;
											}
										}
									}
									if (found)
									{
										found = false;
									}
									else
									{
										System.out.println("an i did not find an iab,i=" + " " + iIndex);
										System.exit(1);
										// 329 continue
									}

								}
							}
						}
					}
				}

				numroof2 = Math.max(1, numroof2);
				numstreet2 = Math.max(1, numstreet2);
				numwall2 = Math.max(1, numwall2);
				numNwall2 = Math.max(1, numNwall2);
				numSwall2 = Math.max(1, numSwall2);
				numEwall2 = Math.max(1, numEwall2);
				numWwall2 = Math.max(1, numWwall2);

				//  For roof heat transfer - find average roof length
				HW_avg2 = numwall2*1.0 / numstreet2*1.0 / 2.;
				Lroof = zH / patchlen / HW_avg2 * lambdapR / (1. - lambdapR) * 2.;

				lambdac = (1.0 * numwall2 + numroof2 + numstreet2 + numtrees2 + numtreetops2) / (numroof2 + numstreet2 + numtreetops2);

				//  from Macdonald, displacement height:
				zd = zH * (1. + Math.pow(4.43, (-(lambdapR + lpactual) / 2.)) * ((lambdapR + lpactual) / 2. - 1.));

				//  frontal index:
				if (lambdaf < 0.0 || calclf)
				{
					lambdaf = (1.0*numwall2 + numtrees2) / 4. / (1.0*numstreet2 + numroof2 + numtreetops2);
					calclf = true;
					System.out.println("lambdaf will be calculated by the model = " + " " + lambdaf);
				}

				//  z0:
				if (calcz0)
				{
					//  Macdonald's method for z0 (estimate lambdaf for now - both it and z0
					//  will be calculated for all future timesteps)
					z0 = zH * (1. - zd / zH) * Math.exp( -1 * Math.pow((0.5 * 1.2 / Math.pow((0.4), 2) * (1. - zd / zH) * lambdaf), (-0.5)));
					System.out.println("domain z0 will be calculated by the model (m) = " + " " + z0);
				}

				//  for radiation (multiple refl by atm of sfc reflected solar back to sfc)
				//  (just an estimate - it has only a minor impact, and will be replaced with
				//  the actual overall surface albedo after the first timestep)
				alb_sfc = (albr * (1.0*numroof2) + albs * (1.0*numstreet2)) / (1.0*numroof2 + numstreet2);
//System.out.println("alb_sfc " + alb_sfc);

				vftot5 = 0.;

				System.out.println("zH,zd,z0 = " + " " + zH + " " + zd + " " + z0);
				System.out.println("lambdap,lambdac,lambdaf = " + " " + lambdapR + " " + lambdac + " " + lambdaf);
				System.out.println("H/L, H/W ratios = " + " " + bhblactual + " " + hwactual);
				System.out.println("numroof2,numstreet2,numwall2,numNwall2,numSwall2,numWwall2,numEwall2" + " "
						+ numroof2 + " " + numstreet2 + " " + numwall2 + " " + numNwall2 + " " + numSwall2 + " "
						+ numWwall2 + " " + numEwall2);

				overall.writeOutput(Constants.inputs_store_out, "zH,zd,z0,lambdapR,lambdac,lambdaf");
				overall.writeOutput(Constants.inputs_store_out,
						zH + " " + zd + " " + z0 + " " + lambdapR + " " + lambdac + " " + lambdaf);
				overall.writeOutput(Constants.inputs_store_out, "Lroof,HW_avg2,al2,aw2");
				overall.writeOutput(Constants.inputs_store_out, Lroof + " " + HW_avg2 + " " + al2 + " " + aw2);

				//  direction vectors
				for (int k = 0; k < 5; k++)
				{
					fx[k] = 0.;
					fy[k] = 0.;
					fz[k] = 0.;
				}
				fx[TUFreg3D.THREE] = 0.5;
				fx[TUFreg3D.FIVE] = -0.5;
				fy[TUFreg3D.TWO] = 0.5;
				fy[TUFreg3D.FOUR] = -0.5;
				fz[TUFreg3D.ONE] = 0.5;

				int iIndex2 = 0-1;//for array zero indexing
				for (int f = TUFreg3D.FACE_ONE; f <= TUFreg3D.FACE_FIVE; f++) 
				{
					for (int z = 0; z < bh; z++)
					{
						for (int y = 1; y <= aw2; y++)
						{
							for (int x = 1; x <= al2; x++)
							{
								if (!surf[x][y][z][f])
								{
									// goto 284
									continue;
								}
								iIndex2 = iIndex2 + 1;

								//  patch i surface center:
								sfc[iIndex2][Constants.sfc_x] = 1.0*x + fx[f-1];
								sfc[iIndex2][Constants.sfc_y] = 1.0*y + fy[f-1];
								sfc[iIndex2][Constants.sfc_z] = 1.0*z + fz[f-1];

								// 284 continue
							}
						}
					}
				}

				System.out.println("------------------------------------------");
	
				vf2 = new double[numsfc * numsfc2];
				vf2j = new int[numsfc*numsfc2];

				dx = new double[3];
				vecti = new double[3];
				vectj = new double[3];
				vns = new double[3];
				vtemp1 = new double[3];
				vtemp2 = new double[3];

				// int x,y,z;
				// ------------------------------------------------------------------
				//  View Factor Calculations (or read in from file/cache)
System.out.println("++++++++++++++++++++++++start vfcalc=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000./60. );

				// ====== VIEW FACTOR CACHE CHECK ======
				boolean loadedFromCache = false;
				String workingDir = System.getProperty("user.dir");

				if (VIEW_FACTOR_CACHE_ENABLED && ViewFactorCache.cacheExists(workingDir, al2, aw2, bh, numsfc2))
				{
					ViewFactorCache.CacheData cacheData = ViewFactorCache.loadCache(workingDir);
					if (cacheData != null)
					{
						// Load data from cache
						numvf = cacheData.numvf;
						vf3 = cacheData.vf3;
						vf3j = cacheData.vf3j;
						vfppos = cacheData.vfppos;

						// Restore sfc_evf values
						for (int sfcIdx = 0; sfcIdx < cacheData.sfc_evf.length && sfcIdx < numsfc; sfcIdx++)
						{
							sfc[sfcIdx][Constants.sfc_evf] = cacheData.sfc_evf[sfcIdx];
						}

						loadedFromCache = true;
						System.out.println("View factors loaded from cache - skipping calculation");
					}
				}

				if (!loadedFromCache && vfcalc == 0)
				{
					// going to always calculate
					//
					// //! must read in
					// vffile[i],sfc[i][Constants.sfc_evf],vfipos[i],mend[i] etc
					// //! from file if view factors are already calculated and
					// stored
					// //! in files
					// open(unit=vfinfoDat,file="vfinfo.dat",access="DIRECT",recl=vfinfoDatRECL);
					// read(unit=vfinfoDat,rec=1)numfiles,numvf;
					// for (int iab=0;iab<numsfc2;iab++)
					// {
					// i=sfc_ab[iab][Constants.sfc_ab_i];
					// read(unit=vfinfoDat,rec=iab+1)vffile[iab],vfipos[iab],mend[iab],sfc[i][Constants.sfc_evf],sfc[i][Constants.sfc_x],sfc[i][sfc_y],sfc[i][sfc_z];
					// }
					// read(unit=vfinfoDat,rec=numsfc2+2)vfipos(numsfc2+1);
					// close(vfinfoDat);
				}
				else if (!loadedFromCache)
				{

					System.out.println("CALCULATING VIEW FACTORS...");
					// write(6,*)'CALCULATING VIEW FACTORS...'

					// vf2 = new double[numsfc*numsfc2];
					// vf2j = new int[numsfc*numsfc2];
					// allocate(vf2(numsfc*numsfc2))
					// allocate(vf2j(numsfc*numsfc2))

					// X=0;
					// Y=0;
					// Z=0;
					// numvf=0;

					//  direction vectors
					for (int k = 0; k < 5; k++)
					{
						fx[k] = 0.;
						fy[k] = 0.;
						fz[k] = 0.;
					}
					fx[TUFreg3D.THREE] = 0.5;
					fx[TUFreg3D.FIVE] = -0.5;
					fy[TUFreg3D.TWO] = 0.5;
					fy[TUFreg3D.FOUR] = -0.5;
					fz[TUFreg3D.ONE] = 0.5;

					for (int k = 0; k < 5; k++)
					{
						fxx[k] = fx[k];
						fyy[k] = fy[k];
						fzz[k] = fz[k];
					}



					double fact2 = 500000.;

					int n = 11;
					// numc="(i1)"+ (n-10);
					// open(unit=n,file="vf"+numc,access="DIRECT",recl=vfRECL);
					int m = 1;
					p = 1-1;
					iab = 0-1;//zero indexed arrays

					mend = new int[numsfc2];
					for (int k = 0; k < numsfc2; k++)
					{
						mend[k] = 0;
					}

					//  RUN THROUGH ALL ARRAY POSITIONS AND ONLY PERFORM CALCULATIONS ON POINTS SEEN
					for (int f = TUFreg3D.FACE_ONE; f <= TUFreg3D.FACE_FIVE; f++) 
					{
						if (f == TUFreg3D.FACE_ONE)
						{
							System.out.println("calculating view factors of horizontal patches...");	
						}
						else if (f == TUFreg3D.FACE_TWO) 
						{
							System.out.println("calculating view factors of north-facing patches...");
						}
						else if (f == TUFreg3D.FACE_THREE) 
						{
							System.out.println("calculating view factors of east-facing patches...");
						}
						else if (f == TUFreg3D.FACE_FOUR) 
						{
							System.out.println("calculating view factors of south-facing patches...");
						}
						else if (f == TUFreg3D.FACE_FIVE) 
						{
							System.out.println("calculating view factors of west-facing patches...");
						}
						int iIndex11 = 0-1;//zero indexed arrays
						for (int z = 0; z <= bh; z++)
						{
							for (int y = b1; y <= b2; y++)
							{
								for (int x = a1; x <= a2; x++)
								{
									if (!surf[x][y][z][f])
									{
										// goto 41;
										continue;
									}
									iab = iab + 1;
									iIndex11 = (int) sfc_ab[iab][Constants.sfc_ab_i];

									//  patch surface i center:
									vx = 1.*x + fx[f-1];
									vy = 1.*y + fy[f-1];
									vz = 1.*z + fz[f-1];

									vftot = 0.;

									j = 0-1;//zero indexed arrays

									if (m > fact2)
									{
										mend[iab - 1] = m - 1;
										m = 1;
										// close(unit=n);
										n = n + 1;
										if (n <= 19)
										{
											numc = "(i1)" + (n - 10);
											// open(unit=n,file="vf"+numc,access="DIRECT",recl=vfRECL);
										}
										else if (n >= 20 && n <= 109)
										{
											numc2 = "(i2)" + (n - 10);
											// open(unit=n,file="vf"+numc2,access="DIRECT",recl=vfRECL);
										}
										else
										{
											System.out.println("need to program in more vf files or increase # vfs");
											// more vf files or increase # vfs";
											System.out.println("each one can hold");
											// stop
											System.exit(1);
										}
									}

									vffile[iab] = n;
									vfipos[iab] = m;

									vfppos[iab] = p;

									for (int ff = TUFreg3D.FACE_ONE; ff <= TUFreg3D.FACE_FIVE; ff++) 
									{
										for (int zz = 0; zz <= bh; zz++)
										{
											for (int yy = 0; yy <= aw2; yy++)
											{
												for (int xx = 0; xx <= al2; xx++)
												{
													if (!surf[xx][yy][zz][ff])
													{
														// goto 81;
														continue;
													}
													

													vfsum = 0.0;

													j = j + 1;

													// patch surface j center:
													vxx = 1.*xx + fxx[ff-1];
													vyy = 1.*yy + fyy[ff-1];
													vzz = 1.*zz + fzz[ff-1];

													dx[TUFreg3D.ONE] = Math.abs(vx - vxx);
													dx[TUFreg3D.TWO] = Math.abs(vy - vyy);
													dx[TUFreg3D.THREE] = Math.abs(vz - vzz);
													separat = Math.sqrt(Math.pow((dx[TUFreg3D.ONE]), 2) + Math.pow((dx[TUFreg3D.TWO]), 2) + Math.pow((dx[TUFreg3D.THREE]), 2));

													//  a surface cannot see itself (no concave surfaces, only flat)
													//  also, ray tracing (function ray) to determine if the 2 sfcs can see each other
													if (iIndex11 == j || 
															!util.ray(x, y, z, f, xx, yy, zz, ff, surf_shade, fx, fy, fz, fxx, fyy, fzz, al2, aw2, maxbh))
													{
														vf = 0.;
														// goto 81;
														continue;
													}
													else
													{

														if (vfcalc == 1)
														{
															// calculation of exact view factors for PLANE PARALLEL facets ONLY
															vecti[TUFreg3D.ONE] = 1.*(sfc[iIndex11][Constants.sfc_x_vector]);
															vecti[TUFreg3D.TWO] = 1.*(sfc[iIndex11][Constants.sfc_y_vector]);
															vecti[TUFreg3D.THREE] = 1.*(sfc[iIndex11][Constants.sfc_z_vector]);
															vectj[TUFreg3D.ONE] = 1.*(sfc[j][6-1]);
															vectj[TUFreg3D.TWO] = 1.*(sfc[j][7-1]);
															vectj[TUFreg3D.THREE] = 1.*(sfc[j][8-1]);

															HashMap<String, Double> returnValues = Dotpro.dotpro(vecti, vectj, 3);
															dp = returnValues.get("dp");
															g = returnValues.get("g");

															if (Math.abs(dp) < 0.0001)
															{
																// 
																// perpendicular
																z1 = 0.;
																y1 = 0.;
																// patch separation distances in the three dimensions:
																for (int k = 0; k < 3; k++)
																{
																	z1 = z1 + Math.abs((1.*vecti[k]) * dx[k]);
																	y1 = y1 + Math.abs((1.*vectj[k]) * dx[k]);
																	// 
																	// if(double(vecti(k)).eq.0.0.and.double(vectj(k)).eq.
																	// ! & 0.0)
																	// x2=dx(k)
																}
																x2 = dx[TUFreg3D.ONE] + dx[TUFreg3D.TWO] + dx[TUFreg3D.THREE] - z1 - y1;
																if (x2 < 0.1)
																{
																	// use F7, the patches are aligned in one dimension
																	vf = util.F7(1.0, (y1 - 0.5), 1.0, (z1 - 0.5), 1.0);
																}
																else
																{
																	// use F9, the patches aren't aligned in any of the three dimensions
																	// subtract 0.5 or 1 to get distance to patch edge instead of patch center
																	vf = util.F9(1.0, (x2 - 1.0), 1.0, (y1 - 0.50), 1.0, (z1 - 0.5), 1.0);
																}
															}
															else
															{
																//  parallel
																x2 = 0.;
																// patch separation distances in the three dimensions:
																for (int k = 0; k < 3; k++)
																{
																	if (Math.abs((1.*vecti[k])) < 0.1)
																	{
																		if (x2 == 0.0)
																		{
																			x2 = dx[k] + 0.1;
																		}
																		z2 = dx[k];
																	}
																}
																x2 = x2 - 0.1;
																yyy = dx[TUFreg3D.ONE] + dx[TUFreg3D.TWO] + dx[TUFreg3D.THREE] - x2 - z2;

																if (z2 == 0.0 || x2 == 0.0)
																{
																	// use F3, the patches are aligned in one dimension, or pll if they are
																	// directly opposite
																	vf = util.pll(1.0, yyy, 1.0);
																	if (z2 + x2 > 0.1)
																	{
																		vf = util.F3(1.0, yyy, 1.0, (Math.max(x2, z2) - 1.0), 1.0);
																	}
																}
																else
																{
																	// use F5, the patches aren't aligned in any of the three dimensions
																	vf = util.F5(1.0, (x2 - 1.0), 1.0, yyy, 1.0, (z2 - 1.0), 1.0);
																}
															}

															if (vf > 1.0 || vf < 0.0)
															{
																System.out.println("vfprobexact" + " " + iIndex11 + " " + j + " " + vf);
																// write(6,*)'vfprobexact',i,j,vf
																// stop
																System.exit(1);
															}

															vf = Math.abs(vf);
														}
														else
														{
															// calculate normal vector of cell face (patch) i
															// to get a positive answer, the progression of corner points around the patch should be CLOCKWISE
															vns[TUFreg3D.ONE] = 1.*fx[f-1];
															vns[TUFreg3D.TWO] = 1.*fy[f-1];
															vns[TUFreg3D.THREE] = 1.*fz[f-1];
															magvns = Math.sqrt( Math.pow(vns[TUFreg3D.ONE], 2) + Math.pow(vns[TUFreg3D.TWO], 2) + Math.pow(vns[TUFreg3D.THREE], 2));

															// normalize the normal vector from point i
															vns[TUFreg3D.ONE] = vns[TUFreg3D.ONE] / magvns;
															vns[TUFreg3D.TWO] = vns[TUFreg3D.TWO] / magvns;
															vns[TUFreg3D.THREE] = vns[TUFreg3D.THREE] / magvns;
															// Find polygon corners for patch j and define the vectors to these vertices
															// These are contained in the array v(iv,k) with k=1,3 corresponding to x,y,z respectively
															// loop through the four corner points of this patch    
															for (int iv = 0; iv < 4; iv++)
															{
																xpinc = -0.5;
																ypinc = -0.5;
																if ((iv >= 2) && (iv <= 3))
																{
																	xpinc = 0.5;
																}
																if ((iv >= 1) && (iv <= 2))
																{
																	ypinc = 0.5;
																}
																//  set corner points
																if (ff == 1)
																{
																	corner[iv][TUFreg3D.ONE] = 1.0* vxx + xpinc;
																	corner[iv][TUFreg3D.TWO] = 1.0* vyy + ypinc;
																	corner[iv][TUFreg3D.THREE] = 1.0* vzz;
																}
																else if (ff == 2)
																{
																	corner[iv][TUFreg3D.ONE] = 1.0* (vxx) - (xpinc);
																	corner[iv][TUFreg3D.TWO] = 1.0* (vyy);
																	corner[iv][TUFreg3D.THREE] = 1.0* (vzz) + (ypinc);
																}
																else if (ff == 4)
																{
																	corner[iv][TUFreg3D.ONE] = 1.0* (vxx) + (xpinc);
																	corner[iv][TUFreg3D.TWO] = 1.0* (vyy);
																	corner[iv][TUFreg3D.THREE] = 1.0* (vzz) + (ypinc);
																}
																else if (ff == 3)
																{
																	corner[iv][TUFreg3D.ONE] = 1.0* (vxx);
																	corner[iv][TUFreg3D.TWO] = 1.0* (vyy) + (xpinc);
																	corner[iv][TUFreg3D.THREE] = 1.0* (vzz) + (ypinc);
																}
																else if (ff == 5)
																{
																	corner[iv][TUFreg3D.ONE] = 1.0* (vxx);
																	corner[iv][TUFreg3D.TWO] = 1.0* (vyy) - (xpinc);
																	corner[iv][TUFreg3D.THREE] = 1.0* (vzz) + (ypinc);
																}
																else
																{
																	System.out.println(
																			"PROBLEM, ff not properly defined");
																	// write(6,*)"PROBLEM,
																	// ff not
																	// properly
																	// defined";
																	// stop;
																	System.exit(1);
																}

																// set vector between point i and corner point of j
																v[iv][TUFreg3D.ONE] = corner[iv][TUFreg3D.ONE] - 1.0*vx;
																v[iv][TUFreg3D.TWO] = corner[iv][TUFreg3D.TWO] - 1.0*vy;
																v[iv][TUFreg3D.THREE] = corner[iv][TUFreg3D.THREE] - 1.0*vz;
																vmag[iv] = Math.sqrt(Math.pow(v[iv][TUFreg3D.ONE], 2) + Math.pow(v[iv][TUFreg3D.TWO], 2) 
																	+ Math.pow(v[iv][TUFreg3D.THREE], 2));
																// ! write(*,*)
																// 'v(iv,1-3)
																// ',v[iv][1-1],v[iv][2-1],v[iv][3-1]
															}

															// The following section is common for different surfaces.                  
															// Find angles between the vectors using the dot product rule: cos angle = u dot v / |u||v|
															// dotproducts are: x1*x2+y1*y2+z1*z2 where x1,y1,z1 and x2,y2,z2 are two vectors

															// Calculate the cross products between the vectors. This defines a vector normal
															// to the plane between the two vectors as required by the view factor calculation.
															for (int iv = 0; iv < 4; iv++)
															{
																for (int k = 0; k < 3; k++)
																{
																	vtemp1[k] = v[iv][k];
																	if (iv <= 3)
																	{
																		vtemp2[k] = v[iv + 1][k];
																	}
																	else
																	{
																		vtemp2[k] = v[TUFreg3D.ONE][k];
																	}
																}

																// Use the dot product to define the angle between vectors between two adjacent corners of the patch
																HashMap<String, Double> dotproreturnValues = Dotpro.dotpro(vtemp1, vtemp2, 3);
																dp = dotproreturnValues.get("dp");
																g = dotproreturnValues.get("g");

																vangle[iv] = g;

																// Now do the cross product
																HashMap crossproReturn = Crosspro.crosspro(vtemp1, vtemp2, 3);
																cp = (double[]) crossproReturn.get("cp");
																mcp = (double) crossproReturn.get("mcp");

																if (mcp <= 0)
																{
																	System.out.println("warning: mcp <=0");
																	// write(*,*)
																	// 'warning:
																	// mcp <=0'
																	System.out.println( "x y z " + " " + x + " " + y + " " + z);
																	// write(*,*)
																	// 'x y z
																	// ',x,y,z
																}
																//  normalize the cross product by the |vtemp1 x vtemp2|
																for (int k = 0; k < 3; k++)
																{
																	cp[k] = cp[k] / mcp;
																}

																// Now find the dot product of the vector normal to the plane between the two vectors and the vector
																// normal to the plane of surface i.
																dotproreturnValues = Dotpro.dotpro(cp, vns, 3);
																dp = dotproreturnValues.get("dp");
																g = dotproreturnValues.get("g");

																// Here is the view factor definition (as per Ashdown 1994, eqn 5.6; see also Baum et al. 1989, and
																// Hottel and Sarofim 1967)     
																vfsum = vfsum + dp * vangle[iv];
															}
															// Here is the view factor for this patch (can use this to test for limits); this is the "normal"
															// view factor definition (i.e. for an entire hemisphere).
															// The direction in which the corner points are processed matters - it yields a positive or
															// negative number. Convert negatives to positives. 
															vf = 1.*vfsum / (2. * Math.PI);

															// Taking absolute sum is necessary because relative progression around patch is sometimes clockwise (+)
															// and sometimes counterclockwise (-) depending on relative position of the patches
															// These could be pre-defined, but probably easier to take absolute sum here.
															if (vf < 0)
															{
																vf = Math.abs(vf);
															}

															// exact or contour integration view factors 'if'
														}

														// whether or not patch i sees patch j 'if'
													}

													if (vf > 0.)
													{
														// write(unit=n,rec=m)ind_ab(j),vf;
														vftot5 = vftot5 + vf;
														numvf = numvf + 1;
														vf2[p] = vf;
														vf2j[p] = ind_ab[j];
														p = p + 1;
														m = m + 1;
													}
													else
													{
													}

													vftot = vftot + vf;

													// 81 continue
												}
												// xx=1;
											}
											// xx=1;
											// yy=1;
										}
										// xx=1;
										// yy=1;
										// zz=0;
									}

									sfc[iIndex11][Constants.sfc_evf] = vftot;
									// 41 continue
								}
								// x=a1;
							}
							// x=a1;
							// y=b1;
						}
						// x=a1;
						// y=b1;
						// z=0;
					}

					vfipos[numsfc2 + 1-1] = m;
					vfppos[numsfc2 + 1-1] = p;
					System.out.println("total number of inter-patch view factors = " + " " + numvf);

					numfiles = n - 10;

					if (iab+1 != numsfc2)
					{
						System.out.println("number of surfaces in view factor calculation wrong");
						System.out.println("PROB w/ numsfc2: iab, numsfc2 =" + " " + iab + " " + numsfc2);
						System.exit(1);
					}

					//  write file so that view factors need not be recomputed
					// open(unit=vfinfoDat,file="vfinfo.dat",access="DIRECT",recl=vfinfoDatRECL)
					// write(unit=vfinfoDat,rec=1)numfiles,numvf
//					for (int iabCount = 0; iabCount < numsfc2; iabCount++)
//					{
//						i = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
//						// write(unit=vfinfoDat,rec=iab+1)vffile[iab],vfipos[iab],mend[iab],sfc[i][Constants.sfc_evf],sfc[i][sfc_x],sfc[i][sfc_y],sfc[i][sfc_z];
//					}
					// write(unit=vfinfoDat,rec=numsfc2+2)vfipos(numsfc2+1);
					// close(vfinfoDat);

				}

				// Move this section outside of the if so that vf3 and vf3j scope remains for the later use
				// Only copy from vf2 arrays if we didn't load from cache
				if (!loadedFromCache)
				{
					vf3 = new double[numvf];
					vf3j = new int[numvf];

					//  arrays of view factors
					for (int k = 0; k < numvf; k++)
					{
						vf3[k] = vf2[k];
						vf3j[k] = vf2j[k];
					}

					// ====== SAVE VIEW FACTOR CACHE ======
					if (VIEW_FACTOR_CACHE_ENABLED)
					{
						ViewFactorCache.saveCache(workingDir, al2, aw2, bh, numsfc2,
							numvf, vf3, vf3j, vfppos, sfc, numsfc);
					}
				}

				// ------------------------------------------------------------------

				if (!loadedFromCache && vfcalc == 0)
				{

					vf3 = new double[numvf];
					vf3j = new int[numvf];
					// allocate(vf3(numvf));
					// allocate(vf3j(numvf));
					// vf3=0.;
					// vf3j=0.;

					p = 1;

					//  open view factor files
					for (int iabCount = 0; iabCount < numsfc2; iabCount++)
					{
						vfppos[iabCount] = p;
						if (iabCount == 1 || vffile[iabCount] != vffile[iabCount - 1])
						{
							// close(unit=vf1Dat);
							if (vffile[iabCount] < 20)
							{
								// numc="(i1)"+ vffile[iab]-10);
								// open(unit=vf1Dat,file="vf"+numc,access="DIRECT",recl=8);
							}
							else if (vffile[iabCount] < 110)
							{
								// numc2="(i2)"+ (vffile[iab]-10);
								// open(unit=vf1Dat,file="vf"+numc2,access="DIRECT",recl=8);
							}
							else if (vffile[iabCount] < 1010)
							{
								// numc3="(i3)"+ (vffile[iab]-10);
								// open(unit=vf1Dat,file="vf"+numc3,access="DIRECT",recl=8);
							}
							else
							{
								System.out.println("TOO MANY VF FILES");
								// write(6,*)"TOO MANY VF FILES";
								// stop;
								System.exit(1);
							}
						}

						if (vfipos[iabCount + 1] == 1)
						{
							vfiend = mend[iabCount];
						}
						else
						{
							vfiend = vfipos[iabCount + 1] - 1;
						}

						//  write the view factors into memory
						for (int q = vfipos[iabCount]; q < vfiend; q++)
						{
							// read(unit=vf1Dat,rec=q)j,vf;
							vf3[p] = vf;
							vf3j[p] = j;
							vftot5 = vftot5 + vf;
							p = p + 1;
							if (vf > 1.0 || vf < 0.0)
							{
								System.out.println("VF PROB");
								// write(6,*)"VF PROB"
								// stop
								System.exit(1);
							}
						}

					}

					vfppos[numsfc2 + 1] = p;

					// close(unit=vf1Dat);

				}

				// Skip this validation if loaded from cache (p won't be set correctly)
				if (!loadedFromCache && numvf != p)
				{
					System.out.println("PROBLEM WITH VFs IN MEM" + " " + (p ) + " " + numvf);
				}

				//  -----------------------------------
				// Latitude and street orientation loops (because geometry and view factors
				// need not be re-computed for new latitudes or street orientations)
				// New forcing data may be advisable for new latitudes, however

				xlat = xlat_in;
				while (xlat <= xlatmax)
				{

					stror = stror_in;
					while (stror <= strormax)
					{

						//  write out intra-facet (patch) surface temperatures
						if (facet_out)
						{
							lptowrite = (int) Math.round(lpin[lpiter] * 100.);
							lpwrite = common.padLeft(lptowrite, 3, '0') ;

							bhbltowrite = (int) Math.round(bh_o_bl[bhiter] * 100.); 
							bhblwrite = common.padLeft(bhbltowrite, 3, '0') ;

							strorwrite = common.padLeft((int) Math.round(stror), 2, '0') ;

							latwrite = common.padLeft( (int) Math.round(Math.abs(xlat)), 2, '0') ;

							if (xlat >= 0.)
							{
								latwrite2 = latwrite + "N";
							}
							else
							{
								latwrite2 = latwrite + "S";
							}
							ydwrite = common.padLeft( yd, 3, '0') ;

							String TsfcSolarSVF_Patch_yd = "TsfcSolarSVF_Patch_yd" + ydwrite + "_lp" + lpwrite + "_bhbl"
									+ bhblwrite + "_lat" + latwrite2 + "_stror" + strorwrite + ".out";
							overall.writeOutput(TsfcSolarSVF_Patch_yd,
									"patch direction is patch normal: 1=upwards, 2=north,3=east, 4=south, 5=west (these are the directionsprior to domain rotation (stror>0)",true);
							overall.writeOutput(TsfcSolarSVF_Patch_yd, "patch_length(m)=" + " " + patchlen);
							overall.writeOutput(TsfcSolarSVF_Patch_yd,
									"time(h),patch_direction,z,y,x,SkyViewFactor,Tsurface(degC),Tbrightness(degC),Kabsorbed(W/m2),Kreflected(W/m2)");
						}

						overall.writeOutput(Constants.inputs_store_out, "________________________________________");
		
						// changing this so that the runs can be restarted at a later timestep
//						timeis = dta_starttime;
						timeis = TUFreg3D.restartedRunStartTimestep;
						timeend = dta_timeend;
						System.out.println("------------------------------------------");
						System.out.println(
								"simulation start time, end time (h) = " + " " + timeis + " " + dta_timeend);
		
						starttime = timeis;
						//  number of times output will be written in Matlab output section:
						numout = (int) ((timeend - starttime) / outpt_tm)
								+ Math.min(100, Math.max(1, (int) (1. / outpt_tm)));

						first_write = true;
						last_write = false;

						Tsfc = util.initTsfc(Tsfc, surf, bh, aw2, al2, a2, b1, b2, a1, sfc, Tsfcs, Tsfcr, Tsfcw);

						//  INTITIAL SUBSTRATE TEMPERATURE PROFILES SUCH THAT
						// Gin=Gout for each layer (i.e. a nonlinear initial T profile)
						for (int iabCount = 0; iabCount < numsfc2; iabCount++)
						{

							int iIndex3 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];

							//  implicit initial T profile assuming Gin=Gout for each
							//  layer, based on the input Tsfc and the input Tint/Tg

							//  roofs and walls
							Tint = Tintw;
							//  streets
							if (Math.abs(sfc[iIndex3][Constants.sfc_surface_type] - 2.) < 0.5)
							{
								Tint = Tints;
							}

							//  first calculate the thermal conductivities between layer centers by adding
							//  thermal conductivities (or resistivities) in series

							for (int k = 0; k < numlayers; k++)
							{
								lambdaav[k] = sfc_ab[iabCount][k + numlayers + 5];
								thick[k] = sfc_ab[iabCount][k + 3 * numlayers + 5];
							}

							//  surface matrix values:
							lambd_o_thick = lambdaav[TUFreg3D.ONE] / (thick[TUFreg3D.ONE] + thick[TUFreg3D.TWO]);
							A[TUFreg3D.ONE] = 0.;
							B[TUFreg3D.ONE] = 2. * ((lambd_o_thick) + lambda_sfc[iabCount] / thick[TUFreg3D.ONE]);
							D[TUFreg3D.ONE] = -2. * lambd_o_thick;
							R[TUFreg3D.ONE] = Tsfc[iabCount] * lambda_sfc[iabCount] / thick[TUFreg3D.ONE] * 2.;

							//  interior matrix values:
							for (int k = 1; k < numlayersMinus1; k++)
							{
								lambd_o_thick = lambdaav[k - 1] / (thick[k - 1] + thick[k]);
								lambd_o_thick2 = lambdaav[k] / (thick[k] + thick[k + 1]);
								A[k] = -2. * lambd_o_thick;
								B[k] = 2. * (lambd_o_thick + lambd_o_thick2);
								D[k] = -2. * lambd_o_thick2;
								R[k] = 0.;
							}

							//  values for conduction between innermost layer and inner air:
							lambd_o_thick = lambdaav[numlayersMinus2] / (thick[numlayersMinus2] + thick[numlayersMinus1]);
							A[numlayersMinus1] = -2. * lambd_o_thick;
							B[numlayersMinus1] = 2. * (lambd_o_thick + lambdaav[numlayersMinus1] / thick[numlayersMinus1] * IntCond);
							D[numlayersMinus1] = 0.;
							R[numlayersMinus1] = 2. * lambdaav[numlayersMinus1] * Tint / thick[numlayersMinus1] * IntCond;

							//  TRIDIAGONAL MATRIX SOLUTION FROM JACOBSON, p.
							// 166
							gam[TUFreg3D.ONE] = -D[TUFreg3D.ONE] / B[TUFreg3D.ONE];
							tlayer[TUFreg3D.ONE] = R[TUFreg3D.ONE] / B[TUFreg3D.ONE];

							for (int k = 1; k < numlayers; k++)
							{
								denom[k] = B[k] + A[k] * gam[k - 1];
								tlayer[k] = (R[k] - A[k] * tlayer[k - 1]) / denom[k];
								gam[k] = -D[k] / denom[k];
							}

							// do k=numlayers-1,1,-1
							for (int k = numlayers-2; k >= 0;)
							{
								tlayer[k] = tlayer[k] + gam[k] * tlayer[k + 1];
								k--;
							}

							for (int k = 0; k < numlayers; k++)
							{
								sfc_ab[iabCount][k + 5] = tlayer[k];
							}

						}
System.out.println("++++++++++++++++++++++++start before time integration=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000./60 );		
						//  INITIALIZATION BEFORE TIME INTEGRATION

						numabovezH = 0;
						numcany = 0;
						for (int iabCount = 0; iabCount < numsfc_ab; iabCount++)
						{
							int i = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
							if (sfc[i][Constants.sfc_in_array] > 1.5)
							{
								int iIndex3 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
								if ((sfc[iIndex3][Constants.sfc_z] - 0.5) * patchlen < zH - 0.01)
								{
									numcany = numcany + 1;
								}
								else
								{
									numabovezH = numabovezH + 1;
								}
							}
						}

						//  initial values:
						Tcan = Tafrc[TUFreg3D.restartedRunStartTimestep] + 273.15 + 0.5;
System.out.println(Tcan + " " + Tafrc[TUFreg3D.restartedRunStartTimestep]);
						Qhcan = 0.;
						Qecan = 0.;
						Tsfc_R = Tsfcr * (1.0*numroof2);

						rhocan = press * 100. / 287.04 / Tcan;

						// This is the average heat capacity of air per m2 below zH (and only
						// for the fraction of the plan area for which building heights < zH)
						// for 3-D geometries: (canyair is the height of the air column below
						// zH if all the buildings below zH were put into one massive building of height
						// much lower than zH, of course - covering the all vertical columns
						// up to zH for which building height < zH in the entire area of interest)
						// units of Cairavg: J/K/m2/unit square of area for which building height<0 or street
						Cairavg = canyair * rhocan * cpair;

						// So that Tcan changes will not be larger than 0.1C (unstable?)
						// in one timestep (there are often instabilities in the ICs)...will
						// attempt to increase it later on
						deltat2 = 0.1 * Cairavg * (1.0*avg_cnt) / ((1.0*numcany)
								* Math.max(Math.abs(Tcan - Tsfcs), Math.abs(Tcan - Tsfcw)) * (7.8 + 4.2 * Ua));
						deltat_cond = deltat;
						deltat = Math.min(deltat, deltat2);
						System.out.println("new time step for Tcan stability (s) = " + " " + deltat);

						timeis = timeis + deltat / 3600.;

						// starting runs in the middle
//						timefrc_index = 0;
						timefrc_index = 0;		
						if (timeis > 0)
						{
							timefrc_index =  (int)Math.round(timeis/deltatfrc);
						}
						
						counter2 = 0;
						tim = 1;

						System.out.println("------------------------------------------");
						System.out.println("lambdap=" + " " + lpactual + " " + " H/L=" + " " + bhblactual + " " + " lat=" + " " + xlat + " " + " stror=" + " " + stror);
				
						//  plan area in patches
						Aplan = (1.0*numroof2 + numstreet2);

						// goto 922; // I don't think I need this section
						// anymore, moved the code into the post goto 937 lines
						//
						// //! continue here if changing the time step
						//// 937 continue;
						// //!print *,'after 937'
						//
						// timeis=timeis+deltat/3600.;
						//
						// 922 continue;
						// //!print *,'after 922'
						//
						// ywrite=true;
System.out.println("++++++++++++++++++++++++start main time=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000./60 );
						//  START OF MAIN TIME
						// LOOP----------------------------------------
						// Start progress tracking
						if (progressTracker != null) {
							progressTracker.start(starttime, timeend);
						}

						while (timeis <= timeend)
						{
							// Update progress tracker
							if (progressTracker != null) {
								progressTracker.update(timeis);
							}
//System.out.println("++++++++++++++++++++++++start next while main time=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000./60 );								
							// do 309 while (timeis<=timeend)
							// !print *,'start 309'
							//  try to increase the timestep for the first 2 hours of simulation
							//  because often the disequilibrium of the ICs causes the above two
							//  tests to reduce the timestep drastically in the early going
							if (counter > 25)
							{
								if (deltat < 8.0 && 3. * deltat < deltat_cond)
								{
									timeis = timeis - deltat / 3600.;
									deltat = deltat * 3.;
									System.out.println("INCREASING TIMESTEP BY 200% TO:" + " " + deltat);
									// write(6,*)'INCREASING TIMESTEP BY 200%
									// TO:',deltat;
									counter = 0;
									// goto 937;
									timeis = timeis + deltat / 3600.;
									ywrite = true;
									continue;
								}
								//  try to increase the timestep every so often throughout the simulation
								//  fast increase if the timestep is small:
								else if (timeis < starttime + 2.0 && 1.5 * deltat < deltat_cond)
								{
									timeis = timeis - deltat / 3600.;
									deltat = deltat * 1.5;
									System.out.println("INCREASING TIMESTEP BY 50% TO:" + " " + deltat);
									counter = 0;
									// goto 937;
									timeis = timeis + deltat / 3600.;
									ywrite = true;
									continue;
								}
							}
							//  slower increase otherwise:
							if (counter > 100 && 1.3 * deltat < deltat_cond)
							{
								timeis = timeis - deltat / 3600.;
								counter = 0;
								deltat = deltat * 1.3;
								System.out.println("INCREASING TIMESTEP BY 30% TO:" + " " + deltat);
								// goto 937;
								timeis = timeis + deltat / 3600.;
								ywrite = true;
								continue;
							}
							//  INTERPOLATE FORCING DATA

							if (timefrc[timefrc_index] <= timeis)
							{
								timefrc_index = Math.min(numfrc + 1, timefrc_index + 1);
							}
							Ktotfrc = Kdnfrc[timefrc_index - 1] + (timeis - timefrc[timefrc_index - 1]) / deltatfrc
									* (Kdnfrc[timefrc_index] - Kdnfrc[timefrc_index - 1]);
							Ldn = Ldnfrc[timefrc_index - 1] + (timeis - timefrc[timefrc_index - 1]) / deltatfrc
									* (Ldnfrc[timefrc_index] - Ldnfrc[timefrc_index - 1]);
							Ta = Tafrc[timefrc_index - 1] + (timeis - timefrc[timefrc_index - 1]) / deltatfrc
									* (Tafrc[timefrc_index] - Tafrc[timefrc_index - 1]);
							Ta = Ta + 273.15;
							ea = eafrc[timefrc_index - 1] + (timeis - timefrc[timefrc_index - 1]) / deltatfrc
									* (eafrc[timefrc_index] - eafrc[timefrc_index - 1]);
							Ua = Math.max(0.1, Uafrc[timefrc_index - 1] + (timeis - timefrc[timefrc_index - 1])
									/ deltatfrc * (Uafrc[timefrc_index] - Uafrc[timefrc_index - 1]));
							Udir = Udirfrc[timefrc_index - 1] + (timeis - timefrc[timefrc_index - 1]) / deltatfrc
									* (Udirfrc[timefrc_index] - Udirfrc[timefrc_index - 1]);
							press = Pressfrc[timefrc_index - 1] + (timeis - timefrc[timefrc_index - 1]) / deltatfrc
									* (Pressfrc[timefrc_index] - Pressfrc[timefrc_index - 1]);
							//  Prata's formula (QJRMS 1996)
							if (calcLdn)
							{
								Ldn = (1. - (1. + 46.5 * ea / Ta)
										* Math.exp(-(Math.pow((1.2 + 3. * 46.5 * ea / Ta), (0.5))))) * sigma
										* Math.pow(Ta, 4);
								Td = (4880.357 - 29.66 * Math.log(ea)) / (19.48 - Math.log(ea));
								Ldn = Ldn * Ldn_fact;
							}

							Udir = (Udir % 360.);
							//  wind direction relative to the domain
							Udirdom = Udir - stror;
							if (Udirdom < 0.)
							{
								Udirdom = Udir + (360. - stror);
							}

							//  calculate frontal area index, taking into account the wind direction
							if (calclf)
							{
								if (Udirdom < 180.)
								{
									if (Udirdom < 90.)
									{
										lambdaf = (util.sind(Udirdom) * 1.0*numEwall2 + util.cosd(Udirdom) * 1.0*numNwall2) / (1.0*numstreet2 + numroof2);
									}
									else
									{
										lambdaf = (util.sind(Udirdom - 90.) * 1.0*numSwall2 + util.cosd(Udirdom - 90.) * 1.0*numEwall2) / (1.0*numstreet2 + numroof2);
									}
								}
								else if (Udirdom < 270.)
								{
									lambdaf = (util.sind(Udirdom - 180.) * 1.0*numWwall2 + util.cosd(Udirdom - 180.) * 1.0*numSwall2) / (1.0*numstreet2 + numroof2);
								}
								else
								{
									lambdaf = (util.sind(Udirdom - 270.) * 1.0*numNwall2 + util.cosd(Udirdom - 270.) * 1.0*numWwall2) / (1.0*numstreet2 + numroof2);
								}
							}

							if (calcz0)
							{
								//  Macdonald's method for z0
								z0 = zH * (1. - zd / zH) * Math.exp(-Math.pow((0.5 * 1.2 / Math.pow((0.4), 2) * (1. - zd / zH) * lambdaf), (-0.5)));
							}

							//  canyon-atm exchange:
							Ri = util.SFC_RI(zref - zH + z0, Ta, Tcan, Ua);
							HashMap<String, Double> htcReturn = util.HTC(Ri, Ua, zref - zH + z0, z0, z0);
							Fh = htcReturn.get("Fh");
							httc_top = htcReturn.get("httc_out");
							Tlog_fact = 0.74 * httc_top * (Tcan - Ta) / Math.pow(vK, 2) / Fh;

							//  -------------------------------------------
							//  Solar angle and incoming shortwave (direct & diffuse) routines
							LAT = xlat * Math.PI / 180.;
							TM = (timeis % 24.);
							yd_actual = yd + (int) (timeis / 24.);
							yd_actual = (yd_actual % 365);
							//  SUNPOS calculates the solar angles
							HashMap<String, Double> sunposReturn = util.SUNPOS(yd_actual, TM, LAT);
							zeni = sunposReturn.get("ZEN");
							AZIM = sunposReturn.get("AZIM");
							CZ = sunposReturn.get("CZ");
							INOT = sunposReturn.get("INOT");
							CA = sunposReturn.get("CA");
							az = AZIM * 180. / Math.PI;
							zen = zeni * 180. / Math.PI;
							ralt = 90. - zen;

							Ta_sol = Ta - 273.15;
							Td_sol = Td - 273.15;
							//  CLRSKY accounts for attenuation by and multiple reflection with the atmosphere
							//  It essentially calculates direct and diffuse shortwave reaching the surface
							//  There is also a basic cloud parameterization in it
							HashMap<String, Double> clrskyreturn = util.CLRSKY(CZ, press / 10., zeni, Ta_sol, Td_sol,
									INOT, Kdir, Kdif, Ktot, CA, yd_actual, alb_sfc, cloudtype, abs_aero, Ktotfrc, DR1F);
							CZ = clrskyreturn.get(VTUF3DUtil.CZ_INDEX);
							press = clrskyreturn.get(VTUF3DUtil.PRESS_INDEX);
							zeni = clrskyreturn.get(VTUF3DUtil.ZEN_INDEX);
							Ta_sol = clrskyreturn.get(VTUF3DUtil.AIR_INDEX);
							Td_sol = clrskyreturn.get(VTUF3DUtil.DEW_INDEX);
							INOT = clrskyreturn.get(VTUF3DUtil.INOT_INDEX);
							Kdir = clrskyreturn.get(VTUF3DUtil.DR1_INDEX);
							Kdif = clrskyreturn.get(VTUF3DUtil.DF1_INDEX);
							Ktot = clrskyreturn.get(VTUF3DUtil.GL1_INDEX);
							CA = clrskyreturn.get(VTUF3DUtil.CA_INDEX);
							alb_sfc = clrskyreturn.get(VTUF3DUtil.alb_sfc_INDEX);
							abs_aero = clrskyreturn.get(VTUF3DUtil.abs_aero_INDEX);
							Ktotfrc = clrskyreturn.get(VTUF3DUtil.Ktotfrc_INDEX);
							DR1F = clrskyreturn.get(VTUF3DUtil.DR1F_INDEX);
//System.out.println("clrsky " + " " + CZ+ " " + (press / 10.)+ " " + zeni+ " " + Ta_sol+ " " + Td_sol+ " " +INOT
//		+ " " + Ktotfrc + " " + cloudtype + " " + alb_sfc + " " + yd_actual);
//System.out.println("Kdif1 " + Kdif);
//System.out.println("Kdir1 " + Kdir);
							
							Kdir_NoAtm = INOT * Math.cos(zeni);
							Kdir_Calc = Kdir;
							Kdif_Calc = Kdif;

							// to allow the solar radiation routine to calc solar radiation amounts if they are not input
							// ! if (calcKdn) Ktotfrc=Ktot
							// ! Kdif=Ktotfrc*Kdif/(Ktot+1.e-9)
							// ! Kdir=Ktotfrc*Kdir/(Ktot+1.e-9)
							if (!calcKdn)
							{
								if (Ktotfrc > 0.)
								{
									//  average of solar scheme DF/Ktot and that calculated from the Orgill/Hollands param			
									Kdif = (Ktotfrc - DR1F + Ktotfrc * Kdif / (Ktot + 1.e-9)) / 2.;
//System.out.println("Kdif2 " + Kdif);

									// ! Kdif=Ktotfrc-DR1F
									// ! Kdif=Ktotfrc*Kdif/(Ktot+1.e-9)
									Kdir = Ktotfrc - Kdif;
//System.out.println("Kdir2 " + Kdir);
								}
								else
								{
									Kdif = 0.;
									Kdir = 0.;
								}
							}

							Ktot = Kdir + Kdif;

							//  SO THAT KBEAM (I.E. FLUX DENSITY PERP TO SUN) DOES NOT GET TOO BIG
							//  FOR LOW SUN ANGLES (IN CASE OBSERVED KDN AND CALCULATED KDN DO NOT AGREE EXACTLY)
							if (!calcKdn && (Kdir - Kdir_Calc) / Math.max(1.e-9, Kdir_Calc) > 0.15 && ralt < 10.0)
							{
								double kDirPrev = Kdir;
								// ! if (!calcKdn.and.abs(Kdir-Kdir_Calc)/
								// ! & max(1.e-9,Kdir_Calc)>0.50) then
								Kbeam = Math.min(INOT * Kdir_Calc / Math.max(1.e-9, Kdir_NoAtm), Kdir / Math.max(1.e-9, util.sind(ralt)));
//System.out.println("Kbeam1 " + Kbeam + " " + ralt);
if (Kbeam > 10000)
{
	Kbeam = 0.0;
}														
								Kdir = Kbeam * util.sind(ralt);


								Kdif = Ktotfrc - Kdir;
//System.out.println("Kdif3 " + Kdif);
//System.out.println("Kdir3 " + Kdir);
//System.out.println("kdir2 " + Kdir);
							}
							else
							{
								Kbeam = Kdir / Math.max(1.e-9, util.sind(ralt));
//System.out.println("Kbeam2 " + Kbeam+ " " + ralt);
if (Kbeam > 10000)
{
	Kbeam = 0.0;
}								
								if (Kbeam > 1390.)
								{
									System.out.println("KBEAM unreasonable; Kbeam,Kdir,ralt,sind(ralt) = " + " " + Kbeam
											+ " " + Kdir + " " + ralt + " " + util.sind(ralt));
	
									overall.writeOutput(Constants.inputs_store_out,
											"KBEAM unreasonable; Kbeam,Kdir,ralt,sind(ralt) = " + " " + Kbeam + " "
													+ Kdir + " " + ralt + " " + util.sind(ralt));
			
									if (Kbeam > 1370.0 * 2.0 || Ktot > 1370.)
									{
										System.out.println(
												"KBEAM or KTOT unreasonable; Ktot,Kbeam,Kdir,ralt,sind(ralt) = " + " "
														+ Ktot + " " + Kbeam + " " + Kdir + " " + ralt + " "
														+ util.sind(ralt));
						
										overall.writeOutput(Constants.inputs_store_out,
												"KBEAM or KTOT unreasonable; Ktot,Kbeam,Kdir,ralt,sind(ralt) = " + " "
														+ Ktot + " " + Kbeam + " " + Kdir + " " + ralt + " "
														+ util.sind(ralt));
						
										System.exit(1);
									}
								}
							}
							// !treeXYMapSunlightPercentageTotal=0.
							// !treeXYMapSunlightPercentagePoints=0.

							if (Ktot > 1.0E-3)
							{
								//  Solar shading of patches
								// -----------------------------------------
								HashMap shadeReturn;
								if (PARALLEL_ENABLED && parallelPool != null) {
									// Use parallel shade calculation
									shadeReturn = Shade.shadeParallel(stror, az, ralt, ypos, surf, surf_shade, al2, aw2,
											maxbh, par, sfc, numsfc, a1, a2, b1, b2, numsfc2, sfc_ab, par_ab, veg_shade,
											timeis, yd_actual, treeXYMapSunlightPercentageTotal, treeXYMap,
											maespaTestflxData, parallelPool);
								} else {
									// Fall back to sequential
									shadeReturn = Shade.shade(stror, az, ralt, ypos, surf, surf_shade, al2, aw2,
											maxbh, par, sfc, numsfc, a1, a2, b1, b2, numsfc2, sfc_ab, par_ab, veg_shade,
											timeis, yd_actual, treeXYMapSunlightPercentageTotal, treeXYMap,
											maespaTestflxData);
								}
								sfc = (double[][]) shadeReturn.get("sfc");
								sfc_ab = (double[][]) shadeReturn.get("sfc_ab");
								treeXYMapSunlightPercentageTotal = (double[][]) shadeReturn.get("treeXYMapSunlightPercentageTotal");
							}

							for (int iabCount = 0; iabCount < numsfc_ab; iabCount++)
							{
								absbs[iabCount] = 0.;
								refls[iabCount] = 0.;
								reflts[iabCount] = 0.;
								refltl[iabCount] = 0.;
							}
							//  CONTINUATION POINT FOR Tsfc-Lup balance iterations (below)--------
							boolean tsfcLupBalanceContinue = true;
							while (tsfcLupBalanceContinue)
							{
								// 898 continue
								// !print *,'after 898'
								Tdiffmax = 0.;

								if (solar_refl_done || Ktot <= 0.)
								{
									//  ---------------------------------
									//  LONGWAVE ONLY (solar has already been done in previous Tsfc-Lup  iteration)
									//  RADIATION INITIALIZATION

									//  zeroth longwave reflection (i.e. emission)
									vfsum2 = 0.;

									for (int iabCount = 0; iabCount < numsfc2; iabCount++)
									{
										int iIndex4 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
										refltl[iabCount] = 0.;
										refll[iabCount] = sfc[iIndex4][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										absbl[iabCount] = 0.;
										vfsum2 = vfsum2 + (1. - sfc[iIndex4][Constants.sfc_evf]);
									}
									//  MULTIPLE REFLECTION
									Lup = 0.;
									Lup_refl = 0.;
									Lup_refl_old = 0.;
									refldiff = 1.1;
									Lup_refl = 0.;
									Lemit5 = 0.;
									int k = 0;
									//  MAIN reflection loop: does at least 1 longwave reflection, and goes until change in
									// overall (1-emis) is less than dalb multiplied by a factor that recognizes that there is
									// little or no multiple reflection at roof level and above (lambdapR is lambdap at roof level)
									while ((k < 2) || (refldiff >= dalb * (1. - lambdapR)))
									{
										k = k + 1;
										if (k > Constants.MAX_REFLECTION_ITERATIONS)
										{
											// exit ;
											break;
										}

										// save reflected values from last reflection
										for (int iabCount = 0; iabCount < numsfc2; iabCount++)
										{
											int iIndex5 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
											reflpl[iabCount] = refll[iabCount];
											refll[iabCount] = 0.;
											if (k == 1)
											{
												absbl[iabCount] = sfc[iIndex5][Constants.sfc_emiss] * (1. - sfc[iIndex5][Constants.sfc_evf]) * Ldn;
												if (absbl[iabCount] > 2000.)
												{
													System.out.println("1,iab,absbl[iab]" + " " + iabCount + " " + absbl[iabCount]);
												}
												refll[iabCount] = (1. - sfc[iIndex5][Constants.sfc_emiss]) * (1. - sfc[iIndex5][Constants.sfc_evf]) * Ldn;
												Lup_refl = Lup_refl - sfc[iIndex5][Constants.sfc_emiss] * (1. - sfc[iIndex5][Constants.sfc_evf]) * sigma * Math.pow(Tsfc[iabCount], 4);
												Lemit5 = Lemit5 + sfc[iIndex5][Constants.sfc_emiss] * sfc[iIndex5][Constants.sfc_evf] * sigma * Math.pow(Tsfc[iabCount], 4);
												refltl[iabCount] = 0.;
											}
										}
										// open view factor files - PARALLELIZED for performance (longwave only)
										final DoubleAdder Lup_accLW = new DoubleAdder();
										final DoubleAdder Lup_refl_accLW = new DoubleAdder();
										Lup_accLW.add(Lup);
										Lup_refl_accLW.add(Lup_refl);

										// Capture final references for lambda
										final double[][] sfc_finalLW = sfc;
										final double[][] sfc_ab_finalLW = sfc_ab;
										final double[] vf3_finalLW = vf3;
										final int[] vf3j_finalLW = vf3j;
										final int[] vfppos_finalLW = vfppos;
										final double[] reflpl_finalLW = reflpl;
										final double[] absbl_finalLW = absbl;
										final double[] refll_finalLW = refll;
										final int numsfc2_finalLW = numsfc2;

										if (PARALLEL_ENABLED && parallelPool != null && numsfc2 > 100) {
											try {
												parallelPool.submit(() ->
													IntStream.range(0, numsfc2_finalLW).parallel().forEach(patchIdx -> {
														int iIdx5 = (int) sfc_ab_finalLW[patchIdx][Constants.sfc_ab_i];
														for (int pCnt = vfppos_finalLW[patchIdx]; pCnt < vfppos_finalLW[patchIdx + 1]; pCnt++) {
															double vfVal = vf3_finalLW[pCnt];
															int jabVal = vf3j_finalLW[pCnt];
															absbl_finalLW[patchIdx] += vfVal * reflpl_finalLW[jabVal] * sfc_finalLW[iIdx5][Constants.sfc_emiss];
															refll_finalLW[patchIdx] += vfVal * reflpl_finalLW[jabVal] * (1. - sfc_finalLW[iIdx5][Constants.sfc_emiss]);
														}

														if (sfc_finalLW[iIdx5][Constants.sfc_in_array] > 1.5) {
															Lup_accLW.add((1. - sfc_finalLW[iIdx5][Constants.sfc_evf]) * reflpl_finalLW[patchIdx]);
															Lup_refl_accLW.add((1. - sfc_finalLW[iIdx5][Constants.sfc_evf]) * reflpl_finalLW[patchIdx]);
														}
													})
												).get();
											} catch (Exception e) {
												// Fall back to sequential on error
												System.err.println("Parallel LW view factor calc failed: " + e.getMessage());
												for (int iabCount = 0; iabCount < numsfc2; iabCount++) {
													int iIndex5 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
													for (int pCount = vfppos[iabCount]; pCount < vfppos[iabCount + 1]; pCount++) {
														vf = vf3[pCount];
														jab = vf3j[pCount];
														absbl[iabCount] += vf * reflpl[jab] * sfc[iIndex5][Constants.sfc_emiss];
														refll[iabCount] += vf * reflpl[jab] * (1. - sfc[iIndex5][Constants.sfc_emiss]);
													}
													if (sfc[iIndex5][Constants.sfc_in_array] > 1.5) {
														Lup_accLW.add((1. - sfc[iIndex5][Constants.sfc_evf]) * reflpl[iabCount]);
														Lup_refl_accLW.add((1. - sfc[iIndex5][Constants.sfc_evf]) * reflpl[iabCount]);
													}
												}
											}
										} else {
											// Sequential fallback
											for (int iabCount = 0; iabCount < numsfc2; iabCount++) {
												int iIndex5 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
												for (int pCount = vfppos[iabCount]; pCount < vfppos[iabCount + 1]; pCount++) {
													vf = vf3[pCount];
													jab = vf3j[pCount];
													absbl[iabCount] += vf * reflpl[jab] * sfc[iIndex5][Constants.sfc_emiss];
													refll[iabCount] += vf * reflpl[jab] * (1. - sfc[iIndex5][Constants.sfc_emiss]);
												}
												if (sfc[iIndex5][Constants.sfc_in_array] > 1.5) {
													Lup_accLW.add((1. - sfc[iIndex5][Constants.sfc_evf]) * reflpl[iabCount]);
													Lup_refl_accLW.add((1. - sfc[iIndex5][Constants.sfc_evf]) * reflpl[iabCount]);
												}
											}
										}

										// Transfer accumulated values back
										Lup = Lup_accLW.sum();
										Lup_refl = Lup_refl_accLW.sum();

										for (int iabCount = 0; iabCount < numsfc2; iabCount++)
										{
											refltl[iabCount] = refltl[iabCount] + refll[iabCount];
										}

										refldiff = (Lup_refl - Lup_refl_old) / (1.0*avg_cnt) / (Ldn + Lemit5 / (1.0*avg_cnt));

										Lup_refl_old = Lup_refl;
										// 313 continue
									}
									for (int iabCount = 0; iabCount < numsfc2; iabCount++)
									{
										int iIndex5 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
										refltl[iabCount] = refltl[iabCount] - sfc[iIndex5][Constants.sfc_evf] * refll[iabCount];
										absbl[iabCount] = absbl[iabCount] + sfc[iIndex5][Constants.sfc_evf] * refll[iabCount];
									}
									// ------------------------------------
								}
								else
								{
									// SOLAR and LONGWAVE (solar has NOT already been done in previous Tsfc-Lup iteration)
									// RADIATION INITIALIZATION

									// the unit vector pointing from the surface towards the sun
									angdif = az - stror;
									if (angdif < 0.)
									{
										angdif = az + (360. - stror);
									}
									angsun[TUFreg3D.ONE] = util.sind(angdif) * util.cosd(ralt);
									angsun[TUFreg3D.TWO] = util.cosd(angdif) * util.cosd(ralt);
									angsun[TUFreg3D.THREE] = util.sind(ralt);

									// ! first solar absorption and reflection, and zeroth longwave reflection (i.e. emission)
									solarin = 0.;
									Kdn_grid = 0.;
									nKgrid = 0;
									vfsum2 = 0.;

									for (int iabCount = 0; iabCount < numsfc_ab; iabCount++)
									{
										int iIndex6 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
										refll[iabCount] = sfc[iIndex6][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										absbl[iabCount] = 0.;
										if (first_write)
										{
											vfsum2 = vfsum2 + (1. - sfc[iIndex6][Constants.sfc_evf]);
										}
										if (Ktot > 1.0e-3)
										{
											absbs[iabCount] = (1. - sfc[iIndex6][Constants.sfc_albedo]) * Kdif * (1. - sfc[iIndex6][Constants.sfc_evf]);
//System.out.println("absbs[iabCount]1 " + absbs[iabCount] + " " + timeis);
											refls[iabCount] = sfc[iIndex6][Constants.sfc_albedo] * Kdif * (1. - sfc[iIndex6][Constants.sfc_evf]);

											Kdn_grid = Kdn_grid + Kdif * (1. - sfc[iIndex6][Constants.sfc_evf]);
											nKgrid = nKgrid + 1;
											if (sfc[iIndex6][Constants.sfc_in_array] > 1.5)
											{
												solarin = solarin + Kdif * (1. - sfc[iIndex6][Constants.sfc_evf]);
											}
										}
										// ! if patch is at least partly sunlit:
										if (sfc[iIndex6][Constants.sfc_sunlight_fact] > 0.5)
										{
											angsfc[TUFreg3D.ONE] = sfc[iIndex6][Constants.sfc_x_vector];
											angsfc[TUFreg3D.TWO] = sfc[iIndex6][Constants.sfc_y_vector];
											angsfc[TUFreg3D.THREE] = sfc[iIndex6][Constants.sfc_z_vector];
											//  if we stay with plane parallel surfaces, the following dot product
											//  need only be computed 3-4 times (roof/street plus 2-3 sunlit walls)
											// call dotpro(angsun,angsfc,3,dp,g)
											HashMap<String, Double> returnValues = Dotpro.dotpro(angsun, angsfc, 3);
											dp = returnValues.get("dp");
											g = returnValues.get("g");

											absbs[iabCount] = absbs[iabCount] + (1. - sfc[iIndex6][Constants.sfc_albedo])
													* Kbeam * Math.cos((g)) * sfc[iIndex6][Constants.sfc_sunlight_fact] / 4.;
//System.out.println("absbs[iabCount]2 " + absbs[iabCount]);
											refls[iabCount] = refls[iabCount] + sfc[iIndex6][Constants.sfc_albedo] * Kbeam * Math.cos((g)) * sfc[iIndex6][Constants.sfc_sunlight_fact] / 4.;

											Kdn_grid = Kdn_grid + Kbeam * Math.cos((g)) * sfc[iIndex6][Constants.sfc_sunlight_fact] / 4.;

											if (sfc[iIndex6][Constants.sfc_in_array] > 1.5)
											{
												solarin = solarin + Kbeam * Math.cos((g)) * sfc[iIndex6][Constants.sfc_sunlight_fact] / 4.;
											}
										}
										else
										{
											absbs[iabCount] = 0.;
											refls[iabCount] = 0.;
										}
										reflts[iabCount] = refls[iabCount];
									}
									if (Math.abs(vfsum2 - (1.0*avg_cnt)) / (1.0*avg_cnt) > 0.05 && first_write)
									{
										System.out.println("patch sky view factor sum > 5% inaccurate");
										System.out.println("value = " + " " + vfsum2 + " " + "should be = " + " " + avg_cnt);
										System.exit(1);
									}
									if (first_write)
									{
										svferror = 100. * Math.abs(vfsum2 - (1.0*avg_cnt)) / (1.0*avg_cnt);
										if (svferror > svfe_store)
										{
											svfe_store = svferror;
										}
										System.out.println("ABSOLUTE VALUE OF RELATIVE SKY VIEW FACTOR ERROR ->" + " " + svferror + " " + "%");

										overall.writeOutput(Constants.inputs_store_out,
												"-----lambdap,H/L,latitude,streetdir" + " " + lpin[lpiter] + " "
														+ bh_o_bl[bhiter] + " " + xlat + " " + stror + " " + "-----");
										overall.writeOutput(Constants.inputs_store_out,
												"ABSOLUTE VALUE OF RELATIVE SVF ERROR ->" + " " + svferror + " "
														+ "% (for the central urban unit)");
										System.out.println("------------------------------------------");
									}

									// compare input Kdn (wrong due to raster grid causing too many
									// or too few patches to be sunlit - representing patches by their center)
									// the resolution for only the shading routine could be increased to help deal with this problem
									Kdn_grid = Kdn_grid / ((wavelenx * waveleny));
									if (Kdir + Kdif > 0.0)
									{
										Kdn_diff = Kdn_diff + 100. * Math.abs(Kdn_grid - Kdir - Kdif) / (Kdir + Kdif + 1.e-9);
										nKdndiff = nKdndiff + 1;
									}
									if (Math.abs(Kdn_grid - Kdir - Kdif) > Kdn_ae_store)
									{
										Kdn_ae_store = Math.abs(Kdn_grid - Kdir - Kdif);
										Kdn_re_store = Math.abs(Kdn_grid - Kdir - Kdif) / (Kdir + Kdif + 1.e-9);
									}
			
									// MULTIPLE REFLECTION
									//  do the same number of reflections for  both solar and longwave, doing the long- and short-wave
									// reflections together is for efficiency reasons: view factors then
									// only have to be read in once instead of twice
									Kup = 0.;
									Lup = 0.;
									Kup_refl = 0.;
									Lup_refl = 0.;
									Kup_refl_old = 0.;
									Lup_refl_old = 0.;
									refldiff = 1.1;
									Lup_refl = 0.;
									Lemit5 = 0.;
									int k = 0;

									//  MAIN reflection loop: does at least 2 shortwave and 1 longwave reflection, and goes until change in
									// both overall albedo and overall (1-emis) are less than dalb multiplied by a
									// factor that recognizes that there is little or no multiple reflection at roof level and above (lambdapR is  lambdap at roof level)
									while (k < 2 || refldiff >= dalb * (1. - lambdapR))
									{
										// do 314 while
										// (k<2||refldiff>=dalb*(1.-lambdapR))
										k = k + 1;

										// Limit iterations for performance
										if (k > Constants.MAX_REFLECTION_ITERATIONS) {
											break;
										}

										// save reflected values from last reflection
										for (int iabCount = 0; iabCount < numsfc_ab; iabCount++)
										{
											int iIndex7 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
											reflps[iabCount] = refls[iabCount];
											reflpl[iabCount] = refll[iabCount];
											refls[iabCount] = 0.;
											refll[iabCount] = 0.;
											if (k == 1)
											{
												absbl[iabCount] = sfc[iIndex7][Constants.sfc_emiss] * (1. - sfc[iIndex7][Constants.sfc_evf]) * Ldn;
												refll[iabCount] = (1. - sfc[iIndex7][Constants.sfc_emiss]) * (1. - sfc[iIndex7][Constants.sfc_evf]) * Ldn;
												if (sfc[iIndex7][Constants.sfc_in_array] > 1.5)
												{
													Lup_refl = Lup_refl - sfc[iIndex7][Constants.sfc_emiss] * (1. - sfc[iIndex7][Constants.sfc_evf]) * sigma
															* Math.pow(Tsfc[iabCount], 4);
													Lemit5 = Lemit5 + sfc[iIndex7][Constants.sfc_emiss] * sfc[iIndex7][Constants.sfc_evf]
																	* sigma * Math.pow(Tsfc[iabCount], 4);
												}
												refltl[iabCount] = 0.;
											}
										}

										// open view factor files - PARALLELIZED for performance
										// Use atomic accumulators for thread-safe reduction
										final DoubleAdder Kup_acc = new DoubleAdder();
										final DoubleAdder Lup_acc = new DoubleAdder();
										final DoubleAdder Lup_refl_acc = new DoubleAdder();
										final DoubleAdder Kup_refl_acc = new DoubleAdder();
										Kup_acc.add(Kup);
										Lup_acc.add(Lup);
										Lup_refl_acc.add(Lup_refl);
										Kup_refl_acc.add(Kup_refl);

										// Capture final references for lambda
										final double[][] sfc_final = sfc;
										final double[][] sfc_ab_final = sfc_ab;
										final double[] vf3_final = vf3;
										final int[] vf3j_final = vf3j;
										final int[] vfppos_final = vfppos;
										final double[] reflps_final = reflps;
										final double[] reflpl_final = reflpl;
										final double[] absbs_final = absbs;
										final double[] refls_final = refls;
										final double[] absbl_final = absbl;
										final double[] refll_final = refll;
										final int numsfc2_final = numsfc2;

										if (PARALLEL_ENABLED && parallelPool != null && numsfc2 > 100) {
											try {
												parallelPool.submit(() ->
													IntStream.range(0, numsfc2_final).parallel().forEach(patchIdx -> {
														int iIdx8 = (int) sfc_ab_final[patchIdx][Constants.sfc_ab_i];
														for (int pCnt = vfppos_final[patchIdx]; pCnt < vfppos_final[patchIdx + 1]; pCnt++) {
															double vfVal = vf3_final[pCnt];
															int jabVal = vf3j_final[pCnt];
															absbs_final[patchIdx] += vfVal * reflps_final[jabVal] * (1. - sfc_final[iIdx8][Constants.sfc_albedo]);
															refls_final[patchIdx] += vfVal * reflps_final[jabVal] * sfc_final[iIdx8][Constants.sfc_albedo];
															absbl_final[patchIdx] += vfVal * reflpl_final[jabVal] * sfc_final[iIdx8][Constants.sfc_emiss];
															refll_final[patchIdx] += vfVal * reflpl_final[jabVal] * (1. - sfc_final[iIdx8][Constants.sfc_emiss]);
														}

														if (sfc_final[iIdx8][Constants.sfc_in_array] > 1.5) {
															Kup_acc.add((1. - sfc_final[iIdx8][Constants.sfc_evf]) * reflps_final[patchIdx]);
															Lup_acc.add((1. - sfc_final[iIdx8][Constants.sfc_evf]) * reflpl_final[patchIdx]);
															Lup_refl_acc.add((1. - sfc_final[iIdx8][Constants.sfc_evf]) * reflpl_final[patchIdx]);
															Kup_refl_acc.add((1. - sfc_final[iIdx8][Constants.sfc_evf]) * reflps_final[patchIdx]);
														}
													})
												).get();
											} catch (Exception e) {
												// Fall back to sequential on error
												System.err.println("Parallel view factor calc failed: " + e.getMessage());
												for (int iabCount = 0; iabCount < numsfc2; iabCount++) {
													int iIndex8 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
													for (int pCount = vfppos[iabCount]; pCount < vfppos[iabCount + 1]; pCount++) {
														vf = vf3[pCount];
														jab = vf3j[pCount];
														absbs[iabCount] += vf * reflps[jab] * (1. - sfc[iIndex8][Constants.sfc_albedo]);
														refls[iabCount] += vf * reflps[jab] * sfc[iIndex8][Constants.sfc_albedo];
														absbl[iabCount] += vf * reflpl[jab] * sfc[iIndex8][Constants.sfc_emiss];
														refll[iabCount] += vf * reflpl[jab] * (1. - sfc[iIndex8][Constants.sfc_emiss]);
													}
													if (sfc[iIndex8][Constants.sfc_in_array] > 1.5) {
														Kup_acc.add((1. - sfc[iIndex8][Constants.sfc_evf]) * reflps[iabCount]);
														Lup_acc.add((1. - sfc[iIndex8][Constants.sfc_evf]) * reflpl[iabCount]);
														Lup_refl_acc.add((1. - sfc[iIndex8][Constants.sfc_evf]) * reflpl[iabCount]);
														Kup_refl_acc.add((1. - sfc[iIndex8][Constants.sfc_evf]) * reflps[iabCount]);
													}
												}
											}
										} else {
											// Sequential fallback
											for (int iabCount = 0; iabCount < numsfc2; iabCount++) {
												int iIndex8 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
												for (int pCount = vfppos[iabCount]; pCount < vfppos[iabCount + 1]; pCount++) {
													vf = vf3[pCount];
													jab = vf3j[pCount];
													absbs[iabCount] += vf * reflps[jab] * (1. - sfc[iIndex8][Constants.sfc_albedo]);
													refls[iabCount] += vf * reflps[jab] * sfc[iIndex8][Constants.sfc_albedo];
													absbl[iabCount] += vf * reflpl[jab] * sfc[iIndex8][Constants.sfc_emiss];
													refll[iabCount] += vf * reflpl[jab] * (1. - sfc[iIndex8][Constants.sfc_emiss]);
												}
												if (sfc[iIndex8][Constants.sfc_in_array] > 1.5) {
													Kup_acc.add((1. - sfc[iIndex8][Constants.sfc_evf]) * reflps[iabCount]);
													Lup_acc.add((1. - sfc[iIndex8][Constants.sfc_evf]) * reflpl[iabCount]);
													Lup_refl_acc.add((1. - sfc[iIndex8][Constants.sfc_evf]) * reflpl[iabCount]);
													Kup_refl_acc.add((1. - sfc[iIndex8][Constants.sfc_evf]) * reflps[iabCount]);
												}
											}
										}

										// Transfer accumulated values back
										Kup = Kup_acc.sum();
										Lup = Lup_acc.sum();
										Lup_refl = Lup_refl_acc.sum();
										Kup_refl = Kup_refl_acc.sum();

										for (int iabCount = 0; iabCount < numsfc2; iabCount++)
										{
											reflts[iabCount] = reflts[iabCount] + refls[iabCount];
											refltl[iabCount] = refltl[iabCount] + refll[iabCount];
										}

										// parameter that determines whether or not to do another reflection
										refldiff = Math.max( (Lup_refl - Lup_refl_old) / (1.0*avg_cnt) / (Ldn + Lemit5 / (1.0*avg_cnt)),
												(Kup_refl - Kup_refl_old) / (1.0*avg_cnt) / Math.max(1.e-9, (Kdir + Kdif)));

										Lup_refl_old = Lup_refl;
										Kup_refl_old = Kup_refl;

										// 314 continue
									}
									// !print *,'after 314'
									solar_refl_done = true;

								}

								alb_sfc = Math.min(albr * lpactual + albs * (1. - lpactual), Kup / (1.0*avg_cnt) / Math.max(1.e-9, (Kdir + Kdif)));
//System.out.println("alb_sfc " + alb_sfc);

								// remaining reflected radiation is partitioned by assuming that sfcs with
								// larger environmental view factors will absorb an amount of this radiation
								// proportional to their total view of other surfaces (approx.), and the
								// remainder will leave the system (to the sky)
								for (int iabCount = 0; iabCount < numsfc2; iabCount++)
								{
									int iIndex9 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
									tots[iabCount] = reflts[iabCount] + absbs[iabCount];
									totl[iabCount] = refltl[iabCount] + absbl[iabCount];
									reflts[iabCount] = reflts[iabCount] - sfc[iIndex9][Constants.sfc_evf] * refls[iabCount];
									absbs[iabCount] = absbs[iabCount] + sfc[iIndex9][Constants.sfc_evf] * refls[iabCount];
//if (iabCount==0) System.out.println("absbs[iabCount]4 " + absbs[iabCount]);
									refltl[iabCount] = refltl[iabCount] - sfc[iIndex9][Constants.sfc_evf] * refll[iabCount];
									absbl[iabCount] = absbl[iabCount] + sfc[iIndex9][Constants.sfc_evf] * refll[iabCount];
									Kup = Kup + (1. - sfc[iIndex9][Constants.sfc_evf]) * refls[iabCount];
									Lup = Lup + (1. - sfc[iIndex9][Constants.sfc_evf]) * refll[iabCount];
									
									
								}

								//
								// -------------------------------------------------------------
								// CONVECTION and Tsfc
//System.out.println("++++++++++++++++++++++++start convection=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000./60.);	
								rhoa = press * 100. / 287.04 / Ta;
								rhocan = press * 100. / 287.04 / Tcan;
								// this is the average heat capacity of air per m2 below zH
								// for 3-D geometries: (canyair is the height of the air column below
								// zH if all the buildings were put into one massive building of height
								// much lower than zH, of course - covering the entire area of interest)
								Cairavg = canyair * rhocan * cpair;

								// momentum transfer, log wind profile
								Tzd = lambdapR * Tsfc_R / (1.0*numroof2) + (1. - lambdapR) * Tcan;
								Ri = util.SFC_RI(zref - zd, Ta, Tzd, Ua);
								HashMap<String, Double> cdReturn = VTUF3DUtil.CD(Ri, zref - zd, z0, z0 / moh);
								Fm = cdReturn.get("Fm");
								cdtown = cdReturn.get("cd_out");
								ustar = Math.sqrt(cdtown) * Ua;
								Qhcan_kin = Math.max(0., Qhcan / rhocan / cpair);
								wstar = Math.pow((9.806 / Tcan * Qhcan_kin * zH), (1. / 3.));

								// BISECTION METHOD FOR U PROFILE!!!
								bp = ustar / vK / Math.sqrt(Fm);
								bm = zH - zd;
								// The following is what Masson uses (but his model is an area average), so
								// I've replaced it with an equivalent 3-D expression
								bn = -2. * lambdaf / (1. - lambdapR) / 4.;
								bq = z0;

								if (ustar / vK * Math.log((zH - zd) / z0) / Math.sqrt(Fm) > Ua
										|| ustar / vK * Math.log((zH - zd) / z0) / Math.sqrt(Fm)
												* Math.exp(-2. * lambdaf / (1. - lambdapR) / 4.) > ustar / vK
														* Math.log((zH - zd) / z0) / Math.sqrt(Fm))
								{
									System.out.println("Utop larger than Ua, or Ucan larger than Utop");
									// write(6,*)"Utop larger than Ua, or Ucan
									// larger than Utop";
									// stop;
									System.exit(1);
								}

								CL = 0.01;
								CR = CL + 0.1;
								FR = bp * Math.exp(-CR * zH) / bm / CR - bp * Math.log(bm / bq) * (1. - Math.exp(bn))
										/ (Math.exp(CR * zH) - Math.exp(CR * zH / 2.));
								while (FR >= 1.e-20)
								{
									CR = CR + 0.1;
									FR = bp * Math.exp(-CR * zH) / bm / CR - bp * Math.log(bm / bq)
											* (1. - Math.exp(bn)) / (Math.exp(CR * zH) - Math.exp(CR * zH / 2.));
									// 957 continue
								}

								while (CR - CL > 0.001)
								{
									Cmid = (CR + CL) / 2.;
									Fmid = bp * Math.exp(-Cmid * zH) / bm / Cmid - bp * Math.log(bm / bq) * (1. - Math.exp(bn)) / (Math.exp(Cmid * zH) - Math.exp(Cmid * zH / 2.));
									if (Fmid > 0.)
									{
										CL = Cmid;
									}
									else if (Fmid < 0.)
									{
										CR = Cmid;
									}
									else if (Fmid == 0.)
									{
										Ccan = Cmid;
										// goto 959;
										break;
									}
									else
									{
										System.out.println("problem in bisection method");
										System.exit(1);
									}
									// 958 continue
								}
								Ccan = (CR + CL) / 2.;
								// 959 continue
						
								// constants for the canyon wind profile (Ccan also)
								Bcan = bp * Math.exp(-Ccan * zH) / bm / Ccan;
								Acan = -Bcan * Math.exp(Ccan * zH) + bp * Math.log(bm / bq);

								for (int iii = 0; iii < (int) Math.round(zH - 0.5); iii++)
								{
									zzz = 1.0*iii;
									Ucantst = Acan + Bcan * Math.exp(Ccan * zzz);
									if (Ucantst > Ua || Ucantst < 0.)
									{
										System.out.println("bad Ucan at z=" + " " + zzz + " " + Ucan);
										System.exit(1);
									}
								}

								for (int iii = 0; iii < (int) Math.round(zref - 0.5); iii++)
								{
									zzz = 1.0*iii;
									if (zzz < zH)
									{
										Uwrite[iii] = Acan + Bcan * Math.exp(Ccan * zzz);
										Twrite[iii] = Tcan;
									}
									else
									{
										Uwrite[iii] = ustar / vK * Math.log((zzz - zd) / z0) / Math.sqrt(Fm);
										Twrite[iii] = Tcan - Tlog_fact / Uwrite[iii] * Math.pow((Math.log((zzz - zH + z0) / z0)), 2);
									}
								}

								Ucanpy = Acan + Bcan * Math.exp(Ccan * zH / 2.);

								// Loop throught the patches in the central urban unit and calculate
								// net radiation, convection at each patch, and solve the energy balance
								Tp = 0.; Trad_R = 0.; Trad_T = 0.; Trad_N = 0.; Trad_S = 0.; Trad_E = 0.; Trad_W = 0.;
								httcT = 0.; httcW = 0.; httcR = 0.; Absbs_W = 0.; Absbl_W = 0.; Emit_W = 0.; Qg_T = 0.; Rnet_T = 0.; Qh_T = 0.;
								Qg_N = 0.; Rnet_N = 0.; Qh_N = 0.; Qg_S = 0.; Rnet_S = 0.; Qh_S = 0.;
								Qg_E = 0.; Rnet_E = 0.; Qh_E = 0.; Qg_W = 0.; Rnet_W = 0.; Qh_W = 0.; Qg_R = 0.;
								Rnet_R = 0.; Qh_R = 0.; Qg_tot = 0.; Rnet_tot = 0.; Qh_tot = 0.; Qhcantmp = 0.; Qh_abovezH = 0.; Qe_tot = 0.; Qecantmp = 0.;
								Qanthro = 0.;Qac = 0.;Qdeep = 0.;Tsfc_cplt = 0.;Tsfc_bird = 0.;Tsfc_R = 0.;Tsfc_N = 0.;Tsfc_S = 0.;Tsfc_E = 0.;Tsfc_W = 0.;
								Tsfc_T = 0.;TTsun = 0.;TTsh = 0.;TNsun = 0.;TNsh = 0.;TSsun = 0.;TSsh = 0.;TEsun = 0.;TEsh = 0.;TWsun = 0.;TWsh = 0.;
								numTsun = 0;numTsh = 0;numNsun = 0;numNsh = 0;numSsun = 0;numSsh = 0;numEsun = 0;numEsh = 0;numWsun = 0;numWsh = 0;Kdn_R = 0.;
								Kup_R = 0.;Ldn_R = 0.;Lup_R = 0.;Kdn_T = 0.;Kup_T = 0.;Ldn_T = 0.;Lup_T = 0.;Kdn_N = 0.;Kup_N = 0.;Ldn_N = 0.;Lup_N = 0.;
								Kdn_S = 0.;Kup_S = 0.;Ldn_S = 0.;Lup_S = 0.;Kdn_E = 0.;Kup_E = 0.;Ldn_E = 0.;Lup_E = 0.;Kdn_W = 0.;Kup_W = 0.;Ldn_W = 0.;Lup_W = 0.;

								iij = 1;

								for (int iabCount = 0; iabCount < numsfc2; iabCount++)
								{				
									int iIndex10 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
									int y = (int) sfc_ab[iabCount][Constants.sfc_ab_y];
									int x = (int) sfc_ab[iabCount][Constants.sfc_ab_x];
								
									if (sfc[iIndex10][Constants.sfc_surface_type] > 2.5)
									{
										// ! WALLS - convection coefficients
										zwall = (sfc[iIndex10][Constants.sfc_z] - 0.5) * patchlen;
										if (zwall >= zH)
										{
											Ucan = ustar / vK * Math.log((zwall - zd) / z0) / Math.sqrt(Fm);
											Ueff = Math.sqrt(Math.pow(Ucan, 2) + Math.pow(wstar, 2));
											// ! for Tconv in Newton's method for Tsfc below:
											Thorz = Tcan - Tlog_fact / Ucan * Math.pow((Math.log((zwall - zH + z0) / z0)), 2);
										}
										else
										{
											Ucan = Acan + Bcan * Math.exp(Ccan * zwall);
											Ueff = Math.sqrt(Math.pow(Ucan, 2) + Math.pow(wstar, 2));
										}
										httc = rw * (11.8 + 4.2 * Ueff) - 4.;
									}
									else
									{										
										// ! STREETS & ROOFS - convection coefficients
										// ! use the windspeed 0.5*patchlen above the surface (changed to Harman:
										// ! 0.1*average roof length)

										// ! streets:
										zhorz = 0.1 * zH;

										// ! roofs:
										if (sfc[iIndex10][Constants.sfc_surface_type] < 1.5)
										{
											zhorz = Math.min(zref, (sfc[iIndex10][Constants.sfc_z] - 0.5 + 0.1 * Lroof) * patchlen);											
											if (zrooffrc > 0.)
											{
												zhorz = Math.min(zref, (sfc[iIndex10][Constants.sfc_z] - 0.5) * patchlen + zrooffrc);
											}
										}

										// ! assume wstar is not relevant for roofs above zH
										if (zhorz > zH)
										{
											Uhorz = ustar / vK * Math.log((zhorz - zd) / z0) / Math.sqrt(Fm);
											Thorz = Tcan - Tlog_fact / Uhorz * Math.pow((Math.log((zhorz - zH + z0) / z0)), 2);
											rhohorz = press * 100. / 287.04 / Thorz;

											if (Math.max(Math.abs(Thorz - Tcan), Math.abs(Thorz - Ta)) > Math.abs(Tcan - Ta) + 0.01)
											{
												System.out.println("Thorz outside of Ta,Tcan range, Thorz,i=" + " " + Thorz + " " + iIndex10);
												System.exit(1);
											}
										}
										else
										{
											// ! effective canyon wind is only for HTC calc, not Ri calc too!
											Uhorz = Acan + Bcan * Math.exp(Ccan * zhorz);
											Thorz = Tcan;
											rhohorz = rhocan;
										}
									if (sfc[iIndex10][Constants.sfc_surface_type] < 1.5)
									{

										// ! roofs:
										// ! Harman et al. 2004 approach: 0.1*average roof length
										Ri = util.SFC_RI(zhorz - (sfc[iIndex10][Constants.sfc_z] - 0.5) * patchlen, Thorz, Tsfc[iabCount], Uhorz);
										if ((sfc[iIndex10][Constants.sfc_z] - 0.5) * patchlen < zH - 0.01)
										{
											HashMap<String, Double> htcReturn2 = util.HTC(Ri,
													Math.sqrt(Math.pow(Uhorz, 2) + Math.pow(wstar, 2)),
													zhorz - (sfc[iIndex10][Constants.sfc_z] - 0.5) * patchlen, z0roofm, z0roofh);
											httc = htcReturn2.get(VTUF3DUtil.HTTC_OUT_INDEX);
											Fh = htcReturn2.get(VTUF3DUtil.FH_INDEX);
											aaaa = 1.;
										}
										else
										{
											HashMap<String, Double> htcReturn3 = 
													util.HTC(Ri, Uhorz, zhorz - (sfc[iIndex10][Constants.sfc_z] - 0.5) * patchlen, 
															z0roofm, z0roofh);
											httc = htcReturn3.get(VTUF3DUtil.HTTC_OUT_INDEX);
											Fh = htcReturn3.get(VTUF3DUtil.FH_INDEX);
											aaaa = 2.;
										}
									}
									else
									{
										// streets: Harman et al. 2004 approach:  0.1*average building height
										Ri = util.SFC_RI(0.1 * zH, Thorz, Tsfc[iabCount], Uhorz);
										HashMap<String, Double> htcReturn4 = util.HTC(Ri,
												Math.sqrt(Math.pow(Uhorz, 2) + Math.pow(wstar, 2)), 0.1 * zH, z0roadm,
												z0roadh);
										httc = htcReturn4.get(VTUF3DUtil.HTTC_OUT_INDEX);
										Fh = htcReturn4.get(VTUF3DUtil.FH_INDEX);
									}
									httc = httc * cpair * rhohorz;
								}

								// This is actually Kdown-Kup+eps*Ldown (the Lup term is calculated in the iteration below)
								Rnet = absbl[iabCount] + absbs[iabCount];
//if (iabCount ==0) System.out.println("Rnet1 " + Rnet + " " + absbl[iabCount] +" " + absbs[iabCount]);

								Tconv = Tcan;
								//adding to initialize i (replaced all the i with iIndex

								if ((sfc[iIndex10][Constants.sfc_z] - 0.5) * patchlen + 0.001 >= zH)
								{
									Tconv = Thorz;
								}

								if (Math.abs(Tsfc[iabCount] - Tconv) > 60.)
								{
//									System.out.println("iab,Tsfc[iab],Tconv" + " " + iabCount + " " + Tsfc[iabCount] + " " + Tconv);
									// write(6,*)"iab,Tsfc[iab],Tconv",iab,Tsfc[iab],Tconv;
									// stop
//									System.exit(1);
								}
								if (Rnet>3000.0 || Rnet<-500.0) 
								{
									System.out.println("Rnet is too big, Rnet = " + Rnet);
//									System.out.println("Problem is at patch x,y,z,f =" + sfc[iIndex10][Constants.sfc_evf]
//											+ " " + sfc[iIndex10][Constants.sfc_emiss]
//											+ " " + sfc[iIndex10][Constants.sfc_albedo]
//											+ " " + sfc[iIndex10][Constants.sfc_sunlight_fact]);
									
//									System.out.println(iabCount + " " + sfc[iIndex10][Constants.sfc_albedo] + " " + Kbeam + " " + " " + sfc[iIndex10][Constants.sfc_sunlight_fact] );									

//									absbs[iabCount] = absbs[iabCount] + (1. - sfc[iIndex6][Constants.sfc_albedo])
//											* Kbeam * Math.cos((g)) * sfc[iIndex6][Constants.sfc_sunlight_fact] / 4.;
									
									
//									System.out.println(absbl[iabCount] +" "+ absbs[iabCount]);
//									System.exit(1);
								}
								// KN, changing this to let Rnet be a little
								// bigger
								// ! write(6,*)'Rnet is too big, Rnet = ',Rnet
								// ! write(6,*)'Problem is at patch x,y,z,f =
								// ',sfc[i][Constants.sfc_evf],sfc[i][Constants.sfc_emiss],sfc[i][Constants.sfc_albedo],sfc[i][Constants.sfc_sunlight_fact]
								// !endif
								// !if (Rnet>2000.0.or.Rnet<-1000.0) then
								// ! write(6,*)'Rnet is too big, Rnet = ',Rnet
								// ! write(6,*)'Problem is at patch x,y,z,f =
								// ',sfc[i][Constants.sfc_evf],sfc[i][Constants.sfc_emiss],sfc[i][Constants.sfc_albedo],sfc[i][Constants.sfc_sunlight_fact]
								// !endif

								// ! stop

								// Store values for parallel Newton's method (Phase 2) and accumulations (Phase 3)
								httc_patch[iabCount] = httc;
								Rnet_patch[iabCount] = Rnet;
								Tconv_patch[iabCount] = Tconv;
							} // End Phase 1 loop

							// ====== PHASE 2: Parallel Newton's method temperature solver ======
							final int numsfc2Final = numsfc2;
							final double sigmaFinal = sigma;
							final int sixPlusThreeTimesNumlayersFinal = sixPlusThreeTimesNumlayers;
							// Final references for lambda access
							final double[][] sfc_ab_final = sfc_ab;
							final double[][] sfc_final = sfc;
							final double[] Tsfc_final = Tsfc;
							final double[] httc_patch_final = httc_patch;
							final double[] lambda_sfc_final = lambda_sfc;
							final double[] Rnet_patch_final = Rnet_patch;
							final double[] Tconv_patch_final = Tconv_patch;

							if (PARALLEL_ENABLED && parallelPool != null)
							{
								// Parallel execution of Newton's method
								final double[] TdiffArray = new double[numsfc2];
								try {
									parallelPool.submit(() -> {
										IntStream.range(0, numsfc2Final).parallel().forEach(idx -> {
											int iIdx = (int) sfc_ab_final[idx][Constants.sfc_ab_i];
											double patchEmissivity = sfc_final[iIdx][Constants.sfc_emiss];
											double patchThick = sfc_ab_final[idx][sixPlusThreeTimesNumlayersFinal];
											double patchTemp = sfc_ab_final[idx][Constants.sfc_ab_layer_temp];

											double newTemp = solveNewtonTsfc(
												Tsfc_final[idx],
												patchEmissivity,
												sigmaFinal,
												httc_patch_final[idx],
												lambda_sfc_final[idx],
												patchThick,
												Rnet_patch_final[idx],
												Tconv_patch_final[idx],
												patchTemp
											);

											TdiffArray[idx] = Math.abs(newTemp - Tsfc_final[idx]);
											Tsfc_final[idx] = newTemp;
										});
									}).get();
								} catch (Exception e) {
									System.err.println("Parallel Newton solver error: " + e.getMessage());
									e.printStackTrace();
								}
								// Find max Tdiff
								for (int idx = 0; idx < numsfc2; idx++) {
									if (TdiffArray[idx] > Tdiffmax) {
										Tdiffmax = TdiffArray[idx];
									}
								}
							}
							else
							{
								// Sequential fallback
								for (int iabCount = 0; iabCount < numsfc2; iabCount++)
								{
									int iIndex10 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
									double patchEmiss = sfc[iIndex10][Constants.sfc_emiss];
									double patchThickness = sfc_ab[iabCount][sixPlusThreeTimesNumlayers];
									double patchLayerTemp = sfc_ab[iabCount][Constants.sfc_ab_layer_temp];

									Tnew = solveNewtonTsfc(
										Tsfc[iabCount],
										patchEmiss,
										sigma,
										httc_patch[iabCount],
										lambda_sfc[iabCount],
										patchThickness,
										Rnet_patch[iabCount],
										Tconv_patch[iabCount],
										patchLayerTemp
									);

									if (Math.abs(Tnew - Tsfc[iabCount]) > Tdiffmax)
									{
										Tdiffmax = Math.abs(Tnew - Tsfc[iabCount]);
									}
									Tsfc[iabCount] = Tnew;
								}
							}

							// ====== PHASE 3: Calculate Trad and accumulations (sequential) ======
							for (int iabCount = 0; iabCount < numsfc2; iabCount++)
							{
								int iIndex10 = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
								int y = (int) sfc_ab[iabCount][Constants.sfc_ab_y];
								int x = (int) sfc_ab[iabCount][Constants.sfc_ab_x];

								// Restore httc, Rnet, Tconv from stored values for accumulations
								httc = httc_patch[iabCount];
								Rnet = Rnet_patch[iabCount];
								Tconv = Tconv_patch[iabCount];

								Trad[iabCount] = Math.pow(((1. / sigma)
										* (sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4) + refltl[iabCount])),
										(0.25));

								// ! STORE OUTPUT: (only the chosen subdomain)
								if (sfc[iIndex10][Constants.sfc_in_array] > 1.5)
								{
									// overall energy balance (per unit plan area):
									// !print *,sfc_ab_map_x[iab],sfc_ab_map_y[iab],sfc_ab_map_z[iab],sfc_ab_map_f[iab],timeis,yd_actual
									leFromEt5 = 0;

									diffShadingValueUsed = Constants.DIFFERENTIALSHADING100PERCENT;
									if (Ktot > 1.0E-3)
									{									
										diffShadingCalculatedValue = treeXYMapSunlightPercentageTotal[sfc_ab_map_x[iabCount]-1][sfc_ab_map_y[iabCount]-1];
										if (diffShadingCalculatedValue >= .50)
										{
											diffShadingValueUsed = Constants.DIFFERENTIALSHADING100PERCENT;
											outputDebugStr = "100%";
										}
										if (diffShadingCalculatedValue < .50)
										{
											diffShadingValueUsed = DIFFERENTIALSHADINGDIFFUSE;
											outputDebugStr = "0%";
										}
									}

									if (treeMapFromConfig.usingDiffShading == 0)
									{
										diffShadingValueUsed = Constants.DIFFERENTIALSHADING100PERCENT;
										// stop;
										System.exit(1);
									}

									if (treeXYMap[sfc_ab_map_x[iabCount]-1][sfc_ab_map_y[iabCount]-1] != 0)
									{
										// ! print
										// *,'----------------------------------------'
										tempTimeis = (int) (timeis * 2);
										if (tempTimeis < 1)
										{
											tempTimeis = 1;
										}

										//TODO figure out how to replace this with online Maespa
										String key = treeXYMap[sfc_ab_map_x[iabCount]-1][sfc_ab_map_y[iabCount]-1] + "_" + diffShadingValueUsed;
										Tsfc[iabCount] = maespaDataArray.get(key).get(tempTimeis-1).getTCAN() + 273.15;
										if (Double.isNaN(Tsfc[iabCount]))
										{
											System.out.println();
										}
										// only use LE from the trunk grid square
										if (treeXYTreeMap[sfc_ab_map_x[iabCount]-1][sfc_ab_map_y[iabCount]-1] > 0) 
										{
											//TODO figure out how to replace this with online Maespa
											leFromEt5 = maespaDataArray.get(key).get(tempTimeis-1).getQeCalc5() ;
										}
									}

									if (treeXYTreeMap[sfc_ab_map_x[iabCount]-1][sfc_ab_map_y[iabCount]-1] > 0)
									{
										// this rnet value would have been calculated using the vegetation alb/emis
										currentRnet[iabCount] = Rnet - sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);

										currentQh[iabCount] = (httc * (Tsfc[iabCount] - Tconv));
										currentQe[iabCount] = leFromEt5;
										currentQg[iabCount] = (Rnet - sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4))
												- (httc * (Tsfc[iabCount] - Tconv)) - leFromEt5;

										Rnet_tot = Rnet_tot + currentRnet[iabCount];
										Qh_tot = Qh_tot + currentQh[iabCount];
										Qe_tot = Qe_tot + currentQe[iabCount];
										// !! calculate Qg as a residual from
										// rnet
										Qg_tot = Qg_tot + currentQg[iabCount];
									}
				
									if (treeXYTreeMap[sfc_ab_map_x[iabCount]-1][sfc_ab_map_y[iabCount]-1] > 0)
									{
//										continue;
										// this isn't a Maespa surface then, so use the normal TUF method
									}
									else
									{
										currentRnet[iabCount] = Rnet - sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);

										currentQh[iabCount] = (httc * (Tsfc[iabCount] - Tconv));
										currentQe[iabCount] = leFromEt5;
										currentQg[iabCount] = (lambda_sfc[iabCount] * (Tsfc[iabCount] - sfc_ab[iabCount][Constants.sfc_ab_layer_temp]) * 2.
												/ sfc_ab[iabCount][sixPlusThreeTimesNumlayers]);
										Rnet_tot = Rnet_tot + currentRnet[iabCount];
										Qh_tot = Qh_tot + currentQh[iabCount];
										Qe_tot = Qe_tot + currentQe[iabCount];
										Qg_tot = Qg_tot + currentQg[iabCount];
									}
									// ! canyon only:
									if ((sfc[iIndex10][Constants.sfc_z] - 0.5) * patchlen < zH - 0.01)
									{
										Qhcantmp = Qhcantmp + httc * (Tsfc[iabCount] - Tconv);
									}
									else
									{
										Qh_abovezH = Qh_abovezH + httc * (Tsfc[iabCount] - Tconv);
									}

									// for evolution of internal building temperature:
									if (sfc[iIndex10][Constants.sfc_surface_type] > 2.5)
									{
										// wall internal T
										Tp = Tp + sfc_ab[iabCount][fivePlusNumlayers];
									}
									else if (sfc[iIndex10][Constants.sfc_surface_type] < 1.5)
									{
										// ! roof internal T; also add internal of floor (user-defined)
										Tp = Tp + sfc_ab[iabCount][fivePlusNumlayers] + Tfloor;
									}

									// ! Surface temperatures and energy balance components.
									//  Averaging patch values to get facet-average values
									//  complete (per unit total area)
									Tsfc_cplt = Tsfc_cplt + Tsfc[iabCount];
									//  bird's eye view sfc T
									if (sfc[iIndex10][Constants.sfc_surface_type] < 2.5)
									{
										Tsfc_bird = Tsfc_bird + Tsfc[iabCount];
									}
									//  roof sfc T and energy balance
									if (sfc[iIndex10][Constants.sfc_surface_type] < 1.5)
									{

										httcR = httcR + httc;
										Tsfc_R = Tsfc_R + Tsfc[iabCount];
										Trad_R = Trad_R + Math.pow(((1. / sigma)
												* (sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4)
														+ refltl[iabCount])),
												(0.25));
										Rnet_R = Rnet_R + Rnet
												- sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Kdn_R = Kdn_R + tots[iabCount];
										Kup_R = Kup_R + reflts[iabCount];
										Ldn_R = Ldn_R + totl[iabCount];
										Lup_R = Lup_R + refltl[iabCount]
												+ sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);

										Qh_R = Qh_R + httc * (Tsfc[iabCount] - Tconv);

										Qg_R = Qg_R + lambda_sfc[iabCount]
												* (Tsfc[iabCount] - sfc_ab[iabCount][Constants.sfc_ab_layer_temp]) * 2.
												/ sfc_ab[iabCount][sixPlusThreeTimesNumlayers];
										Qanthro = Qanthro + Math.max(0., (Tintw - sfc_ab[iabCount][fivePlusNumlayers])
												* lambdaavr[numlayersMinus1] * 2. / thickr[numlayersMinus1]);
										Qac = Qac + Math.max(0., (sfc_ab[iabCount][fivePlusNumlayers] - Tintw)
												* lambdaavr[numlayersMinus1] * 2. / thickr[numlayersMinus1]);
									}

									//  street energy balance (sfc T calc below)
									if (sfc[iIndex10][Constants.sfc_surface_type] > 1.5
											&& sfc[iIndex10][Constants.sfc_surface_type] < 2.5)
									{

										httcT = httcT + httc;
										Trad_T = Trad_T + Math.pow(((1. / sigma)
												* (sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4)
														+ refltl[iabCount])),
												(0.25));
								
										Rnet_T = Rnet_T + Rnet
												- sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Kdn_T = Kdn_T + tots[iabCount];
										Kup_T = Kup_T + reflts[iabCount];
										Ldn_T = Ldn_T + totl[iabCount];
										Lup_T = Lup_T + refltl[iabCount]
												+ sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Qh_T = Qh_T + httc * (Tsfc[iabCount] - Tconv);

										Qg_T = Qg_T + lambda_sfc[iabCount]
												* (Tsfc[iabCount] - sfc_ab[iabCount][Constants.sfc_ab_layer_temp]) * 2.
												/ sfc_ab[iabCount][sixPlusThreeTimesNumlayers];
										Qdeep = Qdeep + (sfc_ab[iabCount][fivePlusNumlayers] - Tints) * lambdaavs[numlayersMinus1] * 2.
												/ thicks[numlayersMinus1];
										if (sfc[iIndex10][Constants.sfc_sunlight_fact] > 3.5)
										{
											TTsun = TTsun + Tsfc[iabCount];
											numTsun = numTsun + 1;
										}
										else if (sfc[iIndex10][Constants.sfc_sunlight_fact] < 0.5)
										{
											TTsh = TTsh + Tsfc[iabCount];
											numTsh = numTsh + 1;
										}
									}
									if (sfc[iIndex10][Constants.sfc_surface_type] > 2.5)
									{
										httcW = httcW + httc;
									}
									//  N wall sfc T and energy balance
									if (sfc[iIndex10][Constants.sfc_y_vector] > 0.5)
									{

										Tsfc_N = Tsfc_N + Tsfc[iabCount];
										Trad_N = Trad_N + Math.pow(((1. / sigma)
												* (sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4)
														+ refltl[iabCount])),
												(0.25));
										Rnet_N = Rnet_N + Rnet
												- sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Kdn_N = Kdn_N + tots[iabCount];
										Kup_N = Kup_N + reflts[iabCount];
										Ldn_N = Ldn_N + totl[iabCount];
										Lup_N = Lup_N + refltl[iabCount]
												+ sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Qh_N = Qh_N + httc * (Tsfc[iabCount] - Tconv);
										Qg_N = Qg_N + lambda_sfc[iabCount]
												* (Tsfc[iabCount] - sfc_ab[iabCount][Constants.sfc_ab_layer_temp]) * 2.
												/ sfc_ab[iabCount][sixPlusThreeTimesNumlayers];
										Qanthro = Qanthro + Math.max(0., (Tintw - sfc_ab[iabCount][fivePlusNumlayers])
												* lambdaavw[numlayersMinus1] * 2. / thickw[numlayersMinus1]);
										Qac = Qac + Math.max(0., (sfc_ab[iabCount][fivePlusNumlayers] - Tintw)
												* lambdaavw[numlayersMinus1] * 2. / thickw[numlayersMinus1]);
										if (sfc[iIndex10][Constants.sfc_sunlight_fact] > 3.5)
										{
											TNsun = TNsun + Tsfc[iabCount];
											numNsun = numNsun + 1;
										}
										else if (sfc[iIndex10][Constants.sfc_sunlight_fact] < 0.5)
										{
											TNsh = TNsh + Tsfc[iabCount];
											numNsh = numNsh + 1;
										}
									}
									//  S wall sfc T and energy balance
									if (sfc[iIndex10][Constants.sfc_y_vector] < -0.5)
									{

										Tsfc_S = Tsfc_S + Tsfc[iabCount];
										Trad_S = Trad_S + Math.pow(((1. / sigma)
												* (sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4)
														+ refltl[iabCount])),
												(0.25));
										Rnet_S = Rnet_S + Rnet
												- sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Kdn_S = Kdn_S + tots[iabCount];
										Kup_S = Kup_S + reflts[iabCount];
										Ldn_S = Ldn_S + totl[iabCount];
										Lup_S = Lup_S + refltl[iabCount]
												+ sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Qh_S = Qh_S + httc * (Tsfc[iabCount] - Tconv);
										Qg_S = Qg_S + lambda_sfc[iabCount]
												* (Tsfc[iabCount] - sfc_ab[iabCount][Constants.sfc_ab_layer_temp]) * 2.
												/ sfc_ab[iabCount][sixPlusThreeTimesNumlayers];
										Qanthro = Qanthro + Math.max(0., (Tintw - sfc_ab[iabCount][fivePlusNumlayers])
												* lambdaavw[numlayersMinus1] * 2. / thickw[numlayersMinus1]);
										Qac = Qac + Math.max(0., (sfc_ab[iabCount][fivePlusNumlayers] - Tintw)
												* lambdaavw[numlayersMinus1] * 2. / thickw[numlayersMinus1]);
										if (sfc[iIndex10][Constants.sfc_sunlight_fact] > 3.5)
										{
											TSsun = TSsun + Tsfc[iabCount];
											numSsun = numSsun + 1;
										}
										else if (sfc[iIndex10][Constants.sfc_sunlight_fact] < 0.5)
										{
											TSsh = TSsh + Tsfc[iabCount];
											numSsh = numSsh + 1;
										}
									}
									//  E wall sfc T and energy balance
									if (sfc[iIndex10][Constants.sfc_x_vector] > 0.5)
									{

										Tsfc_E = Tsfc_E + Tsfc[iabCount];
										Trad_E = Trad_E + Math.pow(((1. / sigma)
												* (sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4)
														+ refltl[iabCount])),
												(0.25));
										Rnet_E = Rnet_E + Rnet
												- sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Kdn_E = Kdn_E + tots[iabCount];
										Kup_E = Kup_E + reflts[iabCount];
										Ldn_E = Ldn_E + totl[iabCount];
										Lup_E = Lup_E + refltl[iabCount]
												+ sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Qh_E = Qh_E + httc * (Tsfc[iabCount] - Tconv);
										Qg_E = Qg_E + lambda_sfc[iabCount]
												* (Tsfc[iabCount] - sfc_ab[iabCount][Constants.sfc_ab_layer_temp]) * 2.
												/ sfc_ab[iabCount][sixPlusThreeTimesNumlayers];

										Qanthro = Qanthro + Math.max(0., (Tintw - sfc_ab[iabCount][fivePlusNumlayers])
												* lambdaavw[numlayersMinus1] * 2. / thickw[numlayersMinus1]);
										Qac = Qac + Math.max(0., (sfc_ab[iabCount][fivePlusNumlayers] - Tintw)
												* lambdaavw[numlayersMinus1] * 2. / thickw[numlayersMinus1]);
										if (sfc[iIndex10][Constants.sfc_sunlight_fact] > 3.5)
										{
											TEsun = TEsun + Tsfc[iabCount];
											numEsun = numEsun + 1;
										}
										else if (sfc[iIndex10][Constants.sfc_sunlight_fact] < 0.5)
										{
											TEsh = TEsh + Tsfc[iabCount];
											numEsh = numEsh + 1;
										}
									}
									//  W wall sfc T and energy balance
									if (sfc[iIndex10][Constants.sfc_x_vector] < -0.5)
									{

										Tsfc_W = Tsfc_W + Tsfc[iabCount];

										Trad_W = Trad_W + Math.pow(((1. / sigma) * (sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4) 
												+ refltl[iabCount])), (0.25));
										Absbs_W = Absbs_W + absbs[iabCount];
										Absbl_W = Absbl_W + absbl[iabCount];
										Emit_W = Emit_W + sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Rnet_W = Rnet_W + Rnet - sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Kdn_W = Kdn_W + tots[iabCount];
										Kup_W = Kup_W + reflts[iabCount];
										Ldn_W = Ldn_W + totl[iabCount];
										Lup_W = Lup_W + refltl[iabCount] + sfc[iIndex10][Constants.sfc_emiss] * sigma * Math.pow(Tsfc[iabCount], 4);
										Qh_W = Qh_W + httc * (Tsfc[iabCount] - Tconv);

										Qg_W = Qg_W + lambda_sfc[iabCount] * (Tsfc[iabCount] - sfc_ab[iabCount][Constants.sfc_ab_layer_temp]) * 2.
												/ sfc_ab[iabCount][sixPlusThreeTimesNumlayers];
										Qanthro = Qanthro + Math.max(0., (Tintw - sfc_ab[iabCount][fivePlusNumlayers])
												* lambdaavw[numlayersMinus1] * 2. / thickw[numlayersMinus1]);
										Qac = Qac + Math.max(0., (sfc_ab[iabCount][fivePlusNumlayers] - Tintw)
												* lambdaavw[numlayersMinus1] * 2. / thickw[numlayersMinus1]);
										if (sfc[iIndex10][Constants.sfc_sunlight_fact] > 3.5)
										{
											TWsun = TWsun + Tsfc[iabCount];
											numWsun = numWsun + 1;
										}
										else if (sfc[iIndex10][Constants.sfc_sunlight_fact] < 0.5)
										{
											TWsh = TWsh + Tsfc[iabCount];
											numWsh = numWsh + 1;
										}	
									}
								}
								}

								//  END OF ITERATIVE TSFC LOOP
//System.out.println("++++++++++++++++++++++++end tsfc newton=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000. + " " + timeis);	
								//  BUT, UNLESS EQUILIBRIUM ACHIEVED IN TERMS OF LONGWAVE EXCHANGE AND TSFC, GO BACK AND DO IT AGAIN (as in Arnfield)
								tsfcLupBalanceContinue = false;
								if (Tdiffmax > Tthreshold)
								{
									// ! adding this to do some extra loops but
									// not to limit the number so it isn't an
									// endless loop
									if (tthresholdLoops < numberOfExtraTthresholdLoops)
									{
										tthresholdLoops = tthresholdLoops + 1;
										// print *,'goto 898
										// Tdiffmax,Tthreshold,tthresholdLoops',Tdiffmax,Tthreshold,tthresholdLoops
										// goto 898;
										tsfcLupBalanceContinue = true;
									}
									else
									{
										tthresholdLoops = 0;
									}
								}
							} // goto 898 replacement
//System.out.println("++++++++++++++++++++++++end 898=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000. );								
							Kup = Kup / (1.0*avg_cnt);
							Lup = Lup / (1.0*avg_cnt);

							solar_refl_done = false;

							//  update internal building air temperature:
							// (Masson et al. 2002)
							//  86400 is the number of seconds in a day
							Tintw = Tintw * (86400. - deltat) / 86400.
									+ Tp / (numwall2 + 2. * numroof2) * deltat / 86400.;
							//  put minimum on internal building temperature
							Tintw = Math.max(Tintw, 273.15 + Tbuild_min);

							Qhcan = Qhcantmp / (1.0*numroof2 + numstreet2) / (1. - lambdapR);

							//  canyon-atm exchange:
							Ri = util.SFC_RI(zref - zH + z0, Ta, Tcan, Ua);
							HashMap<String, Double> htcReturn8 = util.HTC(Ri, Ua, zref - zH + z0, z0, z0);
							httc_top = htcReturn8.get("httc_out");
							Fh = htcReturn8.get("Fh");
							Qhtop = cpair * rhoa * httc_top * (Tcan - Ta);

							//  Checking for oscillations: (0.05 is, from experience, a number that
							//  cuts off oscillations early enough without reacting to normal changes
							//  in canyon temperature)
							//  Here I'm assuming that the canyon temperature cannot be unstable at
							//  timesteps of 1-2 seconds or less...if this is removed, the simulation
							//  sometimes reaches a timestep of 0 simply because dTcan_old is so big
							//  relative to the other term - this is particularly a problem right after
							//  the forcing causes the canyon temperature to reverse trend
							if (Math.abs(deltat * (Qhcan - Qhtop) / Cairavg - dTcan_old) > 0.05 && deltat > 2.)
							{
//System.out.println(   (deltat * (Qhcan - Qhtop) / Cairavg - dTcan_old)   
//		+ " "  + (deltat * (Qhcan - Qhtop) / (Cairavg - dTcan_old))    
//		+ " " + deltat + " "  + Qhcan + " " +  Qhtop + " " + Cairavg + " " +  dTcan_old);
								timeis = timeis - deltat / 3600.;
								deltat = deltat * 5. / 8.;
								counter = 10;
								System.out.println("Oscill. Tcan, starting over with 5/8*deltat=" + " " + deltat);
								dTcan_old = 5. / 8. * dTcan_old;
								// goto 937;
								timeis = timeis + deltat / 3600.;
								ywrite = true;
								continue;
							}

							counter = counter + 1;

							//  NEW Tcan:
							Tcan = Tcan + deltat / Cairavg * (Qhcan - Qhtop);  

							dTcan_old = deltat / Cairavg * (Qhcan - Qhtop);  
							//  WRITE OUTPUT
							if (frcwrite)
							{
//								System.out.println("FORCING=" + lpactual + " " + (2. * bh) / (1.0*bl + bw) + " " + hwactual + " " + stror + " "
//												+ timeis + " " + Kdir + " " + Kdif + " " + Ldn + " " + Ta + " " + ea
//												+ " " + Ua + " " + Udir + " " + press + " " + az + " " + zen);
								overall.writeOutput(Constants.forcing_dat,
										lpactual + " " + (2. * bh) / (1.0*bl + bw) + " " + hwactual + " " + stror + " "
												+ timeis + " " + Kdir + " " + Kdif + " " + Ldn + " " + Ta + " " + ea
												+ " " + Ua + " " + Udir + " " + press + " " + az + " " + zen);
							}

							//  street sfc T
							Tsfc_T = Tsfc_bird - Tsfc_R;

							//  to output averages (every outpt_tm time  interval)
							counter2 = counter2 + 1;
							Kuptot_avg = Kuptot_avg + Kup;
							Luptot_avg = Luptot_avg + Lup;
							Rntot_avg = Rntot_avg + Rnet_tot / Aplan;
							Qhtot_avg = Qhtot_avg + Qh_tot / Aplan;
							Qetot_avg = Qetot_avg + Qe_tot / Aplan; 
							Qgtot_avg = Qgtot_avg + Qg_tot / Aplan; 
							Qanthro_avg = Qanthro_avg + Qanthro / Aplan; 
							Qac_avg = Qac_avg + Qac / Aplan;
							Qdeep_avg = Qdeep_avg + Qdeep / Aplan;
							Qtau_avg = Qtau_avg + rhoa * ustar * ustar;
							TR_avg = TR_avg + Tsfc_R / (1.0*numroof2) - 273.15;
							TT_avg = TT_avg + Tsfc_T / (1.0*numstreet2) - 273.15;
							TN_avg = TN_avg + Tsfc_N / (1.0*numNwall2) - 273.15;
							TS_avg = TS_avg + Tsfc_S / (1.0*numSwall2) - 273.15;
							TE_avg = TE_avg + Tsfc_E / (1.0*numEwall2) - 273.15;
							TW_avg = TW_avg + Tsfc_W / (1.0*numWwall2) - 273.15;
//System.out.println("++++++++++++++++++++++++start Conduction=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000./60. );	
							//  Conduction Loop
							for (int iabCount = 1 - 1; iabCount < numsfc2; iabCount++)
							{

								//  CONDUCTION - combination of Arnfield (198X), Masson (2000), Jacobson (1999)
								//  thermal conductivities (in W/K/m2) are added in series instead of
								//  plain averaging, Tsfc calculated iteratively above acts as the surface
								//  boundary condition

								//  roofs and walls
								Tint = Tintw;
								//  streets
								//add to initialize i (after changing to iIndex)
								int i = (int) sfc_ab[iabCount][Constants.sfc_ab_i];
								if (Math.abs(sfc[i][Constants.sfc_surface_type] - 2.) < 0.5)
								{
									Tint = Tints;
								}

								//  first calculate the thermal conductivities between layer centers by adding
								//  thermal conductivities (or resistivities) in series
								for (int k = 0; k < numlayers; k++)
								{
									tlayer[k] = sfc_ab[iabCount][k + 5];
									tlayerp[k] = tlayer[k];
									lambdaav[k] = sfc_ab[iabCount][k + numlayers + 5];
									htcap[k] = sfc_ab[iabCount][k + 2 * numlayers + 5];
									thick[k] = sfc_ab[iabCount][k + 3 * numlayers + 5];

								}

								//  surface matrix values:
								lambd_o_thick = lambdaav[TUFreg3D.ONE] / (thick[TUFreg3D.ONE] + thick[TUFreg3D.TWO]);
								A[TUFreg3D.ONE] = 0.;
								B[TUFreg3D.ONE] = thick[TUFreg3D.ONE] * htcap[TUFreg3D.ONE] / deltat + 2. * uc * (lambd_o_thick);
								D[TUFreg3D.ONE] = -2. * uc * lambd_o_thick;
								R[TUFreg3D.ONE] = -2. * (1. - uc) * lambd_o_thick * (tlayerp[TUFreg3D.ONE] - tlayerp[TUFreg3D.TWO])
										+ tlayerp[TUFreg3D.ONE] * thick[TUFreg3D.ONE] * htcap[TUFreg3D.ONE] / deltat
										+ (Tsfc[iabCount] - tlayerp[TUFreg3D.ONE]) * lambda_sfc[iabCount] / thick[TUFreg3D.ONE] * 2.;

								//  what I have done above is make the surface boundary condition
								//  "QGsfc" completely explicit, as written below, even though the
								//  conduction can have any level of implicitness, it must conform
								//  to this explicit boundary condition - prior, I had this BC in
								//  the uc and 1-uc brackets to make the BC dependent on the implicitness
								//  but then since the Tsfc solution assumes explicit conduction at
								//  the sfc (i.e. BC using tlayerp(1)), this  would mean a loss or gain
								//  of energy, since the condution solution would assume a different
								//  amount of energy being conducted than the Tsfc solution

								//  interior matrix values:
								for (int k = 1; k < numlayersMinus1; k++)
								{
									lambd_o_thick = lambdaav[k - 1] / (thick[k - 1] + thick[k]);
									lambd_o_thick2 = lambdaav[k] / (thick[k] + thick[k + 1]);
									A[k] = -2. * uc * lambd_o_thick;
									B[k] = thick[k] * htcap[k] / deltat + 2. * uc * (lambd_o_thick + lambd_o_thick2);
									D[k] = -2. * uc * lambd_o_thick2;
									R[k] = -2. * (1. - uc) * (lambd_o_thick * (tlayerp[k] - tlayerp[k - 1])
											+ lambd_o_thick2 * (tlayerp[k] - tlayerp[k + 1]))
											+ tlayerp[k] * thick[k] * htcap[k] / deltat;
								}

								//  values for conduction (+ convection + radiation - Masson et al 2002)
								//  between innermost layer and inner air
								lambd_o_thick = lambdaav[numlayersMinus2] / (thick[numlayersMinus2] + thick[numlayersMinus1]);
								A[numlayersMinus1] = -2. * uc * lambd_o_thick;
								B[numlayersMinus1] = thick[numlayersMinus1] * htcap[numlayersMinus1] / deltat
										+ 2. * uc * (lambd_o_thick + lambdaav[numlayersMinus1] / thick[numlayersMinus1] * IntCond);
								D[numlayersMinus1] = 0.;
								R[numlayersMinus1] = -2. * (1. - uc)
										* (lambd_o_thick * (tlayerp[numlayersMinus1] - tlayerp[numlayersMinus2])
												+ lambdaav[numlayersMinus1] * tlayerp[numlayersMinus1] / thick[numlayersMinus1] * IntCond)
										+ 2. * lambdaav[numlayersMinus1] * Tint / thick[numlayersMinus1] * IntCond
										+ tlayerp[numlayersMinus1] * thick[numlayersMinus1] * htcap[numlayersMinus1] / deltat;

								//  TRIDIAGONAL MATRIX SOLUTION FROM JACOBSON, p. 166
								gam[TUFreg3D.ONE] = -D[TUFreg3D.ONE] / B[TUFreg3D.ONE];
								tlayer[TUFreg3D.ONE] = R[TUFreg3D.ONE] / B[TUFreg3D.ONE];

								for (int k = 2 - 1; k < numlayers; k++)
								{
									denom[k] = B[k] + A[k] * gam[k - 1];
									tlayer[k] = (R[k] - A[k] * tlayer[k - 1]) / denom[k];
									gam[k] = -D[k] / denom[k];
								}

								for (int k = numlayersMinus2; k > 0-1; k--)
								{
									// do k=numlayers-1,1,-1
									tlayer[k] = tlayer[k] + gam[k] * tlayer[k + 1];
								}

								for (int k = 0; k < numlayers; k++)
								{
									sfc_ab[iabCount][k + 5] = tlayer[k];
								}

							}
							// 324 continue
							// 349 continue

							 
							double amodTime = (timeis % 24.);
							UTCI utci = new UTCI();

							// ------------------------------------------------------------------
							//  VISUALIZATION - output for Matlab							
							if (ywrite && (first_write
									|| ((timeis % outpt_tm) * 3600.0 < deltat && (int) (timeis * 100.) != timewrite)
									|| last_write))
							{
System.out.println("++++++++++++++++++++++++start Matlab=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000./60. );	
								ywrite = false;
								timewrite = (int) (timeis * 100.);

								System.out.println("------------------------------------------");
								System.out.println("TIME (hours) = " + " " + timeis + " of " + timeend);
								System.out.println("TIMESTEP (s) = " + " " + deltat);
								System.out.println("" + (timeis % outpt_tm) * 3600. + " " + deltat);
								if (ralt < 0.)
								{
									System.out.println("NIGHTTIME: solar azimuth, elevation angles = " + " " + az + " " + ralt);
								}
								else
								{
									System.out.println("DAYTIME: solar azimuth, elevation angles = " + " " + az + " " + ralt);
								}

								if (ralt > 0.0)
								{
									System.out.println("average relative Kdown absorptionerror = " + " "
											+ Kdn_diff / ((nKdndiff) + 1.e-9) + " " + "%");
								}
								if (Kdn_diff / ((nKdndiff) + 1.e-9) > 5.0 && nKdndiff > 10)
								{
									System.out.println("time average relative Kdn error = " + " "
											+ Kdn_diff / ((nKdndiff) + 1.e-9) + " " + "%");
									overall.writeOutput(Constants.inputs_store_out,
											"-------------------------------------");
									overall.writeOutput(Constants.inputs_store_out,
											"time, time average relative Kdn error = " + " " + timeis + " "
													+ Kdn_diff / ((nKdndiff) + 1.e-9) + " " + "%");
								}
								Kdn_diff = 0.;
								nKdndiff = 0;
								System.out.println("Kdif,Kdir,Kdown(total) = " + " " + Kdif + " " + Kdir + " " + Ktot);
								System.out.println("time,Troof,Tstreet,Tnorth,Tsouth,Teast,Twest" + " " + timeis + " "
										+ Tsfc_R / (numroof2) + " " + Tsfc_T / (numstreet2) + " " + Tsfc_N / (numNwall2)
										+ " " + Tsfc_S / (numSwall2) + " " + Tsfc_E / (numEwall2) + " "
										+ Tsfc_W / (numWwall2));
System.out.println("++++++++++++++++++++++++start writeOutput=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000./60. );		
								//  WRITE OUTPUT
								overall.writeOutput(Constants.EnergyBalanceOverallOut, 
										common.roundToDecimals(lpactual , 3) + "\t" + 
										common.roundToDecimals((2. * bh) / (1.0*bl + bw) , 3) + "\t" 
										+ common.roundToDecimals(hwactual , 3) + "\t" 
										+ common.roundToDecimals(xlat , 3) + "\t" 
										+ common.roundToDecimals(stror , 3) + "\t"
										+ common.roundToDecimals(yd_actual , 3) + "\t" 
										+ common.roundToDecimals(amodTime , 3) + "\t" 
										+ common.roundToDecimals(timeis , 3) + "\t" 
										+ common.roundToDecimals(Rnet_tot / Aplan , 3) + "\t"
										+ common.roundToDecimals(Qh_tot / Aplan , 3) + "\t" 
										+ common.roundToDecimals(Qh_abovezH / Aplan + Qhtop * (1. - lambdapR) , 3) + "\t"
										+ common.roundToDecimals(Qg_tot / Aplan , 3) + "\t" 
										+ common.roundToDecimals(Qg_tot / Aplan + (Qhcan - Qhtop) * (1. - lambdapR), 3) + "\t" 
										+ common.roundToDecimals((Rnet_tot / Aplan - lambdapR * Rnet_R / (1.0*numroof2)) / (1. - lambdapR), 3) + "\t" 
										+ common.roundToDecimals(Qhtop , 3) + "\t" 
										+ common.roundToDecimals(Qhcan , 3) + "\t"
										+ common.roundToDecimals((Qg_tot / Aplan - lambdapR * Qg_R / (1.0*numroof2)) / (1. - lambdapR) + (Qhcan - Qhtop) , 3) + "\t"
										+ common.roundToDecimals(ustar / vK * Math.log((zH - zd) / z0) / Math.sqrt(Fm) * Math.exp(-2. * lambdaf / (1. - lambdapR) / 4.), 3) + "\t" 
										+ common.roundToDecimals(ustar / vK * Math.log((zH - zd) / z0) / Math.sqrt(Fm) , 3) + "\t" 
										+ common.roundToDecimals(Acan + Bcan * Math.exp(Ccan * patchlen / 2.) , 3) + "\t" 
										+ common.roundToDecimals(wstar , 3) + "\t" 
										+ common.roundToDecimals(Kdir + Kdif , 3) + "\t"
										+ common.roundToDecimals(Kup , 3) + "\t" + 
										common.roundToDecimals(Ldn , 3) + "\t" + 
										common.roundToDecimals(Lup , 3) + "\t" + 
										common.roundToDecimals(Kdir_Calc , 3) + "\t" + 
										common.roundToDecimals(Kdif_Calc , 3) + "\t" + 
										common.roundToDecimals(Kdir , 3) + "\t" + 
										common.roundToDecimals(Kdif , 3) + "\t" + 
										common.roundToDecimals((Kup - lambdapR * Kup_R / (1.0*numroof2)) / (1. - lambdapR)	, 3) + "\t" + 
										common.roundToDecimals((Lup - lambdapR * Lup_R / (1.0*numroof2)) / (1. - lambdapR) , 3) + "\t" + 
										common.roundToDecimals(az , 3) + "\t"
										+ common.roundToDecimals(zen , 3) + "\t" + 
										common.roundToDecimals(Math.max(Kdir_NoAtm, 0.) , 3) + "\t" + 
										common.roundToDecimals(Kdn_grid , 3) + "\t" + 
										common.roundToDecimals(Qe_tot / Aplan, 3)
										);

								overall.writeOutput(Constants.energybalancefacets_out, lpactual 
										+ "\t" + common.roundToDecimals((2 * bh) / (1.0*bl + bw)  , 3)
										+ "\t" + common.roundToDecimals(hwactual  , 3)
										+ "\t" + common.roundToDecimals(xlat  , 3)
										+ "\t" + common.roundToDecimals(stror  , 3)
										+ "\t" + common.roundToDecimals(yd_actual  , 3)
										+ "\t" + common.roundToDecimals(amodTime  , 3)
										+ "\t" + common.roundToDecimals(timeis  , 3)
										+ "\t" + common.roundToDecimals(Rnet_R / (1.0*numroof2)  , 3)
										+ "\t" + common.roundToDecimals(Qh_R / (1.0*numroof2)  , 3)
										+ "\t" + common.roundToDecimals(Qg_R / (1.0*numroof2)  , 3)
										+ "\t" + common.roundToDecimals(Rnet_T / (1.0*numstreet2) , 3)
										+ "\t" + common.roundToDecimals(Qh_T / (1.0*numstreet2)  , 3)
										+ "\t" + common.roundToDecimals(Qg_T / (1.0*numstreet2)  , 3)
										+ "\t" + common.roundToDecimals(Rnet_N / (1.0*numNwall2)  , 3)
										+ "\t" + common.roundToDecimals(Qh_N / (1.0*numNwall2)  , 3)
										+ "\t" + common.roundToDecimals(Qg_N / (1.0*numNwall2) , 3)
										+ "\t" + common.roundToDecimals(Rnet_S / (1.0*numSwall2)  , 3)
										+ "\t" + common.roundToDecimals(Qh_S / (1.0*numSwall2)  , 3)
										+ "\t" + common.roundToDecimals(Qg_S / (1.0*numSwall2)  , 3)
										+ "\t" + common.roundToDecimals(Rnet_E / (1.0*numEwall2)  , 3)
										+ "\t" + common.roundToDecimals(Qh_E / (1.0*numEwall2) , 3)
										+ "\t" + common.roundToDecimals(Qg_E / (1.0*numEwall2)  , 3)
										+ "\t" + common.roundToDecimals(Rnet_W / (1.0*numWwall2)  , 3)
										+ "\t" + common.roundToDecimals(Qh_W / (1.0*numWwall2)  , 3)
										+ "\t" + common.roundToDecimals(Qg_W / (1.0*numWwall2) , 3));

								overall.writeOutput(Constants.RadiationBalanceFacetsOut, lpactual 
										+ "\t" + common.roundToDecimals((2 * bh) / (1.0*bl + bw) , 3) 
										+ "\t" + common.roundToDecimals(hwactual  , 3)
										+ "\t" + common.roundToDecimals(xlat  , 3)
										+ "\t" + common.roundToDecimals(stror  , 3)
										+ "\t" + common.roundToDecimals(yd_actual  , 3)
										+ "\t" + common.roundToDecimals(amodTime  , 3)
										+ "\t" + common.roundToDecimals(timeis  , 3)
										+ "\t" + common.roundToDecimals(Kdn_S / (1.0*numSwall2)  , 3)
										+ "\t" + common.roundToDecimals(Kup_S / (1.0*numSwall2)  , 3)
										+ "\t" + common.roundToDecimals(Ldn_S / (1.0*numSwall2)  , 3)
										+ "\t" + common.roundToDecimals(Lup_S / (1.0*numSwall2) , 3)
										+ "\t" + common.roundToDecimals(Kdn_E / (1.0*numEwall2)  , 3)
										+ "\t" + common.roundToDecimals(Kup_E / (1.0*numEwall2)  , 3)
										+ "\t" + common.roundToDecimals(Ldn_E / (1.0*numEwall2)  , 3)
										+ "\t" + common.roundToDecimals(Lup_E / (1.0*numEwall2)  , 3)
										+ "\t" + common.roundToDecimals(Kdn_N / (1.0*numNwall2) , 3)
										+ "\t" + common.roundToDecimals(Kup_N / (1.0*numNwall2)  , 3)
										+ "\t" + common.roundToDecimals(Ldn_N / (1.0*numNwall2)  , 3)
										+ "\t" + common.roundToDecimals(Lup_N / (1.0*numNwall2)  , 3)
										+ "\t" + common.roundToDecimals(Kdn_W / (1.0*numWwall2)  , 3)
										+ "\t" + common.roundToDecimals(Kup_W / (1.0*numWwall2) , 3)
										+ "\t" + common.roundToDecimals(Ldn_W / (1.0*numWwall2)  , 3)
										+ "\t" + common.roundToDecimals(Lup_W / (1.0*numWwall2)  , 3)
										+ "\t" + common.roundToDecimals(Kdn_R / (1.0*numroof2)  , 3)
										+ "\t" + common.roundToDecimals(Kup_R / (1.0*numroof2)  , 3)
										+ "\t" + common.roundToDecimals(Ldn_R / (1.0*numroof2)  , 3)
										+ "\t" + common.roundToDecimals(Lup_R / (1.0*numroof2)  , 3)
										+ "\t" + common.roundToDecimals(Kdn_T / (1.0*numstreet2)  , 3)
										+ "\t" + common.roundToDecimals(Kup_T / (1.0*numstreet2) , 3)
										+ "\t" + common.roundToDecimals(Ldn_T / (1.0*numstreet2)  , 3)
										+ "\t" + common.roundToDecimals(Lup_T / (1.0*numstreet2) , 3));
								overall.writeOutput(Constants.tsfcfacets_out, lpactual 
										+ "\t" + common.roundToDecimals(((2 * bh) / (1.0*bl + bw)) , 3)
										+ "\t" + common.roundToDecimals(hwactual  , 3)
										+ "\t" + common.roundToDecimals(xlat  , 3)
										+ "\t" + common.roundToDecimals(stror  , 3)
										+ "\t" + common.roundToDecimals(yd_actual  , 3)
										+ "\t" + common.roundToDecimals(amodTime , 3)
										+ "\t" + common.roundToDecimals(timeis  , 3)
										+ "\t" + common.roundToDecimals((Tsfc_cplt / (numroof2 + numwall2 + numstreet2) - 273.15) , 3)
										+ "\t" + common.roundToDecimals((Tsfc_bird / Aplan - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((Tsfc_R / (1.0*numroof2) - 273.15) , 3)
										+ "\t" + common.roundToDecimals((Tsfc_T / (1.0*numstreet2) - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((Tsfc_N / (1.0*numNwall2) - 273.15) , 3)
										+ "\t" + common.roundToDecimals((Tsfc_S / (1.0*numSwall2) - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((Tsfc_E / (1.0*numEwall2) - 273.15) , 3)
										+ "\t" + common.roundToDecimals((Tsfc_W / (1.0*numWwall2) - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((Tcan - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((Ta - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((Tintw - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((httcR / (1.0*numroof2))  , 3)
										+ "\t" + common.roundToDecimals((httcT / (1.0*numstreet2))  , 3)
										+ "\t" + common.roundToDecimals((httcW / (1.0*numwall2))  , 3)
										+ "\t" + common.roundToDecimals((Trad_R / (1.0*numroof2) - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((Trad_T / (1.0*numstreet2) - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((Trad_N / (1.0*numNwall2) - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((Trad_S / (1.0*numSwall2) - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((Trad_E / (1.0*numEwall2) - 273.15)  , 3)
										+ "\t" + common.roundToDecimals((Trad_W / (1.0*numWwall2) - 273.15) , 3));
								overall.writeOutput(Constants.tsfcfacetssunshade_out,
										lpactual + "\t" + common.roundToDecimals(((2 * bh) / (1.0*bl + bw))   , 3)
										+ "\t" + common.roundToDecimals(hwactual   , 3)
										+ "\t" + common.roundToDecimals(xlat   , 3)
										+ "\t" + common.roundToDecimals(stror   , 3)
										+ "\t" + common.roundToDecimals(yd_actual   , 3)
										+ "\t" + common.roundToDecimals(amodTime   , 3)
										+ "\t" + common.roundToDecimals(timeis   , 3)
										+ "\t" + common.roundToDecimals((TTsun / Math.max(0.01, (1.0*numTsun)) - 273.15)  , 3) 
										+ "\t" + common.roundToDecimals((TTsh / Math.max(0.01, (1.0*numTsh)) - 273.15)   , 3)
										+ "\t" + common.roundToDecimals((TNsun / Math.max(0.01, (1.0*numNsun)) - 273.15)   , 3)
										+ "\t" + common.roundToDecimals((TNsh / Math.max(0.01, (1.0*numNsh)) - 273.15)   , 3)
										+ "\t" + common.roundToDecimals((TSsun / Math.max(0.01, (1.0*numSsun)) - 273.15)   , 3)
										+ "\t" + common.roundToDecimals((TSsh / Math.max(0.01, (1.0*numSsh)) - 273.15)   , 3)
										+ "\t" + common.roundToDecimals((TEsun / Math.max(0.01, (1.0*numEsun)) - 273.15)   , 3)
										+ "\t" + common.roundToDecimals((TEsh / Math.max(0.01, (1.0*numEsh)) - 273.15)   , 3)
										+ "\t" + common.roundToDecimals((TWsun / Math.max(0.01, (1.0*numWsun)) - 273.15)  , 3) 
										+ "\t" + common.roundToDecimals((TWsh / Math.max(0.01, (1.0*numWsh)) - 273.15)  , 3));

								//  to output time averages
								if (!first_write)
								{
									overall.writeOutput(Constants.energybalancetsfctimeaverage_out,
											lpactual 
											+ "\t" + common.roundToDecimals((2 * bh) / (1.0*bl + bw) , 3)
											+ "\t" + common.roundToDecimals(hwactual , 3)
											+ "\t" + common.roundToDecimals( xlat , 3)
											+ "\t" + common.roundToDecimals(stror , 3)
											+ "\t" +common.roundToDecimals( yd + (int) ((timeis - outpt_tm / 2.) / 24.) , 3)
											+ "\t" + common.roundToDecimals(((timeis - outpt_tm / 2.) % 24.) , 3)
											+ "\t" + common.roundToDecimals((timeis - outpt_tm / 2.), 3)
											+ "\t" + common.roundToDecimals(amodTime , 3)
											+ "\t" + common.roundToDecimals(timeis , 3)
											+ "\t" + common.roundToDecimals((Kuptot_avg / (1.0*counter2)), 3)
											+ "\t" + common.roundToDecimals((Luptot_avg / (1.0*counter2)) , 3)
											+ "\t" + common.roundToDecimals((Rntot_avg / (1.0*counter2)), 3)
											+ "\t" + common.roundToDecimals((Qhtot_avg / (1.0*counter2)) , 3)
											+ "\t" + common.roundToDecimals((Qgtot_avg / (1.0*counter2)), 3)
											+ "\t" + common.roundToDecimals((Qanthro_avg / (1.0*counter2)) , 3)
											+ "\t" + common.roundToDecimals((Qac_avg / (1.0*counter2)), 3)
											+ "\t" + common.roundToDecimals((Qdeep_avg / (1.0*counter2)) , 3)
											+ "\t" + common.roundToDecimals((Qtau_avg / (1.0*counter2)), 3)
											+ "\t" + common.roundToDecimals((TR_avg / (1.0*counter2)) , 3)
											+ "\t" + common.roundToDecimals((TT_avg / (1.0*counter2)) , 3)
											+ "\t" + common.roundToDecimals((TN_avg / (1.0*counter2)) , 3)
											+ "\t" + common.roundToDecimals((TS_avg / (1.0*counter2)) , 3)
											+ "\t" + common.roundToDecimals((TE_avg / (1.0*counter2)) , 3)
											+ "\t" + common.roundToDecimals((TW_avg / (1.0*counter2)), 3));
									counter2 = 0;
									Kuptot_avg = 0.;
									Luptot_avg = 0.;
									Rntot_avg = 0.;
									Qhtot_avg = 0.;
									Qetot_avg = 0.;
									Qgtot_avg = 0.;
									Qanthro_avg = 0.;
									Qac_avg = 0.;
									Qdeep_avg = 0.;
									Qtau_avg = 0.;
									TR_avg = 0.;
									TT_avg = 0.;
									TN_avg = 0.;
									TS_avg = 0.;
									TE_avg = 0.;
									TW_avg = 0.;
								}

								//  write out intra-facet (patch) surface temperatures
								if (facet_out)
								{
									int iIndex15 = 0;
									for (int f = 0; f < 5; f++)
									{
										for (int z = 0; z < bh; z++)
										{
											for (int y = 0; y < aw2; y++)
											{
												for (int x = 0; x < al2; x++)
												{
													if (surf[x][y][z][f])
													{
														iIndex15 = iIndex15 + 1;
														jab = ind_ab[iIndex15];
														if (sfc[iIndex15][Constants.sfc_in_array] > 1.5)
														{
															overall.writeOutput(Constants.TsfcSolarSVF_Patch_yd,
																	timeis + " " + f + " " + z + " " + y + " " + x + " "
																			+ (1. - sfc[iIndex15][Constants.sfc_evf]) + " "
																			+ (Tsfc[jab] - 273.15) + " "
																			+ (Trad[jab] - 273.15) + " " + absbs[jab]
																			+ " " + reflts[jab]);
														}
													}
												}
											}
										}
									}
								}

								time_out = ((int) Math.round(timeis * 10.)) * 10;
								lptowrite = (int) Math.round(lpin[lpiter] * 100.);
								lpwrite = common.padLeft( lptowrite, 2, '0') ;

								bhbltowrite = (int) Math.round(bh_o_bl[bhiter] * 100.);

								bhblwrite = common.padLeft( bhbltowrite, 3, '0') ;

								strorwrite = common.padLeft( (int) Math.round(stror), 2, '0') ;

								latwrite = common.padLeft( (int) Math.round(Math.abs(xlat)), 3, '0') ;

								if (xlat >= 0.)
								{
									latwrite2 = latwrite + "N";
								}
								else
								{
									latwrite2 = latwrite + "S";
								}

								ydwrite = common.padLeft( yd, 3, '0') ;


								tempTimeis = (int) (timeis * 2);
								if (tempTimeis < 1)
								{
									tempTimeis = 1;
								}
								System.out.println("TUF/Maespa, timeis" + " " + timeis + " " + tempTimeis);

								String Tsfc_yd_outFile;
								String Tbright_yd_outFile;
								if (sum_out)
								{

									if (time_out < 1000.)
									{
										time1 = common.padLeft( time_out, 3, '0') ;
										if (time_out == 0)
										{
											time1 = "000";
										}
										Tsfc_yd_outFile = "Tsfc_yd" + ydwrite + "_lp" + lpwrite + "_bhbl" + bhblwrite
												+ "_lat" + latwrite2 + "_stror" + strorwrite + "_tim0" + time1 + ".out";
										Tbright_yd_outFile = "Tbright_yd" + ydwrite + "_lp" + lpwrite + "_bhbl"
												+ bhblwrite + "_lat" + latwrite2 + "_stror" + strorwrite + "_tim0"
												+ time1 + ".out";
									}
									else if (time_out < 10000)
									{

										time2 = common.padLeft( time_out, 4, '0') ;
										Tsfc_yd_outFile = "Tsfc_yd" + ydwrite + "_lp" + lpwrite + "_bhbl" + bhblwrite
												+ "_lat" + latwrite2 + "_stror" + strorwrite + "_tim" + time2 + ".out";
										Tbright_yd_outFile = "Tbright_yd" + ydwrite + "_lp" + lpwrite + "_bhbl"
												+ bhblwrite + "_lat" + latwrite2 + "_stror" + strorwrite + "_tim" + time2 + ".out";
				
									}
									else
									{
										if (time_out < 100000)
										{

											time3 = common.padLeft( time_out, 5, '0') ;
											Tsfc_yd_outFile = "Tsfc_yd" + ydwrite + "_lp" + lpwrite + "_bhbl"
													+ bhblwrite + "_lat" + latwrite2 + "_stror" + strorwrite + "_tim" + time3 + ".out";
											Tbright_yd_outFile = "Tbright_yd" + ydwrite + "_lp" + lpwrite + "_bhbl"
													+ bhblwrite + "_lat" + latwrite2 + "_stror" + strorwrite + "_tim" + time3 + ".out";
			
										}
										else
										{
											Tsfc_yd_outFile = "";
											Tbright_yd_outFile = "";
											System.out.println("coded to only write output up to hour 999");
		
											System.exit(1);
										}
									}

									//  metadata at the top of output files
									overall.writeOutput(Tsfc_yd_outFile, numsfc2 + " " + lpactual + " " + xlat + " " + stror);	
									overall.writeOutput(Tbright_yd_outFile, numsfc2 + " " + lpactual + " " + xlat + " " + stror);				
									overall.writeOutput(Tsfc_yd_outFile, bh + " " + bl + " " + bw + " " + sw + " " + sw2);				
									overall.writeOutput(Tbright_yd_outFile, bh + " " + bl + " " + bw + " " + sw + " " + sw2);				
									overall.writeOutput(Tsfc_yd_outFile, al2 + " " + aw2 + " " + patchlen + " " + yd + " " + ralt);						
									overall.writeOutput(Tbright_yd_outFile, al2 + " " + aw2 + " " + patchlen + " " + yd + " " + ralt);				

									int iIndex13 = 0;

									for (int f = 0; f < 5; f++)
									{
										for (int z = 0; z < bh; z++)
										{
											for (int y = 0; y < aw2; y++)
											{
												for (int x = 0; x < al2; x++)
												{
													if (surf[x][y][z][f])
													{
														iIndex13 = iIndex13 + 1;
														jab = ind_ab[iIndex13];
														if (sfc[iIndex13][Constants.sfc_in_array] > 1.5)
														{
															overall.writeOutput(Tsfc_yd_outFile, "" + Tsfc[jab]);
											
															overall.writeOutput(Tbright_yd_outFile, "" + Trad[jab]);
										
														}
													}
												}
											}
										}
									}
								}
System.out.println("++++++++++++++++++++++++start outputresults=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000./60. );									
								OutputResults outputresults = new OutputResults();
								outputresults.outputMatlab(matlab_out, time_out, first_write, writeTsfc, writeKl, 
										writeKabs, writeKrefl, writeLabs, writeLrefl, writeLdown, writeTmrt, 
										writeUtci, writeEnergyBalances, lpwrite, bhblwrite, ydwrite, latwrite2, 
										strorwrite, newlp, newbhbl, numsfc, iab, bh, aw2, al2, 
										surf, sfc, overall,  ind_ab, Tsfc,  
										tots, totl, reflts, refltl, absbs, timeis, 
										Ldn, treeXYMap, sfc_ab_map_x, sfc_ab_map_y, absbl, 
										diffShadingValueUsed, tempTimeis, Tcan, ea, Ua, 
										zen, Acan, Bcan, Ccan, patchlen, currentRnet, 
										currentQh, currentQe, currentQg, utci, 
										maespaDataArray);		
System.out.println("++++++++++++++++++++++++start outputUrbanPlumber=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000./60. );										
								UrbanPlumberOutput outputUrbanPlumber = new UrbanPlumberOutput();
								outputUrbanPlumber.output( time_out, first_write, overall,  
										tots, totl, reflts, refltl, absbs, timeis, 
										Ldn, Tcan, ea, Ua, 
										Aplan, Rnet_tot, Kup, Lup, Qh_tot, Qe_tot, Kdn_grid, Qg_tot, yd, year,
										Tsfc_R, Tsfc_T, Tsfc_N, Tsfc_S, Tsfc_E, Tsfc_W, 
										numroof2, numstreet2, numNwall2, numSwall2, numEwall2, numWwall2,
										Kdir, Kdif);	
System.out.println("++++++++++++++++++++++++end outputUrbanPlumber=" + (System.currentTimeMillis() - TUFreg3D.startTime)/1000./60. );		
								first_write = false;
								//  whether or not it is a timestep to write outputs
							}
							if (last_write)
							{
								last_write = false;
								frcwrite = false;
								// goto 351;
								return;
							}
							timeis = timeis + deltat / 3600.;

							if ((timeis % outpt_tm) >= outpt_tm - 3.5 * deltat / 3600.)
							{
								ywrite = true;
							}
							//// 309 continue
						}
						//
						// if(ywrite)
						// {
						// last_write=true;
						// //!! KN had to comment this out because compiler crashes
						// //! goto 349
						// }
						//// 351 continue
						// last_write=false;
						// frcwrite=false;
						stror = stror + strorint;
						//  this is the enddo for the street orientation iteration
					}
					xlat = xlat + xlatint;
					//  this is the enddo for the latitude iteration
				}
				//  this is the enddo for the bh iteration

				// Release pooled arrays back to the pool for reuse
				arrayPool.releaseAllDouble(refl_emist, absbs, absbl, tots, totl,
					refls, refll, reflts, refltl, reflps, reflpl,
					Tsfc, Trad, lambda_sfc, Qh, Qe,
					currentRnet, currentQe, currentQh, currentQg);
			}
			//  this is the enddo for the lp iteration
		}
		System.out.println("------------------------------------------");
		System.out.println("absolute value of relative sky view factor error(maximum of all simulations) was:" + " "
				+ svfe_store + " " + "%(averaged over the central urban unit)");
		System.out.println("------------------------------------------");
		System.out.println("absolute value of absolute received Kdown error(maximum of all simulations) was:" + " "
				+ Kdn_ae_store + " "
				+ " W/m2(averaged over the central urban unit) and the absolutevalue of the relative received Kdown error at this timestep was:"
				+ " " + 100. * Kdn_re_store + " " + "%");
		System.out.println("------------------------------------------");
		System.out.println("Received solar radiation was at least 10 W/m2AND 5.0% in error during " + " " + badKdn + " "
				+ " time steps over thecourse of the simulation(s)");
		if (badKdn > 0)
		{
			System.out.println(
					"...you may need to increase the resolution;the file Inputs_Store.out will tell you which simulations (if you performed more than one)suffered the most from a lack of resolution");
		}

		// Shutdown parallel processing and cleanup resources
		shutdownParallelPool();
	}

}
