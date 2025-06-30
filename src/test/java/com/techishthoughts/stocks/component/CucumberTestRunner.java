package com.techishthoughts.stocks.component;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/stock_retrieval.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.techishthoughts.stocks.component.stepdefs.stock")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class CucumberTestRunner {
    // This class is a test runner for Cucumber stock retrieval tests
    // It targets only the stock_retrieval.feature file with specific step definitions
}
