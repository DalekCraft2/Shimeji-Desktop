package com.group_finity.mascot.exception;

/**
 * Thrown when a {@link com.group_finity.mascot.behavior.Behavior Behavior} fails to be built.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class BehaviorInstantiationException extends Exception {
    public BehaviorInstantiationException(final String message) {
        super(message);
    }

    public BehaviorInstantiationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
