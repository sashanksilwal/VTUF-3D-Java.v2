package VTUF3D;

import java.util.ArrayList;
import java.util.List;

/**
 * ProgressTracker provides progress reporting with ETA calculation for VTUF3D simulations.
 *
 * Tracks simulation progress as a percentage and calculates estimated time remaining
 * based on elapsed wall-clock time. Supports both console output and listener callbacks
 * for integration with GUIs or logging systems.
 *
 * Usage:
 *   ProgressTracker tracker = new ProgressTracker();
 *   tracker.start(starttime, timeend);
 *   while (timeis <= timeend) {
 *       tracker.update(timeis);
 *       // ... simulation work ...
 *   }
 *   tracker.finish();
 */
public class ProgressTracker {

    private double startTime;
    private double endTime;
    private long simulationStartMillis;
    private int lastReportedPercent = -1;
    private boolean enabled = true;
    private int progressBarWidth = 40;

    // Callback interface for external listeners
    public interface ProgressListener {
        /**
         * Called when progress updates.
         * @param percent Progress percentage (0-100)
         * @param eta Estimated time remaining as human-readable string
         * @param currentTime Current simulation time (hours)
         * @param endTime End simulation time (hours)
         */
        void onProgress(int percent, String eta, double currentTime, double endTime);
    }

    private final List<ProgressListener> listeners = new ArrayList<>();

    /**
     * Create a new ProgressTracker with default settings.
     */
    public ProgressTracker() {
    }

    /**
     * Create a new ProgressTracker with custom progress bar width.
     */
    public ProgressTracker(int progressBarWidth) {
        this.progressBarWidth = progressBarWidth;
    }

    /**
     * Add a progress listener for callbacks.
     */
    public void addListener(ProgressListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a progress listener.
     */
    public void removeListener(ProgressListener listener) {
        listeners.remove(listener);
    }

    /**
     * Enable or disable progress reporting.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Start tracking progress for a simulation run.
     * @param startTime Simulation start time (hours)
     * @param endTime Simulation end time (hours)
     */
    public void start(double startTime, double endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.simulationStartMillis = System.currentTimeMillis();
        this.lastReportedPercent = -1;

        if (enabled) {
            System.out.println();
            System.out.println("Starting simulation: " + formatTime(startTime) +
                               " to " + formatTime(endTime) +
                               " (" + String.format("%.1f", endTime - startTime) + " hours)");
            System.out.println();
        }
    }

    /**
     * Update progress with current simulation time.
     * Only prints when the percentage changes to avoid flooding output.
     * @param currentTime Current simulation time (hours)
     */
    public void update(double currentTime) {
        if (!enabled) {
            return;
        }

        double totalDuration = endTime - startTime;
        if (totalDuration <= 0) {
            return;
        }

        double progress = (currentTime - startTime) / totalDuration;
        progress = Math.max(0, Math.min(1, progress)); // Clamp to 0-1

        int percent = (int) (progress * 100);

        // Only report on whole percent changes
        if (percent != lastReportedPercent) {
            lastReportedPercent = percent;
            String eta = calculateETA(progress);

            // Print progress bar
            printProgressBar(percent, eta, currentTime);

            // Notify listeners
            for (ProgressListener listener : listeners) {
                listener.onProgress(percent, eta, currentTime, endTime);
            }
        }
    }

    /**
     * Calculate estimated time remaining based on progress.
     */
    private String calculateETA(double progress) {
        if (progress <= 0.01) {
            return "calculating...";
        }

        long elapsed = System.currentTimeMillis() - simulationStartMillis;
        long totalEstimated = (long) (elapsed / progress);
        long remaining = totalEstimated - elapsed;

        if (remaining < 0) {
            remaining = 0;
        }

        return formatDuration(remaining);
    }

    /**
     * Format a duration in milliseconds as a human-readable string.
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }

        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "m " + seconds + "s";
        }

        long hours = minutes / 60;
        minutes = minutes % 60;
        if (hours < 24) {
            return hours + "h " + minutes + "m";
        }

        long days = hours / 24;
        hours = hours % 24;
        return days + "d " + hours + "h";
    }

    /**
     * Format simulation time (hours) as HH:MM.
     */
    private String formatTime(double hours) {
        int h = (int) hours;
        int m = (int) ((hours - h) * 60);
        return String.format("%02d:%02d", h % 24, m);
    }

    /**
     * Print a progress bar to the console.
     * Uses carriage return for in-place updates on terminals that support it.
     */
    private void printProgressBar(int percent, String eta, double currentTime) {
        int filled = (int) (progressBarWidth * percent / 100.0);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < progressBarWidth; i++) {
            if (i < filled) {
                bar.append("=");
            } else if (i == filled) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");

        // Format: [=====>                    ]  25% | Time: 6.0h/24.0h | ETA: 15m 30s
        String output = String.format("\r%s %3d%% | Time: %.1fh/%.1fh | ETA: %-15s",
            bar.toString(), percent, currentTime, endTime, eta);

        System.out.print(output);
        System.out.flush();

        // Print newline at 100% for clean output
        if (percent >= 100) {
            System.out.println();
        }
    }

    /**
     * Mark the simulation as finished and print summary.
     */
    public void finish() {
        if (!enabled) {
            return;
        }

        long totalElapsed = System.currentTimeMillis() - simulationStartMillis;

        System.out.println();
        System.out.println("------------------------------------------");
        System.out.println("Simulation complete!");
        System.out.println("Total wall-clock time: " + formatDuration(totalElapsed));
        System.out.println("------------------------------------------");
    }

    /**
     * Get the elapsed wall-clock time since start() was called.
     * @return Elapsed time in milliseconds
     */
    public long getElapsedMillis() {
        return System.currentTimeMillis() - simulationStartMillis;
    }

    /**
     * Get the elapsed wall-clock time as a formatted string.
     */
    public String getElapsedFormatted() {
        return formatDuration(getElapsedMillis());
    }

    /**
     * Get the current progress percentage (0-100).
     */
    public int getLastReportedPercent() {
        return Math.max(0, lastReportedPercent);
    }
}
