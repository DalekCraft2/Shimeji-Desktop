/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.platform.x11;

import com.group_finity.mascot.platform.TranslucentWindow;
import com.sun.jna.Native;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;

/**
 * Image window with alpha value.
 * {@link BufferedImage} set with {@link #setImage(BufferedImage)} can be displayed on the desktop.
 *
 * @author asdfman
 */
class X11TranslucentWindow extends JWindow implements TranslucentWindow {
    /**
     * Image to display.
     */
    private BufferedImage image;

    private boolean visible;

    X11TranslucentWindow(X11.Display dpy, int dockValue) {
        super();

        setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(final Graphics g) {
                g.clearRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
                if (image != null) {
                    g.drawImage(image, 0, 0, null);
                }
            }
        };
        panel.setOpaque(false);
        setContentPane(panel);

        setLayout(new BorderLayout());

        // Add a listener for when the window becomes visible, because the native window will not be null by then
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                // Get the handle of the native window for this JWindow
                X11.Window win = new X11.Window((int) Native.getWindowID(this));
                X11 x11 = X11.INSTANCE;

                // Set the window type of the window to "dock" so it can move past the bounds of the screen
                IntByReference dockAtom = new IntByReference(dockValue);
                x11.XChangeProperty(dpy, win, x11.XInternAtom(dpy, "_NET_WM_WINDOW_TYPE", false),
                        X11.XA_ATOM, 32, X11.PropModeReplace, dockAtom.getPointer(), 1);
            }
        });
    }

    @Override
    public String toString() {
        return "X11TranslucentWindow[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(final boolean b) {
        /*
         * On Linux, setting the window to visible, then invisible, and then visible again will make an icon for the
         * window appear in the taskbar, which should not happen. The workaround is to simply never call
         * super.setVisible(false), and use WindowUtils.setWindowAlpha() to hide the window instead.
         */
        try {
            if (b) {
                if (!super.isVisible()) {
                    super.setVisible(true);
                }
                WindowUtils.setWindowAlpha(this, 1.0f);
                visible = true;
            } else {
                WindowUtils.setWindowAlpha(this, 0.0f);
                visible = false;
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    protected void addImpl(final Component comp, final Object constraints, final int index) {
        super.addImpl(comp, constraints, index);
        if (comp instanceof JComponent jComp) {
            jComp.setOpaque(false);
        }
    }

    @Override
    public void paint(final Graphics g) {
        if (g instanceof Graphics2D g2d) {
            // Higher-quality image
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        super.paint(g);
    }

    @Override
    public boolean contains(int x, int y) {
        if (image != null && super.contains(x, y) &&
                x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
            // Check whether the pixel at the given position of the image has an alpha greater than 0
            return (image.getRGB(x, y) & 0xff000000) >>> 24 > 0;
        }
        return false;
    }

    @Override
    public Component asComponent() {
        return this;
    }

    @Override
    public void setImage(final BufferedImage image) {
        this.image = image;
    }

    @Override
    public void updateImage() {
        validate();
        repaint();
    }
}
