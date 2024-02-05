package com.group_finity.mascot.exception;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class ActionInstantiationException extends Exception {

    private static final long serialVersionUID = 1L;

    public ActionInstantiationException(final String message) {
        super(message);
    }

    public ActionInstantiationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
