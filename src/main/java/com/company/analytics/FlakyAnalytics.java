package com.company.analytics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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

    public static void main(String[] args) throws Exception {
        Path cucumber = Path.of("target/cucumber.json");
        Summary summary = new Summary();
        if (Files.exists(cucumber)) {
            try (Reader reader = new FileReader(cucumber.toFile())) {
                // Very small parser: count occurrences of "status":"failed" and "status":"passed"
                String content = Files.readString(cucumber);
                int failed = countOccurrences(content, "\"status\":\"failed\"");
                int passed = countOccurrences(content, "\"status\":\"passed\"");
                summary.failed = failed;
                summary.passed = passed;
                summary.total = failed + passed;
            }
        }

        // Placeholder for more advanced flaky detection: read a persisted history file and mark tests that flapped
        Path history = Path.of("target/flaky-history.json");
        Map<String, Integer> hist = new HashMap<>();
        if (Files.exists(history)) {
            Gson g = new Gson();
            try (Reader r = new FileReader(history.toFile())) {
                Map<?,?> m = g.fromJson(r, Map.class);
                for (Object k : m.keySet()) {
                    hist.put(String.valueOf(k), ((Number)((Map)m).get(k)).intValue());
                }
            } catch (Exception ignored) {}
        }

        // Save a simple output
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String out = gson.toJson(summary);
        try (FileWriter fw = new FileWriter("target/flaky-summary.json")) {
            fw.write(out);
        }

        System.out.println("Flaky analysis written to target/flaky-summary.json");
    }

    private static int countOccurrences(String s, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = s.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}

