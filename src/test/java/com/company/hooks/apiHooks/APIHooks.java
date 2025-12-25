package com.company.hooks.apiHooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import io.restassured.RestAssured;

public class APIHooks {

    // Public specs for step classes to reuse
    public static RequestSpecification requestSpec;
    public static ResponseSpecification commonResponseSpec;

    @Before
    public void beforeScenario() {
        // Configure base URI and default headers once per scenario
        RequestSpecBuilder reqBuilder = new RequestSpecBuilder();
        reqBuilder.setBaseUri("https://jsonplaceholder.typicode.com");
        reqBuilder.setContentType("application/json; charset=UTF-8");
        requestSpec = reqBuilder.build();

        // Common response expectations (content-type JSON). Status codes vary per test so not set here.
        ResponseSpecBuilder respBuilder = new ResponseSpecBuilder();
        respBuilder.expectContentType(ContentType.JSON);
        commonResponseSpec = respBuilder.build();

        // Optional global RestAssured settings
        RestAssured.useRelaxedHTTPSValidation();
    }

    @After
    public void afterScenario() {
        // Clear any state if needed
        requestSpec = null;
        commonResponseSpec = null;
    }

    // Helper to create a response spec for a specific expected status code
    public static ResponseSpecification responseSpecFor(int expectedStatusCode) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(expectedStatusCode);
        builder.expectContentType(ContentType.JSON);
        return builder.build();
    }
}
