package com.group_finity.mascot.exception;

/**
 * Thrown when a {@link com.group_finity.mascot.behavior.Behavior Behavior} encounters an error during its
 * initialization or execution.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
/*
 * TODO: Rename this exception, because 1) not every catch block that catches this exception disposes of the associated
 *  mascot, and 2) because it's not really good practice to have a exception named after how it should be handled
 *  rather than where it was thrown or what caused it.
 */
public class CantBeAliveException extends Exception {
    public CantBeAliveException(final String message) {
        super(message);
    }

    public CantBeAliveException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
