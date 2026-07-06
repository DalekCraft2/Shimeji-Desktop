package com.group_finity.mascot.platform.virtual;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.environment.AbstractEnvironment;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.ComplexArea;
import com.group_finity.mascot.image.ImageUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Virtual desktop environment.
 *
 * @author Kilkakon
 * @since 1.0.20
 */
class VirtualEnvironment extends AbstractEnvironment {
    private JFrame display;

    private final Area activeWindow = new Area();

    private final List<Area> screenList = List.of(getScreen());

    private final ComplexArea complexScreen = new ComplexArea() {
        @Override
        public void set(Map<String, Rectangle> rectangles) {
        }

        @Override
        public void set(String name, Rectangle value) {
        }

        @Override
        public void retain(Collection<String> areaNames) {
        }

        @Override
        public Collection<Area> getAreas() {
            return screenList;
        }
    };

    private boolean isInitializing = false;

    private boolean initialized = false;

    @Override
    public void init() {
        if (isInitializing || initialized) {
            return;
        }

        isInitializing = true;

        autoUpdateScreenRect = false;

        Runnable runnable = () -> {
            display = new JFrame();

            display.addWindowListener(new WindowListener() {
                @Override
                public void windowOpened(WindowEvent e) {
                }

                @Override
                public void windowClosing(WindowEvent e) {
                    Main.getInstance().exit();
                }

                @Override
                public void windowClosed(WindowEvent e) {
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
            });
            display.setAutoRequestFocus(false);
            String title = Main.getInstance().getSettings().shimejiEeNameOverride;
            if (title.isEmpty()) {
                title = Main.getInstance().getLanguageBundle().getString("ShimejiEE");
            }
            display.setTitle(title);

            BufferedImage image = null;
            Path backgroundImage = Main.getInstance().getSettings().backgroundImage;
            if (backgroundImage != null) {
                try (InputStream input = Files.newInputStream(backgroundImage)) {
                    image = ImageUtils.toCompatibleImage(ImageIO.read(input));
                } catch (IOException ignored) {
                }
            }
            display.setContentPane(new VirtualContentPanel(new Dimension(Main.getInstance().getSettings().windowSize),
                    Main.getInstance().getSettings().backgroundColor, image, Main.getInstance().getSettings().backgroundMode));
            display.setBackground(display.getContentPane().getBackground());

            display.setIconImage(Main.getIcon());

            display.pack();
            display.setLocationRelativeTo(null);
            display.setVisible(true);
            display.toFront();

            initialized = true;
            isInitializing = false;

            tick();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }

        activeWindow.setVisible(false);
        activeWindow.setRect(-500, -500, 0, 0);
    }

    @Override
    public void tick() {
        if (!initialized) {
            return;
        }

        /*
         * TODO: Figure out a thread-safe way to access data that's on the Event Dispatch Thread, without risking
         *  changing it whilst a Mascot is using it.
         */
        // SwingUtilities.invokeLater(() -> {
        Container contentPane = display.getContentPane();
        getScreen().setRect(0, 0, contentPane.getWidth(), contentPane.getHeight());

        // Use MouseInfo.getPointerInfo() instead of display.getMousePosition() because the latter only returns a
        // non-null value if the cursor is over the component
        PointerInfo info = MouseInfo.getPointerInfo();
        if (info != null) {
            Point point = info.getLocation();
            SwingUtilities.convertPointFromScreen(point, contentPane);
            getCursor().set(point);
        } else {
            getCursor().set(0, 0);
        }
        // });
    }

    @Override
    public Area getWorkAreaAt(int x, int y) {
        return getScreen();
    }

    @Override
    public ComplexArea getComplexWorkArea() {
        return complexScreen;
    }

    @Override
    public ComplexArea getComplexScreen() {
        return complexScreen;
    }

    @Override
    public Area getActiveWindow() {
        return activeWindow;
    }

    @Override
    public String getActiveWindowTitle() {
        return null;
    }

    @Override
    public long getActiveWindowId() {
        return 0;
    }

    @Override
    public void moveActiveWindow(final int x, final int y) {
    }

    @Override
    public void restoreWindows() {
    }

    @Override
    public void refreshCache() {
        // I feel so refreshed

        // good for you buddy
    }

    @Override
    public void dispose() {
        super.dispose();
        if (SwingUtilities.isEventDispatchThread()) {
            display.dispose();
        } else {
            SwingUtilities.invokeLater(display::dispose);
        }
    }

    void addMascot(final VirtualTranslucentPanel mascotPanel) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addMascot(mascotPanel));
            return;
        }
        Container contentPane = display.getContentPane();
        if (contentPane.getWidth() > 0 && contentPane.getHeight() > 0) {
            display.setPreferredSize(display.getSize());
            display.getRootPane().setPreferredSize(display.getRootPane().getSize());
            contentPane.setPreferredSize(contentPane.getSize());
        }
        display.add(mascotPanel);
    }
}
