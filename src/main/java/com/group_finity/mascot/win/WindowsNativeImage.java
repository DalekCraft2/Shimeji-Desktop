package com.group_finity.mascot.win;

import com.group_finity.mascot.image.NativeImage;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAP;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;

import java.awt.image.BufferedImage;

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
     * Creates a Windows bitmap from a Java image object.
     *
     * @param image the image to load into a bitmap
     * @return new Windows bitmap with the image's data
     */
    private static HBITMAP createBitmap(BufferedImage image) {
        int[] rgb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        // Copy at the pixel level. These dimensions are already scaled
        int width = image.getWidth();
        int height = image.getHeight();

        HBITMAP handle = createNative(width, height);

        BITMAP bmp = new BITMAP();
        GDI32.INSTANCE.GetObject(handle, bmp.size(), bmp.getPointer());
        bmp.read();

        final int destPitch = (width * bmp.bmBitsPixel + 31) / 32 * 4;
        int destIndex = destPitch * (height - 1);
        int srcColIndex = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // UpdateLayeredWindow and Photoshop seem to be incompatible.
                // UpdateLayeredWindow has a bug where it ignores the alpha value when the RGB value is FFFFFF,
                // and Photoshop has the property of setting the RGB value to 0 where the alpha value is 0.

                long offset = destIndex + x * 4L;

                int value = rgb[srcColIndex];
                if ((value & 0xFF000000) == 0) {
                    value = 0;
                }

                bmp.bmBits.setInt(offset, value);

                srcColIndex++;
            }

            destIndex -= destPitch;
        }

        return handle;
    }

    /**
     * Creates a Windows bitmap.
     *
     * @param width width of the bitmap
     * @param height height of the bitmap
     * @return handle of the created bitmap
     */
    private static HBITMAP createNative(int width, int height) {
        final BITMAPINFO bmi = new BITMAPINFO();
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
     * @param handle bitmap handle
     * @param rgb ARGB values of the image
     */
    private void flushNative(final HBITMAP handle, final int[] rgb) {
        final BITMAP bmp = new BITMAP();
        GDI32.INSTANCE.GetObject(handle, bmp.size(), bmp.getPointer());
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

                long offset = destIndex + x * 4L;

                int value = rgb[srcColIndex];
                if ((value & 0xFF000000) == 0) {
                    value = 0;
                }

                bmp.bmBits.setInt(offset, value);

                srcColIndex++;
            }

            destIndex -= destPitch;
        }
    }

    /**
     * Free up Windows bitmaps.
     *
     * @param handle bitmap handle
     */
    private static void freeNative(final HBITMAP handle) {
        GDI32.INSTANCE.DeleteObject(handle);
    }

    /**
     * Java image object.
     */
    private final BufferedImage managedImage;

    /**
     * Windows bitmap handle.
     */
    private final HBITMAP bmpHandle;

    public WindowsNativeImage(final BufferedImage image) {
        managedImage = image;
        if (WindowsTranslucentWindow.USE_AWT) {
            bmpHandle = null;
        } else {
            // bmpHandle = createNative(image.getWidth(), image.getHeight());
            //
            // update();
            bmpHandle = createBitmap(image);
        }
    }

    // TODO Migrate this away from using finalize().
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (!WindowsTranslucentWindow.USE_AWT) {
            freeNative(getBmpHandle());
        }
    }

    /**
     * Reflects changes to the image in the Windows bitmap.
     */
    public void update() {
        int[] rbgValues = managedImage.getRGB(0, 0, managedImage.getWidth(), managedImage.getHeight(), null, 0, managedImage.getWidth());

        flushNative(bmpHandle, rbgValues);
    }

    BufferedImage getManagedImage() {
        return managedImage;
    }

    public HBITMAP getBmpHandle() {
        return bmpHandle;
    }
}
