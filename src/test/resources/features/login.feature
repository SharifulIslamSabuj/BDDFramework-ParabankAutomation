@regression
Feature: Customer Sign-In
  As a registered customer
  I want to sign in to my online banking account
  So that I can access my personal banking services securely

  Background:
    Given a registered customer is on the sign-in page

  # ── Positive ──────────────────────────────────────────────────────────────

  @smoke @positive
  Scenario: A registered customer can sign in with valid credentials
    When the customer signs in with username "sqa" and password "sqa"
    And the customer submits their login credentials
    Then the customer is successfully signed in

  # ── Authentication Validation ─────────────────────────────────────────────

  @negative @validation
  Scenario Outline: Sign-in is rejected when credentials do not match any registered account
    When the customer signs in with username "<username>" and password "<password>"
    And the customer submits their login credentials
    Then the customer is shown the sign-in error "The username and password could not be verified."

    Examples:
      | username    | password     |
      | invalidUser | validPass123 |
      | validUser   | wrongPass    |

  # ── Input Validation ──────────────────────────────────────────────────────

  @negative @validation
  Scenario Outline: Sign-in is rejected when required credential fields are not provided
    When the customer signs in with username "<username>" and password "<password>"
    And the customer submits their login credentials
    Then the customer is shown the sign-in error "Please enter a username and password."

    Examples:
      | username  | password     |
      |           | validPass123 |
      | validUser |              |
      |           |              |

  @negative @validation
  Scenario Outline: Sign-in is rejected when credentials do not meet format requirements
    When the customer signs in with username "<username>" and password "<password>"
    And the customer submits their login credentials
    Then the customer is shown the sign-in error "The username and password could not be verified."

    Examples:
      | username                                           | password                                           |
      | aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa | validPass123                                       |
      | validUser                                          | aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa |
      | user@!#                                            | validPass123                                       |

  @negative @validation
  Scenario: Sign-in is rejected when the password consists entirely of whitespace
    When the customer signs in with username "validUser" and password "    "
    And the customer submits their login credentials
    Then the customer is shown the sign-in error "The username and password could not be verified."

  # ── Security Validation ───────────────────────────────────────────────────

  @negative @security
  Scenario Outline: Sign-in is protected against injection and scripting attacks
    When the customer signs in with username "<username>" and password "<password>"
    And the customer submits their login credentials
    Then the customer is shown the sign-in error "The username and password could not be verified."

    Examples:
      | username                       | password    |
      | '; DROP TABLE users; --        | validPass123 |
      | validUser                      | ' OR '1'='1 |
      | <script>alert('xss')</script>  | validPass123 |

  # ── Account State Validation ──────────────────────────────────────────────

  @negative
  Scenario: Sign-in is rejected for a locked or deactivated account
    When the customer signs in with username "lockedUser" and password "validPass123"
    And the customer submits their login credentials
    Then the customer is shown the sign-in error "The username and password could not be verified."
