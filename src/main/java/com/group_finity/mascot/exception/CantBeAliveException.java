package com.group_finity.mascot.exception;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
// TODO Study this how this exception is used to determine things like whether a mascot must be disposed when this exception is caught, because that is not the case for every instance of it.
public class CantBeAliveException extends Exception {
    public CantBeAliveException(final String message) {
        super(message);
    }

    public CantBeAliveException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
