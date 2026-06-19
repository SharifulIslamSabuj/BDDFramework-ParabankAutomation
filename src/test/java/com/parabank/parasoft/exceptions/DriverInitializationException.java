package com.parabank.parasoft.exceptions;

/**
 * DriverInitializationException - Thrown when WebDriver initialization fails
 *
 * @author Automation Team
 */
public class DriverInitializationException extends FrameworkException {
    public DriverInitializationException(String message) {
        super(message);
    }

    public DriverInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}

