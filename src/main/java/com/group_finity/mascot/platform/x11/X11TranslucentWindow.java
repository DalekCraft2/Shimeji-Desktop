/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.platform.x11;

import com.group_finity.mascot.platform.TranslucentWindow;
import com.sun.jna.platform.WindowUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

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

    X11TranslucentWindow() {
        super(WindowUtils.getAlphaCompatibleGraphicsConfiguration());

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

    @Override
    public void setVisible(final boolean b) {
        /*
         * On Linux, setting the window to visible, then invisible, and then visible again will make an icon for the
         * window appear in the taskbar, which should not happen. The workaround is to simply never call
         * super.setVisible(false), and use WindowUtils.setWindowAlpha() to hide the window instead.
         */
        try {
            if (b) {
                if (!isVisible()) {
                    super.setVisible(true);
                }
                WindowUtils.setWindowAlpha(this, 1.0f);
            } else {
                WindowUtils.setWindowAlpha(this, 0.0f);
            }
        } catch (IllegalArgumentException ignored) {
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
