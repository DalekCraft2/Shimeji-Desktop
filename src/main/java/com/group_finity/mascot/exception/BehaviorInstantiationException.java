package com.group_finity.mascot.exception;

/**
 * Thrown when a {@link com.group_finity.mascot.behavior.Behavior Behavior} fails to be built.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class BehaviorInstantiationException extends Exception {
    private final String behaviorName;

    public BehaviorInstantiationException(final String message) {
        super(message);
        behaviorName = null;
    }

    public BehaviorInstantiationException(final String message, final Throwable cause) {
        super(message, cause);
        behaviorName = null;
    }

    public BehaviorInstantiationException(final String message, final String behaviorName) {
        super(message);
        this.behaviorName = behaviorName;
    }

    public BehaviorInstantiationException(final String message, final String behaviorName, final Throwable cause) {
        super(message, cause);
        this.behaviorName = behaviorName;
    }

    public String getBehaviorName() {
        return behaviorName;
    }
}
