package com.parabank.parasoft.exceptions;

/**
 * ConfigurationException - Thrown when configuration loading fails
 *
 * @author Automation Team
 */
public class ConfigurationException extends FrameworkException {
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}

