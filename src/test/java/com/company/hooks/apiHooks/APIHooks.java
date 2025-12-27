package com.company.hooks.apiHooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Autowired;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import io.restassured.RestAssured;
import com.company.config.APIRequestFactory;

@ScenarioScope
public class APIHooks {

    // ThreadLocal specs for parallel test execution
    private static final ThreadLocal<RequestSpecification> requestSpec = new ThreadLocal<>();
    private static final ThreadLocal<ResponseSpecification> commonResponseSpec = new ThreadLocal<>();

    @Autowired
    private APIRequestFactory apiFactory;

    // Accessors for step classes
    public static RequestSpecification getRequestSpec() {
        return requestSpec.get();
    }

    public static ResponseSpecification getCommonResponseSpec() {
        return commonResponseSpec.get();
    }

    @Before
    public void beforeScenario() {
        // Create a fresh, non-proxied RequestSpecification for each scenario using the factory
        requestSpec.set(apiFactory.createRequestSpec());

        // Common response expectations (content-type JSON). Status codes vary per test so not set here.
        ResponseSpecBuilder respBuilder = new ResponseSpecBuilder();
        respBuilder.expectContentType(ContentType.JSON);
        commonResponseSpec.set(respBuilder.build());

        // Optional global RestAssured settings (also set in factory to be safe)
        RestAssured.useRelaxedHTTPSValidation();
    }

    @After
    public void afterScenario() {
        // Clear any state if needed
        requestSpec.remove();
        commonResponseSpec.remove();
    }

    // Helper to create a response spec for a specific expected status code
    public static ResponseSpecification responseSpecFor(int expectedStatusCode) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(expectedStatusCode);
        builder.expectContentType(ContentType.JSON);
        return builder.build();
    }
}
