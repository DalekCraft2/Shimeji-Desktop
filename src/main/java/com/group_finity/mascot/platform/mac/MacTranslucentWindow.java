/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.platform.mac;

import com.group_finity.mascot.platform.TranslucentWindow;
import com.sun.jna.platform.WindowUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author nonowarn
 */
class MacTranslucentWindow extends JWindow implements TranslucentWindow {
    /**
     * Image to display.
     */
    private BufferedImage image;

    MacTranslucentWindow() {
        super(WindowUtils.getAlphaCompatibleGraphicsConfiguration());

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(final Graphics g) {
                super.paintComponent(g);
                if (image != null) {
                    g.drawImage(image, 0, 0, null);
                }
            }
        };
        panel.setBackground(new Color(0, 0, 0, 0));
        setBackground(new Color(0, 0, 0, 0));
        panel.setOpaque(false);
        setContentPane(panel);

        setLayout(new BorderLayout());

        // The shadow of the window will shift, so avoid drawing the shadow.
        getRootPane().putClientProperty("Window.shadow", Boolean.FALSE);

        // Eliminate warnings at runtime
        getRootPane().putClientProperty("apple.awt.draggableWindowBackground", Boolean.TRUE);
    }

    @Override
    public void setVisible(final boolean b) {
        if (isVisible() == b) {
            return;
        }

        if (b) {
            // See https://developer.apple.com/library/archive/technotes/tn2007/tn2196.html#APPLE_AWT_DRAGGABLEWINDOWBACKGROUND
            WindowUtils.setWindowTransparent(this, true);
        }

        super.setVisible(b);
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
        return "MacTranslucentWindow[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
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
