package com.group_finity.mascot.platform.win;

import com.group_finity.mascot.platform.TranslucentWindow;

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

    WindowsTranslucentWindow() {
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
        setBackground(new Color(0, 0, 0, 0));
        setLayout(new BorderLayout());
    }

    @Override
    public String toString() {
        return "WindowsTranslucentWindow[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
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