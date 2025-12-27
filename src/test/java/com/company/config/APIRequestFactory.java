package com.company.config;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import io.restassured.http.ContentType;
import io.restassured.RestAssured;

@Component
@ScenarioScope
public class APIRequestFactory {

    public RequestSpecification createRequestSpec() {
        RequestSpecBuilder reqBuilder = new RequestSpecBuilder();
        reqBuilder.setBaseUri("https://jsonplaceholder.typicode.com");
        reqBuilder.setContentType("application/json; charset=UTF-8");
        RestAssured.useRelaxedHTTPSValidation();
        return reqBuilder.build();
    }

    public ResponseSpecification responseSpecFor(int expectedStatusCode) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(expectedStatusCode);
        builder.expectContentType(ContentType.JSON);
        return builder.build();
    }
}

