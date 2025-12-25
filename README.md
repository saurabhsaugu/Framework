QEArchitect Framework

Lightweight test automation framework combining Cucumber, TestNG, Selenium/Appium and RestAssured for API tests.

Prerequisites
- Java 17
- Maven 3.6+
- Network access for external API tests (jsonplaceholder.typicode.com used in example)

Structure (important folders)
- src/test/resources/features - Cucumber feature files (api/ and web/)
- src/test/java/com/company/steps - Cucumber step definitions
- src/test/java/com/company/hooks - Cucumber hooks (including APIHooks)
- src/test/java/com/company/runner - TestNG/Cucumber runner classes
- src/test/java/com/company/listeners - TestNG listeners (RetryAnalyzer, RetryListener, Extent listener)
- src/test/java/com/company/config - Cucumber + Spring test configuration

Key dependencies (from pom.xml)
- Cucumber (cucumber-java, cucumber-testng, cucumber-spring)
- TestNG
- RestAssured (test scope)
- Spring (spring-context, spring-test)
- Selenium / Appium (for web/mobile tests)
- JavaFaker, ExtentReports, WebDriverManager

How to run
- Default suite (testng.xml) is configured in the Maven Surefire plugin, so the simplest run is:
  - mvn test
- To run a single TestNG/Cucumber runner class directly:
  - mvn -Dtest=APITestNGRunner test
  - Note: when you explicitly pick tests with -Dtest, Surefire will not use the suite Xml. Use the suite run (mvn test) to execute the testng.xml-defined suite.
- To override the suite file from Maven, pass the Surefire property:
  - mvn -Dsurefire.suiteXmlFiles=path/to/other-testng.xml test
- Compile tests only (no execution): mvn -DskipTests=true test-compile

API testing notes
- APIHooks sets up a RequestSpecification (baseUri, content-type) used by API step definitions. Consider converting APIHooks to a Spring-managed component for cleaner DI.
- Use APIHooks.responseSpecFor(status) to validate expected response status + JSON content-type.
- RestAssured is declared with test scope in pom.xml (available during test runs).

Cucumber + Spring
- cucumber-spring is included and a test CucumberSpringConfiguration is present to bootstrap Spring for Cucumber scenarios. Step definitions can @Autowired Spring beans (demonstrated by injecting Faker into APISteps).

Retry behavior
- RetryAnalyzer and RetryListener are provided. The Surefire-run suite (testng.xml) registers listeners; RetryListener also attempts to attach the analyzer at runtime to Cucumber-generated methods so retries work when running runners directly.

Thread-safety and parallelism
- APIHooks currently exposes static RequestSpecification/ResponseSpecification. If you plan to run scenarios in parallel, convert specs to ThreadLocal or make APIHooks a Spring @Component and provide per-scenario instances.

Troubleshooting
- If Cucumber steps can't find a step definition, ensure the runner glue includes the package containing the steps and config classes.
- If DI doesn't work, confirm cucumber-spring is on the test classpath and CucumberSpringConfiguration is on the glue path.
- If retries aren't being applied, run with the suite (mvn test) or enable the runtime attachment logic in RetryListener (already implemented in listener but may depend on TestNG/Surefire versions).

Contact
- Repository owner / maintainer: com.company (adjust as appropriate)
