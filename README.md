QEArchitect Framework

Lightweight test automation framework combining Cucumber, TestNG, Selenium/Appium and RestAssured for API tests.

Prerequisites
- Java 17
- Maven 3.6+
- Network access for external API tests (jsonplaceholder.typicode.com used in examples)

Structure (important folders)
- src/test/resources/features - Cucumber feature files (api/ and web/)
- src/test/java/com/company/steps - Cucumber step definitions
- src/test/java/com/company/hooks - Cucumber hooks (including APIHooks)
- src/test/java/com/company/runner - TestNG/Cucumber runner classes
- src/test/java/com/company/listeners - TestNG listeners (RetryAnalyzer, RetryListener, Extent listener)
- src/test/java/com/company/config - Cucumber + Spring test configuration and test-scoped factories
- src/main/java/com/company/driver - DriverManager (thread-safe WebDriver handling)

Key changes (recent)
- Tests use Spring DI for test objects: a dedicated Cucumber Spring bootstrap (CucumberSpringConfiguration) wires test-scoped factories.
- WebDriver isolation:
  - DriverManager stores WebDriver in a ThreadLocal so each thread has its own instance.
  - WebDriver is provided by a Scenario-scoped factory (WebDriverFactory) so each Cucumber scenario gets an isolated driver.
  - ChromeOptions now use a per-thread user-data-dir and ephemeral DevTools port to reduce profile and port conflicts in parallel runs.
- RestAssured isolation:
  - APIRequestFactory builds a fresh RequestSpecification per scenario.
  - APIHooks stores specs in ThreadLocal and clears them after each scenario.
- Dependencies adjusted so Selenium/Appium and WebDriverManager are test-scoped. RestAssured is test-scoped.
- SLF4J binding: project uses logback-classic to avoid "no binding" warnings and enable richer logging. logback-test.xml in test resources provides defaults.

How to run
- Default suite (testng.xml) is configured in the Maven Surefire plugin. Recommended run:
  - mvn test
- To override the suite file from Maven:
  - mvn -Dsurefire.suiteXmlFiles=path/to/testng.xml test
- Compile tests only (no execution): mvn -DskipTests=true test-compile
- To run a single TestNG runner via Surefire (recommended use suite for Cucumber):
  - mvn -Dtest=APITestNGRunner test
  - Note: using -Dtest bypasses suiteXmlFiles behaviour in some Surefire versions; prefer suite runs to pick up listeners and suite-level config.

Driver selection (web vs mobile)
- The framework uses a unified `WebDriverFactory` that supports both desktop web and mobile runs. Select the target at runtime using either a JVM system property or an environment variable:
  - System property (preferred for Maven runs): `-DdriverType=mobile` or `-DdriverType=web`
  - Environment variable (CI-friendly): `DRIVER_TYPE=mobile` or `DRIVER_TYPE=web`
- Examples (PowerShell):
  - Web (headless Chrome):

    mvn test -DdriverType=web -Dbrowser=chrome -Dheadless=true

  - Mobile (Appium remote, native or mobile-web):

    mvn test -DdriverType=mobile -Dappium.server=http://127.0.0.1:4723/wd/hub -DplatformName=Android -DdeviceName=emulator-5554

- Notes:
  - `DriverManager.initDriver(String)` still controls the details for mobile vs web; the factory simply passes the selected type to DriverManager.
  - For native mobile app runs provide `-DappPackage` and `-DappActivity` (or other Appium capabilities) as needed.
  - Unknown `driverType` values fall back to the web path; consider setting `-DdriverType` explicitly in CI to avoid surprises.

Compatibility / migration notes
- The previous `MobileDriverFactory` class is present as a deprecated compatibility shim. The unified `WebDriverFactory` is the recommended entrypoint.
- `MobileWebSteps` and `WebSteps` now use the same pattern: they receive an injected `WebDriver` (scenario-scoped) and use page objects (e.g., `HomePage`) instead of starting/stopping drivers directly.

Parallel execution
- The code is prepared for parallel scenario execution, but you must enable concurrency in TestNG / Surefire:
  - Configure parallel and thread-count in testng.xml (parallel="methods" or "tests"/"classes") or via Surefire/TestNG configuration.
  - Verify thread-count against machine resources; run small thread counts first.
- Checklist before enabling parallelism:
  - No static mutable state used by tests; convert to ScenarioScope or ThreadLocal if present.
  - Healenium or proxies disabled unless verified thread-safe (-Dhealenium.enabled=true to enable).
  - Per-thread chrome profile folders are cleaned after quit to avoid disk exhaustion.
  - Avoid changing RestAssured global static config at runtime; use per-scenario RequestSpecifications.

Troubleshooting common issues
- mvn test doesn't pick up runners / testng.xml
  - Surefire is configured with a default suiteXmlFiles entry. Overriding with -Dsurefire.suiteXmlFiles=... should work; ensure you are not using -Dtest concurrently.
- Cucumber + Spring duplicate bootstrap errors
  - Ensure only one class is annotated with @CucumberContextConfiguration on the glue path. Use cucumber.glue property in src/test/resources/cucumber.properties to restrict glue scanning.
- RestAssured MissingMethodException / proxy merge issues
  - Do not pass proxied beans into RestAssured; build concrete RequestSpecification objects per scenario (APIRequestFactory) and store them in ThreadLocal (APIHooks) before using RestAssured.given().spec(...).
- WebSocket / "Connection reset" warnings from Selenium/ChromeDevTools
  - Causes: browser crash, profile conflict, DevTools port conflict, resource exhaustion, proxies, or version mismatch among Chrome/ChromeDriver/Selenium.
  - Mitigations in the project: per-thread user-data-dir, remote-debugging-port=0, flags to reduce background throttling. Additional steps:
    - Verify Chrome and chromedriver versions match.
    - Run single-threaded to reproduce; if stable, incrementally increase concurrency.
    - Capture ChromeDriver and browser logs; inspect for OOM/crash messages.
    - Disable Healenium/proxies to rule out middlemen.
    - Ensure adequate CPU/memory/disk on CI runners.

Logging
- logback-test.xml in src/test/resources configures logging for test runs. Project uses logback-classic as SLF4J binding to suppress warnings about missing bindings and provide richer logs.

Healenium
- Healenium support is optional and controlled via -Dhealenium.enabled=true and presence of the dependency on the test classpath. Default is disabled to keep tests stable by default.

Recommendations
- Before enabling high-concurrency runs, run the full suite with a small thread-count and inspect for WebSocket resets or flaky failures.
- Add cleanup of per-thread browser profile directories if running thousands of parallel sessions.
- Address security scan warnings in pom.xml if you plan to publish artifacts.

Contact
- Repository owner / maintainer: com.company (adjust as appropriate)
