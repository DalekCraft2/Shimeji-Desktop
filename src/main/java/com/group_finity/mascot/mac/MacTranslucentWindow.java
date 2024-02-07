package com.group_finity.mascot.mac;

import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;

import javax.swing.*;
import java.awt.*;

class MacTranslucentWindow implements TranslucentWindow {
    private TranslucentWindow delegate;
    private boolean imageChanged = false;
    private NativeImage oldImage = null;

    MacTranslucentWindow(NativeFactory factory) {
        delegate = factory.newTransparentWindow();
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
    public void setImage(NativeImage image) {
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
