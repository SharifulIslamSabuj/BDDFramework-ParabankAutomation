package com.parabank.parasoft.constants;

/**
 * TestDataConstants — environment identifiers, Cucumber tag strings,
 * and the default tag expression used across step definitions and hooks.
 */
public final class TestDataConstants {

    private TestDataConstants() {}

    // ===== Environment names =====
    public static final String ENV_QA         = "qa";
    public static final String ENV_STAGING    = "staging";
    public static final String ENV_PRODUCTION = "prod";

    // ===== Cucumber feature-file tags =====
    public static final String TAG_SMOKE      = "@smoke";
    public static final String TAG_REGRESSION = "@regression";
    public static final String TAG_NEGATIVE   = "@negative";
    public static final String TAG_POSITIVE   = "@positive";
    public static final String TAG_HARDCODED  = "@hardcoded";
    public static final String TAG_LOREM      = "@ipsum";
    public static final String TAG_DDT        = "@ddt";

    /**
     * Default Cucumber tag expression applied when -Dcucumber.filter.tags is not
     * supplied at the command line.  Mirrors the expression previously hard-coded
     * in @CucumberOptions and now driven by build.gradle.
     */
    public static final String DEFAULT_TAG_EXPRESSION = "@smoke or @negative or @regression";
}
