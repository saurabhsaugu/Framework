QEArchitect Framework

Lightweight test automation framework combining Cucumber, TestNG, Selenium/Appium and RestAssured for API tests.

Prerequisites
- Java 17
- Maven 3.6+
- Network access for external API tests (jsonplaceholder.typicode.com used in example)

Structure (important folders)
- src/test/resources/features - Cucumber feature files (api/ and web/)
- src/test/java/com/company/steps - Cucumber step definitions
- src/test/java/com/company/apiHooks - API common setup (RequestSpecification / ResponseSpecification)
- src/test/java/com/company/runner - TestNG/Cucumber runner classes
- src/test/java/com/company/listeners - TestNG listeners (RetryAnalyzer, RetryListener, Extent listener)

How to run
- Run all tests: mvn test
- Run only API runner: mvn -Dtest=APITestNGRunner test
- Run only Web runner: mvn -Dtest=WebTestNGRunner test
- To compile tests without executing: mvn -DskipTests=true test-compile

API testing notes
- APIHooks sets up a RequestSpecification (baseUri, content-type) used by API step definitions.
- Use APIHooks.responseSpecFor(status) to validate expected response status + JSON content-type.
- Example feature: src/test/resources/features/api/api.feature and steps in src/test/java/com/company/steps/APISteps.java

Retry behavior
- RetryAnalyzer and RetryListener exist to enable TestNG retry behavior. Some TestNG/Cucumber combinations may require additional runtime attachment for retry to apply to Cucumber-generated methods. If retries are not observed, consider enabling the runtime attachment approach in RetryListener or use a per-step retry wrapper.

Extensibility
- Base URIs, headers, and other settings can be moved to configuration and injected into APIHooks.
- APIHooks.requestSpec is currently static and not thread-local. If you run scenarios in parallel, consider converting specs to ThreadLocal instances.

Troubleshooting
- If Cucumber steps can't find a step definition, ensure the runner glue includes the package containing the steps.
- If tests fail due to network or external service issues, run them against a mock server or enable offline/mocked mode.

Contact
- Repository owner / maintainer: com.company (adjust as appropriate)

