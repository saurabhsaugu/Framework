package com.company.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class HomePage {
    private final WebDriver driver;
    private final String url = System.getProperty("app.url", "https://example.com");

    public HomePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // Initialized by PageFactory at runtime
    @SuppressWarnings("unused")
    @FindBy(xpath = "/html/body/div/h1")
    private WebElement headerMessage;

    public String getHeaderMessage() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.visibilityOf(headerMessage));
            return headerMessage != null ? headerMessage.getText() : null;
        } catch (Exception e) {
            return null;
        }
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
