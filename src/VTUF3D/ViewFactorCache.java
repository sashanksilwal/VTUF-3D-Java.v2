package VTUF3D;

import java.io.*;
import java.util.Arrays;

/**
 * View Factor Cache - saves and loads precomputed view factors to avoid
 * expensive recalculation on subsequent runs with the same geometry.
 *
 * The cache file stores:
 * - Geometry signature (al2, aw2, bh, numsfc2) for validation
 * - numvf: total number of view factors
 * - vf3[]: view factor values
 * - vf3j[]: view factor target indices
 * - vfppos[]: position pointers
 * - sfc_evf[]: total view factor for each surface (extracted from sfc array)
 */
public class ViewFactorCache
{
    private static final int CACHE_VERSION = 1;
    private static final String CACHE_FILENAME = "vf_cache.bin";

    /**
     * Cache data structure containing all view factor arrays
     */
    public static class CacheData
    {
        public int numvf;
        public double[] vf3;
        public int[] vf3j;
        public int[] vfppos;
        public double[] sfc_evf;  // sfc[i][sfc_evf] values for all surfaces

        // Geometry signature for validation
        public int al2;
        public int aw2;
        public int bh;
        public int numsfc2;
    }

    /**
     * Get the cache file path for a given working directory
     */
    public static String getCacheFilePath(String workingDir)
    {
        if (workingDir == null || workingDir.isEmpty())
        {
            return CACHE_FILENAME;
        }
        return workingDir + File.separator + CACHE_FILENAME;
    }

    /**
     * Check if a valid cache file exists for the given geometry
     */
    public static boolean cacheExists(String workingDir, int al2, int aw2, int bh, int numsfc2)
    {
        String filePath = getCacheFilePath(workingDir);
        File cacheFile = new File(filePath);

        if (!cacheFile.exists())
        {
            return false;
        }

        // Validate the cache signature
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(cacheFile))))
        {
            int version = dis.readInt();
            if (version != CACHE_VERSION)
            {
                System.out.println("View factor cache version mismatch, will recalculate");
                return false;
            }

            int cached_al2 = dis.readInt();
            int cached_aw2 = dis.readInt();
            int cached_bh = dis.readInt();
            int cached_numsfc2 = dis.readInt();

            if (cached_al2 != al2 || cached_aw2 != aw2 || cached_bh != bh || cached_numsfc2 != numsfc2)
            {
                System.out.println("View factor cache geometry mismatch, will recalculate");
                System.out.println("  Cached: al2=" + cached_al2 + " aw2=" + cached_aw2 + " bh=" + cached_bh + " numsfc2=" + cached_numsfc2);
                System.out.println("  Current: al2=" + al2 + " aw2=" + aw2 + " bh=" + bh + " numsfc2=" + numsfc2);
                return false;
            }

            return true;
        }
        catch (IOException e)
        {
            System.out.println("Error reading view factor cache header: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load cached view factors from file
     * @return CacheData or null if loading fails
     */
    public static CacheData loadCache(String workingDir)
    {
        String filePath = getCacheFilePath(workingDir);
        File cacheFile = new File(filePath);

        System.out.println("Loading view factors from cache: " + filePath);
        long startTime = System.currentTimeMillis();

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(cacheFile))))
        {
            CacheData data = new CacheData();

            // Read header
            int version = dis.readInt();
            data.al2 = dis.readInt();
            data.aw2 = dis.readInt();
            data.bh = dis.readInt();
            data.numsfc2 = dis.readInt();
            data.numvf = dis.readInt();

            // Read vf3 array
            data.vf3 = new double[data.numvf];
            for (int i = 0; i < data.numvf; i++)
            {
                data.vf3[i] = dis.readDouble();
            }

            // Read vf3j array
            data.vf3j = new int[data.numvf];
            for (int i = 0; i < data.numvf; i++)
            {
                data.vf3j[i] = dis.readInt();
            }

            // Read vfppos array (size is numsfc2 + 2)
            int vfpposSize = dis.readInt();
            data.vfppos = new int[vfpposSize];
            for (int i = 0; i < vfpposSize; i++)
            {
                data.vfppos[i] = dis.readInt();
            }

            // Read sfc_evf array
            int sfcEvfSize = dis.readInt();
            data.sfc_evf = new double[sfcEvfSize];
            for (int i = 0; i < sfcEvfSize; i++)
            {
                data.sfc_evf[i] = dis.readDouble();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("View factor cache loaded successfully in " + elapsed + " ms");
            System.out.println("  Total view factors: " + data.numvf);

            return data;
        }
        catch (IOException e)
        {
            System.out.println("Error loading view factor cache: " + e.getMessage());
            return null;
        }
    }

    /**
     * Save view factors to cache file
     */
    public static boolean saveCache(String workingDir, int al2, int aw2, int bh, int numsfc2,
                                    int numvf, double[] vf3, int[] vf3j, int[] vfppos,
                                    double[][] sfc, int numsfc)
    {
        String filePath = getCacheFilePath(workingDir);

        System.out.println("Saving view factors to cache: " + filePath);
        long startTime = System.currentTimeMillis();

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath))))
        {
            // Write header
            dos.writeInt(CACHE_VERSION);
            dos.writeInt(al2);
            dos.writeInt(aw2);
            dos.writeInt(bh);
            dos.writeInt(numsfc2);
            dos.writeInt(numvf);

            // Write vf3 array
            for (int i = 0; i < numvf; i++)
            {
                dos.writeDouble(vf3[i]);
            }

            // Write vf3j array
            for (int i = 0; i < numvf; i++)
            {
                dos.writeInt(vf3j[i]);
            }

            // Write vfppos array (use actual array length)
            int vfpposSize = vfppos.length;
            dos.writeInt(vfpposSize);
            for (int i = 0; i < vfpposSize; i++)
            {
                dos.writeInt(vfppos[i]);
            }

            // Write sfc_evf values
            dos.writeInt(numsfc);
            for (int i = 0; i < numsfc; i++)
            {
                dos.writeDouble(sfc[i][Constants.sfc_evf]);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("View factor cache saved successfully in " + elapsed + " ms");
            System.out.println("  Cache file size: " + new File(filePath).length() / 1024 + " KB");

            return true;
        }
        catch (IOException e)
        {
            System.out.println("Error saving view factor cache: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete the cache file
     */
    public static boolean deleteCache(String workingDir)
    {
        String filePath = getCacheFilePath(workingDir);
        File cacheFile = new File(filePath);

        if (cacheFile.exists())
        {
            return cacheFile.delete();
        }
        return true;
    }
}
