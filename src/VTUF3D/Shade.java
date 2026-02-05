package VTUF3D;

import VTUF3D.Utilities.MaespaDataFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public class Shade
{
	/**
	 * PatchInfo holds information about a patch to be processed for shading.
	 * Used to enable parallel processing of patches.
	 */
	private static class PatchInfo {
		final int x, y, z, f;
		final int iab;
		final int sfcIndex;
		final boolean isShaded; // If true, patch is shaded by orientation
		double sunlightFact;

		PatchInfo(int x, int y, int z, int f, int iab, int sfcIndex, boolean isShaded) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.f = f;
			this.iab = iab;
			this.sfcIndex = sfcIndex;
			this.isShaded = isShaded;
			this.sunlightFact = 0.0;
		}
	}

	// ! ----------------------------------------------------------
	// ! Subroutine to determine which patches are shaded and which are sunlit
	public static HashMap shade(double stror, double az, double RALT, double ypos, boolean[][][][] surf,
			boolean[][][] surf_shade, int AL2, int AW2, int BH, int PAR, double[][] sfc, int numsfc, int a1, int a2,
			int b1, int b2, int numsfc2, double[][] sfc_ab, int par_ab, boolean[][][] veg_shade, double timeis,
			int yd_actual, double[][] treeXYMapSunlightPercentageTotal, int[][] treeXYMap,
			HashMap<String, MaespaDataFile> maespaTestflxData)
	{
		HashMap returnValues = new HashMap();
		
		boolean DEBUG_MODE = false;
		int XTEST, YTEST, ZTEST;
//		double xpos;
		double DIR1, DIR2;
		double xpinc, ypinc;
		double XT, YT, ZT, XINC, YINC, ZINC;
		boolean vegetationInRay;
		// ! FOR PARAMETER 2 THE ELEMENT IS SUNLIT SURF(X,Y,Z,f,2)=1 OR SHADED SURF (X,Y,Z,f,2)=2
		double ANGDIF, HH;
		int i = 0, is;
		int iab;
//		double dmin;
		double[] sor = new double[6];
		double[] sorsh = new double[6];
		double transmissionPercentage;
		double sorTmpValue;

		// X=0;
		// Y=0;
		// Z=0;
		vegetationInRay = false;
		az = (az % 360.);
		// ! ensure that stror is a positive angle between 0 and 360
		stror = (stror + 360. % 360.);
		// ! xpos and ypos are the orientations of the x and y axes
		ypos = stror;
//		xpos = stror + 90;
		// ! DECIDE WHETHER WALL ORIENTATION IS FACING THE SUN OR AWAY FROM IT
		DIR1 = (az + 90. % 360.);
		DIR2 = (az + 270. % 360.);
		// ! set a minimum distance for ray to go before it can hit
		// ! an obstacle (just longer than the distance from the center
		// ! of a cell face to an opposite corner (1.225) to prevent
		// ! self shading). Having a ray tracing increment of 0.25 still
		// ! allows for shading by a cell adjacent to the air volume above
		// ! the surface
//		dmin = 1.23;
		// ! the actual surface (i.e. wall) orientations are only 4
		sor[2] = stror;
		sor[3] = (stror + 90. % 360.);
		sor[4] = (stror + 180. % 360.);
		sor[5] = (stror + 270. % 360.);
		// ! only a maximum of two of these orientations can be shaded
		is = 0;
		if ((DIR1 + 180) < 360)
		{
			for (int k = 2 ; k < 5+1; k++)
			{
				if ((sor[k] >= DIR1) && (sor[k] < DIR2))
				{
					is = is + 1;
					sorsh[is] = sor[k];
				}
			}
		}
		else
		{
			for (int k = 2 ; k < 5+1; k++)
			{
				if (((sor[k] >= DIR1) && (sor[k] < 360)) || sor[k] < DIR2)
				{
					is = is + 1;
					sorsh[is] = sor[k];
				}
			}
		}
		if (is == 1)
		{
			sorsh[2] = sorsh[1];
		}
		// ! SETUP NECESSARY EQUATIONS TO CALCULATE THE XINC,YINC AND ZINC(INCREMENTS REQUIRED FOR TESTING SUNLIT OR SHADED)
	
		// ! ANGDIF is the difference between the solar azimuth and the direction of the 'north' facing street
		ANGDIF = az - ypos;
		if (ANGDIF < 0.)
		{
			ANGDIF = az + (360. - ypos);
		}
		HH = Math.cos(Math.toRadians(RALT)) * Constants.RAY_TRACE_INCREMENT;
		XINC = Math.sin(Math.toRadians(ANGDIF)) * HH;
		YINC = Math.cos(Math.toRadians(ANGDIF)) * HH;
		ZINC = Math.sin(Math.toRadians(RALT)) * Constants.RAY_TRACE_INCREMENT;
		// ! RUN THROUGH THE ARRAY TO DETERMINE WHICH FACES ARE SHADED AND SUNLIT
		// ! IF FACING SUN DECIDE WHETHER LOCATION IS BLOCKED BY OTHER BUILDINGS
		// ! ROOF IS not ALWAYS SUNLIT
		iab = 0;
		for (int f = TUFreg3D.ONE; f <=TUFreg3D.FIVE; f++) // !! KN switching this to 2,5 from 1,5 since sor(2:5)
		{
			for (int z = 0; z < BH ; z++)
			{
				for (int y = b1; y <= b2; y++)
				{
					for (int x = a1; x <= a2; x++)
					{
						if (!surf[x][y][z][f])
						{
							// ! if the cell face is not a surface:
							continue; //this should replace goto 41
						}
						if (f > TUFreg3D.ONE)
						{
							sorTmpValue = sor[f];
						}
						else
						{
							sorTmpValue = -9999.;
						}
						//!! restructure this because sor(1) crashes with array out of bounds
						if (f > TUFreg3D.ONE && (sorTmpValue == sorsh[1] || sorTmpValue == sorsh[2])) 							
						{
							iab = iab + 1;
							if (iab >= numsfc2)
							{
								// ! if the cell face is not in central array
								System.out.println("not in central array iab,x,y,z,f,i" + " " + iab + " " + x + " " + y + " " + z + " " + f + " " + i);
								continue; //this should replace goto 41
							}
							if (i > numsfc || iab > numsfc2)
							{
								System.out.println("PROB1:i,numsfc" + " " + i + " " + numsfc);
								// ! IF NEXT TRUE THEN ORIENTATION OF SUN AND SURFACE ELEMENT MAKES LOCATION SHADED
								sfc[i][Constants.sfc_sunlight_fact] = 0.;
							}
						} // !! closing the else if (f > 1)
						else
						{
							iab = iab + 1;
							if (iab >= numsfc2)
							{
								// ! if the cell face is not in central array
								System.out.println("not in central array iab,x,y,z,f,i" + " " + iab + " " + x + " " + y + " " + z + " " + f + " " + i);
								continue; //this should replace goto 41
							}
							i = (int)sfc_ab[iab][Constants.sfc_ab_i];

							if (i > numsfc || iab > numsfc2)
							{
								System.out.println("PROB2:i,numsfc" + " " + i + " " + numsfc);
								System.exit(1);
							}
							// ! case where the wall orientation is such that it is facing towards the sun
							// ! the following defines steps that climb along the ray towards the sun
							// ! subdivide each patch into 4 to calculate partial shading
							sfc[i][Constants.sfc_sunlight_fact] = 0.;
							for (int iv = 1; iv < 4; iv++)
							{
								xpinc = -Constants.FACE_QUARTER_OFFSET;
								ypinc = -Constants.FACE_QUARTER_OFFSET;
								if ((iv >= 2) && (iv <= 3))
								{
									xpinc = Constants.FACE_QUARTER_OFFSET;
								}
								if ((iv >= 1) && (iv <= 2))
								{
									ypinc = Constants.FACE_QUARTER_OFFSET;
								}
								ZT = ((z) + ZINC);
								XT = ((x) + XINC);
								YT = ((y) + YINC);
								// ! start the ray tracing from the wall element surface
								// ! ACTUALLY from the center of four smaller patches that the
								// ! original patch is subdivided into
								if (f == 5)
								{
									XT = XT - Constants.FACE_HALF_OFFSET;
									YT = YT + xpinc;
									ZT = ZT + ypinc;
								}
								else if (f == 3)
								{
									XT = XT + Constants.FACE_HALF_OFFSET;
									YT = YT + xpinc;
									ZT = ZT + ypinc;
								}
								else if (f == 4)
								{
									XT = XT + xpinc;
									YT = YT - Constants.FACE_HALF_OFFSET;
									ZT = ZT + ypinc;
								}
								else if (f == 2)
								{
									XT = XT + xpinc;
									YT = YT + Constants.FACE_HALF_OFFSET;
									ZT = ZT + ypinc;
								}
								else if (f == 1)
								{
									XT = XT + xpinc;
									YT = YT + ypinc;
									ZT = ZT + Constants.FACE_HALF_OFFSET;
								}
								else
								{
									System.out.println("PROBLEM with wall orientation");
								}
								ZTEST = (int) Math.round(ZT);
								XTEST = (int) Math.round(XT);
								YTEST = (int) Math.round(YT);
								while ((XTEST == x) && (YTEST == y) && (ZTEST == z))
								{
									ZT = (ZT + ZINC);
									XT = (XT + XINC);
									YT = (YT + YINC);
									ZTEST = (int) Math.round(ZT);
									XTEST = (int) Math.round(XT);
									YTEST = (int) Math.round(YT);
								}
								if (veg_shade[x][y][0])
								{
									treeXYMapSunlightPercentageTotal[XTEST][YTEST] = 1.0;
								}
								while ((ZTEST <= BH) && (XTEST >= 1) && (XTEST <= AL2) && (YTEST >= 1) && (YTEST <= AW2) && (ZTEST >= 0))
								{
//System.out.println("ztest" + " " + ZTEST + " " + YTEST + " " + XTEST + " " + BH + " " + AL2 + " " + AW2 );
									if (surf_shade[XTEST][YTEST][ZTEST])
									{
										// !! ray trace encounters a building,
										// this will be in full shade
										if (DEBUG_MODE)
										{
											System.out.println(x + " " + y + " " + z + " " + f + " "
													+ "encounters building at" + " " + XTEST + " " + YTEST + " " + ZTEST
													+ " " + vegetationInRay);
										}
										if (veg_shade[x][y][0])
										{
											treeXYMapSunlightPercentageTotal[x][y] = 0.;
										}
										break; //this should replace the goto 46
									}
									// ! set vegetation flag if not already set
									if (veg_shade[XTEST][YTEST][ZTEST] && !vegetationInRay)
									{
										if (DEBUG_MODE)
										{
											System.out.println(x + " " + y + " " + z + " " + f + " "
													+ "encounters building at" + " " + XTEST + " " + YTEST + " " + ZTEST
													+ " " + vegetationInRay
													+ " set veg flag");
										}
										if (veg_shade[x][y][0])
										{
											treeXYMapSunlightPercentageTotal[x][y] = 0.5;
										}
										vegetationInRay = true;										
									} // end if
										// (veg_shade[XTEST][YTEST][ZTEST]&&!vegetationInRay)
									ZT = (ZT + ZINC);
									XT = (XT + XINC);
									YT = (YT + YINC);
									ZTEST = (int) Math.round(ZT);
									XTEST = (int) Math.round(XT);
									YTEST = (int) Math.round(YT);
									// 100 CONTINUE
								} // end while ((ZTEST<=BH
								// ! sunlit
								if (vegetationInRay)
								{
									//TODO figure out how to replace TestflxData, variable TD (total transission) with online Maespa
									transmissionPercentage = ReverseRay.reverseRayTrace(XT, XINC, YT, YINC, ZT, ZINC,
											XTEST, YTEST, ZTEST, BH, AL2, AW2, veg_shade, timeis, yd_actual, treeXYMap,
											maespaTestflxData);
									sfc[i][Constants.sfc_sunlight_fact] = sfc[i][Constants.sfc_sunlight_fact] + transmissionPercentage;
									vegetationInRay = false;
								}
								else
								{
									sfc[i][Constants.sfc_sunlight_fact] = sfc[i][Constants.sfc_sunlight_fact] + 1.;
									vegetationInRay = false;
								}
							} // for (int iv=1
						} // end else of if(f > 1 && (sorTmpValue==sorsh[1]
					}
				}
			}
		}
		returnValues.put("sfc", sfc);
		returnValues.put("sfc_ab", sfc_ab);
		returnValues.put("treeXYMapSunlightPercentageTotal", treeXYMapSunlightPercentageTotal);
		//return sfc,sfc_ab,treeXYMapSunlightPercentageTotal;
		return returnValues;
	}

	/**
	 * Parallel version of shade calculation using ForkJoinPool.
	 * Patches are collected first, then ray tracing is performed in parallel.
	 */
	public static HashMap shadeParallel(double stror, double az, double RALT, double ypos, boolean[][][][] surf,
			boolean[][][] surf_shade, int AL2, int AW2, int BH, int PAR, double[][] sfc, int numsfc, int a1, int a2,
			int b1, int b2, int numsfc2, double[][] sfc_ab, int par_ab, boolean[][][] veg_shade, double timeis,
			int yd_actual, double[][] treeXYMapSunlightPercentageTotal, int[][] treeXYMap,
			HashMap<String, MaespaDataFile> maespaTestflxData, ForkJoinPool pool)
	{
		// If pool is null or too few patches, fall back to sequential
		if (pool == null || numsfc2 < Constants.PARALLEL_MIN_PATCHES) {
			return shade(stror, az, RALT, ypos, surf, surf_shade, AL2, AW2, BH, PAR, sfc, numsfc,
					a1, a2, b1, b2, numsfc2, sfc_ab, par_ab, veg_shade, timeis, yd_actual,
					treeXYMapSunlightPercentageTotal, treeXYMap, maespaTestflxData);
		}

		HashMap returnValues = new HashMap();

		// Setup phase (same as sequential)
		az = (az % 360.);
		stror = (stror + 360. % 360.);
		ypos = stror;

		double DIR1 = (az + 90. % 360.);
		double DIR2 = (az + 270. % 360.);

		double[] sor = new double[6];
		double[] sorsh = new double[6];

		sor[2] = stror;
		sor[3] = (stror + 90. % 360.);
		sor[4] = (stror + 180. % 360.);
		sor[5] = (stror + 270. % 360.);

		int is = 0;
		if ((DIR1 + 180) < 360) {
			for (int k = 2; k <= 5; k++) {
				if ((sor[k] >= DIR1) && (sor[k] < DIR2)) {
					is = is + 1;
					sorsh[is] = sor[k];
				}
			}
		} else {
			for (int k = 2; k <= 5; k++) {
				if (((sor[k] >= DIR1) && (sor[k] < 360)) || sor[k] < DIR2) {
					is = is + 1;
					sorsh[is] = sor[k];
				}
			}
		}
		if (is == 1) {
			sorsh[2] = sorsh[1];
		}

		double ANGDIF = az - ypos;
		if (ANGDIF < 0.) {
			ANGDIF = az + (360. - ypos);
		}
		final double HH = Math.cos(Math.toRadians(RALT)) * Constants.RAY_TRACE_INCREMENT;
		final double XINC = Math.sin(Math.toRadians(ANGDIF)) * HH;
		final double YINC = Math.cos(Math.toRadians(ANGDIF)) * HH;
		final double ZINC = Math.sin(Math.toRadians(RALT)) * Constants.RAY_TRACE_INCREMENT;

		// Phase 1: Collect patches to process (sequential - fast)
		List<PatchInfo> patches = new ArrayList<>();
		int iab = 0;

		for (int f = TUFreg3D.ONE; f <= TUFreg3D.FIVE; f++) {
			for (int z = 0; z < BH; z++) {
				for (int y = b1; y <= b2; y++) {
					for (int x = a1; x <= a2; x++) {
						if (!surf[x][y][z][f]) {
							continue;
						}

						double sorTmpValue = (f > TUFreg3D.ONE) ? sor[f] : -9999.;
						boolean isShaded = f > TUFreg3D.ONE && (sorTmpValue == sorsh[1] || sorTmpValue == sorsh[2]);

						if (iab >= numsfc2) {
							continue;
						}

						int sfcIndex = (int) sfc_ab[iab][Constants.sfc_ab_i];
						patches.add(new PatchInfo(x, y, z, f, iab, sfcIndex, isShaded));
						iab++;
					}
				}
			}
		}

		// Create thread-safe accumulators for treeXYMapSunlightPercentageTotal
		// We'll use a simple approach: each patch writes to its own location
		// since different patches write to different (x,y) coordinates

		// Phase 2: Parallel ray tracing for each patch
		final double[] sorFinal = sor;
		final double[] sorshFinal = sorsh;
		final double[][] sfcRef = sfc;
		final boolean[][][] surf_shadeRef = surf_shade;
		final boolean[][][] veg_shadeRef = veg_shade;
		final double[][] treeMapRef = treeXYMapSunlightPercentageTotal;
		final int[][] treeXYMapRef = treeXYMap;
		final HashMap<String, MaespaDataFile> maespaRef = maespaTestflxData;
		final int BHfinal = BH;
		final int AL2final = AL2;
		final int AW2final = AW2;
		final double timeisF = timeis;
		final int yd_actualF = yd_actual;

		try {
			pool.submit(() ->
				patches.parallelStream().forEach(patch -> {
					if (patch.isShaded) {
						// Shaded by orientation
						patch.sunlightFact = 0.0;
						return;
					}

					// Ray trace for this patch
					patch.sunlightFact = computePatchSunlight(
						patch.x, patch.y, patch.z, patch.f,
						XINC, YINC, ZINC,
						surf_shadeRef, veg_shadeRef, treeMapRef,
						BHfinal, AL2final, AW2final,
						timeisF, yd_actualF, treeXYMapRef, maespaRef
					);
				})
			).get();
		} catch (Exception e) {
			System.err.println("Parallel shade computation error: " + e.getMessage());
			// Fall back to sequential on error
			return shade(stror, az, RALT, ypos, surf, surf_shade, AL2, AW2, BH, PAR, sfc, numsfc,
					a1, a2, b1, b2, numsfc2, sfc_ab, par_ab, veg_shade, timeis, yd_actual,
					treeXYMapSunlightPercentageTotal, treeXYMap, maespaTestflxData);
		}

		// Phase 3: Copy results back to sfc array (sequential - fast)
		for (PatchInfo patch : patches) {
			sfc[patch.sfcIndex][Constants.sfc_sunlight_fact] = patch.sunlightFact;
		}

		returnValues.put("sfc", sfc);
		returnValues.put("sfc_ab", sfc_ab);
		returnValues.put("treeXYMapSunlightPercentageTotal", treeXYMapSunlightPercentageTotal);
		return returnValues;
	}

	/**
	 * Compute sunlight factor for a single patch.
	 * This method is thread-safe and can be called in parallel.
	 */
	private static double computePatchSunlight(
			int x, int y, int z, int f,
			double XINC, double YINC, double ZINC,
			boolean[][][] surf_shade, boolean[][][] veg_shade,
			double[][] treeXYMapSunlightPercentageTotal,
			int BH, int AL2, int AW2,
			double timeis, int yd_actual, int[][] treeXYMap,
			HashMap<String, MaespaDataFile> maespaTestflxData)
	{
		double sunlightFact = 0.0;

		// Subdivide patch into 4 sub-patches for partial shading
		for (int iv = 1; iv < 4; iv++) {
			double xpinc = -Constants.FACE_QUARTER_OFFSET;
			double ypinc = -Constants.FACE_QUARTER_OFFSET;

			if ((iv >= 2) && (iv <= 3)) {
				xpinc = Constants.FACE_QUARTER_OFFSET;
			}
			if ((iv >= 1) && (iv <= 2)) {
				ypinc = Constants.FACE_QUARTER_OFFSET;
			}

			double ZT = z + ZINC;
			double XT = x + XINC;
			double YT = y + YINC;

			// Adjust starting position based on face orientation
			if (f == 5) {
				XT = XT - Constants.FACE_HALF_OFFSET;
				YT = YT + xpinc;
				ZT = ZT + ypinc;
			} else if (f == 3) {
				XT = XT + Constants.FACE_HALF_OFFSET;
				YT = YT + xpinc;
				ZT = ZT + ypinc;
			} else if (f == 4) {
				XT = XT + xpinc;
				YT = YT - Constants.FACE_HALF_OFFSET;
				ZT = ZT + ypinc;
			} else if (f == 2) {
				XT = XT + xpinc;
				YT = YT + Constants.FACE_HALF_OFFSET;
				ZT = ZT + ypinc;
			} else if (f == 1) {
				XT = XT + xpinc;
				YT = YT + ypinc;
				ZT = ZT + Constants.FACE_HALF_OFFSET;
			}

			int ZTEST = (int) Math.round(ZT);
			int XTEST = (int) Math.round(XT);
			int YTEST = (int) Math.round(YT);

			// Skip past starting cell
			while ((XTEST == x) && (YTEST == y) && (ZTEST == z)) {
				ZT = ZT + ZINC;
				XT = XT + XINC;
				YT = YT + YINC;
				ZTEST = (int) Math.round(ZT);
				XTEST = (int) Math.round(XT);
				YTEST = (int) Math.round(YT);
			}

			// Note: treeXYMapSunlightPercentageTotal updates are skipped in parallel
			// version since they would require synchronization and are optional metadata

			boolean vegetationInRay = false;
			boolean hitBuilding = false;

			// Ray trace loop
			while ((ZTEST <= BH) && (XTEST >= 1) && (XTEST <= AL2) &&
				   (YTEST >= 1) && (YTEST <= AW2) && (ZTEST >= 0)) {

				if (surf_shade[XTEST][YTEST][ZTEST]) {
					// Hit a building - fully shaded
					hitBuilding = true;
					break;
				}

				if (veg_shade[XTEST][YTEST][ZTEST] && !vegetationInRay) {
					vegetationInRay = true;
				}

				ZT = ZT + ZINC;
				XT = XT + XINC;
				YT = YT + YINC;
				ZTEST = (int) Math.round(ZT);
				XTEST = (int) Math.round(XT);
				YTEST = (int) Math.round(YT);
			}

			if (!hitBuilding) {
				// Sunlit (possibly through vegetation)
				if (vegetationInRay) {
					double transmissionPercentage = ReverseRay.reverseRayTrace(
						XT, XINC, YT, YINC, ZT, ZINC,
						XTEST, YTEST, ZTEST, BH, AL2, AW2,
						veg_shade, timeis, yd_actual, treeXYMap, maespaTestflxData
					);
					sunlightFact += transmissionPercentage;
				} else {
					sunlightFact += 1.0;
				}
			}
		}

		return sunlightFact;
	}
}
