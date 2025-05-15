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
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 * @author Valkryst
 */
class WindowsNativeImage implements NativeImage {
    /**
     * Java image object.
     */
    private final BufferedImage managedImage;

    public WindowsNativeImage(final BufferedImage image) {
        managedImage = image;
    }

    BufferedImage getManagedImage() {
        return managedImage;
    }
}
