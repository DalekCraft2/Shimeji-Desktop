package com.group_finity.mascot.behavior;

/**
 * Thrown when a {@link Behavior} encounters an error during its initialization or execution.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class BehaviorExecutionException extends Exception {
    public BehaviorExecutionException(final String message) {
        super(message);
    }

    public BehaviorExecutionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
