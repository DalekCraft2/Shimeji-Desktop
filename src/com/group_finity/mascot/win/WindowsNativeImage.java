package com.group_finity.mascot.win;

import com.group_finity.mascot.image.NativeImage;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

/**
 * {@link WindowsTranslucentWindow} a value that can be used with images.
 * <p>
 * {@link WindowsTranslucentWindow} is available because only Windows bitmap
 * {@link BufferedImage} existing copy pixels from a Windows bitmap.
 * <p>
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
class WindowsNativeImage implements NativeImage {

    /**
     * Windows to create a bitmap.
     *
     * @param width  width of the bitmap.
     * @param height the height of the bitmap.
     * @return the handle of a bitmap that you create.
     */
    private static WinDef.HBITMAP createNative(final int width, final int height) {
        final WinGDI.BITMAPINFOHEADER bmiHeader = new WinGDI.BITMAPINFOHEADER();
        bmiHeader.biSize = 40;
        bmiHeader.biWidth = width;
        bmiHeader.biHeight = height;
        bmiHeader.biPlanes = 1;
        bmiHeader.biBitCount = 32;

        final WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader = bmiHeader;

        return GDI32.INSTANCE.CreateDIBSection(
                null, bmi, WinGDI.DIB_RGB_COLORS, null, null, 0);
    }

    /**
     * {@link BufferedImage} to reflect the contents of the bitmap.
     *
     * @param nativeHandle bitmap handle.
     * @param rgb          ARGB of the picture.
     */
    private static void flushNative(final WinDef.HBITMAP nativeHandle, final int[] rgb) {
        final WinGDI.BITMAP bmp = new WinGDI.BITMAP();
        GDI32.INSTANCE.GetObject(nativeHandle, bmp.size(), bmp.getPointer());
        bmp.read();

        // Copy at the pixel level. These dimensions are already scaled
        int width = bmp.bmWidth.intValue();
        int height = bmp.bmHeight.intValue();
        final int destPitch = (width * bmp.bmBitsPixel + 31) / 32 * 4;
        int destIndex = destPitch * (height - 1);
        int srcColIndex = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                // TODO Fix translation errors here... after figuring out what it is supposed to say.
                // UpdateLayeredWindow and Photoshop are incompatible ?Irashii
                // UpdateLayeredWindow FFFFFF RGB value has the bug that it ignores the value of a,
                // Photoshop is where a is an RGB value of 0 have the property value to 0.

                bmp.bmBits.setInt(destIndex + x * 4L,
                        (rgb[srcColIndex] & 0xFF000000) == 0 ? 0 : rgb[srcColIndex]);

                srcColIndex++;
            }

            destIndex -= destPitch;
        }

    }

    /**
     * Windows to open a bitmap.
     *
     * @param nativeHandle bitmap handle.
     */
    private static void freeNative(final WinDef.HBITMAP nativeHandle) {
        GDI32.INSTANCE.DeleteObject(nativeHandle);
    }

    /**
     * Java Image object.
     */
    private final BufferedImage managedImage;

    /**
     * Windows bitmap handle.
     */
    private final WinDef.HBITMAP nativeHandle;

    public WindowsNativeImage(final BufferedImage image) {
        managedImage = image;
        nativeHandle = createNative(image.getWidth(), image.getHeight());

        int[] rbgValues = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        flushNative(getNativeHandle(), rbgValues);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        freeNative(getNativeHandle());
    }

    /**
     * Changes to be reflected in the Windows bitmap image.
     */
    public void update() {
        // this isn't used
    }

    public void flush() {
        getManagedImage().flush();
    }

    public Graphics getGraphics() {
        return getManagedImage().createGraphics();
    }

    public int getWidth() {
        return getManagedImage().getWidth();
    }

    public int getHeight() {
        return getManagedImage().getHeight();
    }

    public int getWidth(final ImageObserver observer) {
        return getManagedImage().getWidth(observer);
    }

    public int getHeight(final ImageObserver observer) {
        return getManagedImage().getHeight(observer);
    }

    public Object getProperty(final String name, final ImageObserver observer) {
        return getManagedImage().getProperty(name, observer);
    }

    public ImageProducer getSource() {
        return getManagedImage().getSource();
    }

    BufferedImage getManagedImage() {
        return managedImage;
    }

    public WinDef.HBITMAP getNativeHandle() {
        return nativeHandle;
    }
}
