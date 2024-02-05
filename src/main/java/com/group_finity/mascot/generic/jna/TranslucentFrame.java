package com.group_finity.mascot.generic.jna;

import com.sun.jna.platform.WindowUtils;

import javax.swing.*;
import java.awt.*;

/**
 * @author Shimeji-ee Group
 */
public class TranslucentFrame extends JWindow {

    private static final long serialVersionUID = 1L;
    private float alpha = 1.0f;

    public TranslucentFrame() {
        super(WindowUtils.getAlphaCompatibleGraphicsConfiguration());
        init();
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

    public void setAlpha(final float alpha) {
        WindowUtils.setWindowAlpha(this, alpha);
        this.alpha = alpha;
    }

    public float getAlpha() {
        return alpha;
    }

}
