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
 * The image window with alpha.
 * {@link #setImage(NativeImage)} set in {@link WindowsNativeImage} can be displayed on the desktop.
 * <p>
 * {@link #setAlpha(int)} may be specified when the concentration of view.
 * <p>
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
class WindowsTranslucentWindow extends JWindow implements TranslucentWindow {

    private static final long serialVersionUID = 1L;

    @Override
    public Component asComponent() {
        return this;
    }

    /**
     * Draw a picture with a value of alpha.
     *
     * @param imageHandle bitmap handle.
     * @param alpha       concentrations shown. 0 = not at all, 255 = full display.
     */
    private void paint(final WinDef.HBITMAP imageHandle, final int alpha) {
        // setSize(WIDTH, HEIGHT);

        final WinDef.HWND hWnd = new WinDef.HWND(Native.getComponentPointer(this));

        if (User32.INSTANCE.IsWindow(hWnd)) {

            final int exStyle = WindowsUtil.GetWindowLong(hWnd, User32.GWL_EXSTYLE).intValue();
            if ((exStyle & User32.WS_EX_LAYERED) == 0) {
                WindowsUtil.SetWindowLong(hWnd, User32.GWL_EXSTYLE, Pointer.createConstant(exStyle | User32.WS_EX_LAYERED));
            }

            // Create a DC source of the image
            final WinDef.HDC clientDC = User32.INSTANCE.GetDC(hWnd);
            final WinDef.HDC memDC = GDI32.INSTANCE.CreateCompatibleDC(clientDC);
            final WinNT.HANDLE oldBmp = GDI32.INSTANCE.SelectObject(memDC, imageHandle);

            User32.INSTANCE.ReleaseDC(hWnd, clientDC);

            // Destination Area
            final Rectangle windowRect = WindowUtils.getWindowLocationAndSize(hWnd);

            // Forward
            final WinUser.BLENDFUNCTION bf = new WinUser.BLENDFUNCTION();
            bf.BlendOp = WinUser.AC_SRC_OVER;
            bf.BlendFlags = 0;
            bf.SourceConstantAlpha = (byte) alpha; // Level set
            bf.AlphaFormat = WinUser.AC_SRC_ALPHA;

            final WinDef.POINT lt = new WinDef.POINT();
            lt.x = windowRect.x;
            lt.y = windowRect.y;
            final WinUser.SIZE size = new WinUser.SIZE();
            size.cx = windowRect.width;
            size.cy = windowRect.height;
            final WinDef.POINT zero = new WinDef.POINT();
            User32.INSTANCE.UpdateLayeredWindow(
                    hWnd, null,
                    lt, size,
                    memDC, zero, 0, bf, User32.ULW_ALPHA);

            // Replace the bitmap you
            GDI32.INSTANCE.SelectObject(memDC, oldBmp);
            GDI32.INSTANCE.DeleteDC(memDC);

            // Bring to front
            /* if (alwaysOnTop) {
                User32.INSTANCE.BringWindowToTop(hWnd);
            } */
        }
    }

    /**
     * Image to display.
     */
    private WindowsNativeImage image;

    /**
     * The concentration shown. 0 = not at all, 255 = full display.
     */
    private int alpha = 255;

    @Override
    public String toString() {
        return "LayeredWindow[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
    }

    @Override
    public void paint(final Graphics g) {
        if (getImage() != null) {
            // JNI with drawing images using the alpha value.
            paint(getImage().getHandle(), getAlpha());
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
        repaint();
    }
}