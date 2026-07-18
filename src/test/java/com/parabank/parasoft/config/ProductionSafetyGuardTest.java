package com.parabank.parasoft.config;

import com.parabank.parasoft.exceptions.ConfigurationException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Focused unit tests for the production-environment write-protection guard.
 *
 * <p>Tests use ConfigManager.resetInstance() and System.setProperty("env", ...)
 * to configure the environment deterministically without launching browsers or
 * contacting any external server.
 *
 * <p>Each test method saves and restores the "env" system property and resets
 * the ConfigManager singleton to avoid cross-test interference.
 */
public class ProductionSafetyGuardTest {

    private String savedEnvProperty;

    @BeforeMethod
    public void saveAndResetConfig() {
        savedEnvProperty = System.getProperty("env");
        ConfigManager.resetInstance();
    }

    @AfterMethod
    public void restoreConfig() {
        if (savedEnvProperty != null) {
            System.setProperty("env", savedEnvProperty);
        } else {
            System.clearProperty("env");
        }
        ConfigManager.resetInstance();
    }

    // ── isProductionEnvironment() — production aliases ────────────────────

    @Test
    public void isProductionEnvironment_prod_returnsTrue() {
        System.setProperty("env", "prod");
        Assert.assertTrue(ConfigManager.getInstance().isProductionEnvironment(),
                "Environment 'prod' must be identified as production");
    }

    @Test
    public void isProductionEnvironment_production_returnsTrue() {
        System.setProperty("env", "production");
        Assert.assertTrue(ConfigManager.getInstance().isProductionEnvironment(),
                "Environment 'production' must be identified as production");
    }

    @Test
    public void isProductionEnvironment_PROD_uppercase_returnsTrue() {
        System.setProperty("env", "PROD");
        Assert.assertTrue(ConfigManager.getInstance().isProductionEnvironment(),
                "Environment 'PROD' (uppercase) must be identified as production — check is case-insensitive");
    }

    @Test
    public void isProductionEnvironment_Production_mixedCase_returnsTrue() {
        System.setProperty("env", "Production");
        Assert.assertTrue(ConfigManager.getInstance().isProductionEnvironment(),
                "Environment 'Production' (mixed case) must be identified as production");
    }

    // ── isProductionEnvironment() — non-production environments ──────────

    @Test
    public void isProductionEnvironment_qa_returnsFalse() {
        System.setProperty("env", "qa");
        Assert.assertFalse(ConfigManager.getInstance().isProductionEnvironment(),
                "Environment 'qa' must NOT be identified as production");
    }

    @Test
    public void isProductionEnvironment_staging_returnsFalse() {
        System.setProperty("env", "staging");
        Assert.assertFalse(ConfigManager.getInstance().isProductionEnvironment(),
                "Environment 'staging' must NOT be identified as production");
    }

    @Test
    public void isProductionEnvironment_test_returnsFalse() {
        System.setProperty("env", "test");
        Assert.assertFalse(ConfigManager.getInstance().isProductionEnvironment(),
                "Environment 'test' must NOT be identified as production");
    }

    // ── guardAgainstProductionWrite() — guard throws for production ───────

    @Test
    public void guard_prod_throwsConfigurationException() {
        System.setProperty("env", "prod");
        ConfigManager cfg = ConfigManager.getInstance();
        try {
            cfg.guardAgainstProductionWrite("default test user registration");
            Assert.fail("Expected ConfigurationException for production environment");
        } catch (ConfigurationException e) {
            Assert.assertTrue(e.getMessage().contains("production environment"),
                    "Exception message must mention 'production environment'");
            Assert.assertTrue(e.getMessage().contains("prod"),
                    "Exception message must include the detected environment name");
            Assert.assertTrue(e.getMessage().contains("default test user registration"),
                    "Exception message must identify the blocked operation");
        }
    }

    @Test
    public void guard_production_throwsConfigurationException() {
        System.setProperty("env", "production");
        ConfigManager cfg = ConfigManager.getInstance();
        try {
            cfg.guardAgainstProductionWrite("default test user registration");
            Assert.fail("Expected ConfigurationException for environment alias 'production'");
        } catch (ConfigurationException e) {
            Assert.assertTrue(e.getMessage().contains("production environment"),
                    "Exception message must mention 'production environment'");
            Assert.assertTrue(e.getMessage().contains("production"),
                    "Exception message must include the detected environment name");
        }
    }

    // ── guardAgainstProductionWrite() — guard does not throw for non-prod ─

    @Test
    public void guard_qa_doesNotThrow() {
        System.setProperty("env", "qa");
        ConfigManager cfg = ConfigManager.getInstance();
        // Must not throw — test user registration is allowed in QA
        cfg.guardAgainstProductionWrite("default test user registration");
    }

    @Test
    public void guard_staging_doesNotThrow() {
        System.setProperty("env", "staging");
        ConfigManager cfg = ConfigManager.getInstance();
        cfg.guardAgainstProductionWrite("default test user registration");
    }

    @Test
    public void guard_defaultEnvironment_doesNotThrow() {
        // No env property set — ConfigManager defaults to "qa"
        System.clearProperty("env");
        ConfigManager cfg = ConfigManager.getInstance();
        cfg.guardAgainstProductionWrite("default test user registration");
    }
}
