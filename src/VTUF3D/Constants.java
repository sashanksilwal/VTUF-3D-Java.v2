package VTUF3D;

public class Constants
{
	public static final int  parameters_dat_file=299;
	public static final int  inputs_store_out=802;
	public static final int  forcing_dat=843;
//	public static final int  energybalancetsfctimeaverage_out=832;
//	public static final int  tsfcfacetssunshade_out=833;
//	public static final int  tsfcfacets_out=835;
//	public static final int  energybalancefacets_out=836;
//	public static final int  EnergyBalanceOverallOut=837;

//	public static final int  RadiationBalanceFacetsOut=847;
//	public static final int  TsfcSolarSVF_Patch_yd=87 ;
//	public static final int  FLUXES_OUT=901;
//	public static final int  forcing_out=902;

	public static final int  VFINFO_DAT=991;
//	public static final int toMatlab_EnergyBalances = 1001;


	public static final double roof_surface=1;
	public static final double street_surface=2;
	public static final double wall_surface=3;

	public static final double not_in_init_array=0;
	public static final double in_init_input_array=1;
//	public static final double in_area_of_interest=2;

//	public static final int  sfc_surface_type=1;
//	public static final int  sfc_sunlight_fact=2;
//	public static final int  sfc_albedo=3;
//	public static final int  sfc_emiss=4;
//	public static final int  sfc_evf=5;
//	public static final int  sfc_x_vector=6;
//	public static final int  sfc_y_vector=7;
//	public static final int  sfc_z_vector=8;
//	public static final int  sfc_in_array=9;
	public static final int  sfc_x_value_patch_center=10;
	public static final int  sfc_y_value_patch_center=11;
	public static final int  sfc_z_value_patch_center=12;

//	public static final int  sfc_ab_i=1;
//	public static final int  sfc_ab_f=2;
//	public static final int  sfc_ab_z=3;
//	public static final int  sfc_ab_y=4;
//	public static final int  sfc_ab_x=5;
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static final double sigma=5.67e-8;
	public static final int TsfcSolarSVF_Patch_yd=1134;

    public static final int  vfinfoDatRECL=64;
    public static final int  vfRECL = 16;
    public static final int  REAL8LEN = 8;
        

   public static final int  parametersRadiationDat = 299;
   public static final int  inputsStoreOut = 802;
   public static final int  forcingRadiationDat = 981;
   public static final int  vfinfoDat = 991;
   public static final int  EnergyBalanceOverallOut = 837;
   public static final int  RadiationBalanceFacetsOut = 847;
   public static final int  RadiationBalanceSectionalOut = 857;
   public static final int  RadiationBalanceSectionalDayAvgOut = 867;
   public static final int  RadiationBalance_SectionalDayStrorAvgOut = 877;
   public static final int  VerticesToMatlabOut = 92 ;
   public static final int  FacesToMatlabOut = 95;
   public static final int  ToMatlabSVFYdOut = 97 ;   
   public static final int  ToMatlabTbrightYdOut = 98;
   public static final int  ToMatlabKabsYdOut = 99;
   public static final int  ToMatlabKreflYdOut= 96;
   public static final int  ToMatlabKLTotOut= 497;
   public static final int  toMatlab_EnergyBalances = 498;
   
   public static final int  vf1Dat= 228;
//   public static final int TsfcSolarSVF_Patch_yd;
   public static final int FLUXES_OUT = 998877  ; 
   public static final int energybalancetsfctimeaverage_out=832;
  public static final int tsfcfacetssunshade_out=833;
  public static final int tsfcfacets_out=835;
  public static final int energybalancefacets_out=836;
  public static final int forcing_out=843;
   
  public static final int vertices_toMatlab_out = 92;
  public static final int faces_toMatlab_out = 95;
  public static final int toMatlab_Tsfc_yd_out = 97;
   public static final int toMatlab_Tbright_yd_out = 98;
   public static final int toMatlab_Kabs_yd_out = 99;
  public static final int toMatlab_Labs_yd_out = 499;
   public static final int toMatlab_Krefl_yd_out = 96;
  public static final int toMatlab_Lrefl_yd_out = 496;
  public static final int toMatlab_Ldown_yd_out = 500;
  public static final int toMatlab_Tmrt_yd_out = 501;
  public static final int toMatlab_Utci_yd_out = 502;
  public static final int Tsfc_yd_out = 197;
  public static final int Tbright_yd_out = 198;
   
   public static final int  faceup=1;
   public static final int  facenorth=2;
   public static final int  faceeast=3;
   public static final int  facesouth=4;
   public static final int  facewest=5;
   public static final int  facevegetation = 6;
   public static final int  sfc_ab_i=1-1;
   public static final int  sfc_ab_f=2-1;
   public static final int  sfc_ab_z=3-1;
   public static final int  sfc_ab_y=4-1;
   public static final int  sfc_ab_x=5-1;
   public static final int  sfc_ab_layer_temp=6-1;
   
   public static final int sfc_surface_type=1-1;
   public static final int sfc_sunlight_fact=2-1;
   public static final int sfc_albedo=3-1;
   public static final int sfc_emiss=4-1;
   public static final int sfc_evf=5-1;
   public static final int sfc_x_vector=6-1;
   public static final int sfc_y_vector=7-1;
   public static final int sfc_z_vector=8-1;
   public static final int sfc_in_array=9-1;
 
   public static final int  sfc_x=10-1;
   public static final int  sfc_y=11-1;
   public static final int  sfc_z=12-1;
   public static final int  not_in_array=0;
   public static final int  in_initial_input=1;
   public static final int  in_area_of_interest=2;
   public static final int  roof =1;
   public static final int  street=2;
   public static final int  wall=3;
   
   public static final int  vfcalc_false=0;
   public static final int  vfcalc_true=1;
   
   public static final int beginF =1;
   public static final int endF =6;
   
   public static final int testDAYCONST=1;
   public static final int testHRCONST=2;
   public static final int testPTCONST=3;
   public static final int testXCONST=4;
   public static final int testYCONST=5;
   public static final int testZCONST=6;
   public static final int testPARCONST=7;
   public static final int testFBEAMCONST=8;
   public static final int testSUNLACONST=9;
   public static final int testTDCONST=10;
   public static final int testTSCATCONST=11;
   public static final int testTTOTCONST=12;
   public static final int testAPARSUNCONST=13;
   public static final int testAPARSHCONST=14;
   public static final int testAPARCONST=15;
   
   // values from Oke 1998, page 12, table 1.1
   public static final double vegetationAlbedo=0.20;
   public static final double vegetationEmissivity=0.97;
   
   public static final int DIFFERENTIALSHADINGDIFFUSE=1;
   public static final int DIFFERENTIALSHADING100PERCENT=2;
   public static final int NUMOFDIFFERENTIALSHADINGS=2;
   public static final boolean DEBUG_MODE=false;

   // ====== RAY TRACING CONSTANTS ======
   // Increment used for stepping along rays during shade calculation
   public static final double RAY_TRACE_INCREMENT = 0.2;
   // Subdivision factor for face ray tracing (each patch divided into 4 sub-patches)
   public static final double RAY_TRACE_FACE_SUBDIVISION = 0.25;
   // Minimum distance before ray can hit obstacle (prevents self-shading)
   // Slightly longer than center-to-corner distance (1.225)
   public static final double SELF_SHADING_MIN_DISTANCE = 1.225;
   // Ray increment scaling for view factor calculations
   public static final double RAY_INCREMENT_SCALE = 0.25;

   // ====== NEWTON'S METHOD CONSTANTS ======
   // Convergence tolerance for temperature solver
   public static final double NEWTON_CONVERGENCE_TOLERANCE = 0.001;
   // Maximum iterations before adjusting httc
   public static final int NEWTON_MAX_ITERATIONS = 40;
   // Maximum httc adjustment retries
   public static final int NEWTON_MAX_HTTC_RETRIES = 10;
   // Amount to adjust httc when convergence fails
   public static final double NEWTON_HTTC_ADJUSTMENT = 0.5;

   // ====== BISECTION METHOD CONSTANTS ======
   // Tolerance for bisection convergence
   public static final double BISECTION_TOLERANCE = 0.001;

   // ====== REFLECTION LOOP LIMITS ======
   // Maximum number of reflection iterations (longwave/shortwave)
   public static final int MAX_REFLECTION_ITERATIONS = 10;

   // ====== TEMPERATURE CALCULATION CONSTANTS ======
   // Quarter offset for subdividing patches
   public static final double FACE_QUARTER_OFFSET = 0.25;
   // Half offset for face positioning
   public static final double FACE_HALF_OFFSET = 0.5;

   // ====== PARALLEL PROCESSING DEFAULTS ======
   // Minimum patches before parallelization is worthwhile
   public static final int PARALLEL_MIN_PATCHES = 100;


}
