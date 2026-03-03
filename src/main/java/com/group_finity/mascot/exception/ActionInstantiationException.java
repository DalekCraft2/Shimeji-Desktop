package com.group_finity.mascot.exception;

/**
 * Thrown when an {@link com.group_finity.mascot.action.Action Action} fails to be built.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class ActionInstantiationException extends Exception {
    public ActionInstantiationException(final String message) {
        super(message);
    }

    public ActionInstantiationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
