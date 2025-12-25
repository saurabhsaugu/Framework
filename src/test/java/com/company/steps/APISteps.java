package com.company.steps;

import com.company.apiHooks.APIHooks;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;

public class APISteps {

    private Response response;

    @When("I GET {string}")
    public void i_get(String path) {
        response = RestAssured
                .given()
                .spec(APIHooks.requestSpec)
                .when()
                .get(path)
                .andReturn();
    }

    @When("I POST to {string} with body")
    public void i_post_with_body(String path, String body) {
        response = RestAssured
                .given()
                .spec(APIHooks.requestSpec)
                .body(body)
                .when()
                .post(path)
                .andReturn();
    }

    @Then("status code should be {int}")
    public void status_code_should_be(int expected) {
        // Use a ResponseSpecification built for the expected status to validate response
        response.then().spec(APIHooks.responseSpecFor(expected));
    }

    @Then("response JSON path {string} should not be empty")
    public void response_json_path_should_not_be_empty(String jsonPath) {
        String val = response.jsonPath().getString(jsonPath);
        Assert.assertNotNull(val, "JSON path " + jsonPath + " was null");
        Assert.assertFalse(val.trim().isEmpty(), "JSON path " + jsonPath + " was empty");
    }

    @Then("response has integer id greater than {int}")
    public void response_has_integer_id_greater_than(int threshold) {
        int id = response.jsonPath().getInt("id");
        Assert.assertTrue(id > threshold, "id was not greater than " + threshold + " (was " + id + ")");
    }
}
