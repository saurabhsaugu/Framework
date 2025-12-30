Feature: Mobile web smoke
  Scenario: Open example.com on mobile web and verify title
    Given I open the mobile browser to "https://example.com"
    Then the page title should contain "example"

