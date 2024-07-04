module com.group_finity.mascot {
    requires java.desktop;
    requires java.management;
    requires java.logging;
    requires java.scripting;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires AbsoluteLayout.RELEASE220;
    requires nimrodlf;
    requires org.openjdk.nashorn;

    exports com.group_finity.mascot;
    exports com.group_finity.mascot.action;
    exports com.group_finity.mascot.animation;
    exports com.group_finity.mascot.behavior;
    exports com.group_finity.mascot.config;
    exports com.group_finity.mascot.environment;
    exports com.group_finity.mascot.exception;
    exports com.group_finity.mascot.generic;
    exports com.group_finity.mascot.hotspot;
    exports com.group_finity.mascot.image;
    exports com.group_finity.mascot.imagesetchooser;
    exports com.group_finity.mascot.mac;
    exports com.group_finity.mascot.mac.jna;
    exports com.group_finity.mascot.menu;
    exports com.group_finity.mascot.script;
    exports com.group_finity.mascot.sound;
    exports com.group_finity.mascot.virtual;
    exports com.group_finity.mascot.win;
    exports com.group_finity.mascot.win.jna;
    exports com.group_finity.mascot.x11;
    exports com.group_finity.mascot.x11.jna;
    exports com.joconner.i18n;
    exports hqx;
}
