package com.parabank.parasoft.exceptions;

/**
 * FrameworkException - Base exception for all framework-related errors
 *
 * @author Automation Team
 */
public class FrameworkException extends RuntimeException {
    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}

