@regression
Feature: User Registration

  @hardcoded
  Scenario: Register new user successfully with hard-coded data
    Given User navigates to Register page
    When User fills registration form with hard data
    And User submits registration
    Then User account should be created

  @ipsum
  Scenario: Register new user successfully with LoremIpsum data
    Given User navigates to Register page
    When User fills registration form with ipsum data
    And User submits registration
    Then User account should be created

  @ddt
  Scenario: Register new user successfully with Excel data
    Given User navigates to Register page
    When User fills registration form with excel data
    And User submits registration
    Then User account should be created

  @fail
  Scenario: Register should fail without username
    Given User navigates to Register page
    When User fills registration form without username
    And User submits registration expecting failure
    Then User should see error message
