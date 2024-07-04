package com.group_finity.mascot.mac.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

public interface CarbonExtra extends Library {
    CarbonExtra INSTANCE = Native.load("Carbon", CarbonExtra.class);

    long GetFrontProcess(ProcessSerialNumber psn);

    long GetProcessPID(final ProcessSerialNumber psn, LongByReference pidp);

    long AXUIElementCopyAttributeValue(
            AXUIElementRef element, CFStringRef attr, PointerByReference value);

    int AXUIElementSetAttributeValue(
            AXUIElementRef element, CFStringRef attr, CFTypeRef value);

    AXUIElementRef AXUIElementCreateApplication(long pid);

    boolean AXValueGetValue(AXValueRef value, long type, Pointer valuep);

    AXValueRef AXValueCreate(long type, Pointer valuep);

    CFTypeRef CFPreferencesCopyValue(
            CFStringRef key, CFStringRef app, Pointer user, Pointer host);

    boolean CFPreferencesAppSynchronize(CFStringRef app);

    int CFStringGetSystemEncoding();

    void CFShow(CFTypeRef any);

    NativeLibrary nl = NativeLibrary.getProcess();
    Pointer kCurrentUser = nl.getGlobalVariableAddress("kCFPreferencesCurrentUser").getPointer(0);
    Pointer kAnyHost = nl.getGlobalVariableAddress("kCFPreferencesAnyHost").getPointer(0);

    long kAXErrorSuccess = 0;
    long
            kAXValueCGPointType = 1,
            kAXValueCGSizeType = 2;
    long
            kCFNumberInt32Type = 3;
}
