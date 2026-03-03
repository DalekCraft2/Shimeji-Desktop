package com.group_finity.mascot.exception;

/**
 * Thrown when a {@link com.group_finity.mascot.Mascot Mascot} should begin falling, usually in response to not being
 * on any {@link com.group_finity.mascot.environment.Border Border} when performing a
 * {@link com.group_finity.mascot.action.BorderedAction BorderedAction}.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class LostGroundException extends Exception {
}
