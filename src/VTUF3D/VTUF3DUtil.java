package VTUF3D;

//import java.util.ArrayList;
import java.util.HashMap;

public class VTUF3DUtil
{

  	public static final String HTTC_OUT_INDEX = "httc_out";
  	public static final String FH_INDEX = "Fh";
  	
  	public static final String CZ_INDEX="CZ";	  	
  	public static final String PRESS_INDEX="PRESS";
  	public static final String ZEN_INDEX="ZEN";
  	public static final String AIR_INDEX="AIR";
  	public static final String DEW_INDEX="DEW";
  	public static final String INOT_INDEX="INOT";
  	public static final String DR1_INDEX="DR1";
  	public static final String DF1_INDEX="DF1";
  	public static final String GL1_INDEX="GL1";
  	public static final String CA_INDEX="CA";
  	public static final String JDAY_INDEX="JDAY";
  	public static final String alb_sfc_INDEX="alb_sfc";
  	public static final String cloudtype_INDEX="cloudtype";
  	public static final String abs_aero_INDEX="abs_aero";
  	public static final String Ktotfrc_INDEX="Ktotfrc";
  	public static final String DR1F_INDEX="DR1F";
	
	private static final int httcR = 0;

	public double calcUC(double uc, int numlayers, double[] thickr, double[] lambdaavr, double[] htcapr, double[] thicks, double[] lambdaavs, double[] htcaps,
			double[] thickw, double[] lambdaavw, double[] htcapw, double[] lambdar, double[] lambdas, double[] lambdaw, double deltat)
	{
		double Fourmin;
		double factR;
		int numlayersMinusOne = numlayers - 1 ;

		//!  uc is the explicit/implicit/Crank-Nicholson control (uc=1 is implicit, uc=0 is Forward Euler or explicit)
		//!  For explicit or partially-explicit diffusion, make sure that the
		//!  time step is small enough, using the diffusivity and
		//!  the thicknesses (thick); based on CFL criterion analog, see Jacobson p. 165
       if(uc<=1.0&&uc>=0.0) 
       {
    	   Fourmin=6.*999.;
    	   //! middle layers
	       for (int k=0;k<numlayersMinusOne;k++)
	       {
	        	factR=Math.pow(((thickr[k]+thickr[k+1])/2.),2)/lambdaavr[k]*Math.min(htcapr[k],htcapr[k+1]);
		        if(factR<Fourmin) 
		        {
		        	 Fourmin=factR;
		        }
		        factR=Math.pow(((thicks[k]+thicks[k+1])/2.),2)	 /lambdaavs[k]*Math.min(htcaps[k],htcaps[k+1]);
		        if(factR<Fourmin) 
		        {
		        	 Fourmin=factR;
		        }
		        factR=Math.pow(((thickw[k]+thickw[k+1])/2.),2) /lambdaavw[k]*Math.min(htcapw[k],htcapw[k+1]);
		        if(factR<Fourmin) 
		        {
		        	 Fourmin=factR;
		        }
	       }
	       //! deep half-layer
	       factR=Math.pow((thickr[numlayersMinusOne]/2.),2)/lambdaavr[numlayersMinusOne]*htcapr[numlayersMinusOne];
	       if(factR<Fourmin) 
	       {
        	 Fourmin=factR;
	       }
	       factR=Math.pow((thicks[numlayersMinusOne]/2.),2)/lambdaavs[numlayersMinusOne]*htcaps[numlayersMinusOne];
	       if(factR<Fourmin) 
	       {
        	 Fourmin=factR;
	       }
	       factR=Math.pow((thickw[numlayersMinusOne]/2.),2)/lambdaavw[numlayersMinusOne]*htcapw[numlayersMinusOne];
	       if(factR<Fourmin) 
	       {
        	 Fourmin=factR;
	       }
	       //! surface half-layer
	       factR=Math.pow((thickr[1-1]/2.),2)/lambdar[1-1]*htcapr[1-1];
	       if(factR<Fourmin) 
	       {
	        	 Fourmin=factR;
	       }
	       factR=Math.pow((thicks[1-1]/2.),2)/lambdas[1-1]*htcaps[1-1];
	       if(factR<Fourmin) 
	       {
	        	 Fourmin=factR;
	       }
	       factR=Math.pow((thickw[1-1]/2.),2)/lambdaw[1-1]*htcapw[1-1];
	       if(factR<Fourmin) 
	       {
	        	 Fourmin=factR;
	       }
	        
			//!  so as to not have excessive timesteps with implicit or near
			//!  implicit values of uc, which would still be stable but not
			//!  very temporally accurate (for typical urban parameters):
	        double uc_temp=Math.min(uc,0.9);
	        if(deltat>Fourmin/(1.-uc_temp)/6.) 
	        {
	        	System.out.println("------------------------------------------");
	        	System.out.println("REDUCING TIME STEP FOR STABILITY OR ACCURACY OF THECONDUCTION SCHEME (this is the maximum for the whole simulation)");
	        	System.out.println("old time step ="+" " + deltat+" " + "  new time step = "+" " + Fourmin/(1.-uc_temp)/6.);
	        	deltat=Fourmin/(1.-uc_temp)/6.;
	        }
	       else
	       {
	    	   System.out.println("invalid uc value:"+" " + uc+" " + "must be 0.0-1.0 (inclusive)");
	    	   System.exit(1);
	       }
	   }
	       
       return deltat;
		
	}
	
    public  boolean  ray(int x, int y, int z, int f, int xx, int yy, int zz, int ff, boolean[][][] surf_shade, double[] fx, double[] fy, double[] fz, double[] fxx, double[] fyy, double[] fzz, 
    		int al, int aw, int bh)
    {
    	boolean ray;

//!  function 'ray' determines if surface 1 (vx,vy,vz,f) can see surface 2
//!  (vxx,vyy,vzz,ff) using ray tracing and testing for obstructions
     double vx,vy,vz,vxx,vyy,vzz;
     double xinc,yinc,zinc,xt,yt,zt,mag,dist,inc;
     int xtest,ytest,ztest;

//! patch surface centers:
//!  patch surface i:
         vx = 1.0*x + fx[f-1];
         vy = 1.0*y + fy[f-1];
         vz = 1.0*z + fz[f-1];
//!  patch surface j:
         vxx = 1.0*xx + fxx[ff-1];
         vyy = 1.0*yy + fyy[ff-1];
         vzz = 1.0*zz + fzz[ff-1];

//! these tests will need to be changed for non plane-parallel sfcs
     if (f==ff
    		 ||(f==1&&zz<=z)
    		 ||(f==2&&yy<=y)
    		 ||(f==4&&yy>=y)
    		 ||(f==3&&xx<=x)
    		 ||(f==5&&xx>=x)
    		 ||(ff==1&&z<=zz)
    		 ||(ff==2&&y<=yy)
    		 ||(ff==4&&y>=yy)
    		 ||(ff==3&&x<=xx)
    		 ||(ff==5&&x>=xx))
     {
      ray=false;
//      goto 67;
      	return ray;
     }
     else
     {
      xinc=vxx-vx;
      yinc=vyy-vy;
      zinc=vzz-vz;
      mag=Math.sqrt(Math.pow(xinc,2)+Math.pow(yinc,2)+Math.pow(zinc,2));
      xinc=xinc/mag*Constants.RAY_INCREMENT_SCALE;
      yinc=yinc/mag*Constants.RAY_INCREMENT_SCALE;
      zinc=zinc/mag*Constants.RAY_INCREMENT_SCALE;
      inc=Math.sqrt(Math.pow(xinc,2)+Math.pow(yinc,2)+Math.pow(zinc,2));

      xt=vx+xinc;
      yt=vy+yinc;
      zt=vz+zinc;
      dist=inc;
      xtest=(int)Math.round(xt);
      ytest=(int)Math.round(yt);
      ztest=(int)Math.round(zt);
     

      if(surf_shade[xtest][ytest][ztest])
      {           
          if (x==xtest && y==ytest && z==ztest) 
          {
              //!print *,'ignoring cell not empty because test values are the same',xtest,ytest,ztest,surf_shade[xtest][ytest][ztest],surf_shade[x][y][z],x,y,z,f
          }
          else  
          {
        	  System.out.println("problem: cell not empty that should be");
             System.out.println(xt+" "+yt+" "+zt+" "+xtest+" "+ytest+" "+ztest+" "+vx+" "+vy+" "+vz+" "+x+" "+y+" "+z+" "+f+" "+xinc+" "+yinc+" "+zinc+" "+surf_shade[xtest][ytest][ztest]+" "+surf_shade[x][y][z]);
             System.exit(1);
          }
      }

      while (dist<mag)
      {
       if(surf_shade[xtest][ytest][ztest])
       {
         ray=false;
//         goto 67;
         return ray;
       }
       else
       {
        xt=xt+xinc;
        yt=yt+yinc;
        zt=zt+zinc;
        dist=dist+inc;
        xtest=(int)Math.round(xt);
        ytest=(int)Math.round(yt);
        ztest=(int)Math.round(zt);
       }
//302   continue
      }

      ray=true;

     }

//67   continue;

     return ray;
    }


     

	
	//!------------------------------------------------------------------
	//! conversion to radians

	  public double ANGRAD(double DEG)
	  {
		  double ANGRAD=DEG*1745.3293E-5;
	
		  return ANGRAD;
	  }
	      
  	public double SFC_RI(double dz, double Thi, double Tlo, double Uhi)
  	{

  		double Ri;

  //! correct for lapse rate (equivalent to using theta values for
  //! Thi and Tlo for T difference)
      double Tcorrhi=Thi+9.806/1004.67*dz;

      Ri=9.806*dz*(Tcorrhi-Tlo)*2./(Thi+Tlo)/Math.pow((Math.max(Uhi,1e-3)),2);

      return Ri;
  	}
  	public HashMap<String,Double> HTC(double Ri,double u,double z,double z0m,double z0h)
  	{
  		//!---------------------------------------------------------
  		//! Mascart (1995) BLM heat and momentum transfer coefficients

  		double httc_out;
  		double Fh;
  		//! inputs
  	      //double Ri,u,z,z0m,z0h;
  		//! outputs
  	      //double httc_out,Fh;
  		//! other variables
  	      double R,mu,Cstarh,ph,lnzz0m,lnzz0h,aa,Ch;
  	      
  	    HashMap<String,Double> returnValues = new HashMap<String,Double>();

  	    //!print *,'sub htc Ri,u,z,z0m,z0h,httc_out,Fh',Ri,u,z,z0m,z0h,httc_out,Fh
  	    //! from Louis (1979):
  	      R=0.74;

  	      //! checks: Mascart procedure not so good if these to conditions
  	      //! are not met (i.e. z0m/z0h must be between 1 and 200)
  	      z0h=Math.max(z0m/200.,z0h);
  	      mu=Math.max(0.,Math.log(z0m/z0h));

  	      Cstarh=3.2165+4.3431*mu+0.536*Math.pow(mu,2)-0.0781*Math.pow(mu,3);
  	      ph=0.5802-0.1571*mu+0.0327*Math.pow(mu,2)-0.0026*Math.pow(mu,3);

  	      lnzz0m=Math.log(z/z0m) ;     
  	      lnzz0h=Math.log(z/z0h);
  	      aa=Math.pow((0.4/lnzz0m),2);

  	      Ch=Cstarh*aa*9.4*(lnzz0m/lnzz0h)*Math.pow((z/z0h),ph);
  	      if(Ri>0.) 
  	      {
  	       Fh=lnzz0m/lnzz0h*Math.pow((1.+4.7*Ri),(-2));
  	      }
  	      else
  	      {
  	       Fh=lnzz0m/lnzz0h*(1.-9.4*Ri/(1.+Ch*Math.pow((Math.abs(Ri)),(0.5))));
  	      }

  	      httc_out=u*aa/R*Fh;
  	   
	  	  returnValues.put(HTTC_OUT_INDEX, httc_out);
  	      returnValues.put(FH_INDEX, Fh);


  	      return returnValues;
  	}
	  	


	  	public static HashMap<String,Double> CD(double Ri,double z,double z0m,double z0h)
	  	{

	  	//! inputs
	  		//double Ri,z,z0m,z0h;
	  	//! outputs
	  		//double cd_out,Fm;
	  	//! other variables
	  		double mu,Cstarm,pm,lnzz0m,aa,Cm;
	  		double Fm;
	  		double cd_out;
	  		HashMap<String,Double> returnValues = new HashMap<String,Double>();


	  	//! checks: Mascart procedure not so good if these to conditions
	  	//! are not met (i.e. z0m/z0h must be between 1 and 200)
	  	      z0h=Math.max(z0m/200.,z0h);
	  	      mu=Math.max(0.,Math.log(z0m/z0h));

	  	      Cstarm=6.8741+2.6933*mu-0.3601*Math.pow(mu,2)+0.0154*Math.pow(mu,3);
	  	      pm=0.5233-0.0815*mu+0.0135*Math.pow(mu,2)-0.001*Math.pow(mu,3);

	  	      lnzz0m=Math.log(z/z0m);
	  	      aa=Math.pow((0.4/(lnzz0m)),2) ;

	  	      Cm=Cstarm*aa*9.4*Math.pow((z/z0m),pm);

	  	      if(Ri>0.) 
	  	      {
	  	       Fm=Math.pow( (1.+4.7*Ri),(-2)) ;
	  	      }
	  	      else
	  	      {
	  	       Fm=1.-9.4*Ri/(1.+Cm*Math.pow((Math.abs(Ri)),(0.5)));
	  	      }

	  	      cd_out=aa*Fm;

	  	      returnValues.put("Fm", Fm);
	  	    returnValues.put("cd_out", cd_out);
	  	      return returnValues;
	  	 
	  	}
	  	

	  	
		//!------------------------------------------------------
		//!
		//!     SUBROUTINE CLRSKY DETERMINES THE SOLAR RADIATION RECEIVED AT THE
		//!     SURFACE UNDER CLEAR SKY CONDITIONS (clouds have been added)
		//!
		      public HashMap<String,Double> CLRSKY(double CZ,double PRESS,double ZEN,double AIR,double DEW,double INOT,double DR1,double DF1,
		    		  double GL1,double CA,int JDAY,double alb_sfc,int cloudtype,double abs_aero,double Ktotfrc,double DR1F)

		      {
		    	  HashMap<String,Double> returnValues = new HashMap<String,Double>();
		  		  returnValues.put(CZ_INDEX, CZ);
				    returnValues.put(PRESS_INDEX, PRESS*10);
				    returnValues.put(ZEN_INDEX, ZEN);
				    returnValues.put(AIR_INDEX, AIR);
				    returnValues.put(DEW_INDEX, DEW);
				    returnValues.put(INOT_INDEX, INOT);
				    returnValues.put(DR1_INDEX, DR1);
				    returnValues.put(DF1_INDEX, DF1);
				    returnValues.put(GL1_INDEX, GL1);
				    returnValues.put(CA_INDEX, CA);
//				    returnValues.put(JDAY_INDEX, JDAY);
				    returnValues.put(alb_sfc_INDEX, alb_sfc);
//				    returnValues.put(cloudtype_INDEX, cloudtype);
				    returnValues.put(abs_aero_INDEX, abs_aero);
				    returnValues.put(Ktotfrc_INDEX, Ktotfrc);
				    returnValues.put(DR1F_INDEX, DR1F);

		      double PI,ZEND,TR,AHAT,UW,AW,TA,WO,BAA,ALPHAB,XO,TO,M;
		      double DRR,DA,DS;

		      double DF1F,I_I0F;

		      double DR1_save,zendiff,weight,AO;
		      double visib,TAprime,ozone,MR,I_I0,TCL =0;

		      PI=Math.PI;

		      DR1=0.;
		      DF1=0.;
		      GL1=0.;
		      abs_aero=0.;

		//! this is the cloud transmissivity//!!! (Haurwitz, 1948), in Atwater and
		//! Brown JAM March 1974 (all assumed at 100% cloud cover)
		      if(cloudtype == 0) 
		      {
		//! clear: (also affects ALPHAB (below) and comment out Orgill and
		//!  Hollands (below)
		       TCL=1.;
		      }
		      else if(cloudtype == 1) 
		      {
		//! cirrus:
		       TCL=0.8717-0.0179*(PRESS/(101.325*CZ));
		      }
		      else if(cloudtype == 2) 
		      {
		//! cirrostratus:
		       TCL=0.9055-0.0638*(PRESS/(101.325*CZ));
		      }
		      else if(cloudtype == 3) 
		      {
		//! altocumulus:
		       TCL=0.5456-0.0236*(PRESS/(101.325*CZ));
		      }
		      else if(cloudtype == 4) 
		      {
		//! altostratus:
		       TCL=0.4130-0.0014*(PRESS/(101.325*CZ));
		      }
		      else if(cloudtype == 7) 
		      {
		//! cumulonimbus:
		       TCL=0.2363+0.0145*(PRESS/(101.325*CZ));
		      }
		      else if(cloudtype == 5) 
		      {
		//! stratocumulus/cumulus:
		       TCL=0.3658-0.0149*(PRESS/(101.325*CZ));
		      }
		      else if(cloudtype == 6) 
		      {
		//! thick stratus (Ns?):
		       TCL=0.2684-0.0101*(PRESS/(101.325*CZ));
		      }
		      else
		      {
		      System.out.println("cloudtype must be between 0 and 7, cloudtype = " + " " + cloudtype);
		      }

		      ZEND=ZEN*180/PI;
		//!CC see Iqbal p. 175-195 for much of this parameterization
		//!      M repr. thickness of atm. through which solar beam travels(rel. mass)
		      if(ZEND >=90.0) 
		      {
//		    	  goto 945
		    	  return returnValues;
		      }
		      MR=1./(CZ+(0.15*(Math.pow((93.885-ZEND),(-1.253)))));
		      if(MR < 1.) 
		      {
		    	  System.out.println("problem with solar routine (MR>0), MR = " + " " + MR);
		    	  if (MR > 0.99)
		    	  {
		    		  MR = 1.0;
		    	  }
		    	  else
		    	  {
//			    	  System.out.println("problem with solar routine (MR>0), MR = " + " " + MR);
			    	  System.exit(1); 
		    	  }
		    	  

		      }
		      M=(PRESS/101.325)*MR;
		//!     TR is the transmissivity due to Rayleigh scattering

		//! SO THAT SMALL SUN ELEVATION ANGLES DON'T GIVE LARGE KDIR FOR VERTICAL
		//! SURFACES DUE TO TR INCREASING ABOVE 1 FOR ZENITH ANGLES APPROACHING 90
		      TR=Math.exp(-0.0903*(Math.pow(M,0.84))*(1.+M-Math.pow(M,(1.0074))));
		      if(TR > 1.0) 
		      {
		       System.out.println("TR > 1,TR,M,MR,ZEND=" + " " + TR + " " + M + " " + MR + " " + ZEND);
		       System.exit(1);
		      }

		      AHAT=2.2572;
		//!     UW is a water vapour factor...DEW is dewpoint in Celsius
		      UW=0.1*Math.exp((0.05454*DEW)+AHAT);
		      double X2=UW*MR*(Math.pow((PRESS/101.325),0.75))*(Math.pow((273.15/(273.15+AIR)),0.5));
		      AW=(2.9*X2)/((Math.pow((1.+141.5*X2),0.635))+5.925*X2);

		//!  visibility in km (70=clean,20=turbid, 6=very turbid)
		      visib=20.;
		      TA=Math.pow((0.97-1.265*Math.pow(visib,(-0.66))),(Math.pow(M,0.9)));

		//!  the following value is fraction scattered (vs absorbed)
		//!  (0.9 for rural/agricultural,0.6 for cities)
		      WO=0.70;

		//!  for multiple reflection of diffuse, assume rel. air mass 1.66
		      TAprime=Math.pow((0.97-1.265*Math.pow(visib,(-0.66))),(Math.pow((1.66),(0.9)))) ;

		//!  ratio of forward scattering
		      BAA=-0.3012*Math.pow(CZ,2)		    		  
		    		  +0.7368*CZ+0.4877;
		      ALPHAB=Math.max(0.0685+(1.-TCL),0.0685+(1.-BAA)*(1.-TAprime)*WO);

		//!  ozone (in cm) from Table in Iqbal p.89
		//!  first is for lat=40, second for lat=50
		      ozone=1.595e-8*Math.pow(JDAY,3)		    		  
		    		  -0.9642e-5*Math.pow(JDAY,2)		    		  
		    		  +0.001458*JDAY+0.2754;
		//!      ozone=2.266e-8*jday**3-1.347e-5*jday**2+0.001958*jday+0.2957
		      XO=10.*ozone*MR;
		      AO=((0.1082*XO)/(Math.pow((1.+13.86*XO),0.805)		    		  
		    		  ))+(0.00658*XO)/(1.+Math.pow((10.36*XO),3)		    		  
		    		  )+(0.002118*XO)/(1.+(0.0042*XO)+(0.0000323*XO*XO));
		      TO=1.-AO;

		//!
		//!     CALCULATE DIRECT, DIFFUSE, AND GLOBAL RADIATION
		//!
		//!  direct
		      DR1=INOT*CZ*(TO*TR-AW)*TA*TCL;
		//!  contributions to diffuse
		      DRR=INOT*CZ*TO*(TA/2.0)*(1.-TR)*TCL;
		      DA=INOT*CZ*WO*BAA*(1.-TA)*(TO*TR-AW)*TCL;
		//!  diffuse due to multiple reflection between the surface and the atmosphere
		      DS=alb_sfc*ALPHAB*(DR1+DRR+DA)/(1.-alb_sfc*ALPHAB);
		//!  total incident diffuse
		      DF1=DRR+DA+DS;
		//!  aerosol absorption
		      abs_aero=INOT*CZ*(TO*TR-AW)*(1.-TA)*(1.-WO);

		    DR1_save=DR1;

		      if(DR1 < 0.0 || DR1 > 1400.)
		      {
		       DR1=0.;
		       abs_aero=0.;
		      }
		      if(DF1 < 0.0 || DF1 > 1400.)
		      {
		       DF1=0.;
		       abs_aero=0.;
		      }

		      GL1=DR1+DF1;
		      if(cloudtype > 0)
		      {
		//!  Orgill and Hollands correlation taken from Iqbal p.269
		       if(INOT*CZ <= 0.)
		       {
//		        goto 47
		    	   return returnValues;
		       }
		       else
		       {
		        I_I0=GL1/(INOT*CZ);
		       }
		//!  This is a polynomial fit to Orgill and Hollands
		       if(I_I0 >=0.85)
		       {
		        DF1=0.159873*GL1;
		       }
		       else
		       {
		      DF1=GL1*(-27.59*Math.pow(I_I0,6)		    		  
		    		  +66.449*Math.pow(I_I0,5)		    		  
		    		  -48.232*Math.pow(I_I0,4)		    		  
		    		  +7.7246*Math.pow(I_I0,3)		    		  
		    		  +1.3433*Math.pow(I_I0,2)		    		  
		    		  -0.5357*I_I0+1.);
		       }
		       DR1=GL1-DF1;
		      }

		//!  Orgill and Hollands changes DR1, which can cause problems later when we
		//!  find the beam solar flux density (perpendicular to the incoming solar)
		//!  from the direct solar flux density (perpendicular to a horizontal surface);
		//!  Therefore, for small solar elevation angles we use DR1 as originally
		//!  calculated above (before the Orgill and Hollands parameterization), and
		//!  we interpolate between the two so that they match at solar elevation of 10 degrees
		      if(ZEND > 85.0) 
		      {
		       DR1=Math.min(DR1_save,GL1);
		       DF1=GL1-DR1;
		      }
		      else if(ZEND > 80.0 && ZEND <= 85.) 
		      {
		       zendiff=85.-ZEND;
		       weight=1.-Math.sqrt(1.-zendiff*zendiff/25.);
		       DR1=Math.min(DR1*weight+DR1_save*(1.-weight),GL1);
		       DF1=GL1-DR1;
		      }
		      else
		      {
		//!      should be fine as is
		      }


		//!  Now Orgill and Hollands correlation for observed (forcing) radiation
		    if (Ktotfrc > 0.) 
		    { 
		       if(INOT*CZ <= 0.)
		       {
//		        goto 47
		    	   return returnValues;
		       }
		       else
		       {
		        I_I0F=Ktotfrc/(INOT*CZ);
		       }
		//!  This is a polynomial fit to Orgill and Hollands
		       if(I_I0F >=0.85)
		       {
		        DF1F=0.159873*Ktotfrc;
		       }
		       else
		       {
		    	   DF1F=Ktotfrc*(-27.59*Math.pow(I_I0F,6)		    			   
		    			   +66.449*Math.pow(I_I0F,5)		    			   
		    			   -48.232*Math.pow(I_I0F,4)		    			   
		    			   +7.7246*Math.pow(I_I0F,3)		    			   
		    			   +1.3433*Math.pow(I_I0F,2)		    			   
		    			   -0.5357*I_I0F+1.);
		       }
		       DR1F=Ktotfrc-DF1F;
		    }


//		47   continue
//		945  continue
		    
		    
  		
		    
		    returnValues.put(CZ_INDEX, CZ);
		    returnValues.put(PRESS_INDEX, PRESS*10);
		    returnValues.put(ZEN_INDEX, ZEN);
		    returnValues.put(AIR_INDEX, AIR);
		    returnValues.put(DEW_INDEX, DEW);
		    returnValues.put(INOT_INDEX, INOT);
		    returnValues.put(DR1_INDEX, DR1);
		    returnValues.put(DF1_INDEX, DF1);
		    returnValues.put(GL1_INDEX, GL1);
		    returnValues.put(CA_INDEX, CA);
//			    returnValues.put(JDAY_INDEX, JDAY);
		    returnValues.put(alb_sfc_INDEX, alb_sfc);
//			    returnValues.put(cloudtype_INDEX, cloudtype);
		    returnValues.put(abs_aero_INDEX, abs_aero);
		    returnValues.put(Ktotfrc_INDEX, Ktotfrc);
		    returnValues.put(DR1F_INDEX, DR1F);
  		  

		return returnValues;
		

		

	}
	
		      
		  	//!------------------------------------------------------------------
		      public HashMap<String,Double> SUNPOS(int JDAY, double TM, double LAT)

		      {
		    	  double ZEN;
		    	  double AZIM;
		    	  double CZ;
		    	  double INOT;
		    	  double CA;
		    	  HashMap<String,Double> returnValues = new HashMap<String,Double>();

		      double DEC,HL,THETA,PI,THETA_INOT;
		    boolean SH;
		      double HR_RAD;
		      PI=Math.PI;
		    SH=false;
		     HR_RAD=15.*PI/180.;
		      THETA = (JDAY-1)*(2.*PI)/365.;
		      THETA_INOT=THETA;
		//! for southern hemisphere:
		    if(LAT < 0) 
		    {
		    	THETA=( (THETA+PI) % (2.*PI) );
		     SH=true;
		     LAT=Math.abs(LAT);
		    } 
		//! declination angle
		      DEC = 0.006918-0.399912*Math.cos(THETA)+0.070257*Math.sin(THETA)-0.006758*Math.cos(2*THETA)+0.000907*Math.sin(2*THETA)-0.002697*Math.cos(3*THETA)+0.00148*Math.sin(3*THETA);

		//! all the changes are from Stull, Meteorology for Scientists and
		//! Engineers 2000 - NOTE: the current definition of HL give the solar
		//! position based on local mean solar time - to have solar position as
		//! as function of standard time in the time zone, must use Stull's
		//! equation 2.9 on p. 26
		      HL=TM*HR_RAD;
		//! cos(solar zenith)
		      CZ = (Math.sin(LAT)*Math.sin(DEC))-(Math.cos(LAT)*Math.cos(DEC)*Math.cos(HL));
		//! solar zenith
		      ZEN=Math.acos(CZ);
		//! cos(azimuth angle)
		      CA = Math.max(-1.,Math.min(1.,(Math.sin(DEC)-Math.sin(LAT)*CZ)/(Math.cos(LAT)*Math.sin(ZEN))));
		//! azimuth angle
		      AZIM=Math.acos(CA);
		      if(TM > 12.) 
		      {
		    	  AZIM=2.*PI-AZIM;
		      }
		//! southern hemisphere:
		      if(SH) 
		      {
		     if(AZIM <= PI) 
		     {
		    	 AZIM=PI-AZIM;
		     }
		     else
		     {
		    	 AZIM=3.*PI-AZIM;
		     }
		       LAT=-LAT;
		      }
		//! incoming flux density based on solar geometry (no atmosphere yet)
		      INOT=1365.*(1.0001+0.034221*Math.cos(THETA_INOT)+0.001280*Math.sin(THETA_INOT)+0.000719*Math.cos(2.*THETA_INOT)+0.000077*Math.sin(2.*THETA_INOT));
		      
		      returnValues.put("ZEN", ZEN);
		      returnValues.put("AZIM", AZIM);
		      returnValues.put("CZ", CZ);
		      returnValues.put("INOT", INOT);
		      returnValues.put("CA", CA);
		      
		      return returnValues;

	}
		      



	// !------------------------------------------------------------------
	// ! view factor between two identical, parallel rectangular surfaces
	// ! of dimensions x by z, separated by a distance y; the surfaces
	// ! directly oppose each other
	// ! this is Lin Wu's F1
	public double pll(double xa, double ya, double za)
	{
		double pll;
		double x, y, z, pi;
		// double xa,ya,za; //!! KN use local variables to fix crash on
		// reassigning constants

		x = Math.abs(xa);
		y = Math.abs(ya);
		z = Math.abs(za);
		if (x == 0.0 || y == 0.0 || z == 0.0)
		{
			// goto 134
			pll = 0.0;
			System.out.println("x or y or z is 0 in pll " + " " + x + " " + y + " " + z);
		}
		else
		{

			pi = 4.0 * Math.atan(1.0);
			// ! Hottel / Sparrow and Cess
//			pll = 2.0 / (x * z * pi) * (x * Math.pow((z * z + y * y), (0.5))
//					* Math.atan(x / Math.pow((z * z + y * y), (0.5)))
//					+ z * Math.pow((x * x + y * y), (0.5)) * Math.atan(z / Math.pow((x * x + y * y), (0.5))
//							- x * y * Math.atan(x / y) - z * y * Math.atan(z / y) + y * y / 2.0
//									* Math.log((x * x + y * y) * (z * z + y * y) / (y * y * (x * x + z * z + y * y)))));
			
			double factor1 = Math.pow((x*x+y*y),(0.5));
			double factor2 = Math.pow((z*z+y*y),(0.5));
			double factor3 = Math.log((x*x+y*y)*(z*z+y*y)/(y*y*(x*x+z*z+y*y)));
			pll = 2.0/(x*z*pi) * ( x*
					factor2
					*Math.atan(x/
							factor2
							)+z*
					factor1
					*Math.atan(z/
							factor1
							)-x*y*Math.atan(x/y)-z*y*Math.atan(z/y)+y*y/2.0*
					factor3 );

			if (pll < 0.0)
			{
				System.out.println("pll<0 " + " " + pll + " " + x + " " + y + " " + z);
			}
			if (pll > 1.0)
			{
				System.out.println("pll>1 " + " " + pll + " " + x + " " + y + " " + z);
			}
			pll = Math.max(Math.min(pll, 1.0), 0.0);

			// goto 135
		}
		// 134 continue
		// pll=0.0;
		//// write(6,*)'x or y or z is 0 in pll',x,y,z;
		// System.out.println("x or y or z is 0 in pll " + " " + x + " " + y + "
		// " + z);
		// 135 continue

		return pll;
	}


				//!------------------------------------------------------------------
				//!  this is Lin Wu's F2: two parallel faces, one is x by z1, looking at
				//!  the other (x by z2), and sharing a common boundary in the z
				//!  dimension; y is the separation distance
				      public double F2(double x,double y,double z1,double z2)
				      {
//				       double x,y,z1,z2;
				       //double pll;

				       x=Math.abs(x);
				       y=Math.abs(y);
				       z1=Math.abs(z1);
				       z2=Math.abs(z2);

				      double F2 = 1.0/2.0/x/z1* ( x*(z1+z2)*pll(x,y,(z1+z2))- x*z1*pll(x,y,z1)- x*z2*pll(x,y,z2) );

				      return F2;
				      }


				//!------------------------------------------------------------------
				//!  this is Lin Wu's F3: two parallel faces, one is x by z1, looking at
				//!  the other (x by z3), not sharing a common boundary in the z
				//!  dimension (separated by z2), but still in the same x dimension;
				//!  y is the separation distance
				      public double F3(double xa,double ya,double z1a,double z2a,double z3a)
				      {
				       double x,y,z1,z2,z3;
//				       double F2;
				       //double pll;
				       double F3;
//				       double xa,ya,z1a,z2a,z3a; //!! need fresh variables so passing a constant doesn't crash on reassign

				       x=Math.abs(xa);
				       y=Math.abs(ya);
				       z1=Math.abs(z1a);
				       z2=Math.abs(z2a);
				       z3=Math.abs(z3a);

				       if(z2 == 0.) 
				       {
				    	   F3 = F2 (x,y,z1,z3);
				       }
				       else
				       {
				    	   F3 = (z1+z2+z3)/z1*pll(x,y,(z1+z2+z3)) - pll(x,y,z1)- F2(x,y,z1,z2) - (z2+z3)/z1* ( pll(x,y,(z2+z3))+ F2(x,y,(z2+z3),z1) );
				       }
				       
				      return F3;
				      }

				//!------------------------------------------------------------------
				//!  this is Lin Wu's F4: two parallel faces, one is x1 by z1, looking at
				//!  the other (x2 by z2), sharing a common corner in the z and x
				//!  dimensions; y is the separation distance
				     public double F4(double x1,double x2,double y,double z1,double z2)
				      {
//				       double x1,x2,y,z1,z2;
//				       double pll,F2;

				       x1=Math.abs(x1);
				       x2=Math.abs(x2);
				       y=Math.abs(y);
				       z1=Math.abs(z1);
				       z2=Math.abs(z2);

				      double F4 = 1.0/2.0/x1/z1* ( (x1+x2)*(z1+z2)*pll((x1+x2),y,(z1+z2))- (x1+x2)*z1*pll((x1+x2),y,z1)- 
				    		  (x1+x2)*z2*pll((x1+x2),y,z2)- (x1+x2)*z2*F2((x1+x2),y,z2,z1)- x1*z1*F2(x1,y,z1,z2)- x2*z1*F2(x2,y,z1,z2) );

				      return F4;
				      }

	
				//!------------------------------------------------------------------
				//!  this is Lin Wu's F5: two parallel faces, one is x1 by z1, looking at
				//!  the other (x3 by z3), sharing no dimensions in common, x2 and z2 are
				//!  their separation distances in the x and z dimensions; y is the
				//!  separation distance
				     public double F5(double x1a,double x2a,double x3a,double ya,double z1a,double z2a,double z3a)
				      {
				       double x1,x2,x3,y,z1,z2,z3,pt2,pt3,pt4,F4;
				       double F5;
//				       double x1a,x2a,x3a,ya,z1a,z2a,z3a; //!! need local variables so passing a constant doesn't crash on reassign

				       x1=Math.abs(x1a);
				       x2=Math.abs(x2a);
				       x3=Math.abs(x3a);
				       y=Math.abs(ya);
				       z1=Math.abs(z1a);
				       z2=Math.abs(z2a);
				       z3=Math.abs(z3a);

				       pt2 = 0.0;
				       pt3 = 0.0;
				       pt4 = 0.0;

				       if(x2 > 0.1) 
				       {
				    	   pt3=F4(x1,x2,y,z1,(z2+z3));
				       }
				       if(z2 > 0.1) 
				       {
				    	   pt2=F4(x1,(x2+x3),y,z1,z2);
				       }
				       if(x2 > 0.1 && z2 > 0.1) 
				       {
				    	   pt4=F4(x1,x2,y,z1,z2);
				       }

				       F5 = F4(x1,(x2+x3),y,z1,(z2+z3)) - pt2 - pt3 + pt4;

				      return F5;
				      }


				//!------------------------------------------------------------------
				//!  view factor from surface 1 of length y perpendicular to surface 2
				//!  of length z, sharing a common edge of length x
				     public double per(double x,double y,double z)
				      {
				    	 double per;
//				       double x,y,z;
				       double pi,W,H;

				       x=Math.abs(x);
				       y=Math.abs(y);
				       z=Math.abs(z);
				       if(x == 0.0 || y == 0.0 || z == 0.0)
				       {
//				    	   goto 136
						      per=0.0;
//						      write(6,*)'x or y or z is 0 in per',x,y,z;
						      System.out.println("x or y or z is 0 in per " + " " + x + " " + y + " " + z);
				       }
				       else
				       {

				       W=y/x;
				       H=z/x;

				       pi = 4.0 * Math.atan(1.0);
				 
				//!  Siegel and Howell / Modest
//				      per=1.0/pi/W*(W*Math.atan(1.0/W)+H*Math.atan(1.0/H)- Math.pow((H*H+W*W),(0.5))				    		  
//				    		  *Math.atan(1.0/Math.sqrt(H*H+W*W))+ 1.0/4.0*Math.log((1.0+W*W)*(1.0+H*H)/(1.0+W*W+H*H)*
//				    		Math.pow((W*W*(1.0+W*W+H*H)/(1.0+W*W)/(W*W+H*H)),(W*W))	*Math.pow((H*H*(1.0+H*H+W*W)/(1.0+H*H)/(H*H+W*W)),(H*H))));
				      
				      per = 1.0/pi/W*(W*Math.atan(1.0/W)+H*Math.atan(1.0/H)- Math.pow((H*H+W*W),(0.5))
//				    		  (H*H+W*W)**(0.5)
				    		  *Math.atan(1.0/Math.sqrt(H*H+W*W))+ 1.0/4.0*Math.log((1.0+W*W)*(1.0+H*H)/(1.0+W*W+H*H)*
				    				  Math.pow((W*W*(1.0+W*W+H*H)/(1.0+W*W)/(W*W+H*H)),(W*W))				    				  
//				    				  (W*W*(1.0+W*W+H*H)/(1.0+W*W)/(W*W+H*H))**(W*W)
				    				  *
				    				  Math.pow((H*H*(1.0+H*H+W*W)/(1.0+H*H)/(H*H+W*W)),(H*H))
//				    				  (H*H*(1.0+H*H+W*W)/(1.0+H*H)/(H*H+W*W))**(H*H)
				    				  ));

				       if(per < 0.0)
				       {				    	
				    	   System.out.println("per<0 " + " " + per + " " + H + " " + W + " " + x + " " + y + " " + z);
				       }
				       if(per > 1.0)
				       {
//				    	   write(6,*)'per>1',per,H,W,x,y,z;
				    	   System.out.println("per>1 " + " " + per + " " + H + " " + W + " " + x + " " + y + " " + z);
				       }
				       per=Math.max(Math.min(per,1.0),0.0);

//				       goto 137
				       }
//				136   continue
//				      per=0.0;
////				      write(6,*)'x or y or z is 0 in per',x,y,z;
//				      System.out.println("x or y or z is 0 in per " + " " + x + " " + y + " " + z);
//				137   continue

				      return per;
				      }

				//!------------------------------------------------------------------
				//!  Lin Wu's F7 (modified)
				//!  view factor from surface 1 of length y2 perpendicular to surface 2
				//!  of length z2, separated in the y dimension by y1, and in the z
				//!  dimension by z1; both surfaces are aligned in the x dimension, and
				//!  are have width x
				     public double F7(double xa,double y1a,double y2a,double z1a,double z2a)
				      {
				       double x,y1,y2,z1,z2,per;
//				       double xa,y1a,y2a,z1a,z2a; //!! adding these because if you pass a constant value, the function crashes trying to reassign it

				       x=Math.abs(xa);
				       y1=Math.abs(y1a);
				       y2=Math.abs(y2a);
				       z1=Math.abs(z1a);
				       z2=Math.abs(z2a);
				       double F7;

				       if(y1*z1 == 0.) 
				       {
				        F7 = x*(y1+y2)*per(x,y1+y2,z1+z2);
				        if(y1 != 0.0) 
				        {
				        	
				        	F7 = F7 - x*y1*per(x,y1,z1+z2);
				        }
				        if(z1 != 0.0) 
				        {
				        	F7 = F7 - x*(y1+y2)*per(x,y1+y2,z1);
				        }
				        F7 = F7 * 1.0/x/y2;
				        }
				       else
				       {
				    	   F7 = 1.0/x/y2 * (x*(y1+y2)*per(x,y1+y2,z1+z2)+ x*y1*per(x,y1,z1) - x*(y1+y2)*per(x,y1+y2,z1)- x*y1*per(x,y1,z1+z2));
				       }

				      return F7;
				      }

				//!------------------------------------------------------------------
				//!  Lin Wu's F8
				//!  view factor from surface 1 of length y and width x1 perpendicular
				//!  to surface 2 of length z and width x2, with corners touching in the
				//!  x dimension
				     public double F8(double x1,double x2,double y,double z)
				      {
//				       double x1,x2,y,z;
//				       double per;

				       x1=Math.abs(x1);
				       x2=Math.abs(x2);
				       y=Math.abs(y);
				       z=Math.abs(z);

				      double F8 = 1.0/2.0/x1/y * ((x1+x2)*y*per(x1+x2,y,z)- x1*y*per(x1,y,z)- x2*y*per(x2,y,z));

				      return F8;
				      }

				//!------------------------------------------------------------------
				//!  Lin Wu's F9
				//!  view factor from surface 1 of length y2 and width x1 perpendicular
				//!  to surface 2 of length z2 and width x3, separate in the x,y, and z
				//!  dimensions by x2, y1, and z1, respectively
				     public double F9(double x1a,double x2a,double x3a,double y1a,double y2a,double z1a,double z2a)
				      {
				       double x1,x2,x3,y1,y2,z1,z2,F8;
				       
				       double F9;
				       //!! KN using local variables, otherwise abs() seems to crash
//				       double x1a,x2a,x3a,y1a,y2a,z1a,z2a;

				       x1=Math.abs(x1a);
				       x2=Math.abs(x2a);
				       x3=Math.abs(x3a);
				       y1=Math.abs(y1a);
				       y2=Math.abs(y2a);
				       z1=Math.abs(z1a);
				       z2=Math.abs(z2a);

				//! to get rid of terms that cause problems if one or more of z1,y1,x2
				//! are zero (otherwise nan's generated by 'per' aren't eliminated)

				       if(y1*z1*x2 == 0.0) 
				       {
				        F9=(y1+y2)/y2 * F8(x1,x2+x3,y1+y2,z1+z2);
				        if(y1 != 0.0) 
				        {
				        	F9 = F9 - y1/y2 * F8(x1,x2+x3,y1,z1+z2);
				        }
				        if(z1 != 0.0) 
				        {
				        	F9 = F9 - (y1+y2)/y2 * F8(x1,x2+x3,y1+y2,z1);
				        }
				        if(x2 != 0.0) 
				        {
				        	F9 = F9 - (y1+y2)/y2 * F8(x1,x2,y1+y2,z1+z2);
				        }
				      if(y1 != 0.0 && z1 != 0.0) 
				      {
				    	  F9 = F9 + y1/y2 * F8(x1,x2+x3,y1,z1);
				      }
				      if(y1 != 0.0 && x2 != 0.0) 
				      {
				    	  F9 = F9 + y1/y2 * F8(x1,x2,y1,z1+z2);
				      }
				      if(x2 != 0.0 && z1 != 0.0) 
				      {
				    	  F9 = F9 + (y1+y2)/y2* F8(x1,x2,y1+y2,z1);
				      }
				       }
				       else
				       {
				    	   F9 = (y1+y2)/y2 * (F8(x1,x2+x3,y1+y2,z1+z2)- F8(x1,x2,y1+y2,z1+z2)- 
				    		  F8(x1,x2+x3,y1+y2,z1)+ F8(x1,x2,y1+y2,z1))- y1/y2 * (F8(x1,x2+x3,y1,z1+z2) - 
				    				  F8(x1,x2+x3,y1,z1)- F8(x1,x2,y1,z1+z2) + F8(x1,x2,y1,z1));

				       }

				      return F9;
				      }
				 	      
		      
		      
		      
	// ! --- sin function, taking argument in degrees
	public double sind(double arg)
	{
		// double arg, pi;
		// pi = 4.0*atan(1.0) //! or 3.14159265358979
		double sind = Math.sin(arg * Math.PI / 180.0);
		return sind;
	}

	// ! --- cos function, taking argument in degrees
	public double cosd(double arg)
	{
		// double arg, pi;
		// pi = 4.0*atan(1.0) //! or 3.14159265358979
		double cosd = Math.cos(arg * Math.PI / 180.0);
		return cosd;
	}

	// ! --- tan function, taking argument in degrees
	public double tand(double arg)
	{
		// double arg, pi;
		// pi = 4.0*atan(1.0) //! or 3.14159265358979
		double tand = Math.tan(arg * Math.PI / 180.0);
		return tand;
	}

	// ! --- arcsin function, returning argument in degrees
	public double asind(double arg)
	{
		// double arg, pi;
		// pi = 4.0*atan(1.0) //! or 3.14159265358979
		double asind = Math.asin(arg) * 180.0 / Math.PI;
		return asind;
	}

	// ! --- arccos function, returning argument in degrees
	public double acosd(double arg)
	{
		// double arg, pi;
		// pi = 4.0*atan(1.0) //! or 3.14159265358979
		double acosd = Math.acos(arg) * 180.0 / Math.PI;
		return acosd;
	}

	// ! --- arctan function, returning argument in degrees
	public double atand(double arg)
	{
		// double arg, pi;
		// pi = 4.0*atan(1.0) //! or 3.14159265358979
		double atand = Math.atan(arg) * 180.0 / Math.PI;
		return atand;
	}
		
	public double[] initTsfc(double[] Tsfc, boolean[][][][] surf, int bh, int aw2, int al2, int a2, int b1, int b2, int a1, 
			double[][] sfc, double Tsfcs, double Tsfcr, double Tsfcw)
	{
	      int i=0-1;
	      int iab=0-1;
	      for (int f=1;f<=5;f++)
	      {
		       for (int z=0;z<bh;z++)
		       {
			        for (int y=1;y<=aw2;y++)
			        {
				         for (int x=1;x<=al2;x++)
				         {
					          if(surf[x][y][z][f])
					          {				
						          i=i+1;
						          
							      if(x>=a1&&x<=a2&&y>=b1&&y<=b2) 
							      {
							           iab=iab+1;
							      }
					
							      //!  set the roof, wall, and road temperatures to their initial values
						          if (f==1&&z==0) 
						          {
							           if(sfc[i][Constants.sfc_in_array]>1.5) 
							           {
							        	   Tsfc[iab]=Tsfcs;
							           }
						          }
						          else if (f==1&&z>0) 
						          {
							           if(sfc[i][Constants.sfc_in_array]>1.5) 
							           {
							        	   Tsfc[iab]=Tsfcr;
							           }
						          }
						          else
						          {
							           if(sfc[i][Constants.sfc_in_array]>1.5) 
							           {
							        	   Tsfc[iab]=Tsfcw;
							           }
						          }
					          }
				         }
			        }
		       }
	      }
	      return Tsfc;
	}
	
//	public void function1(int numsfc_ab, double[][] sfc, int i, int[][] sfc_ab, double patchlen, double zH, double numcany, double numabovezH)
//	{
//		
//	      for (int iabCount=0;iabCount<numsfc_ab;iabCount++)
//	      {
//	       if(sfc[i][Constants.sfc_in_array]>1.5) 
//	       {
//	        i=sfc_ab[iabCount][Constants.sfc_ab_i];
//	        if((sfc[i][Constants.sfc_z]-0.5)*patchlen<zH-0.01) 
//	        {
//	         numcany=numcany+1;
//	        }
//	        else
//	        {
//	         numabovezH=numabovezH+1;
//	        }
//	       }
//	      }
//		
//	}
	
	

		      
}
