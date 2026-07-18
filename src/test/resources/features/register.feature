@regression
Feature: Customer Account Registration
  As a new customer
  I want to register for a bank account online
  So that I can access the full range of ParaBank's banking services

  Background:
    Given a new customer is on the registration page

  # ── Positive ──────────────────────────────────────────────────────────────

  @positive
  Scenario: A new customer can open a bank account with their personal information
    When the customer provides their personal information to complete registration
    And the customer submits the registration form
    Then the customer's account should be created successfully

  @positive
  Scenario: A new customer can open a bank account with a freshly generated profile
    When the customer provides a freshly generated profile to complete registration
    And the customer submits the registration form
    Then the customer's account should be created successfully

  @positive
  Scenario: A customer can open a bank account using details provided by an external source
    When the customer provides registration details supplied from an external source
    And the customer submits the registration form
    Then the customer's account should be created successfully

  # ── Input Validation ──────────────────────────────────────────────────────

  @negative @validation
  Scenario: Registration is rejected when the customer does not provide a username
    When the customer completes the form without providing a username
    And the customer attempts to submit the incomplete registration form
    Then the customer should see a registration validation error
