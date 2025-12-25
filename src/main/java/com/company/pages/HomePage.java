package com.company.pages;

import org.openqa.selenium.WebDriver;

public class HomePage {
    private final WebDriver driver;
    private final String url = System.getProperty("app.url", "https://example.com");

    public HomePage(WebDriver driver) {
        this.driver = driver;
    }

    public void open() {
        driver.get(url);
    }

    public void open(String overrideUrl) {
        driver.get(overrideUrl);
    }

    public String getTitle() {
        return driver.getTitle();
    }
}

