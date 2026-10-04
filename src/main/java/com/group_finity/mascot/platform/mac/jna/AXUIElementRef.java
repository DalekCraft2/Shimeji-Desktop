/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.platform.mac.jna;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;

/**
 * <a href="https://developer.apple.com/documentation/applicationservices/axuielementref">Apple docs: AXUIElementRef</a>
 * <p>
 * A structure used to refer to an accessibility object.
 * <h2>Overview</h2>
 * An accessibility object provides information about the user interface object it represents. This information includes
 * the object's position in the accessibility hierarchy, its position on the display, details about what it is, and what
 * actions it can perform. Accessibility objects respond to messages sent by assistive applications and send
 * notifications that describe state changes.
 *
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
