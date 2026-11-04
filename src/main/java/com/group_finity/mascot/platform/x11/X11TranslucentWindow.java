/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.platform.x11;

import com.group_finity.mascot.platform.TranslucentWindow;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.ptr.NativeLongByReference;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;

/**
 * An implementation of {@link TranslucentWindow} that is specialized for X11.
 *
 * @author asdfman
 */
class X11TranslucentWindow extends JWindow implements TranslucentWindow {
    /**
     * The image to display.
     *
     * @see #setImage(BufferedImage)
     */
    private BufferedImage image;

    /**
     * Initializes a new {@code X11TranslucentWindow}.
     *
     * @param dpy the display used to access the native X11 window of this {@code X11TranslucentWindow}
     */
    X11TranslucentWindow(X.Display dpy) {
        super();

        setContentPane(new JPanel() {
            @Override
            protected void paintComponent(final Graphics g) {
                super.paintComponent(g);
                if (image != null) {
                    g.drawImage(image, 0, 0, null);
                }
            }
        });
        Color transparent = new Color(0, 0, 0, 0);
        setBackground(transparent);
        // We need to set the root pane's background to transparent too on Linux,
        // otherwise the window will not be transparent.
        getRootPane().setBackground(transparent);
        setLayout(new BorderLayout());

        // Add a listener for when the window becomes visible, because the native window will not be null by then
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && e.getComponent().isShowing()) {
                X11 x11 = X11.INSTANCE;

                // Get the handle of the native window for this JWindow
                X11.Window win = new X11.Window(Native.getWindowID(this));

                // Set the window type of the window to "dock" so the window can move past the bounds of the work area
                NativeLongByReference dockTypeRef = new NativeLongByReference(dpy.getAtom("_NET_WM_WINDOW_TYPE_DOCK"));
                x11.XChangeProperty(dpy.getX11Display(), win, dpy.getAtom("_NET_WM_WINDOW_TYPE"),
                        X11.XA_ATOM, 32, X11.PropModeReplace, dockTypeRef.getPointer(), 1);

                // Hide the native window and wait for all events to be processed before showing it again.
                // This fixes the issue in which the JWindow sometimes displays a taskbar icon after being made visible,
                // invisible, and then visible again.
                x11.XUnmapWindow(dpy.getX11Display(), win);
                x11.XSync(dpy.getX11Display(), false);
                x11.XMapWindow(dpy.getX11Display(), win);
            }
        });
    }

    @Override
    public String paramString() {
        return "hashCode=" + hashCode() +
                ",x=" + getX() +
                ",y=" + getY() +
                ",width=" + getWidth() +
                ",height=" + getHeight();
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
