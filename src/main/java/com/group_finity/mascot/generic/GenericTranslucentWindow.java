package com.group_finity.mascot.generic;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.platform.WindowUtils;

import javax.swing.*;
import java.awt.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
class GenericTranslucentWindow extends JWindow implements TranslucentWindow {

    private static final long serialVersionUID = 1L;

    /**
     * Image to display.
     */
    private GenericNativeImage image;

    private float alpha = 1.0f;

    public GenericTranslucentWindow() {
        super(WindowUtils.getAlphaCompatibleGraphicsConfiguration());
        init();

        JPanel panel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(final Graphics g) {
                super.paintComponent(g);
                if (getImage() != null) {
                    g.drawImage(getImage().getManagedImage(), 0, 0, null);
                }
            }
        };

        if (Mascot.DRAW_DEBUG) {
            panel.setBackground(new Color(0, 0, 0, 0));
            setBackground(new Color(0, 0, 0, 0));

            panel.setOpaque(false);
        }

        setContentPane(panel);

        if (Mascot.DRAW_DEBUG) {
            setLayout(new BorderLayout());
        }
    }

    private void init() {
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.java2d.opengl", "true");
    }

    @Override
    public void setVisible(final boolean b) {
        super.setVisible(b);
        if (b) {
            WindowUtils.setWindowTransparent(this, true);
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

    @Override
    public Component asComponent() {
        return this;
    }

    @Override
    public String toString() {
        return "LayeredWindow[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
    }

    public GenericNativeImage getImage() {
        return image;
    }

    @Override
    public void setImage(final NativeImage image) {
        this.image = (GenericNativeImage) image;
    }

    @Override
    public void updateImage() {
        WindowUtils.setWindowMask(this, getImage().getIcon());
        validate();
        repaint();
    }
}
