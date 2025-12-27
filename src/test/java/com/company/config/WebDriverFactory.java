package com.company.config;

import com.company.driver.DriverManager;
import org.openqa.selenium.WebDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import io.cucumber.spring.ScenarioScope;

@Configuration
@ComponentScan(basePackages = {"com.company"})
public class WebDriverFactory {

    // Provide a WebDriver bean using the existing DriverManager initialization logic.
    // Make it scenario-scoped so each Cucumber scenario/thread gets its own instance.
    @Bean
    @ScenarioScope
    public WebDriver webDriver() {
        // initialize driver as "web" (keeps existing behavior in DriverManager)
        DriverManager.initDriver("web");
        return DriverManager.getDriver();
    }

    // Ensure driver is quit when the scenario context is closed
    @Bean
    @ScenarioScope
    public org.springframework.beans.factory.DisposableBean driverCloser() {
        return () -> DriverManager.quitDriver();
    }
}
