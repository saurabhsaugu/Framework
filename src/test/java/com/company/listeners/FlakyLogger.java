package com.company.listeners;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FlakyLogger {
    private static final Path FLAKY_LOG = Paths.get("target", "flaky", "flaky.jsonl");
    private static final Gson GSON = new GsonBuilder().create();

    public static synchronized void log(Map<String, Object> entry) {
        try {
            ensureDirExists(FLAKY_LOG.getParent());
            String json = GSON.toJson(entry);
            appendLine(FLAKY_LOG.toFile(), json);
        } catch (Exception ignored) {
            // don't fail tests if logging fails
        }
    }

    private static void ensureDirExists(Path dir) throws IOException {
        if (dir != null && !Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    private static void appendLine(File file, String line) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            bw.write(line);
            bw.newLine();
            bw.flush();
        }
    }
}

