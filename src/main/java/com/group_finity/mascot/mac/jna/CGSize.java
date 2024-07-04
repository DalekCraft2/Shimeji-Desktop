package com.group_finity.mascot.mac.jna;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

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

