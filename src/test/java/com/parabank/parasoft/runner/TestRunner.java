package com.parabank.parasoft.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * TestRunner — Cucumber + TestNG test runner with parallel execution support.
 *
 * <p>Tag filtering is driven at runtime via the {@code cucumber.filter.tags}
 * system property rather than being hard-coded in this annotation.
 * The default expression is set in {@code build.gradle} and equals
 * {@code @smoke or @negative or @regression}.
 *
 * <p>Override from the command line:
 * <pre>
 *   ./gradlew test -Dcucumber.filter.tags="@smoke"
 *   ./gradlew test -Dcucumber.filter.tags="@regression"
 *   ./gradlew test -Dcucumber.filter.tags="@negative and @security"
 *   ./gradlew test -Dcucumber.filter.tags="@smoke or @regression"
 * </pre>
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
        publish  = false,
        dryRun   = false
        // No 'tags' attribute here — tag selection is driven by the
        // cucumber.filter.tags system property set in build.gradle.
)
@Test
public class TestRunner extends AbstractTestNGCucumberTests {

    /**
     * Enables parallel scenario execution.
     * Thread count is controlled by -Ddataproviderthreadcount=N (default 2).
     */
    @Override
    @DataProvider(parallel = true, name = "scenarios")
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
