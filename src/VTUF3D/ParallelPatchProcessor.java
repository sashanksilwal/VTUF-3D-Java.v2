package VTUF3D;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

/**
 * Parallel processor for surface patch temperature calculations.
 * Uses ForkJoinPool to parallelize the Newton's method solver across patches.
 */
public class ParallelPatchProcessor {

    private static final ForkJoinPool pool;
    private static final int NUM_THREADS;

    static {
        // Use all available processors
        NUM_THREADS = Runtime.getRuntime().availableProcessors();
        pool = new ForkJoinPool(NUM_THREADS);
        System.out.println("ParallelPatchProcessor initialized with " + NUM_THREADS + " threads");
    }

    /**
     * Result container for a single patch calculation.
     * Stores temperature and intermediate values needed for accumulation.
     */
    public static class PatchResult {
        public double Tsfc;
        public double Trad;
        public double httc;
        public double Rnet;
        public double Tconv;
        public double currentRnet;
        public double currentQh;
        public double currentQe;
        public double currentQg;
        public boolean valid;

        // Surface type flags
        public boolean isRoof;
        public boolean isStreet;
        public boolean isNorthWall;
        public boolean isSouthWall;
        public boolean isEastWall;
        public boolean isWestWall;
        public boolean inCentralArray;
        public boolean belowZH;
        public boolean isSunlit;
        public boolean isShaded;

        // Additional values for accumulation
        public double TradValue;
        public double RnetValue;
        public double QhValue;
        public double QgValue;
        public double KdnValue;
        public double KupValue;
        public double LdnValue;
        public double LupValue;
        public double TpContrib;
        public double leFromEt;
    }

    /**
     * Accumulator for thread-safe summation of patch results.
     */
    public static class PatchAccumulator {
        // Use DoubleAdder for lock-free concurrent accumulation
        public final DoubleAdder Tdiffmax = new DoubleAdder();
        public final DoubleAdder Tp = new DoubleAdder();
        public final DoubleAdder Tsfc_cplt = new DoubleAdder();
        public final DoubleAdder Tsfc_bird = new DoubleAdder();

        // Roof
        public final DoubleAdder Tsfc_R = new DoubleAdder();
        public final DoubleAdder Trad_R = new DoubleAdder();
        public final DoubleAdder Rnet_R = new DoubleAdder();
        public final DoubleAdder Qh_R = new DoubleAdder();
        public final DoubleAdder Qg_R = new DoubleAdder();
        public final DoubleAdder httcR = new DoubleAdder();
        public final DoubleAdder Kdn_R = new DoubleAdder();
        public final DoubleAdder Kup_R = new DoubleAdder();
        public final DoubleAdder Ldn_R = new DoubleAdder();
        public final DoubleAdder Lup_R = new DoubleAdder();

        // Street
        public final DoubleAdder Tsfc_T = new DoubleAdder();
        public final DoubleAdder Trad_T = new DoubleAdder();
        public final DoubleAdder Rnet_T = new DoubleAdder();
        public final DoubleAdder Qh_T = new DoubleAdder();
        public final DoubleAdder Qg_T = new DoubleAdder();
        public final DoubleAdder httcT = new DoubleAdder();
        public final DoubleAdder Kdn_T = new DoubleAdder();
        public final DoubleAdder Kup_T = new DoubleAdder();
        public final DoubleAdder Ldn_T = new DoubleAdder();
        public final DoubleAdder Lup_T = new DoubleAdder();

        // North Wall
        public final DoubleAdder Tsfc_N = new DoubleAdder();
        public final DoubleAdder Trad_N = new DoubleAdder();
        public final DoubleAdder Rnet_N = new DoubleAdder();
        public final DoubleAdder Qh_N = new DoubleAdder();
        public final DoubleAdder Qg_N = new DoubleAdder();
        public final DoubleAdder Kdn_N = new DoubleAdder();
        public final DoubleAdder Kup_N = new DoubleAdder();
        public final DoubleAdder Ldn_N = new DoubleAdder();
        public final DoubleAdder Lup_N = new DoubleAdder();

        // South Wall
        public final DoubleAdder Tsfc_S = new DoubleAdder();
        public final DoubleAdder Trad_S = new DoubleAdder();
        public final DoubleAdder Rnet_S = new DoubleAdder();
        public final DoubleAdder Qh_S = new DoubleAdder();
        public final DoubleAdder Qg_S = new DoubleAdder();
        public final DoubleAdder Kdn_S = new DoubleAdder();
        public final DoubleAdder Kup_S = new DoubleAdder();
        public final DoubleAdder Ldn_S = new DoubleAdder();
        public final DoubleAdder Lup_S = new DoubleAdder();

        // East Wall
        public final DoubleAdder Tsfc_E = new DoubleAdder();
        public final DoubleAdder Trad_E = new DoubleAdder();
        public final DoubleAdder Rnet_E = new DoubleAdder();
        public final DoubleAdder Qh_E = new DoubleAdder();
        public final DoubleAdder Qg_E = new DoubleAdder();
        public final DoubleAdder Kdn_E = new DoubleAdder();
        public final DoubleAdder Kup_E = new DoubleAdder();
        public final DoubleAdder Ldn_E = new DoubleAdder();
        public final DoubleAdder Lup_E = new DoubleAdder();

        // West Wall
        public final DoubleAdder Tsfc_W = new DoubleAdder();
        public final DoubleAdder Trad_W = new DoubleAdder();
        public final DoubleAdder Rnet_W = new DoubleAdder();
        public final DoubleAdder Qh_W = new DoubleAdder();
        public final DoubleAdder Qg_W = new DoubleAdder();
        public final DoubleAdder httcW = new DoubleAdder();
        public final DoubleAdder Kdn_W = new DoubleAdder();
        public final DoubleAdder Kup_W = new DoubleAdder();
        public final DoubleAdder Ldn_W = new DoubleAdder();
        public final DoubleAdder Lup_W = new DoubleAdder();
        public final DoubleAdder Absbs_W = new DoubleAdder();
        public final DoubleAdder Absbl_W = new DoubleAdder();
        public final DoubleAdder Emit_W = new DoubleAdder();

        // Totals
        public final DoubleAdder Rnet_tot = new DoubleAdder();
        public final DoubleAdder Qh_tot = new DoubleAdder();
        public final DoubleAdder Qg_tot = new DoubleAdder();
        public final DoubleAdder Qe_tot = new DoubleAdder();
        public final DoubleAdder Qhcantmp = new DoubleAdder();
        public final DoubleAdder Qh_abovezH = new DoubleAdder();
        public final DoubleAdder Qanthro = new DoubleAdder();
        public final DoubleAdder Qac = new DoubleAdder();
        public final DoubleAdder Qdeep = new DoubleAdder();

        // Sunlit/shaded counters and temps
        public final DoubleAdder TTsun = new DoubleAdder();
        public final DoubleAdder TTsh = new DoubleAdder();
        public final DoubleAdder TNsun = new DoubleAdder();
        public final DoubleAdder TNsh = new DoubleAdder();
        public final DoubleAdder TSsun = new DoubleAdder();
        public final DoubleAdder TSsh = new DoubleAdder();
        public final DoubleAdder TEsun = new DoubleAdder();
        public final DoubleAdder TEsh = new DoubleAdder();
        public final DoubleAdder TWsun = new DoubleAdder();
        public final DoubleAdder TWsh = new DoubleAdder();

        public final LongAdder numTsun = new LongAdder();
        public final LongAdder numTsh = new LongAdder();
        public final LongAdder numNsun = new LongAdder();
        public final LongAdder numNsh = new LongAdder();
        public final LongAdder numSsun = new LongAdder();
        public final LongAdder numSsh = new LongAdder();
        public final LongAdder numEsun = new LongAdder();
        public final LongAdder numEsh = new LongAdder();
        public final LongAdder numWsun = new LongAdder();
        public final LongAdder numWsh = new LongAdder();

        public void reset() {
            // Reset all accumulators
            Tdiffmax.reset(); Tp.reset();
            Tsfc_cplt.reset(); Tsfc_bird.reset();
            Tsfc_R.reset(); Trad_R.reset(); Rnet_R.reset(); Qh_R.reset(); Qg_R.reset();
            httcR.reset(); Kdn_R.reset(); Kup_R.reset(); Ldn_R.reset(); Lup_R.reset();
            Tsfc_T.reset(); Trad_T.reset(); Rnet_T.reset(); Qh_T.reset(); Qg_T.reset();
            httcT.reset(); Kdn_T.reset(); Kup_T.reset(); Ldn_T.reset(); Lup_T.reset();
            Tsfc_N.reset(); Trad_N.reset(); Rnet_N.reset(); Qh_N.reset(); Qg_N.reset();
            Kdn_N.reset(); Kup_N.reset(); Ldn_N.reset(); Lup_N.reset();
            Tsfc_S.reset(); Trad_S.reset(); Rnet_S.reset(); Qh_S.reset(); Qg_S.reset();
            Kdn_S.reset(); Kup_S.reset(); Ldn_S.reset(); Lup_S.reset();
            Tsfc_E.reset(); Trad_E.reset(); Rnet_E.reset(); Qh_E.reset(); Qg_E.reset();
            Kdn_E.reset(); Kup_E.reset(); Ldn_E.reset(); Lup_E.reset();
            Tsfc_W.reset(); Trad_W.reset(); Rnet_W.reset(); Qh_W.reset(); Qg_W.reset();
            httcW.reset(); Kdn_W.reset(); Kup_W.reset(); Ldn_W.reset(); Lup_W.reset();
            Absbs_W.reset(); Absbl_W.reset(); Emit_W.reset();
            Rnet_tot.reset(); Qh_tot.reset(); Qg_tot.reset(); Qe_tot.reset();
            Qhcantmp.reset(); Qh_abovezH.reset(); Qanthro.reset(); Qac.reset(); Qdeep.reset();
            TTsun.reset(); TTsh.reset(); TNsun.reset(); TNsh.reset();
            TSsun.reset(); TSsh.reset(); TEsun.reset(); TEsh.reset();
            TWsun.reset(); TWsh.reset();
            numTsun.reset(); numTsh.reset(); numNsun.reset(); numNsh.reset();
            numSsun.reset(); numSsh.reset(); numEsun.reset(); numEsh.reset();
            numWsun.reset(); numWsh.reset();
        }
    }

    /**
     * Solve surface temperature using Newton's method.
     * This is the core computation that benefits from parallelization.
     *
     * @param Tsfc_init Initial temperature estimate
     * @param emiss Surface emissivity
     * @param sigma Stefan-Boltzmann constant
     * @param httc Heat transfer coefficient
     * @param lambda_sfc Surface thermal conductivity
     * @param thickness Layer thickness
     * @param Rnet Net radiation
     * @param Tconv Convection temperature
     * @param layerTemp Subsurface layer temperature
     * @return Converged surface temperature
     */
    public static double solveNewtonTemperature(
            double Tsfc_init, double emiss, double sigma,
            double httc, double lambda_sfc, double thickness,
            double Rnet, double Tconv, double layerTemp) {

        double Tnew = Tsfc_init;
        double Told = Tnew + 999.0;
        double Fold, Fold_prime;
        int iterations = 0;
        int httcRetries = 0;
        double httcLocal = httc;

        while (Math.abs(Tnew - Told) > 0.001) {
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
                return Tsfc_init; // Fall back to initial value
            }

            iterations++;
            if (iterations > 40) {
                // Modify httc to help convergence
                if (httcLocal < 0) {
                    httcLocal += 0.5;
                } else {
                    httcLocal -= 0.5;
                }
                iterations = 0;
                httcRetries++;

                if (httcRetries > 10) {
                    return Tsfc_init; // Fall back to initial value
                }
            }
        }

        return Tnew;
    }

    /**
     * Process patches in parallel using ForkJoinPool.
     *
     * @param numsfc2 Number of patches to process
     * @param processor Lambda function that processes a single patch index
     */
    public static void processInParallel(int numsfc2, java.util.function.IntConsumer processor) {
        try {
            pool.submit(() ->
                IntStream.range(0, numsfc2).parallel().forEach(processor)
            ).get();
        } catch (Exception e) {
            // Fall back to sequential processing
            System.err.println("Parallel processing failed, falling back to sequential: " + e.getMessage());
            IntStream.range(0, numsfc2).forEach(processor);
        }
    }

    /**
     * Get the number of threads being used.
     */
    public static int getNumThreads() {
        return NUM_THREADS;
    }

    /**
     * Shutdown the thread pool (call when simulation is complete).
     */
    public static void shutdown() {
        pool.shutdown();
    }
}
