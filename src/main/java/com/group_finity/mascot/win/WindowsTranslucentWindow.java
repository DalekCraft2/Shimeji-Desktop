package com.group_finity.mascot.win;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.image.TranslucentWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Image window with alpha value.
 * {@link BufferedImage} set with {@link #setImage(BufferedImage)} can be displayed on the desktop.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 * @author Valkryst
 */
class WindowsTranslucentWindow extends JWindow implements TranslucentWindow {
    /**
     * Image to display.
     */
    private BufferedImage image;

    public WindowsTranslucentWindow() {
        super();

        setBackground(new Color(0, 0, 0, 0));

        if (Mascot.DRAW_DEBUG) {
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(final Graphics g) {
                    super.paintComponent(g);
                    if (image != null) {
                        g.drawImage(image, 0, 0, null);
                    }
                }
            };
            panel.setOpaque(false);
            setContentPane(panel);

            setLayout(new BorderLayout());
        }
    }

    @Override
    protected void addImpl(final Component comp, final Object constraints, final int index) {
        super.addImpl(comp, constraints, index);
        if (Mascot.DRAW_DEBUG && comp instanceof JComponent) {
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
        return "WindowsTranslucentWindow[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
    }

    @Override
    public void paint(final Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;

            // Higher-quality image
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        super.paint(g);

        if (!Mascot.DRAW_DEBUG && image != null) {
            g.drawImage(image, 0, 0, null);
        }
    }

    @Override
    public void setImage(final BufferedImage image) {
        this.image = image;
    }

    @Override
    public void updateImage() {
        if (Mascot.DRAW_DEBUG) {
            validate();
        }
        repaint();
    }
}