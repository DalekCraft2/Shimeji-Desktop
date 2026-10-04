package com.group_finity.mascot.platform.win.jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Provides access to the {@link #DwmGetWindowAttribute(HWND, int, Pointer, int)} function
 * in the Windows {@code dwmapi}
 * (<a href="https://learn.microsoft.com/en-us/windows/win32/api/_dwm/">Desktop Window Manager</a>) library.
 * This mainly exists to get access to the {@link #DWMWA_CLOAKED} window attribute.
 *
 * @author Kilkakon
 */
public interface Dwmapi extends StdCallLibrary {
    /**
     * The instance of the {@code dwmapi} library interface.
     */
    Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * <b>IMPORTANT</b>. The value of {@code DWMWA_NCRENDERING_ENABLED} is 1.
     * <pre>
     * typedef enum DWMWINDOWATTRIBUTE {
     *   DWMWA_NCRENDERING_ENABLED = 1,
     *   DWMWA_NCRENDERING_POLICY,
     *   ...
     * </pre>
     * Use with {@link #DwmGetWindowAttribute}. Discovers whether non-client rendering is enabled.
     * The retrieved value is of type {@code BOOL}. {@code TRUE} if non-client rendering is enabled; otherwise, {@code FALSE}.
     */
    int DWMWA_NCRENDERING_ENABLED = 1;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute">DwmSetWindowAttribute</a>.
     * Sets the non-client rendering policy. The {@code pvAttribute} parameter points to a value from the
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmncrenderingpolicy">DWMNCRENDERINGPOLICY</a> enumeration.
     */
    int DWMWA_NCRENDERING_POLICY = 2;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute">DwmSetWindowAttribute</a>.
     * Enables or forcibly disables DWM transitions. The {@code pvAttribute} parameter points to a value of type {@code BOOL}.
     * {@code TRUE} to disable transitions, or {@code FALSE} to enable transitions.
     */
    int DWMWA_TRANSITIONS_FORCEDISABLED = 3;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute">DwmSetWindowAttribute</a>.
     * Enables content rendered in the non-client area to be visible on the frame drawn by DWM.
     * The {@code pvAttribute} parameter points to a value of type {@code BOOL}.
     * {@code TRUE} to enable content rendered in the non-client area to be visible on the frame; otherwise, {@code FALSE}.
     */
    int DWMWA_ALLOW_NCPAINT = 4;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with {@link #DwmGetWindowAttribute}.
     * Retrieves the bounds of the caption button area in the window-relative space. The retrieved value is of type
     * {@link com.sun.jna.platform.win32.WinDef.RECT RECT}. If the window is minimized or otherwise not visible
     * to the user, then the value of the {@code RECT} retrieved is undefined. You should check whether the retrieved
     * {@code RECT} contains a boundary that you can work with, and if it doesn't then you can conclude that the window
     * is minimized or otherwise not visible.
     */
    int DWMWA_CAPTION_BUTTON_BOUNDS = 5;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute">DwmSetWindowAttribute</a>.
     * Specifies whether non-client content is right-to-left (RTL) mirrored.
     * The {@code pvAttribute} parameter points to a value of type {@code BOOL}.
     * {@code TRUE} if the non-client content is right-to-left (RTL) mirrored; otherwise, {@code FALSE}.
     */
    int DWMWA_NONCLIENT_RTL_LAYOUT = 6;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute">DwmSetWindowAttribute</a>.
     * Forces the window to display an iconic thumbnail or peek representation (a static bitmap), even if a live or
     * snapshot representation of the window is available. This value is normally set during a window's creation, and
     * not changed throughout the window's lifetime. Some scenarios, however, might require the value to change over
     * time. The {@code pvAttribute} parameter points to a value of type {@code BOOL}. {@code TRUE} to require a iconic
     * thumbnail or peek representation; otherwise, {@code FALSE}.
     */
    int DWMWA_FORCE_ICONIC_REPRESENTATION = 7;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute">DwmSetWindowAttribute</a>.
     * Sets how Flip3D treats the window. The {@code pvAttribute} parameter points to a value from the
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmflip3dwindowpolicy">DWMFLIP3DWINDOWPOLICY</a> enumeration.
     */
    int DWMWA_FLIP3D_POLICY = 8;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with {@link #DwmGetWindowAttribute}.
     * Retrieves the extended frame bounds rectangle in screen space. The retrieved value is of type
     * {@link com.sun.jna.platform.win32.WinDef.RECT RECT}.
     */
    int DWMWA_EXTENDED_FRAME_BOUNDS = 9;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute">DwmSetWindowAttribute</a>.
     * The window will provide a bitmap for use by DWM as an iconic thumbnail or peek representation (a static bitmap)
     * for the window. {@code DWMWA_HAS_ICONIC_BITMAP} can be specified with {@code DWMWA_FORCE_ICONIC_REPRESENTATION}.
     * {@code DWMWA_HAS_ICONIC_BITMAP} normally is set during a window's creation and not changed throughout the
     * window's lifetime. Some scenarios, however, might require the value to change over time. The {@code pvAttribute}
     * parameter points to a value of type {@code BOOL}. {@code TRUE} to inform DWM that the window will provide an
     * iconic thumbnail or peek representation; otherwise, {@code FALSE}.
     * <p>
     * <b>Windows Vista and earlier:</b> This value is not supported.
     */
    int DWMWA_HAS_ICONIC_BITMAP = 10;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute">DwmSetWindowAttribute</a>.
     * Do not show peek preview for the window. The peek view shows a full-sized preview of the window when the mouse
     * hovers over the window's thumbnail in the taskbar. If this attribute is set, hovering the mouse pointer over the
     * window's thumbnail dismisses peek (in case another window in the group has a peek preview showing). The
     * {@code pvAttribute} parameter points to a value of type {@code BOOL}. {@code TRUE} to prevent peek functionality,
     * or {@code FALSE} to allow it.
     * <p>
     * <b>Windows Vista and earlier:</b> This value is not supported.
     */
    int DWMWA_DISALLOW_PEEK = 11;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute">DwmSetWindowAttribute</a>.
     * Prevents a window from fading to a glass sheet when peek is invoked. The {@code pvAttribute} parameter points to
     * a value of type {@code BOOL}. {@code TRUE} to prevent the window from fading during another window's peek, or
     * {@code FALSE} for normal behavior.
     * <p>
     * <b>Windows Vista and earlier:</b> This value is not supported.
     */
    int DWMWA_EXCLUDED_FROM_PEEK = 12;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute">DwmSetWindowAttribute</a>.
     * Cloaks the window such that it is not visible to the user. The window is still composed by DWM.
     * <p>
     * <b>Using with DirectComposition:</b> Use the {@code DWMWA_CLOAK} flag to cloak the layered child window when
     * animating a representation of the window's content via a DirectComposition visual that has been associated with
     * the layered child window. For more details on this usage case, see
     * <a href="https://learn.microsoft.com/en-us/windows/win32/directcomp/how-to--animate-the-bitmap-of-a-layered-child-window">
     * How to animate the bitmap of a layered child window.</a>
     * <p>
     * <b>Windows 7 and earlier:</b> This value is not supported.
     */
    int DWMWA_CLOAK = 13;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with {@link #DwmGetWindowAttribute}.
     * If the window is cloaked, provides one of the following values explaining why.
     * <p>
     * <b>DWM_CLOAKED_APP</b> (value 0x00000001). The window was cloaked by its owner application.
     * <p>
     * <b>DWM_CLOAKED_SHELL</b> (value 0x00000002). The window was cloaked by the Shell.
     * <p>
     * <b>DWM_CLOAKED_INHERITED</b> (value 0x00000004). The cloak value was inherited from its owner window.
     * <p>
     * <b>Windows 7 and earlier:</b> This value is not supported.
     */
    int DWMWA_CLOAKED = 14;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">Microsoft docs: DWMWINDOWATTRIBUTE</a>
     * <p>
     * Use with <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute">DwmSetWindowAttribute</a>.
     * Freeze the window's thumbnail image with its current visuals. Do no further live updates on the thumbnail image
     * to match the window's contents.
     * <p>
     * <b>Windows 7 and earlier:</b> This value is not supported.
     */
    int DWMWA_FREEZE_REPRESENTATION = 15;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmgetwindowattribute">Microsoft docs: DwmGetWindowAttribute</a>
     * <p>
     * Retrieves the current value of a specified Desktop Window Manager (DWM) attribute applied to a window.
     * <p>
     * For programming guidance, and code examples, see
     * <a href="https://learn.microsoft.com/en-us/windows/win32/dwm/composition-ovw#controlling-non-client-region-rendering">Controlling non-client region rendering</a>.
     *
     * @param hwnd The handle to the window from which the attribute value is to be retrieved.
     * @param dwAttribute A flag describing which value to retrieve, specified as a value of the
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">DWMWINDOWATTRIBUTE</a> enumeration.
     * This parameter specifies which attribute to retrieve, and the {@code pvAttribute} parameter points to
     * an object into which the attribute value is retrieved.
     * @param pvAttribute A pointer to a value which, when this function returns successfully, receives the current value of the attribute.
     * The type of the retrieved value depends on the value of the {@code dwAttribute} parameter. The
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">DWMWINDOWATTRIBUTE</a>
     * enumeration topic indicates, in the row for each flag, what type of value you should pass a pointer to in
     * the {@code pvAttribute} parameter.
     * @param cbAttribute The size, in bytes, of the attribute value being received via the {@code pvAttribute} parameter.
     * The type of the retrieved value, and therefore its size in bytes, depends on the value of the
     * {@code dwAttribute} parameter.
     * @return If the function succeeds, it returns {@link WinError#S_OK S_OK}. Otherwise, it returns an {@link HRESULT HRESULT}
     * <a href="https://learn.microsoft.com/en-us/windows/win32/com/com-error-codes-10">error code</a>.
     */
    HRESULT DwmGetWindowAttribute(HWND hwnd, int dwAttribute, Pointer pvAttribute, int cbAttribute);
}
