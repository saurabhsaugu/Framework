package com.company.config;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.test.context.ContextConfiguration;

@CucumberContextConfiguration
@ContextConfiguration(classes = {AppConfig.class, WebDriverFactory.class, APIRequestFactory.class})
public class CucumberSpringConfiguration {
    // Cucumber will bootstrap Spring using the test-scoped factory classes for tests
}