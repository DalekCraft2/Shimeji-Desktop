/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.x11;

import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.platform.WindowUtils;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Image window with alpha value.
 * {@link X11NativeImage} set with {@link #setImage(NativeImage)} can be displayed on the desktop.
 *
 * @author asdfman
 */
class X11TranslucentWindow extends JWindow implements TranslucentWindow {
    /**
     * Image to display.
     */
    private X11NativeImage image;

    public X11TranslucentWindow() {
        super(WindowUtils.getAlphaCompatibleGraphicsConfiguration());
        init();

        setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
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
