package com.group_finity.mascot.win;

import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.*;

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
    }

    @Override
    public Component asComponent() {
        return this;
    }

    /**
     * Draws an image with alpha value.
     *
     * @param imageHandle bitmap handle
     * @param alpha display alpha. 0 = not displayed at all, 255 = completely displayed.
     */
    // FIXME This method does not work on Java 11.
    private void paint(final WinDef.HBITMAP imageHandle, final int alpha) {
        final WinDef.HWND hWnd = new WinDef.HWND(Native.getComponentPointer(this));

        if (User32.INSTANCE.IsWindow(hWnd)) {

            final int exStyle = WindowsUtil.GetWindowLong(hWnd, User32.GWL_EXSTYLE).intValue();
            if ((exStyle & User32.WS_EX_LAYERED) == 0) {
                WindowsUtil.SetWindowLong(hWnd, User32.GWL_EXSTYLE, Pointer.createConstant(exStyle | User32.WS_EX_LAYERED));
            }

            // Create image transfer source DC
            final WinDef.HDC clientDC = User32.INSTANCE.GetDC(hWnd);
            final WinDef.HDC memDC = GDI32.INSTANCE.CreateCompatibleDC(clientDC);
            final WinNT.HANDLE oldBmp = GDI32.INSTANCE.SelectObject(memDC, imageHandle);

            User32.INSTANCE.ReleaseDC(hWnd, clientDC);

            // Transfer destination area
            final Rectangle windowRect = WindowUtils.getWindowLocationAndSize(hWnd);

            // Transfer
            final WinUser.BLENDFUNCTION bf = new WinUser.BLENDFUNCTION();
            bf.SourceConstantAlpha = (byte) alpha; // Set alpha
            bf.AlphaFormat = WinUser.AC_SRC_ALPHA;

            final WinDef.POINT lt = new WinDef.POINT(windowRect.x, windowRect.y);
            final WinUser.SIZE size = new WinUser.SIZE(windowRect.width, windowRect.height);
            final WinDef.POINT zero = new WinDef.POINT();
            User32.INSTANCE.UpdateLayeredWindow(
                    hWnd, null,
                    lt, size,
                    memDC, zero, 0, bf, User32.ULW_ALPHA);

            // Revert the bitmap to its original state
            GDI32.INSTANCE.SelectObject(memDC, oldBmp);
            GDI32.INSTANCE.DeleteDC(memDC);

            // Bring to front
            /* if (alwaysOnTop) {
                User32.INSTANCE.BringWindowToTop(hWnd);
            } */
        }
    }

    @Override
    public String toString() {
        return "LayeredWindow[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
    }

    @Override
    public void paint(final Graphics g) {
        super.paint(g);
        if (getImage() != null) {
            // Draw an image with alpha value using JNI.
            // paint(getImage().getNativeHandle(), getAlpha());

            // Using AWT as a temporary fix until I get paint() to work with Java 11.
            // Though I may keep using AWT here since I prefer to use high-level stuff...
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
        validate();
        repaint();
    }
}