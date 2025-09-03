/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.mac.jna;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * @author nonowarn
 */
@FieldOrder({"highLongOfPSN", "lowLongOfPSN"})
public class ProcessSerialNumber extends Structure {
    public long highLongOfPSN, lowLongOfPSN;
}
