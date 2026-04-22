package com.group_finity.mascot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;

/**
 * Manages everything related to the program's tray icon and its associated popup menu.
 *
 * @author DalekCraft
 */
public class TrayMenu {
    private static final Logger log = LoggerFactory.getLogger(TrayMenu.class);

    private TrayIcon trayIcon;
    private Window trayMenuWindow;
    private TrayMenuPanel trayMenuPanel;
    private WindowListener trayMenuWindowListener;
    private boolean debouncing = false;
    private Timer debounceTimer;

    /**
     * Creates a tray icon, or recreates the existing tray icon if one already existed.
     */
    void createTrayIcon() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
            debounceTimer.stop();
            debounceTimer = null;
        }
        if (trayMenuWindow != null) {
            if (trayMenuWindowListener != null) {
                trayMenuWindow.removeWindowListener(trayMenuWindowListener);
                trayMenuWindowListener = null;
            }
            trayMenuWindow.dispose();
            trayMenuWindow = null;
            trayMenuPanel = null;
        }

        final Settings settings = Main.getInstance().getSettings();
        final boolean showTrayIcon = settings.showTrayIcon;
        // If this is false, replace the tray icon with a window that stops the program when closed.
        final boolean useSystemTray = showTrayIcon && SystemTray.isSupported();

        if (!useSystemTray) {
            // If the settings say to show the tray icon but the system tray isn't supported,
            // tell the user (via log) that the persistent menu window is being used as an alternative
            if (showTrayIcon && !SystemTray.isSupported()) {
                log.warn("System tray not supported; creating persistent menu window instead");
                // Change the setting to false so the warning doesn't happen on every startup after this
                settings.showTrayIcon = false;
            } else
                log.info("Creating persistent menu window");
            createTrayMenuWindow(false, null);
            return;
        }

        log.info("Creating tray icon");

        // get the tray icon image
        BufferedImage image = Main.getIcon();

        // Create the tray icon
        String tooltip = settings.shimejiEeNameOverride;
        if (tooltip.isEmpty()) {
            tooltip = Main.getInstance().getLanguageBundle().getString("ShimejiEE");
        }
        trayIcon = new TrayIcon(image, tooltip);
        trayIcon.setImageAutoSize(true);

        debounceTimer = new Timer(1000, event -> debouncing = false);
        // attach menu
        trayIcon.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (debouncing) {
                    return;
                }

                debouncing = true;
                debounceTimer.setRepeats(false);
                debounceTimer.restart();

                if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
                    // Create a mascot when the icon is left-clicked
                    Main.getInstance().createMascot();
                } else if (SwingUtilities.isMiddleMouseButton(e) && e.getClickCount() == 2) {
                    // When the icon is double-middle-clicked, dispose of all mascots, but do not close the program
                    /* BUG: On Windows 11, Java seems to think the middle mouse button is the left mouse button, so this code never gets executed.
                    This is a JDK bug: https://bugs.openjdk.org/browse/JDK-8341173 */
                    Manager manager = Main.getInstance().getManager();
                    if (manager.isExitOnLastRemoved()) {
                        manager.setExitOnLastRemoved(false);
                        manager.disposeAll();
                    } else {
                        // If the mascots are already gone, recreate one mascot for each active image set
                        for (String imageSet : Main.getInstance().getImageSets()) {
                            Main.getInstance().createMascot(imageSet);
                            manager.setExitOnLastRemoved(true);
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Check for popup triggers in both mousePressed and mouseReleased
                // because it works differently on different systems
                if (e.isPopupTrigger()) {
                    onPopupTrigger(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Check for popup triggers in both mousePressed and mouseReleased
                // because it works differently on different systems
                if (e.isPopupTrigger()) {
                    onPopupTrigger(e);
                }
            }

            private void onPopupTrigger(MouseEvent event) {
                createTrayMenuWindow(true, event);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        try {
            // Show tray icon
            SystemTray.getSystemTray().add(trayIcon);
        } catch (final AWTException e) {
            log.error("Failed to create tray icon", e);
            Main.showError(Main.getInstance().getLanguageBundle().getString("FailedDisplaySystemTrayErrorMessage"), e);
            Main.getInstance().exit();
        }
    }

    /**
     * Creates the popup menu used by the tray icon.
     *
     * @param useSystemTray whether the system tray is being used.
     * If true, the menu will be disposed after one of its options has been pressed.
     * If false, the menu will not be disposed.
     * @param event the mouse event that will be used to position the menu, if the menu was opened via the system tray
     */
    private void createTrayMenuWindow(boolean useSystemTray, MouseEvent event) {
        // close the tray menu window if it's open
        if (useSystemTray && trayMenuWindow != null) {
            trayMenuWindow.dispose();
        }

        String title = Main.getInstance().getSettings().shimejiEeNameOverride;
        if (title.isEmpty()) {
            title = Main.getInstance().getLanguageBundle().getString("ShimejiEE");
        }

        // create the tray menu window
        if (useSystemTray) {
            trayMenuWindow = new JDialog(Main.getFrame(), title, false);
            ((JDialog) trayMenuWindow).setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        } else {
            // Use a JFrame when not using the system tray,
            // because the JFrame has a minimize button and shows up in the taskbar
            trayMenuWindow = new JFrame(title);
            ((JFrame) trayMenuWindow).setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            trayMenuWindowListener = new WindowListener() {
                @Override
                public void windowOpened(WindowEvent e) {

                }

                @Override
                public void windowClosing(WindowEvent e) {

                }

                @Override
                public void windowClosed(WindowEvent e) {
                    Main.getInstance().exit();
                }

                @Override
                public void windowIconified(WindowEvent e) {

                }

                @Override
                public void windowDeiconified(WindowEvent e) {

                }

                @Override
                public void windowActivated(WindowEvent e) {

                }

                @Override
                public void windowDeactivated(WindowEvent e) {

                }
            };
            trayMenuWindow.addWindowListener(trayMenuWindowListener);
        }
        trayMenuWindow.setIconImage(Main.getIcon());
        trayMenuWindow.toFront();
        trayMenuWindow.setAlwaysOnTop(useSystemTray);

        trayMenuPanel = new TrayMenuPanel(useSystemTray);
        trayMenuWindow.add(trayMenuPanel);

        // set the window dimensions
        trayMenuWindow.pack();
        trayMenuWindow.setMinimumSize(trayMenuWindow.getSize());

        if (event != null) {
            // get the DPI of the screen, and divide 96 by it to get a ratio
            double dpiScaleInverse = 96.0 / Toolkit.getDefaultToolkit().getScreenResolution();

            // setting location of the window
            trayMenuWindow.setLocation((int) Math.round(event.getX() * dpiScaleInverse) - trayMenuWindow.getWidth(), (int) Math.round(event.getY() * dpiScaleInverse) - trayMenuWindow.getHeight());

            // make sure that it is on the screen if people are using exotic taskbar locations
            Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            if (trayMenuWindow.getX() < screen.getX()) {
                trayMenuWindow.setLocation((int) Math.round(event.getX() * dpiScaleInverse), trayMenuWindow.getY());
            }
            if (trayMenuWindow.getY() < screen.getY()) {
                trayMenuWindow.setLocation(trayMenuWindow.getX(), (int) Math.round(event.getY() * dpiScaleInverse));
            }
        } else {
            // Center the window
            trayMenuWindow.setLocationRelativeTo(null);
        }

        trayMenuWindow.setVisible(true);
    }

    void refreshPauseText() {
        if (trayMenuPanel != null) {
            trayMenuPanel.refreshPauseText();
        }
    }
}
