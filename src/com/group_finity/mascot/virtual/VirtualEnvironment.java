package com.group_finity.mascot.virtual;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.Environment;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Virtual desktop environment by Kilkakon
 * <p>
 * <a href="https://kilkakon.com">kilkakon.com</a>
 *
 * @author Kilkakon
 */
class VirtualEnvironment extends Environment {
    private final JFrame display = new JFrame();

    private final Area activeIE = new Area();

    @Override
    public Area getWorkArea() {
        return getScreen();
    }

    @Override
    public Area getActiveIE() {
        return activeIE;
    }

    @Override
    public String getActiveIETitle() {
        return null;
    }

    @Override
    public void moveActiveIE(final Point point) {
    }

    @Override
    public void restoreIE() {
    }

    @Override
    public void refreshCache() {
        // I feel so refreshed

        // good for you buddy
    }

    @Override
    public void init() {
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
        display.setTitle(Main.getInstance().getLanguageBundle().getString("ShimejiEE"));
        String[] windowArray = Main.getInstance().getProperties().getProperty("WindowSize", "600x500").split("x");
        display.getContentPane().setPreferredSize(new Dimension(Integer.parseInt(windowArray[0]), Integer.parseInt(windowArray[1])));
        display.getContentPane().setLayout(null);
        display.getContentPane().setBackground(Color.decode(Main.getInstance().getProperties().getProperty("Background", "#00FF00")));
        display.setBackground(Color.decode(Main.getInstance().getProperties().getProperty("Background", "#00FF00")));
        display.setAutoRequestFocus(false);

        BufferedImage image = null;
        try {
            image = ImageIO.read(new File("./img/icon.png"));
        } catch (IOException e) {
            // not bothering reporting errors with loading the tray icon as it would have already been reported to the user by now
        } finally {
            if (image == null) {
                image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
            }
        }
        display.setIconImage(image);

        SwingUtilities.invokeLater(() -> {
            display.pack();
            display.setVisible(true);
            display.toFront();
        });

        activeIE.set(new Rectangle(-500, -500, 0, 0));

        tick();
    }

    @Override
    public void tick() {
        screenRect.setBounds(display.getContentPane().getBounds());
        screen.set(screenRect);

        PointerInfo info = MouseInfo.getPointerInfo();
        Point point = new Point(0, 0);
        if (info != null) {
            point = info.getLocation();
            SwingUtilities.convertPointFromScreen(point, display.getContentPane());
        }
        cursor.set(point);
    }

    @Override
    public void dispose() {
        display.dispose();
    }

    public void addShimeji(final VirtualTranslucentPanel shimeji) {
        SwingUtilities.invokeLater(() -> {
            if (display.getContentPane().getSize().width > 0 && display.getContentPane().getSize().height > 0) {
                display.setPreferredSize(display.getSize());
                display.getRootPane().setPreferredSize(display.getRootPane().getSize());
                display.getContentPane().setPreferredSize(display.getContentPane().getSize());
            }
            shimeji.setOpaque(false);
            display.getContentPane().add(shimeji);
        });
    }
}
