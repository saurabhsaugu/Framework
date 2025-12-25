package com.company.analytics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple offline flaky test analyzer.
 * Reads target/cucumber.json (or TestNG results) and produces a summary JSON.
 * This is intentionally minimal — replace with an enterprise solution as needed.
 */
public class FlakyAnalytics {

    public static class Summary {
        public int total = 0;
        public int passed = 0;
        public int failed = 0;
        public int flaky = 0;
        public Map<String, Integer> failures = new HashMap<>();
    }

    public static void main(String[] args) {
        Path input = Paths.get("target", "flaky", "flaky.jsonl");
        Path outDir = Paths.get("target", "flaky");
        try {
            if (!Files.exists(input)) {
                System.out.println("No flaky log found at " + input.toString());
                return;
            }

            Map<String, Aggregate> map = new HashMap<>();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();

            try (BufferedReader br = new BufferedReader(new FileReader(input.toFile()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    Map<String, Object> entry = gson.fromJson(line, mapType);
                    String cls = (String) entry.getOrDefault("testClass", "<unknown>");
                    String mth = (String) entry.getOrDefault("testMethod", "<unknown>");
                    String key = cls + "#" + mth;
                    Aggregate agg = map.computeIfAbsent(key, k -> new Aggregate(cls, mth));
                    agg.count.incrementAndGet();
                    Object attemptObj = entry.get("attempt");
                    if (attemptObj != null) {
                        try {
                            int a = ((Number) attemptObj).intValue();
                            if (a > agg.maxAttempt) agg.maxAttempt = a;
                        } catch (Exception ignored) { }
                    }
                    Object ts = entry.get("timestamp");
                    if (ts != null) {
                        agg.lastSeen = ts.toString();
                    }
                }
            }

            // ensure output dir
            Files.createDirectories(outDir);

            // Build list of recommendation records
            List<Recommendation> recommendations = new ArrayList<>();
            for (Aggregate a : map.values()) {
                Recommendation r = analyzeAggregate(a);
                recommendations.add(r);
            }

            // write JSON summary
            Path jsonOut = outDir.resolve("flaky-summary.json");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(jsonOut.toFile()))) {
                bw.write(gson.toJson(recommendations));
            }

            // write CSV for Jenkins processing
            Path csvOut = outDir.resolve("flaky-summary.csv");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvOut.toFile()))) {
                bw.write("test,clazz,method,occurrences,maxAttempt,lastSeen,recommendation\n");
                for (Recommendation r : recommendations) {
                    bw.write(escapeCsv(r.test) + "," + escapeCsv(r.clazz) + "," + escapeCsv(r.method) + "," + r.occurrences + "," + r.maxAttempt + "," + escapeCsv(r.lastSeen) + "," + escapeCsv(r.recommendation) + "\n");
                }
            }

            // write simple HTML report
            Path htmlOut = outDir.resolve("flaky-report.html");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(htmlOut.toFile()))) {
                bw.write("<html><head><meta charset=\"utf-8\"><title>Flaky Report</title></head><body>");
                bw.write("<h1>Flaky Tests Report</h1>");
                bw.write("<p>Generated: " + Instant.now().toString() + "</p>");
                bw.write("<table border=\"1\" cellpadding=\"6\"><tr><th>Test</th><th>Class</th><th>Method</th><th>Occurrences</th><th>MaxAttempt</th><th>LastSeen</th><th>Recommendation</th></tr>");
                for (Recommendation r : recommendations) {
                    bw.write("<tr>");
                    bw.write("<td>" + escapeHtml(r.test) + "</td>");
                    bw.write("<td>" + escapeHtml(r.clazz) + "</td>");
                    bw.write("<td>" + escapeHtml(r.method) + "</td>");
                    bw.write("<td align=\"right\">" + r.occurrences + "</td>");
                    bw.write("<td align=\"right\">" + r.maxAttempt + "</td>");
                    bw.write("<td>" + escapeHtml(r.lastSeen) + "</td>");
                    bw.write("<td>" + escapeHtml(r.recommendation) + "</td>");
                    bw.write("</tr>");
                }
                bw.write("</table>");
                bw.write("</body></html>");
            }

            // write plain text recommendations
            Path txtOut = outDir.resolve("flaky-recommendations.txt");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(txtOut.toFile()))) {
                for (Recommendation r : recommendations) {
                    bw.write("Test: " + r.test + "\n");
                    bw.write("  Recommendation: " + r.recommendation + "\n");
                    bw.write("  Occurrences: " + r.occurrences + ", MaxAttempt: " + r.maxAttempt + ", LastSeen: " + r.lastSeen + "\n\n");
                }
            }

            System.out.println("Flaky JSON summary: " + jsonOut.toAbsolutePath());
            System.out.println("Flaky CSV: " + csvOut.toAbsolutePath());
            System.out.println("Flaky HTML report: " + htmlOut.toAbsolutePath());
            System.out.println("Flaky recommendations: " + txtOut.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("Failed to produce flaky report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Recommendation analyzeAggregate(Aggregate a) {
        Recommendation r = new Recommendation();
        r.test = a.cls + "#" + a.method;
        r.clazz = a.cls;
        r.method = a.method;
        r.occurrences = a.count.get();
        r.maxAttempt = a.maxAttempt;
        r.lastSeen = a.lastSeen != null ? a.lastSeen : "";

        // Simple heuristic "AI" rules to produce recommendations
        // - If a test retried multiple times across runs (occurrences >=3) -> Likely Flaky: quarantine or assign to investigator
        // - If a test had a high max attempt (>=2) -> likely transient timing/resource issue: suggest increasing retry or stabilizing
        // - If occurrences == 1 but maxAttempt >=2 -> transient: mark for investigation
        // - Otherwise: monitor
        if (r.occurrences >= 5 || r.maxAttempt >= 3) {
            r.recommendation = "HIGH_RISK: Consider quarantining the test and open an investigation ticket. Increase monitoring and capture full logs/screenshots on failure.";
        } else if (r.occurrences >= 3 || r.maxAttempt >= 2) {
            r.recommendation = "LIKELY_FLAKY: Mark as flaky, consider quarantining or assigning for investigation. Add more logging and consider adding targeted retries.";
        } else if (r.occurrences == 1 && r.maxAttempt >= 2) {
            r.recommendation = "TRANSIENT: Occurred once with retries. Consider adding targeted retry or investigate intermittent resource/timing issues.";
        } else {
            r.recommendation = "MONITOR: Low frequency. Continue monitoring; consider adding better logging if it re-occurs.";
        }

        return r;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        String out = s.replace("\"", "\"\"");
        if (out.contains(",") || out.contains("\n") || out.contains("\"")) {
            return "\"" + out + "\"";
        }
        return out;
    }

    private static class Aggregate {
        String cls;
        String method;
        AtomicInteger count = new AtomicInteger(0);
        int maxAttempt = 0;
        String lastSeen;

        Aggregate(String cls, String method) {
            this.cls = cls;
            this.method = method;
        }
    }

    private static class Recommendation {
        String test;
        String clazz;
        String method;
        int occurrences;
        int maxAttempt;
        String lastSeen;
        String recommendation;
    }
}
