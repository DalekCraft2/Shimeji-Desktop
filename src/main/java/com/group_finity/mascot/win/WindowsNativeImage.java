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
 * An image with alpha value that can be used for {@link WindowsTranslucentWindow}.
 * <p>
 * Only Windows bitmaps can be used for {@link WindowsTranslucentWindow}, so
 * copy pixels from an existing {@link BufferedImage} to a Windows bitmap.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
class WindowsNativeImage implements NativeImage {

    /**
     * Creates a Windows bitmap.
     *
     * @param width width of the bitmap
     * @param height height of the bitmap
     * @return handle of the created bitmap
     */
    private static WinDef.HBITMAP createNative(final int width, final int height) {
        final WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = height;
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
        bmi.bmiHeader.biSizeImage = width * height * 4;

        return GDI32.INSTANCE.CreateDIBSection(
                null, bmi, WinGDI.DIB_RGB_COLORS, null, null, 0);
    }

    /**
     * Reflects the contents of a {@link BufferedImage} in the bitmap.
     *
     * @param nativeHandle bitmap handle
     * @param rgb ARGB values of the image
     */
    private void flushNative(final WinDef.HBITMAP nativeHandle, final /*Raster raster*/ int[] rgb) {
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
                // UpdateLayeredWindow and Photoshop seem to be incompatible.
                // UpdateLayeredWindow has a bug where it ignores the alpha value when the RGB value is FFFFFF,
                // and Photoshop has the property of setting the RGB value to 0 where the alpha value is 0.

                bmp.bmBits.setInt(destIndex + x * 4L,
                        (rgb[srcColIndex] & 0xFF000000) == 0 ? 0 : rgb[srcColIndex]);

                srcColIndex++;
            }

            destIndex -= destPitch;
        }

        /* int[] pixel = new int[4];
        int[] bits = new int[width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                raster.getPixel(col, row, pixel);
                int alpha = (pixel[3] & 0xFF) << 24;
                int red = (pixel[2] & 0xFF);
                int green = (pixel[1] & 0xFF) << 8;
                int blue = (pixel[0] & 0xFF) << 16;
                bits[col] = alpha | red | green | blue;
            }
            int v = height - (origin.y + row) - 1;
            bmp.bmBits.write((v * width + origin.x) * 4, bits, 0, bits.length);
        } */
    }

    /**
     * Free up Windows bitmaps.
     *
     * @param nativeHandle bitmap handle
     */
    private static void freeNative(final WinDef.HBITMAP nativeHandle) {
        GDI32.INSTANCE.DeleteObject(nativeHandle);
    }

    /**
     * Java image object.
     */
    private final BufferedImage managedImage;

    /**
     * Windows bitmap handle.
     */
    private final WinDef.HBITMAP nativeHandle;

    public WindowsNativeImage(final BufferedImage image) {
        managedImage = image;
        if (WindowsTranslucentWindow.USE_AWT) {
            nativeHandle = null;
        } else {
            nativeHandle = createNative(image.getWidth(), image.getHeight());

            update();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (WindowsTranslucentWindow.USE_AWT) {
            freeNative(getNativeHandle());
        }
    }

    /**
     * Reflects changes to the image in the Windows bitmap.
     */
    public void update() {
        int[] rbgValues = managedImage.getRGB(0, 0, managedImage.getWidth(), managedImage.getHeight(), null, 0, managedImage.getWidth());

        flushNative(nativeHandle, rbgValues);
        // flushNative(nativeHandle, managedImage.getData());
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
