/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.x11;

import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

/**
 * Image window with alpha value.
 * {@link X11NativeImage} set with {@link #setImage(NativeImage)} can be displayed on the desktop.
 * <p>
 * You can also specify the alpha when displaying with {@link #setAlpha(float)}.
 *
 * @author asdfman
 */
class X11TranslucentWindow extends JWindow implements TranslucentWindow {

    private static final long serialVersionUID = 1L;

    /**
     * To view images.
     */
    private X11NativeImage image;

    private static final X11 x11 = X11.INSTANCE;
    private final X11.Display dpy = x11.XOpenDisplay(null);
    private X11.Window win = null;
    private float alpha = 1.0f;
    private final JWindow alphaWindow = this;

    public X11TranslucentWindow() {
        super(WindowUtils.getAlphaCompatibleGraphicsConfiguration());
        init();

        setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            private static final long serialVersionUID = 1L;

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
    }

    private void init() {
        System.setProperty("sun.java2d.noddraw", "true");
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

    private Memory buffer;
    private int[] pixels;

    private void updateX11() {
        // FIXME This does not work with setAlpha()/setOpacity(). It always draws the image as if the alpha is 1.0.
        try {
            if (win == null) {
                win = new X11.Window(Native.getWindowID(alphaWindow));
            }
            int w = image.getWidth();
            int h = image.getHeight();
            alphaWindow.setSize(w, h);


            if (buffer == null || buffer.size() != (long) w * h * 4) {
                buffer = new Memory((long) w * h * 4);
                pixels = new int[w * h];
            }

            BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics g = buf.getGraphics();
            g.drawImage(image.getManagedImage(), 0, 0, w, h, null);

            X11.GC gc = x11.XCreateGC(dpy, win, new NativeLong(0), null);

            try {
                Raster raster = buf.getData();
                int[] pixel = new int[4];

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        raster.getPixel(x, y, pixel);
                        int alpha = (pixel[3] & 0xFF) << 24;
                        int red = pixel[2] & 0xFF;
                        int green = (pixel[1] & 0xFF) << 8;
                        int blue = (pixel[0] & 0xFF) << 16;
                        pixels[y * w + x] = alpha | red | green | blue;
                    }
                }
                X11.XImage image = x11.XCreateImage(dpy, null,
                        32, X11.ZPixmap,
                        0, buffer, w, h, 32, w * 4);
                buffer.write(0, pixels, 0, pixels.length);

                x11.XPutImage(dpy, win, gc, image, 0, 0, 0, 0, w, h);
                x11.XFree(image.getPointer());

            } finally {
                if (gc != null) {
                    x11.XFreeGC(dpy, gc);
                }
            }

        } catch (HeadlessException ignored) {
        }
        if (!alphaWindow.isVisible()) {
            alphaWindow.setVisible(true);
            // hack for initial refresh (X11)
            repaint();
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
        this.alpha = alpha;
        WindowUtils.setWindowAlpha(this, this.alpha);
    }

    public void setToDock(int value) {
        IntByReference dockAtom = new IntByReference(value);
        x11.XChangeProperty(dpy, win, x11.XInternAtom(dpy, "_NET_WM_WINDOW_TYPE", false),
                x11.XA_ATOM, 32, x11.PropModeReplace, dockAtom.getPointer(), 1);
    }


    @Override
    public Component asComponent() {
        return this;
    }

    @Override
    public String toString() {
        return "LayeredWindow[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
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
        // repaint();
        updateX11();
    }
}
