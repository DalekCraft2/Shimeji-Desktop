/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.platform.mac.jna;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;

/**
 * @author nonowarn
 */
public class AXUIElementRef extends CFTypeRef {
    public AXUIElementRef() {
        super();
    }

    public AXUIElementRef(Pointer p) {
        super(p);
    }
}
