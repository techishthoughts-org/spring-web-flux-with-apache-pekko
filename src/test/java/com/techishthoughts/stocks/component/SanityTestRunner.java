package com.techishthoughts.stocks.component;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/sanity_test.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.techishthoughts.stocks.component.stepdefs.sanity")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class SanityTestRunner {
    // This class is a test runner for Cucumber sanity tests
    // It targets only the sanity_test.feature file with specific step definitions
}
