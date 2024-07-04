package com.group_finity.mascot.win;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.group_finity.mascot.win.jna.GDI32Extra;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.BLENDFUNCTION;
import com.sun.jna.platform.win32.WinUser.SIZE;

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
 */
class WindowsTranslucentWindow extends JWindow implements TranslucentWindow {

    private static final long serialVersionUID = 1L;

    /**
     * Whether to render with AWT instead of JNA.
     * <p>
     * Currently, on Windows, {@linkplain Mascot#DRAW_DEBUG debug rendering} will only work if this is {@code true}.
     */
    public static final boolean USE_AWT = true;

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

        if (USE_AWT) {
            setBackground(new Color(0, 0, 0, 0));
        }

        if (USE_AWT && Mascot.DRAW_DEBUG) {
            JPanel panel = new JPanel() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(final Graphics g) {
                    super.paintComponent(g);
                    if (getImage() != null) {
                        // Currently, on Windows, debug drawing only works with AWT.
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
        if (USE_AWT && Mascot.DRAW_DEBUG && comp instanceof JComponent) {
            final JComponent jcomp = (JComponent) comp;
            jcomp.setOpaque(false);
        }
    }

    @Override
    public Component asComponent() {
        return this;
    }

    /**
     * Draws an image with alpha value.
     *
     * @param bmpHandle bitmap handle
     * @param alpha display alpha. 0 = not displayed at all, 255 = completely displayed.
     */
    private void paintNative(final HBITMAP bmpHandle, final int alpha) {
        final HWND hWnd = new HWND(Native.getComponentPointer(this));

        if (User32.INSTANCE.IsWindow(hWnd)) {
            final int exStyle = WindowsUtil.GetWindowLong(hWnd, WinUser.GWL_EXSTYLE).intValue();
            if ((exStyle & WinUser.WS_EX_LAYERED) == 0) {
                WindowsUtil.SetWindowLong(hWnd, WinUser.GWL_EXSTYLE, Pointer.createConstant(exStyle | WinUser.WS_EX_LAYERED));

                /*
                This function call is required when the DPI is not 96; if not included, the mascot will be stuck in the
                first frame of its falling pose and can not be dragged by the mouse. (Interestingly, the mascots only
                became not draggable after I added a StretchBlt call to this method.)
                However, it also makes translucent pixels look awful because it basically chroma-keys out every black
                pixel, and *only* the black pixels (0x000000 and nothing else). The translucent pixels end up appearing
                fully opaque.
                */
                // FIXME Find another way to fix the "stuck in first frame/not draggable" issue.
                User32.INSTANCE.SetLayeredWindowAttributes(hWnd, 0, (byte) 255, WinUser.LWA_COLORKEY);
            }

            // Transfer destination area
            final Rectangle windowRect = WindowUtils.getWindowLocationAndSize(hWnd);
            final POINT lt = new POINT(windowRect.x, windowRect.y);
            final SIZE size = new SIZE(windowRect.width, windowRect.height);
            final POINT zero = new POINT();

            // Transfer
            final BLENDFUNCTION blend = new BLENDFUNCTION();
            blend.SourceConstantAlpha = (byte) alpha; // Set alpha
            blend.AlphaFormat = WinUser.AC_SRC_ALPHA;

            // Create image transfer source DC
            final HDC clientDC = User32.INSTANCE.GetDC(hWnd);
            final HDC memDC = GDI32.INSTANCE.CreateCompatibleDC(clientDC);
            final HANDLE oldBmp = GDI32.INSTANCE.SelectObject(memDC, bmpHandle);

            // If DPI scale is not 1, scale the bitmap with StretchBlt()
            double dpiScaleInverse = 96.0 / Toolkit.getDefaultToolkit().getScreenResolution();
            if (dpiScaleInverse != 1) {
                Rectangle windowRectScaled = (Rectangle) windowRect.clone();
                windowRectScaled.x = (int) Math.round(windowRectScaled.x * dpiScaleInverse);
                windowRectScaled.y = (int) Math.round(windowRectScaled.y * dpiScaleInverse);
                windowRectScaled.width = (int) Math.round(windowRectScaled.width * dpiScaleInverse);
                windowRectScaled.height = (int) Math.round(windowRectScaled.height * dpiScaleInverse);

                GDI32Extra.INSTANCE.StretchBlt(clientDC, 0, 0, windowRect.width, windowRect.height,
                        memDC, 0, 0, windowRectScaled.width, windowRectScaled.height, GDI32.SRCCOPY);
            }

            User32.INSTANCE.UpdateLayeredWindow(
                    hWnd, clientDC,
                    lt, size,
                    memDC, zero, 0, blend, WinUser.ULW_ALPHA);

            // Revert the bitmap to its original state
            GDI32.INSTANCE.SelectObject(memDC, oldBmp);
            GDI32.INSTANCE.DeleteDC(memDC);
            User32.INSTANCE.ReleaseDC(hWnd, clientDC);
        }
    }

    @Override
    public String toString() {
        return "LayeredWindow[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
    }

    @Override
    public void paint(final Graphics g) {
        if (USE_AWT) {
            if (g instanceof Graphics2D) {
                Graphics2D g2d = (Graphics2D) g;

                // Higher-quality image
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            }
            super.paint(g);
        }
        if (!(USE_AWT && Mascot.DRAW_DEBUG) && getImage() != null) {
            if (USE_AWT) {
                // I am using AWT as a temporary fix until I get paintNative() to work with DPIs other than 96.
                g.drawImage(getImage().getManagedImage(), 0, 0, null);
            } else {
                // Draw an image with alpha value using JNA.
                paintNative(getImage().getBmpHandle(), getAlpha());
            }
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
        if (USE_AWT && Mascot.DRAW_DEBUG) {
            validate();
        }
        repaint();
    }
}