package com.parabank.parasoft.exceptions;

/**
 * ElementInteractionException - Thrown when element interaction fails
 *
 * @author Automation Team
 */
public class ElementInteractionException extends FrameworkException {
    public ElementInteractionException(String message) {
        super(message);
    }

    public ElementInteractionException(String message, Throwable cause) {
        super(message, cause);
    }
}

