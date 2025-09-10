/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.mac;

import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.image.TranslucentWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author nonowarn
 */
class MacTranslucentWindow implements TranslucentWindow {
    private final TranslucentWindow delegate;
    private boolean imageChanged = false;
    private BufferedImage oldImage = null;

    MacTranslucentWindow(NativeFactory factory) {
        delegate = factory.newTranslucentWindow();
        JRootPane rootPane = ((JWindow) delegate.asComponent()).getRootPane();

        // The shadow of the window will shift, so avoid drawing the shadow.
        rootPane.putClientProperty("Window.shadow", Boolean.FALSE);

        // Eliminate warnings at runtime
        rootPane.putClientProperty("apple.awt.draggableWindowBackground", Boolean.TRUE);
    }

    @Override
    public Component asComponent() {
        return delegate.asComponent();
    }

    @Override
    public String toString() {
        return "MacTranslucentWindow[hashCode=" + hashCode() + ",bounds=" + asComponent().getBounds() + "]";
    }

    @Override
    public void setImage(BufferedImage image) {
        imageChanged = oldImage != null && image != oldImage;
        oldImage = image;
        delegate.setImage(image);
    }

    @Override
    public void updateImage() {
        if (imageChanged) {
            delegate.updateImage();
            imageChanged = false;
        }
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public void setAlwaysOnTop(boolean onTop) {
        ((JWindow) delegate.asComponent()).setAlwaysOnTop(onTop);
    }
}
