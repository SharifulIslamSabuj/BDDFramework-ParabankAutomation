package com.parabank.parasoft.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * TestRunner - Cucumber + TestNG test runner
 * Executes Cucumber scenarios with parallel execution support
 * Integrates with ExtentReports for comprehensive reporting
 *
 * @author Automation Team
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {
                "com.parabank.parasoft.stepdefinitions",
                "com.parabank.parasoft.hooks"
        },
        plugin = {
                "pretty",
                "html:build/reports/cucumber/cucumber-report.html",
                "json:build/reports/cucumber/cucumber-report.json",
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        monochrome = true,
        publish = false,
        dryRun = false,
        tags = "@smoke or @negative or @regression"
)
@Test
public class TestRunner extends AbstractTestNGCucumberTests {

    /**
     * Overrides scenarios method to enable parallel execution
     * Configured to run with multiple threads based on Gradle configuration
     *
     * @return scenarios array for parallel execution
     */
    @Override
    @DataProvider(parallel = true, name = "scenarios")
    public Object[][] scenarios() {
        return super.scenarios();
    }

}