package com.group_finity.mascot.virtual;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.image.TranslucentWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Virtual desktop translucent panel.
 *
 * @author Kilkakon
 * @since 1.0.20
 */
class VirtualTranslucentPanel extends JPanel implements TranslucentWindow {
    /**
     * Image to display.
     */
    private BufferedImage image;

    public VirtualTranslucentPanel() {
        super();

        if (Mascot.DRAW_DEBUG) {
            setBackground(new Color(0, 0, 0, 0));
            setOpaque(false);
            setLayout(new BorderLayout());
        }
    }

    @Override
    public String toString() {
        return "VirtualTranslucentPanel[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, null);
        }
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
    public Component asComponent() {
        return this;
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
    public void setAlwaysOnTop(boolean onTop) {
    }

    @Override
    public void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void dispose() {
        Container parent = getParent();
        if (parent != null) {
            parent.remove(this);
            parent.repaint();
        }
    }

    @Override
    public void updateImage() {
        if (Mascot.DRAW_DEBUG) {
            validate();
        }
        repaint();
    }

    @Override
    public boolean contains(int x, int y) {
        if (super.contains(x, y)) {
            try {
                return (image.getRGB(x, y) & 0xff000000) >>> 24 > 0;
            } catch (RuntimeException ex) {
                return false;
            }
        }
        return false;
    }
}