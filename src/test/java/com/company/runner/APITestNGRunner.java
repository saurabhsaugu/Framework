package com.company.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.Listeners;

@CucumberOptions(
        features = "src/test/resources/features/api",
        glue = {"com.company.steps", "com.company.hooks.apiHooks", "com.company.config"},
        plugin = {"pretty", "json:target/cucumber.json"}
)
@Listeners({com.company.listeners.ExtentTestNGListener.class, com.company.listeners.RetryListener.class})
public class APITestNGRunner extends AbstractTestNGCucumberTests {
    // Inherits run logic from AbstractTestNGCucumberTests
}

