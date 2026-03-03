package com.group_finity.mascot.exception;

/**
 * Thrown when an {@link com.group_finity.mascot.animation.Animation Animation} fails to be built.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class AnimationInstantiationException extends Exception {
    public AnimationInstantiationException(final String message) {
        super(message);
    }

    public AnimationInstantiationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
