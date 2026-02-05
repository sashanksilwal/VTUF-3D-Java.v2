package VTUF3D;

import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ArrayPool provides object pooling for arrays to reduce GC pressure.
 *
 * Arrays are pooled by size, allowing reuse of previously allocated arrays
 * instead of creating new ones. This is particularly beneficial for the
 * VTUF3D simulation which allocates many large arrays per geometry iteration.
 *
 * Usage:
 *   ArrayPool pool = ArrayPool.getInstance();
 *   double[] arr = pool.acquireDouble(1000);
 *   // ... use array ...
 *   pool.releaseDouble(arr);
 *
 *   // At end of simulation:
 *   pool.clear();
 */
public class ArrayPool {

    // Singleton instance
    private static final ArrayPool instance = new ArrayPool();

    // Pools organized by array size
    private final Map<Integer, Queue<double[]>> doubleArrayPools;
    private final Map<Integer, Queue<int[]>> intArrayPools;

    // Statistics for monitoring (optional debugging)
    private long acquireCount = 0;
    private long reuseCount = 0;
    private long allocCount = 0;

    // Maximum arrays to keep per size (prevents memory bloat)
    private static final int MAX_POOL_SIZE = 50;

    private ArrayPool() {
        doubleArrayPools = new ConcurrentHashMap<>();
        intArrayPools = new ConcurrentHashMap<>();
    }

    /**
     * Get the singleton instance.
     */
    public static ArrayPool getInstance() {
        return instance;
    }

    /**
     * Acquire a double array of the specified size.
     * Returns a zeroed array (either newly allocated or recycled and cleared).
     */
    public double[] acquireDouble(int size) {
        if (size <= 0) {
            return new double[0];
        }

        acquireCount++;
        Queue<double[]> pool = doubleArrayPools.get(size);
        double[] arr = null;

        if (pool != null) {
            arr = pool.poll();
        }

        if (arr == null) {
            // No pooled array available, allocate new one
            allocCount++;
            arr = new double[size];
        } else {
            // Reusing pooled array, clear it
            reuseCount++;
            Arrays.fill(arr, 0.0);
        }

        return arr;
    }

    /**
     * Acquire a double array and initialize with a specific value.
     */
    public double[] acquireDouble(int size, double initValue) {
        double[] arr = acquireDouble(size);
        if (initValue != 0.0) {
            Arrays.fill(arr, initValue);
        }
        return arr;
    }

    /**
     * Release a double array back to the pool for reuse.
     */
    public void releaseDouble(double[] arr) {
        if (arr == null || arr.length == 0) {
            return;
        }

        Queue<double[]> pool = doubleArrayPools.computeIfAbsent(
            arr.length, k -> new ConcurrentLinkedQueue<>()
        );

        // Only keep up to MAX_POOL_SIZE arrays per size
        if (pool.size() < MAX_POOL_SIZE) {
            pool.offer(arr);
        }
        // Otherwise let the array be garbage collected
    }

    /**
     * Acquire an int array of the specified size.
     * Returns a zeroed array (either newly allocated or recycled and cleared).
     */
    public int[] acquireInt(int size) {
        if (size <= 0) {
            return new int[0];
        }

        Queue<int[]> pool = intArrayPools.get(size);
        int[] arr = null;

        if (pool != null) {
            arr = pool.poll();
        }

        if (arr == null) {
            arr = new int[size];
        } else {
            Arrays.fill(arr, 0);
        }

        return arr;
    }

    /**
     * Release an int array back to the pool for reuse.
     */
    public void releaseInt(int[] arr) {
        if (arr == null || arr.length == 0) {
            return;
        }

        Queue<int[]> pool = intArrayPools.computeIfAbsent(
            arr.length, k -> new ConcurrentLinkedQueue<>()
        );

        if (pool.size() < MAX_POOL_SIZE) {
            pool.offer(arr);
        }
    }

    /**
     * Release multiple double arrays at once.
     * Convenience method for cleanup at end of geometry iteration.
     */
    public void releaseAllDouble(double[]... arrays) {
        for (double[] arr : arrays) {
            releaseDouble(arr);
        }
    }

    /**
     * Release multiple int arrays at once.
     */
    public void releaseAllInt(int[]... arrays) {
        for (int[] arr : arrays) {
            releaseInt(arr);
        }
    }

    /**
     * Clear all pools and release memory.
     * Call this at the end of simulation.
     */
    public void clear() {
        doubleArrayPools.clear();
        intArrayPools.clear();
    }

    /**
     * Get current pool statistics as a string.
     * Useful for debugging and performance tuning.
     */
    public String getStats() {
        int totalDoublePooled = 0;
        long totalDoubleMemory = 0;
        for (Map.Entry<Integer, Queue<double[]>> entry : doubleArrayPools.entrySet()) {
            int count = entry.getValue().size();
            totalDoublePooled += count;
            totalDoubleMemory += (long) count * entry.getKey() * 8; // 8 bytes per double
        }

        int totalIntPooled = 0;
        long totalIntMemory = 0;
        for (Map.Entry<Integer, Queue<int[]>> entry : intArrayPools.entrySet()) {
            int count = entry.getValue().size();
            totalIntPooled += count;
            totalIntMemory += (long) count * entry.getKey() * 4; // 4 bytes per int
        }

        double reuseRate = acquireCount > 0 ? (100.0 * reuseCount / acquireCount) : 0;

        return String.format(
            "ArrayPool Stats: acquires=%d, reuses=%d (%.1f%%), allocs=%d, " +
            "pooled: %d double arrays (%.1f MB), %d int arrays (%.1f MB)",
            acquireCount, reuseCount, reuseRate, allocCount,
            totalDoublePooled, totalDoubleMemory / 1048576.0,
            totalIntPooled, totalIntMemory / 1048576.0
        );
    }

    /**
     * Reset statistics counters.
     */
    public void resetStats() {
        acquireCount = 0;
        reuseCount = 0;
        allocCount = 0;
    }
}
