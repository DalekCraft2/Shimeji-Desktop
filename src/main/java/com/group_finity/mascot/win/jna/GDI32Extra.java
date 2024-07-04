package com.group_finity.mascot.win.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HRGN;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public interface GDI32Extra extends StdCallLibrary {
    GDI32Extra INSTANCE = Native.load("gdi32", GDI32Extra.class, W32APIOptions.DEFAULT_OPTIONS);

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/wingdi/nf-wingdi-getrgnbox">Microsoft docs: GetRgnBox</a>
     * <p>
     * The GetRgnBox function retrieves the bounding rectangle of the specified region.
     *
     * @param hrgn A handle to the region.
     * @param lprc A pointer to a {@link RECT RECT} structure that receives the bounding rectangle in logical units.
     * @return The return value specifies the region's complexity. It can be one of the following values:
     * <p>{@code NULLREGION} - Region is empty.
     * <p>{@code SIMPLEREGION} - Region is a single rectangle.
     * <p>{@code COMPLEXREGION} - Region is more than a single rectangle.
     * <p>
     * If the {@param hrgn} parameter does not identify a valid region, the return value is zero.
     */
    int GetRgnBox(HRGN hrgn, RECT lprc);

    int BLACKONWHITE = 1;
    int WHITEONBLACK = 2;
    int COLORONCOLOR = 3;
    int HALFTONE = 4;
    int STRETCH_ANDSCANS = 1;
    int STRETCH_ORSCANS = 2;
    int STRETCH_DELETESCANS = 3;
    int STRETCH_HALFTONE = 4;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/wingdi/nf-wingdi-setstretchbltmode">Microsoft docs: SetStretchBltMode</a>
     * <p>
     * The SetStretchBltMode function sets the bitmap stretching mode in the specified device context.
     *
     * @param hdc A handle to the device context.
     * @param mode The stretching mode. This parameter can be one of the following values.
     * <p>{@link #BLACKONWHITE} - Performs a Boolean AND operation using the color values for the eliminated and existing pixels. If the bitmap is a monochrome bitmap, this mode preserves black pixels at the expense of white pixels.
     * <p>{@link #COLORONCOLOR} - Deletes the pixels. This mode deletes all eliminated lines of pixels without trying to preserve their information.
     * <p>{@link #HALFTONE} - Maps pixels from the source rectangle into blocks of pixels in the destination rectangle. The average color over the destination block of pixels approximates the color of the source pixels.
     * After setting the HALFTONE stretching mode, an application must call the SetBrushOrgEx function to set the brush origin. If it fails to do so, brush misalignment occurs.
     * <p>{@link #STRETCH_ANDSCANS} - Same as {@link #BLACKONWHITE}.
     * <p>{@link #STRETCH_DELETESCANS} - Same as {@link #COLORONCOLOR}.
     * <p>{@link #STRETCH_HALFTONE} - Same as {@link #HALFTONE}.
     * <p>{@link #STRETCH_ORSCANS} - Same as {@link #WHITEONBLACK}.
     * <p>{@link #WHITEONBLACK} - Performs a Boolean OR operation using the color values for the eliminated and existing pixels. If the bitmap is a monochrome bitmap, this mode preserves white pixels at the expense of black pixels.
     * @return If the function succeeds, the return value is the previous stretching mode.
     * <p>
     * If the function fails, the return value is zero.
     * <p>
     * This function can return the following value.
     * <p>{@link com.sun.jna.platform.win32.WinError#ERROR_INVALID_PARAMETER ERROR_INVALID_PARAMETER} - One or more of the input parameters is invalid.
     */
    int SetStretchBltMode(HDC hdc, int mode);

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/wingdi/nf-wingdi-stretchblt">Microsoft docs: StretchBlt</a>
     * <p>
     * The StretchBlt function copies a bitmap from a source rectangle into a destination rectangle, stretching or compressing the bitmap to fit the dimensions of the destination rectangle, if necessary. The system stretches or compresses the bitmap according to the stretching mode currently set in the destination device context.
     *
     * @param hdcDest A handle to the destination device context.
     * @param xDest The x-coordinate, in logical units, of the upper-left corner of the destination rectangle.
     * @param yDest The y-coordinate, in logical units, of the upper-left corner of the destination rectangle.
     * @param wDest The width, in logical units, of the destination rectangle.
     * @param hDest The height, in logical units, of the destination rectangle.
     * @param hdcSrc A handle to the source device context.
     * @param xSrc The x-coordinate, in logical units, of the upper-left corner of the source rectangle.
     * @param ySrc The y-coordinate, in logical units, of the upper-left corner of the source rectangle.
     * @param wSrc The width, in logical units, of the source rectangle.
     * @param hSrc The height, in logical units, of the source rectangle.
     * @param rop The raster operation to be performed. Raster operation codes define how the system combines colors in output operations that involve a brush, a source bitmap, and a destination bitmap.
     * <p>
     * See {@link com.sun.jna.platform.win32.GDI32#BitBlt(HDC, int, int, int, int, HDC, int, int, int) BitBlt} for a list of common raster operation codes (ROPs). Note that the CAPTUREBLT ROP generally cannot be used for printing device contexts.
     * @return If the function succeeds, the return value is nonzero.
     * <p>
     * If the function fails, the return value is zero.
     */
    boolean StretchBlt(HDC hdcDest, int xDest, int yDest, int wDest, int hDest, HDC hdcSrc, int xSrc, int ySrc, int wSrc, int hSrc,
                       int rop);
}
