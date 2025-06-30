package com.techishthoughts.stocks.component.stepdefs.sanity;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "finnhub.api.key=test-key",
        "logging.level.com.techishthoughts.stocks=DEBUG"
})
public class SanityCucumberTestConfiguration {
    // This class provides the Spring context configuration for sanity Cucumber tests
}
