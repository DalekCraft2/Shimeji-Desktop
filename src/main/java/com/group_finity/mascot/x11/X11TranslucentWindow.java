/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.x11;

import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Image window with alpha value.
 * {@link X11NativeImage} set with {@link #setImage(NativeImage)} can be displayed on the desktop.
 * <p>
 * You can also specify the alpha when displaying with {@link #setAlpha(float)}.
 *
 * @author asdfman
 */
class X11TranslucentWindow extends JWindow implements TranslucentWindow {

    private static final long serialVersionUID = 1L;

    /**
     * To view images.
     */
    private X11NativeImage image;

    private static final X11 x11 = X11.INSTANCE;
    private final X11.Display dpy = x11.XOpenDisplay(null);
    private X11.Window win = null;
    private float alpha = 1.0f;

    public X11TranslucentWindow() {
        super(WindowUtils.getAlphaCompatibleGraphicsConfiguration());
        init();

        setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(final Graphics g) {
                g.clearRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
                if (getImage() != null) {
                    g.drawImage(getImage().getManagedImage(), 0, 0, null);
                }
            }
        };
        panel.setOpaque(false);
        setContentPane(panel);

        setLayout(new BorderLayout());

        // Fix for JDK-8016530 (https://bugs.openjdk.org/browse/JDK-8016530), from https://stackoverflow.com/a/75807264
        AtomicBoolean updating = new AtomicBoolean();
        addPropertyChangeListener("graphicsConfiguration", evt -> {
            if (updating.compareAndSet(false, true)) {
                /*
                 * trigger frame to pick a graphics context with transparency support again
                 */
                try {
                    setBackground(new Color(0, 0, 0, 255));
                    setBackground(new Color(0, 0, 0, 0));
                } finally {
                    updating.set(false);
                }
            }
        });
    }

    private void init() {
        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("sun.java2d.opengl", "true");
    }

    @Override
    public void setVisible(final boolean b) {
        super.setVisible(b);
        if (b) {
            try {
                WindowUtils.setWindowTransparent(this, true);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    protected void addImpl(final Component comp, final Object constraints, final int index) {
        super.addImpl(comp, constraints, index);
        if (comp instanceof JComponent) {
            final JComponent jcomp = (JComponent) comp;
            jcomp.setOpaque(false);
        }
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(final float alpha) {
        WindowUtils.setWindowAlpha(this, alpha);
        this.alpha = alpha;
    }

    public void setToDock(int value) {
        IntByReference dockAtom = new IntByReference(value);
        x11.XChangeProperty(dpy, win, x11.XInternAtom(dpy, "_NET_WM_WINDOW_TYPE", false),
                x11.XA_ATOM, 32, x11.PropModeReplace, dockAtom.getPointer(), 1);
    }


    @Override
    public Component asComponent() {
        return this;
    }

    @Override
    public String toString() {
        return "X11TranslucentWindow[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
    }

    @Override
    public void paint(final Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;

            // Higher-quality image
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        super.paint(g);
    }

    public X11NativeImage getImage() {
        return image;
    }

    @Override
    public void setImage(final NativeImage image) {
        this.image = (X11NativeImage) image;
    }

    @Override
    public void updateImage() {
        validate();
        repaint();
    }
}
