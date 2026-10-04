/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.platform.mac.jna;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;

/**
 * Represents the macOS Application Services
 * <a href="https://developer.apple.com/documentation/applicationservices/axvalueref">AXValueRef</a> struct.
 *
 * @author nonowarn
 */
public class AXValueRef extends CFTypeRef {
    public AXValueRef() {
        super();
    }

    public AXValueRef(Pointer p) {
        super(p);
    }
}
