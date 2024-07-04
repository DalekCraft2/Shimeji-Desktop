package com.group_finity.mascot.mac.jna;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

@FieldOrder({"highLongOfPSN", "lowLongOfPSN"})
public class ProcessSerialNumber extends Structure {
    public long highLongOfPSN, lowLongOfPSN;
}
