module com.group_finity.mascot {
    requires java.desktop;
    requires java.logging;
    requires java.scripting;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.openjdk.nashorn;
    requires org.apache.commons.exec;
    requires com.jthemedetector;
    requires com.formdev.flatlaf;
    requires org.slf4j;

    // Export classes used in scripting
    // (i.e., any classes that are accessible from Mascot and Action)
    exports com.group_finity.mascot;
    exports com.group_finity.mascot.action;
    exports com.group_finity.mascot.animation;
    exports com.group_finity.mascot.behavior;
    exports com.group_finity.mascot.environment;
    exports com.group_finity.mascot.image;
    exports com.group_finity.mascot.script;

    // Open JNA packages to JNA
    opens com.group_finity.mascot.platform.mac.jna to com.sun.jna;
    opens com.group_finity.mascot.platform.win.jna to com.sun.jna;
}
