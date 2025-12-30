package com.company.config;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream;
import java.util.Properties;
import java.util.HashSet;
import java.util.Set;

/**
 * Simplified centralized reader for framework properties.
 * Behavior:
 *  - First, attempt to load a small set of well-known property files from the classpath under `properties/`.
 *  - Then, load any .properties files found in src/test/resources/properties (useful during development/IDE runs).
 *  - Property resolution: System property (-Dkey=...) takes precedence, then loaded properties, then provided default value.
 */
public final class PropertyConfig {

    private static final Properties PROPS = new Properties();

    static {
        Set<String> loaded = new HashSet<>();
        ClassLoader cl = PropertyConfig.class.getClassLoader();

        try {
            Path projectProps = Paths.get("src", "test", "resources", "properties");
            if (Files.exists(projectProps) && Files.isDirectory(projectProps)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(projectProps, "*.properties")) {
                    for (Path p : ds) {
                        String name = p.getFileName().toString();
                        if (loaded.add(name)) {
                            try (InputStream is = Files.newInputStream(p)) {
                                PROPS.load(is);
                                System.out.println("[PropertyConfig] Loaded " + name + " from project: " + p);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[PropertyConfig] Failed to load properties from src/test/resources/properties: " + e.getMessage());
        }
    }

    private PropertyConfig() { }

    public static String get(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (v != null) return v;
        v = PROPS.getProperty(key);
        return v != null ? v : defaultValue;
    }
}
