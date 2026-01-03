package com.company.config;

import org.openqa.selenium.remote.DesiredCapabilities;

public class MobileCapabilityFactory {

    public static DesiredCapabilities getLocalCapabilities(String platform, String runType) {
        DesiredCapabilities caps = new DesiredCapabilities();

        if (platform.equalsIgnoreCase("android")) {
            if (runType.equalsIgnoreCase("web")) {
                // Mobile Web on Chrome
                caps.setCapability("platformName", "Android");
                caps.setCapability("appium:deviceName", getConfig("mobile.android.deviceName", "AndroidPhone"));
                caps.setCapability("appium:platformVersion", getConfig("mobile.android.platformVersion", "13.0"));
                caps.setCapability("appium:automationName", getConfig("mobile.android.automationName", "UiAutomator2"));
                caps.setCapability("browserName", getConfig("mobile.android.browserName", "Chrome"));

                // chromedriver autodownload and executable directory
                boolean chromedriverAutodownload = Boolean.parseBoolean(getConfig("appium.chromedriver.autodownload", "true"));
                caps.setCapability("appium:chromedriverAutodownload", chromedriverAutodownload);
                caps.setCapability("chromedriverAutodownload", chromedriverAutodownload);

                String defaultTmp = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "appium-chromedrivers";
                String chromeDir = getConfig("appium.chromedriver.dir", defaultTmp);
                caps.setCapability("appium:chromedriverExecutableDir", chromeDir);
                caps.setCapability("chromedriverExecutableDir", chromeDir);

                // Allow overriding chromedriver executable via PropertyConfig (system property or properties file)
                String explicitChromedriver = getConfig("appium.chromedriver.path", "");
                if ((explicitChromedriver == null || explicitChromedriver.isEmpty())) {
                    // fallback to environment variable if PropertyConfig doesn't have it
                    explicitChromedriver = System.getenv("APPIUM_CHROMEDRIVER");
                }
                if (explicitChromedriver != null && !explicitChromedriver.isEmpty()) {
                    caps.setCapability("appium:chromedriverExecutable", explicitChromedriver);
                    caps.setCapability("chromedriverExecutable", explicitChromedriver);
                }
            } else {
                // Native App
                caps.setCapability("platformName", "Android");
                caps.setCapability("deviceName", getConfig("mobile.android.deviceName", "AndroidPhone"));
                caps.setCapability("automationName", getConfig("mobile.android.automationName", "UiAutomator2"));
                caps.setCapability("app", System.getProperty("user.dir") + "/apps/android-app.apk");
            }
        } else if (platform.equalsIgnoreCase("ios")) {
            if (runType.equalsIgnoreCase("web")) {
                // Mobile Web on Safari
                caps.setCapability("platformName", "iOS");
                caps.setCapability("deviceName", getConfig("mobile.ios.deviceName", "iPhone 14"));
                caps.setCapability("automationName", getConfig("mobile.ios.automationName", "XCUITest"));
                caps.setCapability("browserName", getConfig("mobile.ios.browserName", "Safari"));
            } else {
                // Native App
                caps.setCapability("platformName", "iOS");
                caps.setCapability("deviceName", getConfig("mobile.ios.deviceName", "iPhone 14"));
                caps.setCapability("automationName", getConfig("mobile.ios.automationName", "XCUITest"));
                caps.setCapability("app", System.getProperty("user.dir") + "/apps/ios-app.app");
            }
        }
        return caps;
    }

    public static DesiredCapabilities getContainerCapabilities(String platform, String runType) {
        DesiredCapabilities caps = getLocalCapabilities(platform, runType);
        // Add container-specific overrides if needed
        return caps;
    }

    public static DesiredCapabilities getSauceLabsCapabilities(String platform, String runType) {
        DesiredCapabilities caps = new DesiredCapabilities();
        // Prefer properties loaded via PropertyConfig, fall back to environment variables
        String sauceUser = getConfig("sauce.username", System.getenv("SAUCE_USERNAME") == null ? "" : System.getenv("SAUCE_USERNAME"));
        String sauceKey = getConfig("sauce.accessKey", System.getenv("SAUCE_ACCESS_KEY") == null ? "" : System.getenv("SAUCE_ACCESS_KEY"));
        if (sauceUser != null && !sauceUser.isEmpty()) caps.setCapability("username", sauceUser);
        if (sauceKey != null && !sauceKey.isEmpty()) caps.setCapability("accessKey", sauceKey);

        if (platform.equalsIgnoreCase("android")) {
            if (runType.equalsIgnoreCase("web")) {
                caps.setCapability("platformName", getConfig("sauce.platformName", "Android"));
                caps.setCapability("appium:deviceName", getConfig("sauce.deviceName", "Android GoogleAPI Emulator"));
                caps.setCapability("appium:platformVersion", getConfig("sauce.platformVersion", "12.0"));
                caps.setCapability("browserName", getConfig("sauce.browserName", "Chrome"));
                // Allow sauce-specific chromedriver autodownload preference
                boolean cdAuto = Boolean.parseBoolean(getConfig("sauce.chromedriver.autodownload", "true"));
                caps.setCapability("appium:chromedriverAutodownload", cdAuto);
                caps.setCapability("chromedriverAutodownload", cdAuto);
            } else {
                caps.setCapability("platformName", getConfig("sauce.platformName", "Android"));
                caps.setCapability("deviceName", getConfig("sauce.deviceName", "Android GoogleAPI Emulator"));
                caps.setCapability("platformVersion", getConfig("sauce.platformVersion", "12.0"));
                caps.setCapability("app", getConfig("sauce.android.app", "sauce-storage:android-app.apk"));
            }
        } else if (platform.equalsIgnoreCase("ios")) {
            if (runType.equalsIgnoreCase("web")) {
                caps.setCapability("platformName", getConfig("sauce.platformName", "iOS"));
                caps.setCapability("deviceName", getConfig("sauce.deviceName", "iPhone Simulator"));
                caps.setCapability("platformVersion", getConfig("sauce.platformVersion", "15.0"));
                caps.setCapability("browserName", getConfig("sauce.browserName", "Safari"));
            } else {
                caps.setCapability("platformName", getConfig("sauce.platformName", "iOS"));
                caps.setCapability("deviceName", getConfig("sauce.deviceName", "iPhone Simulator"));
                caps.setCapability("platformVersion", getConfig("sauce.platformVersion", "15.0"));
                caps.setCapability("app", getConfig("sauce.ios.app", "sauce-storage:ios-app.zip"));
            }
        }
        return caps;
    }

    // Configuration is delegated to PropertyConfig (reads system properties and properties file)
    private static String getConfig(String key, String defaultValue) {
        return PropertyConfig.get(key, defaultValue);
    }

}