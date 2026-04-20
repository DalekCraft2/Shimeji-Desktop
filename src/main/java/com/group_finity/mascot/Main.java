package com.group_finity.mascot;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.image.Filter;
import com.group_finity.mascot.image.ImagePairs;
import com.group_finity.mascot.image.ImageUtils;
import com.group_finity.mascot.imagesetchooser.ImageSetChooser;
import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.sound.Sounds;
import com.jthemedetecor.OsThemeDetector;
import hqx.RgbYuv;
import org.apache.commons.exec.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

/**
 * Program entry point.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static final Path CONFIG_DIRECTORY = Path.of("conf");
    public static final Path IMAGE_DIRECTORY = Path.of("img");
    public static final Path SOUND_DIRECTORY = Path.of("sound");
    public static final Path SETTINGS_FILE = CONFIG_DIRECTORY.resolve("settings.properties");
    public static final Path LOGGING_FILE = CONFIG_DIRECTORY.resolve("logging.properties");
    public static final Path ICON_FILE = IMAGE_DIRECTORY.resolve("icon.png");

    /**
     * Action that matches the "Gather Around Mouse!" context menu command
     */
    static final String BEHAVIOR_GATHER = "ChaseMouse";

    static {
        try (InputStream input = Files.newInputStream(LOGGING_FILE)) {
            LogManager.getLogManager().readConfiguration(input);
        } catch (final IOException | SecurityException e) {
            log.error("Failed to load log properties", e);
        }
    }

    private static final Main INSTANCE = new Main();
    private final Manager manager = new Manager();
    private List<String> imageSets = new ArrayList<>();
    private final Map<String, Configuration> configurations = new ConcurrentHashMap<>();
    private final Map<String, List<String>> childImageSets = new ConcurrentHashMap<>();

    /**
     * A collection of configurations that failed to load.
     * This is used to avoid attempting to load these configurations more than once.
     */
    private final Collection<String> failedConfigurations = new ArrayList<>();

    private final Settings settings = new Settings();
    private ResourceBundle languageBundle;

    /**
     * The icon for the program.
     * Should be accessed through {@link #getIcon}, which initializes this field if it is {@code null}.
     */
    private static BufferedImage icon;

    private static JFrame frame;
    private TrayIcon trayIcon;
    private Window trayMenuWindow;
    private TrayMenuPanel trayMenuPanel;
    private WindowListener trayMenuWindowListener;

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static Main getInstance() {
        return INSTANCE;
    }

    static JFrame getFrame() {
        return frame;
    }

    static ExecutorService getExecutorService() {
        return executorService;
    }

    public static void showError(String message) {
        // TODO: Call this on the EDT safely without letting other windows appear in front of it before it's closed
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showError(String message, Throwable exception) {
        StringBuilder messageBuilder = new StringBuilder(message);
        do {
            if (exception.getClass().getPackageName().equals("com.group_finity.mascot.exception")) {
                messageBuilder.append("\n").append(exception.getMessage());
            } else if (exception instanceof SAXParseException) {
                messageBuilder.append("\nLine ").append(((SAXParseException) exception).getLineNumber()).append(": ").append(exception.getMessage());
            } else {
                messageBuilder.append("\n").append(exception);
            }
            exception = exception.getCause();
        }
        while (exception != null);
        message = messageBuilder.toString();
        showError(message + "\n" + INSTANCE.languageBundle.getString("SeeLogForDetails"));
    }

    public static void main(final String[] args) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            // Load theme before any Swing components are created
            updateLookAndFeel();
            // Create frame before anything else happens, in case showError() gets called
            frame = new JFrame();
        });
        OsThemeDetector.getDetector().registerListener(ignored -> SwingUtilities.invokeLater(Main::updateLookAndFeel));

        try {
            INSTANCE.run();
        } catch (OutOfMemoryError err) {
            log.error("Out of memory. There are probably too many "
                    + "Shimeji mascots in the image folder for your computer to handle. "
                    + "Select fewer image sets or move some to the "
                    + "img/unused folder and try again.", err);
            showError("Out of memory. There are probably too many\n"
                    + "Shimeji mascots for your computer to handle.\n"
                    + "Select fewer image sets or move some to the\n"
                    + "img/unused folder and try again.");
            System.exit(0);
        }
    }

    public void run() {
        // Load settings
        settings.load();

        // Load language
        loadLanguage(settings.language);

        // Get the image sets to use
        if (!settings.alwaysShowShimejiChooser) {
            for (String set : settings.activeImageSets)
                if (!set.trim().isEmpty()) {
                    imageSets.add(set.trim());
                }
        }

        // Load mascot configurations
        configurationLoadLoop();

        // Create the tray icon
        SwingUtilities.invokeLater(this::createTrayIcon);

        // Initialize the environment
        if (settings.windowedMode) {
            try {
                /*
                 * If in windowed mode, initialize the environment on the EDT before loading any mascots
                 * so the mascots spawn at the correct positions
                 */
                SwingUtilities.invokeAndWait(() -> NativeFactory.getInstance().getEnvironment().init());
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else
            NativeFactory.getInstance().getEnvironment().init();

        // Create mascots
        for (String imageSet : imageSets) {
            if (configurations.get(imageSet).containsInformationKey("SplashImage") &&
                    (settings.alwaysShowInformationScreen || !settings.informationDismissed.contains(imageSet))) {
                SwingUtilities.invokeLater(() -> {
                    InformationWindow info = new InformationWindow();
                    info.init(imageSet, configurations.get(imageSet));
                    info.display();
                });
                setMascotInformationDismissed(imageSet);
                settings.saveInformationDismissed();
            }
            createMascot(imageSet);
        }

        manager.start();
    }

    /**
     * Shows the image set chooser if there are no image sets selected, and then loads the selected image sets.
     * If none of the selected image sets' configurations successfully load, the process repeats.
     */
    private void configurationLoadLoop() {
        boolean isUsingHqx = settings.filter == Filter.HQX && (settings.scaling % 2 == 0 || settings.scaling % 3 == 0);
        if (isUsingHqx) {
            RgbYuv.hqxInit();
        }

        do {
            if (imageSets.isEmpty()) {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        imageSets = new ImageSetChooser(frame, true).display();
                        if (imageSets == null) {
                            exit();
                        }
                    });
                } catch (InterruptedException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }

            // Load mascot configurations
            for (int index = 0; index < imageSets.size(); index++) {
                String imageSet = imageSets.get(index);
                if (!loadConfiguration(imageSet)) {
                    // failed to load
                    imageSets.remove(imageSet);
                    index--;
                }
            }
            // Clear any items that were added to this collection during the loading sequence
            failedConfigurations.clear();
        }
        while (imageSets.isEmpty());

        if (isUsingHqx) {
            RgbYuv.hqxDeinit();
        }
    }

    /**
     * Loads the configuration files for the given image set.
     *
     * @param imageSet the image set to load
     */
    private boolean loadConfiguration(final String imageSet) {
        if (configurations.containsKey(imageSet)) {
            return true;
        } else if (failedConfigurations.contains(imageSet)) {
            return false;
        }
        try {
            // try to load in the correct XML files
            Path actionsFile = getActionsFile(imageSet);

            log.info("Reading action file \"{}\" for image set \"{}\"", actionsFile, imageSet);

            final Document actions;
            try (InputStream input = Files.newInputStream(actionsFile)) {
                actions = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
            }

            Configuration configuration = new Configuration();

            configuration.load(new Entry(actions.getDocumentElement()), imageSet);

            // Save the schema for the actions file so we can use it later
            ResourceBundle actionsSchema = configuration.getSchema();

            Path behaviorsFile = getBehaviorsFile(imageSet);

            log.info("Reading behavior file \"{}\" for image set \"{}\"", behaviorsFile, imageSet);

            final Document behaviors;
            try (InputStream input = Files.newInputStream(behaviorsFile)) {
                behaviors = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
            }

            configuration.load(new Entry(behaviors.getDocumentElement()), imageSet);

            Path infoFile = getInfoFile(imageSet);

            if (Files.isRegularFile(infoFile)) {
                log.info("Reading information file \"{}\" for image set \"{}\"", infoFile, imageSet);

                final Document information;
                try (InputStream input = Files.newInputStream(infoFile)) {
                    information = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
                }

                configuration.load(new Entry(information.getDocumentElement()), imageSet);
            }

            configuration.validate();

            configurations.put(imageSet, configuration);

            List<String> childMascots = new ArrayList<>();

            // born mascot bit goes here...
            for (final Entry list : new Entry(actions.getDocumentElement()).selectChildren(actionsSchema.getString("ActionList"))) {
                for (final Entry node : list.selectChildren(actionsSchema.getString("Action"))) {
                    if (node.hasAttribute(actionsSchema.getString("BornMascot"))) {
                        String set = node.getAttribute(actionsSchema.getString("BornMascot"));
                        if (!childMascots.contains(set)) {
                            childMascots.add(set);
                        }
                        if (!configurations.containsKey(set)) {
                            loadConfiguration(set);
                        }
                    }
                    if (node.hasAttribute(actionsSchema.getString("TransformMascot"))) {
                        String set = node.getAttribute(actionsSchema.getString("TransformMascot"));
                        if (!childMascots.contains(set)) {
                            childMascots.add(set);
                        }
                        if (!configurations.containsKey(set)) {
                            loadConfiguration(set);
                        }
                    }
                }
            }

            childImageSets.put(imageSet, childMascots);

            return true;
        } catch (IOException | ParserConfigurationException | SAXException | ConfigurationException |
                 RuntimeException e) {
            log.error("Failed to load configuration for image set \"{}\"", imageSet, e);
            showError(String.format(languageBundle.getString("FailedLoadConfigErrorMessage"), imageSet), e);
            configurations.remove(imageSet);
            childImageSets.remove(imageSet);
            ImagePairs.removeAll(imageSet);
            Sounds.removeAll(imageSet);
            failedConfigurations.add(imageSet);
        }

        return false;
    }

    public static Path getActionsFile(String imageSet) {
        Path filePath = IMAGE_DIRECTORY.resolve(imageSet).resolve(CONFIG_DIRECTORY);
        if (Files.isRegularFile(filePath.resolve("actions.xml"))) {
            return filePath.resolve("actions.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u52D5\u4F5C.xml"))) {
            return filePath.resolve("\u52D5\u4F5C.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml"))) {
            return filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00A6-\u00BA@.xml"))) {
            return filePath.resolve("\u00A6-\u00BA@.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml"))) {
            return filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml");
        } else if (Files.isRegularFile(filePath.resolve("one.xml"))) {
            return filePath.resolve("one.xml");
        } else if (Files.isRegularFile(filePath.resolve("1.xml"))) {
            return filePath.resolve("1.xml");
        }

        filePath = CONFIG_DIRECTORY.resolve(imageSet);
        if (Files.isRegularFile(filePath.resolve("actions.xml"))) {
            return filePath.resolve("actions.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u52D5\u4F5C.xml"))) {
            return filePath.resolve("\u52D5\u4F5C.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml"))) {
            return filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00A6-\u00BA@.xml"))) {
            return filePath.resolve("\u00A6-\u00BA@.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml"))) {
            return filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml");
        } else if (Files.isRegularFile(filePath.resolve("one.xml"))) {
            return filePath.resolve("one.xml");
        } else if (Files.isRegularFile(filePath.resolve("1.xml"))) {
            return filePath.resolve("1.xml");
        }

        filePath = CONFIG_DIRECTORY;
        if (Files.isRegularFile(filePath.resolve("\u52D5\u4F5C.xml"))) {
            return filePath.resolve("\u52D5\u4F5C.xml");
        }

        return filePath.resolve("actions.xml");
    }

    public static Path getBehaviorsFile(String imageSet) {
        Path filePath = IMAGE_DIRECTORY.resolve(imageSet).resolve(CONFIG_DIRECTORY);
        if (Files.isRegularFile(filePath.resolve("behaviors.xml"))) {
            return filePath.resolve("behaviors.xml");
        } else if (Files.isRegularFile(filePath.resolve("behavior.xml"))) {
            return filePath.resolve("behavior.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u884C\u52D5.xml"))) {
            return filePath.resolve("\u884C\u52D5.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml"))) {
            return filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00AA\u00B5\u00A6-.xml"))) {
            return filePath.resolve("\u00AA\u00B5\u00A6-.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00ECs\u00F4\u00AB.xml"))) {
            return filePath.resolve("\u00ECs\u00F4\u00AB.xml");
        } else if (Files.isRegularFile(filePath.resolve("two.xml"))) {
            return filePath.resolve("two.xml");
        } else if (Files.isRegularFile(filePath.resolve("2.xml"))) {
            return filePath.resolve("2.xml");
        }

        filePath = CONFIG_DIRECTORY.resolve(imageSet);
        if (Files.isRegularFile(filePath.resolve("behaviors.xml"))) {
            return filePath.resolve("behaviors.xml");
        } else if (Files.isRegularFile(filePath.resolve("behavior.xml"))) {
            return filePath.resolve("behavior.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u884C\u52D5.xml"))) {
            return filePath.resolve("\u884C\u52D5.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml"))) {
            return filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00AA\u00B5\u00A6-.xml"))) {
            return filePath.resolve("\u00AA\u00B5\u00A6-.xml");
        } else if (Files.isRegularFile(filePath.resolve("\u00ECs\u00F4\u00AB.xml"))) {
            return filePath.resolve("\u00ECs\u00F4\u00AB.xml");
        } else if (Files.isRegularFile(filePath.resolve("two.xml"))) {
            return filePath.resolve("two.xml");
        } else if (Files.isRegularFile(filePath.resolve("2.xml"))) {
            return filePath.resolve("2.xml");
        }

        filePath = CONFIG_DIRECTORY;
        if (Files.isRegularFile(filePath.resolve("\u884C\u52D5.xml"))) {
            return filePath.resolve("\u884C\u52D5.xml");
        }

        return filePath.resolve("behaviors.xml");
    }

    public static Path getInfoFile(String imageSet) {
        Path filePath = IMAGE_DIRECTORY.resolve(imageSet).resolve(CONFIG_DIRECTORY);
        if (Files.isRegularFile(filePath.resolve("info.xml"))) {
            return filePath.resolve("info.xml");
        }

        filePath = CONFIG_DIRECTORY.resolve(imageSet);
        if (Files.isRegularFile(filePath.resolve("info.xml"))) {
            return filePath.resolve("info.xml");
        }

        filePath = CONFIG_DIRECTORY;
        return filePath.resolve("info.xml");
    }

    /**
     * Creates a tray icon.
     */
    void createTrayIcon() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
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
                settings.saveUserSettings();
            } else
                log.info("Creating persistent menu window");
            createTrayMenu(false, null);
            return;
        }

        log.info("Creating tray icon");

        // get the tray icon image
        BufferedImage image = getIcon();

        try {
            // Create the tray icon
            String tooltip = settings.shimejiEeNameOverride;
            if (tooltip.isEmpty()) {
                tooltip = languageBundle.getString("ShimejiEE");
            }
            trayIcon = new TrayIcon(image, tooltip);
            trayIcon.setImageAutoSize(true);

            // attach menu
            trayIcon.addMouseListener(new MouseListener() {
                boolean debouncing = false;
                final Timer debounceTimer = new Timer(1000, event -> debouncing = false);

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
                        createMascot();
                    } else if (SwingUtilities.isMiddleMouseButton(e) && e.getClickCount() == 2) {
                        // When the icon is double-middle-clicked, dispose of all mascots, but do not close the program
                        /* BUG: On Windows 11, Java seems to think the middle mouse button is the left mouse button, so this code never gets executed.
                        This is a JDK bug: https://bugs.openjdk.org/browse/JDK-8341173 */
                        if (manager.isExitOnLastRemoved()) {
                            manager.setExitOnLastRemoved(false);
                            manager.disposeAll();
                        } else {
                            // If the mascots are already gone, recreate one mascot for each active image set
                            for (String imageSet : imageSets) {
                                createMascot(imageSet);
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
                    createTrayMenu(true, event);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }
            });

            // Show tray icon
            SystemTray.getSystemTray().add(trayIcon);
        } catch (final AWTException e) {
            log.error("Failed to create tray icon", e);
            showError(languageBundle.getString("FailedDisplaySystemTrayErrorMessage"), e);
            exit();
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
    private void createTrayMenu(boolean useSystemTray, MouseEvent event) {
        // close the tray menu window if it's open
        if (useSystemTray && trayMenuWindow != null) {
            trayMenuWindow.dispose();
        }

        String title = settings.shimejiEeNameOverride;
        if (title.isEmpty()) {
            title = languageBundle.getString("ShimejiEE");
        }

        // create the tray menu window
        if (useSystemTray) {
            trayMenuWindow = new JDialog(frame, title, false);
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
                    exit();
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
        trayMenuWindow.setIconImage(getIcon());
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

    void refreshTrayMenuPauseText() {
        if (trayMenuPanel != null) {
            trayMenuPanel.refreshPauseText();
        }
    }

    /**
     * Creates a random {@link Mascot}.
     */
    public void createMascot() {
        int length = imageSets.size();
        int random = (int) (length * Math.random());
        createMascot(imageSets.get(random));
    }

    /**
     * Creates a {@link Mascot} with the specified image set.
     *
     * @param imageSet the image set to use
     */
    public void createMascot(String imageSet) {
        log.info("Creating mascot with image set \"{}\"", imageSet);

        // Create one mascot
        final Mascot mascot = new Mascot(imageSet);

        // Create it outside the bounds of the screen
        mascot.setAnchor(new Point(-4000, -4000));

        // Randomize the initial orientation
        mascot.setLookRight(Math.random() < 0.5);

        try {
            mascot.setBehavior(getConfiguration(imageSet).buildNextBehavior(null, mascot));
            manager.add(mascot);
        } catch (final BehaviorInstantiationException | CantBeAliveException e) {
            // Not sure why this says "first action" instead of "first behavior", but changing it would require changing all of the translations, so...
            log.error("Failed to initialize the first action for mascot \"{}\"", mascot, e);
            showError(String.format(languageBundle.getString("FailedInitialiseFirstActionErrorMessage"), mascot), e);
            mascot.dispose();
        } catch (RuntimeException e) {
            log.error("Could not create mascot \"{}\"", mascot, e);
            showError(String.format(languageBundle.getString("CouldNotCreateShimejiErrorMessage"), imageSet), e);
            mascot.dispose();
        }
    }

    void loadLanguage(Locale locale) {
        try {
            URL[] urls = {CONFIG_DIRECTORY.toUri().toURL()};
            try (URLClassLoader loader = new URLClassLoader(urls)) {
                languageBundle = ResourceBundle.getBundle("language", locale, loader);
            }
        } catch (IOException e) {
            log.error("Failed to load language file for locale {}", locale.toLanguageTag(), e);
            showError("The language file for locale " + locale.toLanguageTag() + " could not be loaded. Ensure that you have the latest Shimeji language.properties in your conf directory.");
            exit();
        }
    }

    private void setMascotInformationDismissed(final String imageSet) {
        if (!settings.informationDismissed.contains(imageSet)) {
            settings.informationDismissed.add(imageSet);
        }
    }

    public void setMascotBehaviorEnabled(final String name, final Mascot mascot, boolean enabled) {
        List<String> list = new ArrayList<>();
        if (settings.disabledBehaviors.containsKey(mascot.getImageSet()))
            list = settings.disabledBehaviors.get(mascot.getImageSet());

        if (list.contains(name) && enabled) {
            list.remove(name);
        } else if (!list.contains(name) && !enabled) {
            list.add(name);
        }

        if (list.isEmpty()) {
            settings.disabledBehaviors.remove(mascot.getImageSet());
        } else {
            settings.disabledBehaviors.put(mascot.getImageSet(), list);
        }

        settings.savePopupSettings();
    }

    void reloadAllImageSets() {
        boolean isExit = manager.isExitOnLastRemoved();
        manager.setExitOnLastRemoved(false);
        manager.disposeAll();

        // Wipe all loaded data
        ImagePairs.clear();
        Sounds.clear();
        configurations.clear();

        // Load mascot configurations
        configurationLoadLoop();

        // Create mascots
        for (String imageSet : imageSets) {
            createMascot(imageSet);
        }

        manager.setExitOnLastRemoved(isExit);
    }

    /**
     * Replaces the current set of active image sets without modifying
     * valid image sets that are already active. Does nothing if {@code newImageSets == null}.
     *
     * @param newImageSets all the image sets that should now be active
     * @author LavenderSnek
     * @author Kilkakon (did some tweaks)
     */
    void setActiveImageSets(Collection<String> newImageSets) {
        if (newImageSets == null) {
            return;
        }

        // I don't think there would be enough image sets chosen at any given
        // time for it to be worth using HashSet, but I might be wrong
        Collection<String> toRemove = new ArrayList<>(imageSets);
        toRemove.removeAll(newImageSets);

        Collection<String> toAdd = new ArrayList<>();
        Collection<String> toRetain = new ArrayList<>();
        for (String set : newImageSets) {
            if (!imageSets.contains(set)) {
                toAdd.add(set);
            }
            if (!toRetain.contains(set)) {
                toRetain.add(set);
            }
            populateCollectionWithChildSets(set, toRetain);
        }

        boolean isExit = manager.isExitOnLastRemoved();
        manager.setExitOnLastRemoved(false);

        for (String r : toRemove)
            removeLoadedImageSet(r, toRetain);

        boolean isUsingHqx = settings.filter == Filter.HQX && (settings.scaling % 2 == 0 || settings.scaling % 3 == 0);
        if (isUsingHqx) {
            RgbYuv.hqxInit();
        }
        for (String a : toAdd)
            addImageSet(a);
        if (isUsingHqx) {
            RgbYuv.hqxDeinit();
        }

        // Clear any items that were added to this collection during the loading sequence
        failedConfigurations.clear();

        if (imageSets.isEmpty()) {
            // All configurations failed to load, so prompt the user to select image sets again
            configurationLoadLoop();
        }

        manager.setExitOnLastRemoved(isExit);
    }

    /**
     * Recursively populates the given collection with all child image sets of the given image set.
     *
     * @param imageSet the image set whose children should be added to the collection
     * @param childList the collection to populate
     */
    private void populateCollectionWithChildSets(String imageSet, Collection<String> childList) {
        if (childImageSets.containsKey(imageSet)) {
            for (String set : childImageSets.get(imageSet)) {
                if (!childList.contains(set)) {
                    childList.add(set);
                    populateCollectionWithChildSets(set, childList);
                }
            }
        }
    }

    /**
     * Unloads the given image set and disposes of any mascots of that image set, unless it is a child image set of
     * an image set that has been selected in the image set chooser.
     * If the given image set has any children image sets that have not been selected in the image set chooser,
     * those image sets will also be unloaded and their mascots will be disposed.
     *
     * @param imageSet the image set to remove
     * @param setsToIgnore a collection of image sets that should not be removed
     */
    private void removeLoadedImageSet(String imageSet, Collection<String> setsToIgnore) {
        if (!setsToIgnore.contains(imageSet)) {
            setsToIgnore.add(imageSet);
            imageSets.remove(imageSet);
            manager.remainNone(imageSet);
            configurations.remove(imageSet);
            ImagePairs.removeAll(imageSet);
            Sounds.removeAll(imageSet);

            if (childImageSets.containsKey(imageSet)) {
                for (String set : childImageSets.get(imageSet)) {
                    removeLoadedImageSet(set, setsToIgnore);
                }
            }

            childImageSets.remove(imageSet);
        }
    }

    /**
     * Loads the given image set's configuration if it is not yet loaded, adds it to the list of loaded image sets,
     * and creates a mascot of the image set.
     * If the given image set's configuration is not yet loaded and its information has not been seen,
     * its information window will be shown after the configuration has loaded.
     *
     * @param imageSet the image set to add
     */
    private void addImageSet(String imageSet) {
        if (configurations.containsKey(imageSet)) {
            imageSets.add(imageSet);
            createMascot(imageSet);
        } else if (!failedConfigurations.contains(imageSet)) {
            if (loadConfiguration(imageSet)) {
                imageSets.add(imageSet);
                if (configurations.get(imageSet).containsInformationKey("SplashImage") &&
                        (settings.alwaysShowInformationScreen || !settings.informationDismissed.contains(imageSet))) {
                    InformationWindow info = new InformationWindow();
                    info.init(imageSet, configurations.get(imageSet));
                    info.display();
                    setMascotInformationDismissed(imageSet);
                    settings.saveInformationDismissed();
                }
                createMascot(imageSet);
            }
        }
    }

    public Configuration getConfiguration(String imageSet) {
        return configurations.get(imageSet);
    }

    public Settings getSettings() {
        return settings;
    }

    public ResourceBundle getLanguageBundle() {
        return languageBundle;
    }

    Manager getManager() {
        return manager;
    }

    public void exit() {
        try {
            executorService.shutdownNow();

            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                log.warn("The executor service did not terminate in the allotted time.");
            }
        } catch (final SecurityException | InterruptedException e) {
            log.error("Failed to shutdown the executor service.", e);
        }
        manager.disposeAll();
        manager.stop();
        System.exit(0);
    }

    /** Updates the {@link LookAndFeel} of the application based on the current OS and whether it's using dark/light mode. */
    private static void updateLookAndFeel() {
        final boolean isDark = OsThemeDetector.isSupported() && OsThemeDetector.getDetector().isDark();

        if (OS.isFamilyMac()) {
            if (isDark) {
                FlatMacDarkLaf.setup();
            } else {
                FlatMacLightLaf.setup();
            }


            FlatLaf.updateUI();
            return;
        }

        if (isDark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        FlatLaf.updateUI();
    }

    /**
     * Loads the icon file and returns it as a {@link BufferedImage}.
     * If a custom icon has been placed at the path {@code img/icon.png}, then it will be loaded. Otherwise, the default
     * icon will be loaded.
     *
     * @return The loaded {@link BufferedImage} icon, or a blank image if loading fails.
     */
    public static BufferedImage getIcon() {
        if (icon != null) {
            return icon;
        }

        if (Files.isRegularFile(ICON_FILE)) {
            try (InputStream input = Files.newInputStream(ICON_FILE)) {
                icon = ImageUtils.toCompatibleImage(ImageIO.read(input));
                return icon;
            } catch (final IOException e) {
                log.warn("Failed to load custom icon file", e);
            }
        }

        try (InputStream input = Objects.requireNonNull(Main.class.getResourceAsStream("/icon.png"))) {
            icon = ImageUtils.toCompatibleImage(ImageIO.read(input));
            return icon;
        } catch (final IOException e) {
            log.warn("Failed to load default icon file", e);
        }

        icon = ImageUtils.createCompatibleImage(16, 16);
        return icon;
    }
}
