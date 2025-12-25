Feature: Sample web scenario
  Scenario: Open homepage
    Given I open the application
    Then the title should contain "Example"
    Then the message should displayed on the screen "Example Domain"

