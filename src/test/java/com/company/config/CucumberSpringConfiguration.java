package com.company.config;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.test.context.ContextConfiguration;

@CucumberContextConfiguration
@ContextConfiguration(classes = AppConfig.class)
public class CucumberSpringConfiguration {
    // Cucumber will bootstrap Spring using AppConfig for tests
}