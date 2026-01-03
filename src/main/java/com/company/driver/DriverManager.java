package com.company.driver;

import com.company.config.MobileCapabilityFactory;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;
import com.company.config.PropertyConfig;

// Simple thread-safe singleton driver manager supporting Selenium and Appium via RemoteWebDriver.
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
                AppiumDriver appiumDriver;

                // parameterize environment and platform via system properties first (mvn -D...) then properties file
                String environment = System.getProperty("appium.environment");
                if (environment == null || environment.isEmpty()) {
                    environment = getConfig("appium.environment", "local");
                }

                String platform = System.getProperty("mobile.platform");
                if (platform == null || platform.isEmpty()) {
                    platform = getConfig("mobile.platform", "Android");
                }
                appiumDriver = switch (environment.toLowerCase()) {
                    case "local" -> createLocalMobileDriver(platform);
                    case "container" -> createContainerMobileDriver(platform);
                    case "saucelabs" -> createSauceLabsMobileDriver(platform);
                    default -> throw new IllegalArgumentException("Unknown environment: " + environment);
                };
                setDriver(appiumDriver);
            } else {
                // Web execution - support local, container/grid, and Sauce Labs based on properties or -D args
                String webEnv = System.getProperty("web.environment");
                if (webEnv == null || webEnv.isEmpty()) {
                    webEnv = getConfig("web.environment", "local");
                }

                String browser = System.getProperty("web.browserName");
                if (browser == null || browser.isEmpty()) {
                    browser = getConfig("web.browserName", "chrome");
                }

                String headless = System.getProperty("web.headless");
                if (headless == null || headless.isEmpty()) {
                    headless = getConfig("web.headless", "false");
                }

                WebDriver wd = switch (webEnv.toLowerCase()) {
                    case "local" -> createLocalWebDriver(browser, headless);
                    case "container" -> createContainerWebDriver(browser);
                    case "saucelabs" -> createSauceLabsWebDriver(browser);
                    default -> throw new IllegalArgumentException("Unknown web environment: " + webEnv);
                };

                boolean healeniumEnabled = Boolean.parseBoolean(getConfig("healenium.enabled", "false"));
                if (healeniumEnabled) {
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
                        try {
                          java.lang.reflect.Method createMethod = shClass.getMethod("create", WebDriver.class);
                          Object obj = createMethod.invoke(null, wd);
                          if (obj instanceof WebDriver) healed = (WebDriver) obj;
                        } catch (NoSuchMethodException ignore) {
                        }

                        if (healed == null) {
                            try {
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

    // Configuration is delegated to PropertyConfig (reads system properties and properties file)
    private static String getConfig(String key, String defaultValue) {
        return PropertyConfig.get(key, defaultValue);
    }

    // Create a local WebDriver (Chrome/Firefox/Edge) using WebDriverManager
    private static WebDriver createLocalWebDriver(String browser, String headless) {
        switch (browser.toLowerCase()) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                return new FirefoxDriver();
            case "edge":
                WebDriverManager.edgedriver().setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                if (Boolean.parseBoolean(headless)) edgeOptions.addArguments("--headless=new");
                edgeOptions.addArguments("--no-sandbox");
                edgeOptions.addArguments("--disable-dev-shm-usage");
                try {
                    String tmp = System.getProperty("java.io.tmpdir");
                    String profile = tmp + System.getProperty("file.separator") + "chrome-profile-" + Thread.currentThread().getId();
                    edgeOptions.addArguments("--user-data-dir=" + profile);
                } catch (Exception ignored) { }

                // Let Chrome pick an ephemeral remote debugging port to avoid port conflicts
                edgeOptions.addArguments("--remote-debugging-port=0");

                // Disable background throttling which can cause renderer to pause and trigger unexpected disconnects
                edgeOptions.addArguments("--disable-background-timer-throttling");
                edgeOptions.addArguments("--disable-renderer-backgrounding");
                edgeOptions.addArguments("--disable-backgrounding-occluded-windows");

                // Reduce automation banner and potential interference
                edgeOptions.setExperimentalOption("excludeSwitches", java.util.Arrays.asList("enable-automation"));
                return new EdgeDriver(edgeOptions);
            case "chrome":
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                if (Boolean.parseBoolean(headless)) chromeOptions.addArguments("--headless=new");
                chromeOptions.addArguments("--no-sandbox");
                chromeOptions.addArguments("--disable-dev-shm-usage");
                try {
                    String tmp = System.getProperty("java.io.tmpdir");
                    String profile = tmp + System.getProperty("file.separator") + "chrome-profile-" + Thread.currentThread().getId();
                    chromeOptions.addArguments("--user-data-dir=" + profile);
                } catch (Exception ignored) { }

                // Let Chrome pick an ephemeral remote debugging port to avoid port conflicts
                chromeOptions.addArguments("--remote-debugging-port=0");

                // Disable background throttling which can cause renderer to pause and trigger unexpected disconnects
                chromeOptions.addArguments("--disable-background-timer-throttling");
                chromeOptions.addArguments("--disable-renderer-backgrounding");
                chromeOptions.addArguments("--disable-backgrounding-occluded-windows");

                // Reduce automation banner and potential interference
                chromeOptions.setExperimentalOption("excludeSwitches", java.util.Arrays.asList("enable-automation"));
                return new ChromeDriver(chromeOptions);
        }
    }

    // Create a remote WebDriver pointing to a Selenium/Grid container
    private static WebDriver createContainerWebDriver(String browser) throws MalformedURLException {
        String remote = getConfig("web.remote.url", "http://docker-host:4444/wd/hub");
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("browserName", browser);
        String version = getConfig("web.browserVersion", "");
        if (version != null && !version.isEmpty()) caps.setCapability("browserVersion", version);
        // Accept additional capabilities via properties if needed
        return new RemoteWebDriver(new URL(remote), caps);
    }

    // Create a Sauce Labs remote WebDriver
    private static WebDriver createSauceLabsWebDriver(String browser) throws MalformedURLException {
        // Prefer properties (sauce.properties via PropertyConfig), fall back to environment variables
        String user = getConfig("sauce.username", "");
        if (user == null || user.isEmpty()) user = System.getenv("SAUCE_USERNAME");

        String key = getConfig("sauce.accessKey", "");
        if (key == null || key.isEmpty()) key = System.getenv("SAUCE_ACCESS_KEY");

        if (user == null || user.isEmpty() || key == null || key.isEmpty()) {
            throw new IllegalStateException("Sauce credentials not configured (sauce.username / sauce.accessKey). Provide them via src/test/resources/properties/sauce.properties or environment variables SAUCE_USERNAME/SAUCE_ACCESS_KEY.");
        }

        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("browserName", browser);
        caps.setCapability("username", user);
        caps.setCapability("accessKey", key);
        caps.setCapability("platformName", getConfig("sauce.platformName", "Windows 10"));
        caps.setCapability("browserVersion", getConfig("sauce.browserVersion", "latest"));

        // Optional Sauce-specific capabilities
        String build = getConfig("sauce.build", "");
        if (build != null && !build.isEmpty()) caps.setCapability("build", build);
        String name = getConfig("sauce.name", "");
        if (name != null && !name.isEmpty()) caps.setCapability("name", name);

        String sauceUrl = getConfig("sauce.url", "https://ondemand.saucelabs.com/wd/hub");
        return new RemoteWebDriver(new URL(sauceUrl), caps);
    }

    private static AppiumDriver createLocalMobileDriver(String platform) {
        DesiredCapabilities caps = MobileCapabilityFactory.getLocalCapabilities(platform, "web");
        // Ensure chromedriverExecutableDir exists so Appium can download chromedrivers into it
        Object dirObj = caps.getCapability("appium:chromedriverExecutableDir");
        if (dirObj == null) dirObj = caps.getCapability("chromedriverExecutableDir");
        if (dirObj != null) {
            try {
                File dir = new File(dirObj.toString());
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                    if (!created) {
                        System.out.println("[DriverManager] Warning: could not create chromedriver dir: " + dir.getAbsolutePath());
                    }
                }
            } catch (Throwable t) {
                System.out.println("[DriverManager] Warning: error ensuring chromedriver dir exists: " + t.getMessage());
            }
        }

        // If Appium cannot autodownload chromedriver, try to provision a matching chromedriver locally
        try {
            if (isAdbDeviceConnected()) {
                String chromeVersion = getChromeVersionFromDevice();
                if (chromeVersion != null) {
                    String major = chromeVersion.split("\\.")[0];
                    try {
                        System.out.println("[DriverManager] Detected Chrome on device: " + chromeVersion + " -> attempting to download matching chromedriver (major=" + major + ") locally via WebDriverManager");
                        io.github.bonigarcia.wdm.WebDriverManager.chromedriver().browserVersion(major).setup();
                        String localDriver = System.getProperty("webdriver.chrome.driver");
                        System.out.println("[DriverManager] WebDriverManager set webdriver.chrome.driver=" + localDriver);
                        if (localDriver != null && !localDriver.isEmpty()) {
                            File src = new File(localDriver);
                            if (src.exists()) {
                                // If a chromedriverExecutableDir is set in caps, copy the downloaded binary there so Appium can use it
                                Object targetDirObj = caps.getCapability("appium:chromedriverExecutableDir");
                                if (targetDirObj == null) targetDirObj = caps.getCapability("chromedriverExecutableDir");
                                if (targetDirObj != null) {
                                    try {
                                        File targetDir = new File(targetDirObj.toString());
                                        if (!targetDir.exists()) targetDir.mkdirs();
                                        File dest = new File(targetDir, src.getName());
                                        if (!dest.exists()) {
                                            java.nio.file.Files.copy(src.toPath(), dest.toPath());
                                            dest.setExecutable(true);
                                        }
                                        caps.setCapability("appium:chromedriverExecutable", dest.getAbsolutePath());
                                        caps.setCapability("chromedriverExecutable", dest.getAbsolutePath());
                                        System.out.println("[DriverManager] Copied chromedriver to: " + dest.getAbsolutePath());
                                    } catch (Throwable cpy) {
                                        System.out.println("[DriverManager] Failed to copy chromedriver to chromedriverExecutableDir: " + cpy.getMessage());
                                        // fallback to using original local path if copy failed
                                        caps.setCapability("appium:chromedriverExecutable", src.getAbsolutePath());
                                        caps.setCapability("chromedriverExecutable", src.getAbsolutePath());
                                    }
                                } else {
                                    caps.setCapability("appium:chromedriverExecutable", src.getAbsolutePath());
                                    caps.setCapability("chromedriverExecutable", src.getAbsolutePath());
                                }
                                System.out.println("[DriverManager] Using local chromedriver (exists): " + src.getAbsolutePath() + " (size=" + src.length() + ")");
                            } else {
                                System.out.println("[DriverManager] webdriver.chrome.driver was set but file not found: " + localDriver);
                            }
                        }
                    } catch (Throwable wdm) {
                        System.out.println("[DriverManager] Failed to download local chromedriver via WebDriverManager: " + wdm.getMessage());
                    }
                } else {
                    System.out.println("[DriverManager] Could not determine Chrome version on device via adb. Skipping local chromedriver provision.");
                }
            } else {
                System.out.println("[DriverManager] No adb device connected; skipping local chromedriver provision.");
            }
        } catch (Throwable t) {
            System.out.println("[DriverManager] Error while attempting local chromedriver provisioning: " + t.getMessage());
        }

        // Probe Appium status to provide clearer diagnostics if Appium can't start a session
        String appiumBase = getConfig("appium.server", "http://127.0.0.1:4723");
        String status = probeAppiumStatus(appiumBase, 2000);
        if (!"ready".equals(status)) {
            System.out.println("[DriverManager] Appium status: " + status + " (proceeding to attempt session)");
        }

        // Log capabilities for debugging (helps diagnose chromedriver issues)
        try {
            System.out.println("[DriverManager] Creating Appium session at: " + appiumBase);
            System.out.println("[DriverManager] Capabilities: " + caps.asMap());
        } catch (Throwable ignored) { }

        // Extra diagnostics: check chromedriverExecutable and directory contents so troubleshooting is easier
        try {
            Object exe = caps.getCapability("appium:chromedriverExecutable");
            if (exe == null) exe = caps.getCapability("chromedriverExecutable");
            Object exeDir = caps.getCapability("appium:chromedriverExecutableDir");
            if (exeDir == null) exeDir = caps.getCapability("chromedriverExecutableDir");

            System.out.println("[DriverManager] appium:chromedriverExecutable=" + exe);
            System.out.println("[DriverManager] appium:chromedriverExecutableDir=" + exeDir);

            if (exe != null) {
                try {
                    File f = new File(exe.toString());
                    System.out.println("[DriverManager] chromedriver file exists=" + f.exists() + ", path=" + f.getAbsolutePath() + ", size=" + (f.exists() ? f.length() : 0));
                } catch (Throwable t) {
                    System.out.println("[DriverManager] Error while checking chromedriver file: " + t.getMessage());
                }
            }

            if (exeDir != null) {
                try {
                    File d = new File(exeDir.toString());
                    if (d.exists() && d.isDirectory()) {
                        System.out.println("[DriverManager] Listing chromedriverExecutableDir (" + d.getAbsolutePath() + "): ");
                        java.nio.file.Files.list(d.toPath()).forEach(p -> System.out.println("  - " + p.getFileName() + " (size=" + p.toFile().length() + ")"));
                    } else {
                        System.out.println("[DriverManager] chromedriverExecutableDir does not exist: " + d.getAbsolutePath());
                    }
                } catch (Throwable t) {
                    System.out.println("[DriverManager] Error while listing chromedriverExecutableDir: " + t.getMessage());
                }
            }
        } catch (Throwable ignored) { }


        try {
            System.out.println("[DriverManager] Creating Appium session at: " + appiumBase);
            return new AppiumDriver(new URL(appiumBase), caps);
        } catch (Exception sessionEx) {
            System.out.println("[DriverManager] Failed to create Appium session: " + sessionEx.getClass().getName() + ": " + sessionEx.getMessage());
            try {
                String statusDetail = probeAppiumStatus(appiumBase, 2000);
                System.out.println("[DriverManager] Appium /status: " + statusDetail);
            } catch (Throwable t) { System.out.println("[DriverManager] Error probing Appium /status: " + t.getMessage()); }

            try {
                String sessions = fetchAppiumSessions(appiumBase, 2000);
                System.out.println("[DriverManager] Appium /sessions response: " + sessions);
            } catch (Throwable t) { System.out.println("[DriverManager] Error fetching Appium /sessions: " + t.getMessage()); }

            // Provide guidance for common chromedriver failures
            System.out.println("[DriverManager] Diagnostic hints: \n  - Verify Appium server logs for chromedriver download/match errors.\n  - Ensure Appium has network & write permission to chromedriverExecutableDir.\n  - If using Appium 2.x, ensure the appium-chromedriver driver/plugin is installed and enabled.\n  - As a fallback, pre-download a chromedriver matching device Chrome and set appium:chromedriverExecutable capability.\n");

            throw new RuntimeException("Failed to create Appium session. See logs above for diagnostics.", sessionEx);
        }
    }

    private static AppiumDriver createContainerMobileDriver(String platform) throws MalformedURLException {
        DesiredCapabilities caps = MobileCapabilityFactory.getContainerCapabilities(platform, "web");
        return new AppiumDriver(new URL("http://docker-host:4723/wd/hub"), caps);
    }

    private static AppiumDriver createSauceLabsMobileDriver(String platform) throws MalformedURLException {
        DesiredCapabilities caps = MobileCapabilityFactory.getSauceLabsCapabilities(platform, "web");
        return new AppiumDriver(new URL("https://ondemand.saucelabs.com/wd/hub"), caps);
    }

    // Fetches Appium /sessions (returns raw body or error string)
    private static String fetchAppiumSessions(String baseUrl, int timeoutMs) {
        try {
            String sessionsUrl = baseUrl.endsWith("/") ? baseUrl + "sessions" : baseUrl + "/sessions";
            URL url = new URL(sessionsUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            try (java.io.InputStream in = conn.getInputStream(); java.util.Scanner s = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A")) {
                String body = s.hasNext() ? s.next() : "";
                return "HTTP " + code + " -> " + (body.length() > 1000 ? body.substring(0, 1000) + "..." : body);
            }
        } catch (Throwable t) {
            return "error: " + t.getMessage();
        }
    }

    // Try to determine the Chrome version installed on the connected Android device using adb.
    private static String getChromeVersionFromDevice() {
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "shell", "dumpsys", "package", "com.android.chrome");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    // Look for lines like "versionName=103.0.5060.70" or "versionName: 103.0.5060"
                    if (line.contains("versionName=") || line.toLowerCase().contains("versionname")) {
                        int idx = line.indexOf("versionName=");
                        if (idx >= 0) {
                            String v = line.substring(idx + "versionName=".length()).trim();
                            // Strip non-digit suffixes
                            if (!v.isEmpty()) return v.split("\\s")[0];
                        } else {
                            // fallback simple parse
                            String[] parts = line.split(":");
                            if (parts.length > 1) {
                                String v = parts[1].trim();
                                if (!v.isEmpty()) return v.split("\\s")[0];
                            }
                        }
                    }
                }
            }
            p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Throwable ignored) { }
        return null;
    }

    // Probe Appium /status endpoint. Returns "ready" when OK or an error description.
    private static String probeAppiumStatus(String baseUrl, int timeoutMs) {
        try {
            String statusUrl = baseUrl.endsWith("/") ? baseUrl + "status" : baseUrl + "/status";
            URL url = new URL(statusUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 400) {
                return "HTTP " + code;
            }
            try (java.io.InputStream in = conn.getInputStream(); java.util.Scanner s = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A")) {
                String body = s.hasNext() ? s.next() : "";
                if (body.contains("\"ready\":true") || body.contains("\"status\":0") || body.toLowerCase().contains("\"ready\": true")) {
                    return "ready";
                }
                return "unready: " + (body.length() > 200 ? body.substring(0, 200) + "..." : body);
            }
        } catch (Throwable t) {
            return "error: " + t.getMessage();
        }
    }

    // Check if adb lists at least one device in 'device' state.
    // Requires 'adb' available on PATH.
    private static boolean isAdbDeviceConnected() {
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "devices");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                boolean headerSkipped = false;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (!headerSkipped) { headerSkipped = true; continue; }
                    if (line.isEmpty()) continue;
                    if (line.endsWith("\tdevice") || line.matches(".+\\s+device$")) {
                        return true;
                    }
                }
            }
            p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Throwable ignored) { }
        return false;
    }
}
