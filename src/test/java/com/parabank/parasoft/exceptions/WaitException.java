package com.parabank.parasoft.exceptions;

/**
 * WaitException - Thrown when explicit wait operations fail
 *
 * @author Automation Team
 */
public class WaitException extends FrameworkException {
    public WaitException(String message) {
        super(message);
    }

    public WaitException(String message, Throwable cause) {
        super(message, cause);
    }
}

