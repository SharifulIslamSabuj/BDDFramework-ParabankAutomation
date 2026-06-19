@smoke
Feature: ParaBank Login
  Verify that the login page correctly handles valid and invalid inputs.

  Scenario: Successful login
    Given User is on Login Page
    When User enters username "sqa" and password "sqa"
    And User clicks login button
    Then User should see logout link

  # 1️⃣ Invalid credentials
  @negative
  Scenario: Login with invalid username and valid password
    Given User is on Login Page
    When User enters username "invalidUser" and password "validPass123"
    And User clicks login button
    Then User should see error message "The username and password could not be verified."

  @negative
  Scenario: Login with valid username and invalid password
    Given User is on Login Page
    When User enters username "validUser" and password "wrongPass"
    And User clicks login button
    Then User should see error message "The username and password could not be verified."

  @negative
  Scenario: Login with blank username and valid password
    Given User is on Login Page
    When User leaves username blank and enters password "validPass123"
    And User clicks login button
    Then User should see error message "Please enter a username and password."

  @negative
  Scenario: Login with valid username and blank password
    Given User is on Login Page
    When User enters username "validUser" and leaves password blank
    And User clicks login button
    Then User should see error message "Please enter a username and password."

  @negative
  Scenario: Login with blank username and blank password
    Given User is on Login Page
    When User leaves username and password blank
    And User clicks login button
    Then User should see error message "Please enter a username and password."

  # 2️⃣ SQL Injection / Script Injection
  @negative
  Scenario: SQL injection attempt in username
    Given User is on Login Page
    When User enters username "'; DROP TABLE users; --" and password "validPass123"
    And User clicks login button
    Then User should see error message "The username and password could not be verified."

  @negative
  Scenario: SQL injection attempt in password
    Given User is on Login Page
    When User enters username "validUser" and password "' OR '1'='1"
    And User clicks login button
    Then User should see error message "The username and password could not be verified."

  @negative
  Scenario: Script injection attempt in username
    Given User is on Login Page
    When User enters username "<script>alert('xss')</script>" and password "validPass123"
    And User clicks login button
    Then User should see error message "The username and password could not be verified."

  # 3️⃣ Input validation / boundary checks
  @negative
  Scenario: Username exceeds maximum allowed length
    Given User is on Login Page
    When User enters username "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" and password "validPass123"
    And User clicks login button
    Then User should see error message "The username and password could not be verified."

  @negative
  Scenario: Password exceeds maximum allowed length
    Given User is on Login Page
    When User enters username "validUser" and password "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    And User clicks login button
    Then User should see error message "The username and password could not be verified."

  @negative
  Scenario: Username contains invalid characters
    Given User is on Login Page
    When User enters username "user@!#" and password "validPass123"
    And User clicks login button
    Then User should see error message "The username and password could not be verified."

  @negative
  Scenario: Password contains only spaces
    Given User is on Login Page
    When User enters username "validUser" and password "    "
    And User clicks login button
    Then User should see error message "The username and password could not be verified."

  # 4️⃣ Locked or inactive account
  @negative
  Scenario: Login with a locked account
    Given User is on Login Page
    When User enters username "lockedUser" and password "validPass123"
    And User clicks login button
    Then User should see error message "The username and password could not be verified."
