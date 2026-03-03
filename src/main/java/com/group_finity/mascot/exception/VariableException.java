package com.group_finity.mascot.exception;

/**
 * Thrown when a scripted parameter in a mascot's configuration has an invalid value or can not be
 * parsed by the script engine.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class VariableException extends Exception {
    public VariableException(final String message) {
        super(message);
    }

    public VariableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
