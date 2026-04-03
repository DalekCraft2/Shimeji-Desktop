/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.platform.mac.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * @author nonowarn
 */
public interface CarbonExtra extends Library {
    CarbonExtra INSTANCE = Native.load("Carbon", CarbonExtra.class);

    NativeLibrary nl = NativeLibrary.getProcess();

    /**
     * <a href="https://developer.apple.com/documentation/corefoundation/kcfpreferencescurrentuser">Apple docs: kCFPreferencesCurrentUser</a>
     * <p>
     * Indicates a preference that applies only to the current user.
     */
    Pointer kCFPreferencesCurrentUser = nl.getGlobalVariableAddress("kCFPreferencesCurrentUser").getPointer(0);

    /**
     * <a href="https://developer.apple.com/documentation/corefoundation/kcfpreferencesanyhost">Apple docs: kCFPreferencesAnyHost</a>
     * <p>
     * Indicates a preference that applies only to the current user.
     * <h1>Discussion</h1>
     * This option is not supported.
     */
    Pointer kCFPreferencesAnyHost = nl.getGlobalVariableAddress("kCFPreferencesAnyHost").getPointer(0);

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/axerror/kaxerrorsuccess">Apple docs: kAXErrorSuccess</a>
     * <p>
     * No error occurred.
     */
    long kAXErrorSuccess = 0;

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/axvaluetype/kaxvaluecgpointtype">Apple docs: kAXValueCGPointType</a>
     * <p>
     * a wrapper for CGPoint; see CoreGraphics.h
     */
    long kAXValueCGPointType = 1;

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/axvaluetype/kaxvaluecgsizetype">Apple docs: kAXValueCGSizeType</a>
     * <p>
     * a wrapper for CGSize; see CoreGraphics.h
     */
    long kAXValueCGSizeType = 2;

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/1501050-getfrontprocess">Apple docs: GetFrontProcess</a>
     *
     * @param psn
     * @return
     */
    @Deprecated
    long GetFrontProcess(ProcessSerialNumber psn);

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/1500992-getprocesspid">Apple docs: GetProcessPID</a>
     *
     * @param psn
     * @param pid
     * @return
     */
    @Deprecated
    long GetProcessPID(final ProcessSerialNumber psn, LongByReference pid);

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/1462085-axuielementcopyattributevalue">Apple docs: AXUIElementCopyAttributeValue</a>
     * <p>
     * Returns the value of an accessibility object's attribute.
     *
     * @param element The AXUIElementRef representing the accessibility object.
     * @param attribute The attribute name.
     * @param value On return, the value associated with the specified attribute.
     * @return If unsuccessful, {@code AXUIElementCopyAttributeValue} may return one of the following error codes, among others:
     * <p>{@code kAXErrorAttributeUnsupported} - The specified AXUIElementRef does not support the specified attribute.
     * <p>{@code kAXErrorNoValue} - The specified attribute does not have a value.
     * <p>{@code kAXErrorIllegalArgument} - One or more of the arguments is an illegal value.
     * <p>{@code kAXErrorInvalidUIElement} - The AXUIElementRef is invalid.
     * <p>{@code kAXErrorCannotComplete} - The function cannot complete because messaging has failed in some way.
     * <p>{@code kAXErrorNotImplemented} - The process does not fully support the accessibility API.
     */
    long AXUIElementCopyAttributeValue(
            AXUIElementRef element, CFStringRef attribute, PointerByReference value);

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/1460434-axuielementsetattributevalue">Apple docs: AXUIElementSetAttributeValue</a>
     * <p>
     * Sets the accessibility object's attribute to the specified value.
     * <h1>Discussion</h1>
     * You can send and receive many different CFTypeRefs using the accessibility API. These include all CFPropertyListRef types, AXUIElementRef, AXValueRef, AXTextMarkerRef, AXTextMarkerRangeRef, CFNullRef, CFAttributedStringRef, and CRURLRef.
     *
     * @param element The AXUIElementRef representing the accessibility object.
     * @param attribute The attribute name.
     * @param value The new value for the attribute.
     * @return If unsuccessful, {@code AXUIElementSetAttributeValue} may return one of the following error codes, among others:
     * <p>{@code kAXErrorIllegalArgument} - The value is not recognized by the accessible application or one of the other arguments is an illegal value.
     * <p>{@code kAXErrorAttributeUnsupported} - The specified AXUIElementRef does not support the specified attribute.
     * <p>{@code kAXErrorInvalidUIElement} - The AXUIElementRef is invalid.
     * <p>{@code kAXErrorCannotComplete} - The function cannot complete because messaging has failed in some way.
     * <p>{@code kAXErrorNotImplemented} - The process does not fully support the accessibility API.
     */
    int AXUIElementSetAttributeValue(
            AXUIElementRef element, CFStringRef attribute, CFTypeRef value);

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/1459374-axuielementcreateapplication">Apple docs: AXUIElementCreateApplication</a>
     * <p>
     * Creates and returns the top-level accessibility object for the application with the specified process ID.
     *
     * @param pid The process ID of an application.
     * @return The AXUIElementRef representing the top-level accessibility object for the application with the specified process ID.
     */
    AXUIElementRef AXUIElementCreateApplication(long pid);

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/1462933-axvaluegetvalue">Apple docs: AXValueGetValue</a>
     * <p>
     * Decodes the structure stored in value and copies it into valuePtr. If the structure stored in value is not the same as requested by theType, the function returns false.
     *
     * @param value
     * @param theType
     * @param valuePtr
     * @return
     */
    boolean AXValueGetValue(AXValueRef value, long theType, Pointer valuePtr);

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/1459351-axvaluecreate">Apple docs: AXValueCreate</a>
     * <p>
     * Encodes a structure pointed to by valuePtr into a CFTypeRef.
     *
     * @param theType
     * @param valuePtr
     * @return
     */
    AXValueRef AXValueCreate(long theType, Pointer valuePtr);

    /**
     * <a href="https://developer.apple.com/documentation/corefoundation/cfpreferencescopyvalue(_:_:_:_:)">Apple docs: CFPreferencesCopyValue</a>
     * <p>
     * Returns a preference value for a given domain.
     * <h1>Discussion</h1>
     * This function is the primitive get mechanism for the higher level preference function {@code CFPreferencesCopyAppValue}.
     * Unlike the high-level function, {@code CFPreferencesCopyValue} searches only the exact domain specified. Do not
     * use this function directly unless you have a need. All arguments must be non-{@code NULL}. Do not use arbitrary user and
     * host names, instead pass the pre-defined domain qualifier constants.
     * <p>
     * Note that values returned from this function are immutable, even if you have recently set the value using a
     * mutable object.
     *
     * @param key Preferences key for the value to obtain.
     * @param applicationID The ID of the application whose preferences are searched.
     * Takes the form of a Java package name, such as {@code com.foosoft}.
     * @param userName {@link CarbonExtra#kCFPreferencesCurrentUser} if to search the current-user domain, otherwise {@code kCFPreferencesAnyUser} to search the any-user domain.
     * @param hostName {@code kCFPreferencesCurrentHost} if to search the current-host domain, otherwise {@link CarbonExtra#kCFPreferencesAnyHost} to search the any-host domain.
     * @return The preference data for the specified domain. If no value was located, returns {@code NULL}. Ownership follows
     * the <a href="https://developer.apple.com/library/archive/documentation/CoreFoundation/Conceptual/CFMemoryMgmt/Concepts/Ownership.html#//apple_ref/doc/uid/20001148-103029">The Create Rule</a>.
     */
    CFTypeRef CFPreferencesCopyValue(
            CFStringRef key, CFStringRef applicationID, Pointer userName, Pointer hostName);

    /**
     * <a href="https://developer.apple.com/documentation/corefoundation/cfpreferencesappsynchronize(_:)">Apple docs: CFPreferencesAppSynchronize</a>
     * <p>
     * Writes to permanent storage all pending changes to the preference data for the application,
     * and reads the latest preference data from permanent storage.
     * <h1>Discussion</h1>
     * Calling the function {@code CFPreferencesSetAppValue} is not in itself sufficient for storing preferences.
     * The {@code CFPreferencesAppSynchronize} function writes to permanent storage all pending preference changes
     * for the application. Typically you would call this function after multiple calls to {@code CFPreferencesSetAppValue}.
     * Conversely, preference data is cached after it is first read. Changes made externally are not automatically incorporated.
     * The {@code CFPreferencesAppSynchronize} function reads the latest preferences from permanent storage.
     *
     * @param applicationID The ID of the application whose preferences to write to storage, typically {@code kCFPreferencesCurrentApplication}.
     * Do not pass {@code NULL} or {@code kCFPreferencesAnyApplication}. Takes the form of a Java package name, such as {@code com.foosoft}.
     * @return {@code true} if synchronization was successful, otherwise {@code false}.
     */
    boolean CFPreferencesAppSynchronize(CFStringRef applicationID);

    /**
     * <a href="https://developer.apple.com/documentation/corefoundation/cfstringgetsystemencoding()">Apple docs: CFStringGetSystemEncoding</a>
     * <p>
     * Returns the default encoding used by the operating system when it creates strings.
     * <h1>Discussion</h1>
     * This function returns the default text encoding used by the OS when it creates strings. In macOS, this encoding
     * is determined by the user’s preferred language setting. The preferred language is the first language listed in
     * the International pane of the System Preferences.
     * <p>
     * In most situations you will not want to use this function, however, because your primary interest will be your
     * application’s default text encoding. The application encoding is required when you create a CFStringRef from
     * strings stored in Resource Manager resources, which typically use one of the Mac encodings such as MacRoman or MacJapanese.
     * <p>
     * To get your application’s default text encoding, call the {@code GetApplicationTextEncoding} Carbon function.
     *
     * @return The default string encoding.
     */
    int CFStringGetSystemEncoding();

    /**
     * <a href="https://developer.apple.com/documentation/corefoundation/cfshow(_:)">Apple docs: CFShow</a>
     * <p>
     * Prints a description of a Core Foundation object to stderr.
     *
     * <h1>Discussion</h1>
     * The output is printed to the standard I/O standard error (stderr).
     * <p>
     * This function is useful as a debugging aid for Core Foundation objects. Because these objects are based on opaque
     * types, it is difficult to examine their contents directly. However, the opaque types implement {@code description}
     * function callbacks that return descriptions of their objects. This function invokes these callbacks.
     * <h2>Special Considerations</h2>
     * You can use {@code CFShow} in one of two general ways. If your debugger supports function calls (such as {@code gdb}does),
     * call {@code CFShow} in the debugger:
     * <pre>
     * (gdb) call (void) CFShow(string)
     * Hello World
     * </pre>
     * You can also incorporate calls to {@code CFShow} in a test version of your code to print out “snapshots” of Core Foundation objects to the console.
     *
     * @param obj A Core Foundation object derived from CFType. If {@code obj} is not a Core Foundation object, an assertion is raised.
     */
    void CFShow(CFTypeRef obj);
}
