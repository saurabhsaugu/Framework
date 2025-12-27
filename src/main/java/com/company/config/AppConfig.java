package com.company.config;

import com.github.javafaker.Faker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import com.company.driver.DriverManager;
import org.openqa.selenium.WebDriver;

@Configuration
@ComponentScan(basePackages = {"com.company"})
public class AppConfig {

    @Bean
    public Faker faker() {
        return new Faker();
    }

    // Provide a WebDriver bean using the existing DriverManager initialization logic.
    // This allows Cucumber + Spring to inject WebDriver into step classes.
    @Bean
    public WebDriver webDriver() {
        // initialize driver as "web" (keeps existing behavior in DriverManager)
        DriverManager.initDriver("web");
        return DriverManager.getDriver();
    }

    // Ensure driver is quit when the Spring context is closed
    @Bean
    public org.springframework.beans.factory.DisposableBean driverCloser() {
        return () -> DriverManager.quitDriver();
    }
}
