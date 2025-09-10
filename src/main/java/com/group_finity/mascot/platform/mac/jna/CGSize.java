/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.platform.mac.jna;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * @author nonowarn
 */
@FieldOrder({"width", "height"})
public class CGSize extends Structure {
    public double width, height;

    public int getWidth() {
        return (int) Math.round(width);
    }

    public int getHeight() {
        return (int) Math.round(height);
    }
}

