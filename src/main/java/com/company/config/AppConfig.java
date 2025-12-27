package com.company.config;

import com.github.javafaker.Faker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.company"})
public class AppConfig {

    @Bean
    public Faker faker() {
        return new Faker();
    }
}
