package com.company.driver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import java.net.URL;
import org.openqa.selenium.remote.DesiredCapabilities;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;

// Simple thread-safe singleton driver manager supporting Selenium and Appium.
public class DriverManager {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    private DriverManager() { }

    public static WebDriver getDriver() {
        return driver.get();
    }

    public static void setDriver(WebDriver webDriver) {
        driver.set(webDriver);
    }

    public static void initDriver(String type) {
        if (driver.get() != null) {
            return;
        }

        try {
            if ("mobile".equalsIgnoreCase(type)) {
                // Appium Android example - requires Appium server running and capabilities set via system properties
                DesiredCapabilities caps = new DesiredCapabilities();
                caps.setCapability("platformName", System.getProperty("platformName", "Android"));
                caps.setCapability("deviceName", System.getProperty("deviceName", "emulator-5554"));
                caps.setCapability("appPackage", System.getProperty("appPackage", ""));
                caps.setCapability("appActivity", System.getProperty("appActivity", ""));
                URL appiumUrl = new URL(System.getProperty("appium.server", "http://127.0.0.1:4723/wd/hub"));
                // use raw types for compatibility with installed Appium java-client
                AppiumDriver appium = new AndroidDriver(appiumUrl, caps);
                setDriver(appium);
            } else {
                // Desktop web example - support multiple browsers and use WebDriverManager to provision binaries
                String browser = System.getProperty("browser", "chrome").toLowerCase();
                WebDriver wd;

                switch (browser) {
                    case "firefox":
                        WebDriverManager.firefoxdriver().setup();
                        wd = new FirefoxDriver();
                        break;
                    case "edge":
                        WebDriverManager.edgedriver().setup();
                        wd = new EdgeDriver();
                        break;
                    case "chrome":
                    default:
                        WebDriverManager.chromedriver().setup();
                        ChromeOptions options = new ChromeOptions();
                        options.addArguments("--no-sandbox");
                        options.addArguments("--disable-dev-shm-usage");
                        // allow headless execution via system property
                        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
                            options.addArguments("--headless=new");
                        }
                        wd = new ChromeDriver(options);
                        break;
                }

                // Optional Healenium wrapper if available at runtime - use reflection so build succeeds without the lib
                try {
                    Class<?> shClass = Class.forName("com.epam.healenium.SelfHealingDriver");
                    java.lang.reflect.Method createMethod = shClass.getMethod("create", WebDriver.class);
                    WebDriver healed = (WebDriver) createMethod.invoke(null, wd);
                    setDriver(healed);
                } catch (ClassNotFoundException cnf) {
                    // Healenium not on classpath, use raw driver
                    setDriver(wd);
                } catch (Throwable t) {
                    // Any other error while wrapping, fall back to raw driver
                    setDriver(wd);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize driver", e);
        }
    }

    public static void quitDriver() {
        WebDriver wd = driver.get();
        if (wd != null) {
            try { wd.quit(); } catch (Exception ignored) {}
            driver.remove();
        }
    }
}
