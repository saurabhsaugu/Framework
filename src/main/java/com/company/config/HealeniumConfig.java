package com.company.config;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Centralized reader for Healenium-related properties.
 * Resolution order: System properties (-Dkey=...), classpath properties/proj root properties, then default value.
 */
public final class HealeniumConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = HealeniumConfig.class.getClassLoader().getResourceAsStream("properties/healenium.properties")) {
            if (is != null) {
                PROPS.load(is);
                System.out.println("[HealeniumConfig] Loaded healenium.properties from classpath: properties/healenium.properties");
            } else {
                Path p = Paths.get("healenium.properties");
                if (Files.exists(p)) {
                    try (InputStream fis = Files.newInputStream(p)) {
                        PROPS.load(fis);
                        System.out.println("[HealeniumConfig] Loaded healenium.properties from project root: healenium.properties");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[HealeniumConfig] Failed to load healenium.properties: " + e.getMessage());
        }
    }

    private HealeniumConfig() { }

    public static String get(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (v != null) return v;
        v = PROPS.getProperty(key);
        return v != null ? v : defaultValue;
    }
}

