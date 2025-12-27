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
import com.company.config.HealeniumConfig;

// Simple thread-safe singleton driver manager supporting Selenium and Appium.
public class DriverManager {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    // Configuration is delegated to HealeniumConfig (reads system properties and properties file)
    private static String getConfig(String key, String defaultValue) {
        return HealeniumConfig.get(key, defaultValue);
    }

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

                // Healenium is optional. Enable explicitly via -Dhealenium.enabled=true and ensure the dependency is on the classpath.
                boolean healeniumEnabled = Boolean.parseBoolean(getConfig("healenium.enabled", "false"));
                if (healeniumEnabled) {
                    // Optional backend health-check: if user specifies a backend URL and it is unreachable, skip Healenium to avoid exceptions
                    String backendUrl = getConfig("healenium.backend.url", "");
                    if (backendUrl != null && !backendUrl.isEmpty()) {
                        try {
                            java.net.URL url = new java.net.URL(backendUrl);
                            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                            conn.setConnectTimeout(2000);
                            conn.setReadTimeout(2000);
                            conn.setRequestMethod("GET");
                            int code = conn.getResponseCode();
                            if (code < 200 || code >= 400) {
                                System.out.println("[DriverManager] Healenium backend unreachable (status=" + code + "). Skipping Healenium wrapper. URL: " + backendUrl);
                                setDriver(wd);
                            }
                        } catch (Exception e) {
                            System.out.println("[DriverManager] Healenium backend not reachable: " + e.getMessage() + ". Skipping Healenium wrapper. URL: " + backendUrl);
                            setDriver(wd);
                        }
                    }

                    try {
                        Class<?> shClass = Class.forName("com.epam.healenium.SelfHealingDriver");
                        WebDriver healed = null;

                        // Try known factory method first: create(WebDriver)
                        try {
                          java.lang.reflect.Method createMethod = shClass.getMethod("create", WebDriver.class);
                          Object obj = createMethod.invoke(null, wd);
                          if (obj instanceof WebDriver) healed = (WebDriver) obj;
                        } catch (NoSuchMethodException ignore) {
                          // try constructor fallback
                        }

                        // Try constructor(WebDriver)
                        if (healed == null) {
                            try {
                                // Look for any constructor that accepts a single parameter compatible with WebDriver
                                java.lang.reflect.Constructor<?>[] ctors = shClass.getConstructors();
                                for (java.lang.reflect.Constructor<?> c : ctors) {
                                    Class<?>[] params = c.getParameterTypes();
                                    if (params.length == 1 && params[0].isAssignableFrom(WebDriver.class)) {
                                        Object obj = c.newInstance(wd);
                                        if (obj instanceof WebDriver) {
                                            healed = (WebDriver) obj;
                                            System.out.println("[DriverManager] Healenium wrapper created via compatible constructor: " + c);
                                            break;
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {
                                // no suitable constructor or instantiation failed
                            }
                        }

                        if (healed != null) {
                          System.out.println("[DriverManager] Healenium SelfHealingDriver applied");
                          setDriver(healed);
                        } else {
                          System.out.println("[DriverManager] Healenium present but could not create wrapper; using raw driver");
                          setDriver(wd);
                        }
                    } catch (ClassNotFoundException cnf) {
                        System.out.println("[DriverManager] Healenium not found on classpath. To enable add Healenium dependency and set -Dhealenium.enabled=true");
                        setDriver(wd);
                    } catch (Throwable t) {
                        System.err.println("[DriverManager] Error while applying Healenium: " + t.getMessage());
                        setDriver(wd);
                    }
                } else {
                    // Not enabled - use raw driver
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
