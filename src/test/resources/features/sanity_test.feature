Feature: Stock Application Sanity Tests
  As a user of the stocks application
  I want to verify that the application is working correctly
  So that I can trust the data it provides

  Scenario: Application health check
    When I check the application health
    Then the application should be healthy

  Scenario: Retrieve stock information via REST API
    When I request stock information for symbol "AAPL"
    Then I should receive a valid response
    And the response should contain stock information for "AAPL"

  Scenario: Retrieve all stocks via REST API
    When I request information for all stocks
    Then I should receive a valid response
    And the response should contain a list of stocks
