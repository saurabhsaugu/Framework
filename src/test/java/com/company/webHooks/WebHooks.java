package com.company.webHooks;

import com.company.driver.DriverManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;

public class WebHooks {

    @Before
    public void beforeScenario() {
        // Default to web; can be overridden with -Ddriver=mobile
        String driverType = System.getProperty("driver", "web");
        DriverManager.initDriver(driverType);
    }

    @After
    public void afterScenario() {
        DriverManager.quitDriver();
    }
}

