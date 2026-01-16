package com.group_finity.mascot.exception;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
/*
 * TODO: Study this how this exception is used to determine things like whether a mascot must be disposed when this
 *  exception is caught, because that is not the case for every instance of it.
 *  Alternatively, replace every usage of this exception with a different exception, because it's not really good
 *  practice to have a exception named after how it should be handled rather than where it was thrown or what caused it.
 */
public class CantBeAliveException extends Exception {
    public CantBeAliveException(final String message) {
        super(message);
    }

    public CantBeAliveException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
