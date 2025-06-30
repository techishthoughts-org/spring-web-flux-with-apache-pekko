Feature: Stock Retrieval
  As a user of the stocks application
  I want to retrieve stock information
  So that I can make informed investment decisions

  Scenario: Retrieve a stock by symbol
    Given the stock service is available
    When I request stock information for symbol "AAPL"
    Then I should receive stock information for "AAPL"
    And the stock information should include company name "Apple Inc."

  Scenario: Retrieve all stocks
    Given the stock service is available
    When I request information for all stocks
    Then I should receive a list of stocks
    And the list should contain at least 1 stock