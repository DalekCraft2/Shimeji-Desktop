package com.group_finity.mascot.image;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Image window with alpha value.
 * {@link BufferedImage} set with {@link #setImage(BufferedImage)} can be displayed on the desktop.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public interface TranslucentWindow {

    /**
     * Gets the underlying AWT {@link Component} of this window.
     *
     * @return the underlying AWT {@link Component} of this window
     */
    Component asComponent();

    /**
     * Sets the image that should be drawn on this window.
     * Changes will not take effect until {@link #updateImage()} has been called.
     *
     * @param image the image that should be drawn, or {@code null} if nothing should be drawn
     */
    void setImage(BufferedImage image);

    /**
     * Redraws the image for this window.
     * An image should be set with {@link #setImage(BufferedImage)} before calling this method.
     */
    void updateImage();

    /**
     * Releases the resources used by the window.
     *
     * @see Window#dispose()
     */
    void dispose();

    /**
     * Sets whether this window should always be above other windows.
     *
     * @param onTop whether this window should always be above other windows
     * @see Window#setAlwaysOnTop(boolean)
     */
    void setAlwaysOnTop(boolean onTop);
}
