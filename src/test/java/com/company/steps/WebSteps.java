package com.company.steps;

import com.company.driver.DriverManager;
import com.company.pages.HomePage;
import com.github.javafaker.Faker;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.testng.Assert;
import org.openqa.selenium.WebDriver;

public class WebSteps {

    private WebDriver driver;
    private HomePage home;
    private final Faker faker = new Faker();

    @Given("I open the application")
    public void openApplication() {
        DriverManager.initDriver("web");
        driver = DriverManager.getDriver();
        home = new HomePage(driver);
        home.open("https://example.com");
    }

    @Then("the title should contain {string}")
    public void titleShouldContain(String expected) {
        String actual = driver.getTitle();
        // Use Faker once to show AI faker usage
        String randomName = faker.name().firstName();
        System.out.println("Generated test data: " + randomName);
        Assert.assertTrue(actual.contains(expected), "Title did not contain expected text");
    }

    @Then("the message should displayed on the screen {string}")
    public void messageShouldBeDisplayed(String expected) {
        // Use PageObject's header message (initialized via PageFactory)
        String body = home != null ? home.getHeaderMessage() : null;
        Assert.assertTrue(body != null && body.contains(expected), "Expected message was not displayed on the screen");
    }
}
