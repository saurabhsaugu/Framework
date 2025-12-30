package com.company.steps;

import com.company.pages.HomePage;
import com.github.javafaker.Faker;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.testng.Assert;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import io.cucumber.spring.ScenarioScope;

@ScenarioScope
public class MobileWebSteps {

    @Autowired
    private WebDriver driver;

    private HomePage home;

    @Autowired
    private Faker faker;

    @Given("I open the mobile browser to {string}")
    public void openUrl(String url) {
        // Initialize page object using the injected WebDriver
        home = new HomePage(driver);
        home.open(url);
    }

    @Then("the page title should contain {string}")
    public void titleShouldContain(String expected) {
        String actual = driver.getTitle();
        if (actual == null) actual = "";
        // Use Faker once to demonstrate test data usage
        String randomName = faker.name().firstName();
        System.out.println("Generated test data: " + randomName);
        Assert.assertTrue(actual.toLowerCase().contains(expected.toLowerCase()), "Title did not contain expected text");
    }
}
