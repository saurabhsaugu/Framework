package com.company.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.Listeners;

@CucumberOptions(
        features = "src/test/resources/features/mobile",
        glue = {"com.company.steps", "com.company.hooks.mobileHooks", "com.company.config"},
        plugin = {"pretty", "json:target/cucumber.json"}
)
@Listeners({com.company.listeners.ExtentTestNGListener.class, com.company.listeners.RetryListener.class})
public class MobileTestNGRunner extends AbstractTestNGCucumberTests {
    // Inherits run logic from AbstractTestNGCucumberTests
}
