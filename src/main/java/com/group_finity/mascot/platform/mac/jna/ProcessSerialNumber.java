/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.platform.mac.jna;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * <a href="https://dev.os9.ca/techpubs/mac/Processes/Processes-34.html">Apple docs: ProcessSerialNumber</a>
 * <p>
 * The Process Manager uses process serial numbers to identify open processes. A process serial number is a 64-bit
 * quantity whose structure is defined by the {@code ProcessSerialNumber} data type.
 * <h2>IMPORTANT</h2>
 * The meaning of the bits in a process serial number is internal to the Process Manager. You should not attempt to
 * interpret the value of the process serial number. If you need to compare two process serial numbers, call the
 * <a href="https://developer.apple.com/documentation/applicationservices/1501087-sameprocess">SameProcess</a> function.
 *
 * @author nonowarn
 */
@FieldOrder({"highLongOfPSN", "lowLongOfPSN"})
public class ProcessSerialNumber extends Structure {
    /** The high-order long integer of the process serial number. */
    public long highLongOfPSN;

    /** The low-order long integer of the process serial number. */
    public long lowLongOfPSN;
}
