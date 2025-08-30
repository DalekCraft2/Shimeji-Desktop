package com.group_finity.mascot.win;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;

import javax.swing.*;
import java.awt.*;

/**
 * Image window with alpha value.
 * {@link WindowsNativeImage} set with {@link #setImage(NativeImage)} can be displayed on the desktop.
 * <p>
 * You can also specify the alpha when displaying with {@link #setAlpha(int)}.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 * @author Valkryst
 */
class WindowsTranslucentWindow extends JWindow implements TranslucentWindow {
    private static final long serialVersionUID = 1L;

    /**
     * Image to display.
     */
    private WindowsNativeImage image;

    /**
     * Display concentration. 0 = not displayed at all, 255 = completely displayed.
     */
    private int alpha = 255;

    public WindowsTranslucentWindow() {
        super();

        setBackground(new Color(0, 0, 0, 0));

        if (Mascot.DRAW_DEBUG) {
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

        if (!Mascot.DRAW_DEBUG && getImage() != null) {
            g.drawImage(getImage().getManagedImage(), 0, 0, null);
        }
    }

    private WindowsNativeImage getImage() {
        return image;
    }

    @Override
    public void setImage(final NativeImage image) {
        this.image = (WindowsNativeImage) image;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(final int alpha) {
        this.alpha = alpha;
    }

    @Override
    public void updateImage() {
        if (Mascot.DRAW_DEBUG) {
            validate();
        }
        repaint();
    }
}