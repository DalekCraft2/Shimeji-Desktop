package com.group_finity.mascot.generic;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.platform.WindowUtils;
import org.apache.commons.exec.OS;

import javax.swing.*;
import java.awt.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
class GenericTranslucentWindow extends JWindow implements TranslucentWindow {
    /**
     * Image to display.
     */
    private GenericNativeImage image;

    public GenericTranslucentWindow() {
        super(WindowUtils.getAlphaCompatibleGraphicsConfiguration());

        JPanel panel = new JPanel() {
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

    @Override
    public void setVisible(final boolean b) {
        if (super.isVisible() == b) {
            return;
        }

        if (b && OS.isFamilyMac()) {
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
        return "GenericTranslucentWindow[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
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
        /*
         * We set the Window mask to ensure that the Shimeji can only be interacted with when the mouse is over a
         * non-transparent pixel of the image. A drawback to this is that the call can sometimes take a long time to
         * complete.
         *
         * This doesn't work on macOS, so we can skip it to avoid the performance hit.
         */
        if (!OS.isFamilyMac()) {
            WindowUtils.setWindowMask(this, getImage().getIcon());
        }

        validate();
        repaint();
    }
}
