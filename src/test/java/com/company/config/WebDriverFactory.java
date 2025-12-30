package com.company.config;

import com.company.driver.DriverManager;
import org.openqa.selenium.InvalidArgumentException;
import org.openqa.selenium.WebDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;

import java.util.Locale;

/**
 * Unified WebDriver factory for both web and mobile targets.
 *
 * Configuration sources (priority):
 * 1) JVM system property: -DdriverType=mobile (bound to Spring property driverType)
 * 2) Environment variable: DRIVER_TYPE=mobile (fallback if driverType is not set)
 * 3) Defaults to "web"
 *
 * Only the exact values "web" and "mobile" are accepted. Any other value will cause the test startup to fail fast
 * with a clear InvalidArgumentException.
 */
@Configuration
@ComponentScan(basePackages = {"com.company"})
public class WebDriverFactory {

    // Binds to system property -DdriverType=... if provided. Falls back to empty string if absent.
    @Value("${driverType:}")
    private String driverTypeProp;

    @Bean
    @ScenarioScope
    public WebDriver webDriver() {
        // Prefer explicit Spring property (which maps system properties). If empty, check env var.
        String type = driverTypeProp;
        if (type == null || type.isEmpty()) {
            type = System.getenv("DRIVER_TYPE");
        }
        if (type == null || type.isEmpty()) {
            type = "web"; // default
        }

        type = type.toLowerCase(Locale.ROOT).trim();

        // Strict validation: only accept "web" or "mobile"
        if (!"web".equals(type) && !"mobile".equals(type)) {
            throw new InvalidArgumentException("Unsupported driverType='" + type + "'. Expected 'web' or 'mobile'. Set with -DdriverType=web|mobile or env DRIVER_TYPE.");
        }

        // Initialize driver via DriverManager (delegates to Appium or browser flows)
        DriverManager.initDriver(type);
        return DriverManager.getDriver();
    }

    // Ensure driver is quit when the scenario context is closed
    @Bean
    @ScenarioScope
    public org.springframework.beans.factory.DisposableBean driverCloser() {
        return () -> DriverManager.quitDriver();
    }
}
