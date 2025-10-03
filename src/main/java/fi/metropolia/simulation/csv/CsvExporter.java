package fi.metropolia.simulation.csv;

import fi.metropolia.simulation.model.Survivor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * CSV writer:
 *  • Best-effort deletes old CSVs in ./exports
 *  • Writes ./exports/survivors.csv; if locked (e.g., open in Excel) falls back to a timestamped file
 *  • Renames header "assignment_time" -> "processing_completion_time"
 *  • Writes times with 2 decimals:
 *      processing_completion_time, camp_arrival_time, total_time_in_camp, waiting_time
 *  • Appends an "Overall Simulation Statistics" section at the end
 */
public class CsvExporter {

    private static final String EXPORT_DIR = "exports";
    private static final String PREFERRED_NAME = "survivors.csv";

    /** Public API: write ONLY the current run (rows + stats). */
    public static File writeOnlyCurrentWithStats(List<Survivor> allSurvivors,
                                                 List<Survivor> fullyProcessed) {
        File dir = new File(EXPORT_DIR);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        // Best-effort cleanup (ignore if locked by Excel)
        deleteAllCsvsBestEffort(dir);

        File preferred = new File(dir, PREFERRED_NAME);
        try {
            writeFile(preferred, allSurvivors, fullyProcessed);
            return preferred;
        } catch (IOException lockedOrOther) {
            // Fallback to timestamped file so we don't fail the run
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            File fallback = new File(dir, "survivors_" + ts + ".csv");
            try {
                writeFile(fallback, allSurvivors, fullyProcessed);
                System.err.println("[WARN] Could not write " + preferred.getAbsolutePath()
                        + " (likely locked by another program). Saved to: "
                        + fallback.getAbsolutePath());
                return fallback;
            } catch (IOException stillFailed) {
                lockedOrOther.addSuppressed(stillFailed);
                lockedOrOther.printStackTrace();
                return preferred;
            }
        }
    }

    /* ---------------- internal helpers ---------------- */

    private static void deleteAllCsvsBestEffort(File dir) {
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".csv"));
        if (files == null) return;
        for (File f : files) {
            try {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            } catch (Exception ignored) { }
        }
    }

    private static void writeFile(File out,
                                  List<Survivor> allSurvivors,
                                  List<Survivor> fullyProcessed) throws IOException {

        // Sort numerically by ID (avoid "10" before "9")
        List<Survivor> sorted = new ArrayList<>(allSurvivors);
        sorted.sort(Comparator.comparingInt(Survivor::getSurvivorId));

        try (BufferedWriter w = new BufferedWriter(new FileWriter(out, /*append*/ false))) {
            // Start from Survivor header and replace column 8 name
            // Expected order:
            // 0 id, 1 age_cat, 2 age, 3 health, 4 requires_m, 5 requests_c, 6 stage,
            // 7 assigned_home, 8 assignment_time, 9 camp_arrival_time
            String header = Survivor.csvHeader();
            String[] h = header.split(",", -1);
            if (h.length >= 10) {
                h[8] = "processing_completion_time";
            }
            // Add our extra columns after camp_arrival_time
            w.write(String.join(",", h) + ",total_time_in_camp,waiting_time");
            w.newLine();

            // Survivor rows
            for (Survivor s : sorted) {
                String row = s.toCsvRow();
                String[] cols = row.split(",", -1);

                // Ensure we have expected columns; then override with correct values (2 decimals)
                if (cols.length >= 10) {
                    // processing completion time from model (replaces old "assignment_time")
                    cols[8] = fmt2(safeGetProcessingCompletionTime(s));

                    // camp_arrival_time -> parse & reformat to 2 decimals (no getter in your model)
                    cols[9] = fmt2(parseD(cols[9]));

                    row = String.join(",", cols);
                }

                // total_time_in_camp and waiting_time, both with 2 decimals
                String total = fmt2(s.getTotalTimeInCamp());
                String wait  = fmt2(s.getTotalWaitingTime());

                w.write(row + "," + total + "," + wait);
                w.newLine();
            }

            // ---- Append Overall Simulation Statistics ----
            int processed = fullyProcessed != null ? fullyProcessed.size() : 0;

            double sumTotal = 0.0, sumWait = 0.0;
            if (fullyProcessed != null) {
                for (Survivor s : fullyProcessed) {
                    sumTotal += s.getTotalTimeInCamp();
                    sumWait  += s.getTotalWaitingTime();
                }
            }
            double avgTotal = processed > 0 ? sumTotal / processed : 0.0;
            double avgWait  = processed > 0 ? sumWait  / processed : 0.0;

            w.newLine();
            w.write("Overall Simulation Statistics");
            w.newLine();
            w.write("Total survivors processed:," + processed);
            w.newLine();
            w.write("Average total time in camp:," + fmt2(avgTotal));
            w.newLine();
            w.write("Average waiting time:," + fmt2(avgWait));
            w.newLine();
        }
    }

    /* ---- tiny helpers ---- */

    private static String fmt2(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static double parseD(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static double safeGetProcessingCompletionTime(Survivor s) {
        try {
            return s.getProcessingCompletionTime();  // provided by your model
        } catch (Throwable t) {
            return 0.0;
        }
    }
}
