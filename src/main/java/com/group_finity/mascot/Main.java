package com.group_finity.mascot;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.image.ImagePairs;
import com.group_finity.mascot.imagesetchooser.ImageSetChooser;
import com.group_finity.mascot.sound.Sounds;
import com.nilo.plaf.nimrod.NimRODLookAndFeel;
import com.nilo.plaf.nimrod.NimRODTheme;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Program entry point.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static final Path CONFIG_DIRECTORY = Paths.get("conf");
    public static final Path IMAGE_DIRECTORY = Paths.get("img");
    public static final Path SOUND_DIRECTORY = Paths.get("sound");
    public static final Path SETTINGS_FILE = CONFIG_DIRECTORY.resolve("settings.properties");
    public static final Path LOGGING_FILE = CONFIG_DIRECTORY.resolve("logging.properties");
    public static final Path THEME_FILE = CONFIG_DIRECTORY.resolve("theme.properties");
    public static final Path ICON_FILE = IMAGE_DIRECTORY.resolve("icon.png");

    /**
     * Action that matches the "Gather Around Mouse!" context menu command
     */
    static final String BEHAVIOR_GATHER = "ChaseMouse";

    static {
        try (InputStream input = Files.newInputStream(LOGGING_FILE)) {
            LogManager.getLogManager().readConfiguration(input);
        } catch (final SecurityException | IOException e) {
            log.log(Level.SEVERE, "Failed to load log properties", e);
        } catch (OutOfMemoryError err) {
            log.log(Level.SEVERE, "Out of memory. There are probably too many "
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

    private final Manager manager = new Manager();
    private ArrayList<String> imageSets = new ArrayList<>();
    private ConcurrentHashMap<String, Configuration> configurations = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ArrayList<String>> childImageSets = new ConcurrentHashMap<>();
    private static Main instance = new Main();
    private Properties properties = new Properties();
    private ResourceBundle languageBundle;

    private JDialog form;

    public static Main getInstance() {
        return instance;
    }

    private static JFrame frame = new JFrame();

    public static void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(final String[] args) {
        try {
            getInstance().run();
        } catch (OutOfMemoryError err) {
            log.log(Level.SEVERE, "Out of memory. There are probably too many "
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
        // load properties
        properties = new Properties();
        if (Files.isRegularFile(SETTINGS_FILE)) {
            try (InputStream input = Files.newInputStream(SETTINGS_FILE)) {
                properties.load(input);
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to load settings", e);
            }
        }

        // load languages
        Locale locale = Locale.forLanguageTag(properties.getProperty("Language", Locale.UK.toLanguageTag()));
        try {
            URL[] urls = {CONFIG_DIRECTORY.toUri().toURL()};
            try (URLClassLoader loader = new URLClassLoader(urls)) {
                // ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl(false);
                // languageBundle = ResourceBundle.getBundle("language", locale, loader, utf8Control);
                languageBundle = ResourceBundle.getBundle("language", locale, loader);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to load language file for locale " + locale.toLanguageTag(), e);
            showError("The language file for locale " + locale.toLanguageTag() + " could not be loaded. Ensure that you have the latest Shimeji language.properties in your conf directory.");
            exit();
        }

        // load theme
        try {
            // default light theme
            NimRODLookAndFeel lookAndFeel = new NimRODLookAndFeel();

            // check for theme properties
            NimRODTheme theme = null;
            try {
                if (Files.isRegularFile(THEME_FILE)) {
                    theme = new NimRODTheme(THEME_FILE.toString());
                }
            } catch (RuntimeException exc) {
                log.log(Level.SEVERE, "Failed to load theme properties", exc);
            }

            if (theme == null) {
                // default back to light theme if not found/valid
                theme = new NimRODTheme();
                theme.setPrimary1(Color.decode("#1EA6EB"));
                theme.setPrimary2(Color.decode("#28B0F5"));
                theme.setPrimary3(Color.decode("#32BAFF"));
                theme.setSecondary1(Color.decode("#BCBCBE"));
                theme.setSecondary2(Color.decode("#C6C6C8"));
                theme.setSecondary3(Color.decode("#D0D0D2"));
                theme.setWhite(Color.WHITE);
                theme.setBlack(Color.BLACK);
                theme.setMenuOpacity(255);
                theme.setFrameOpacity(255);
            }

            NimRODLookAndFeel.setCurrentTheme(theme);
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            // all done
            lookAndFeel.initialize();
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (HeadlessException | NumberFormatException | UnsupportedLookAndFeelException ex) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException |
                     UnsupportedLookAndFeelException ex1) {
                log.log(Level.SEVERE, "Failed to set Look & Feel", ex1);
                exit();
            }
        }

        // Get the image sets to use
        if (!Boolean.parseBoolean(properties.getProperty("AlwaysShowShimejiChooser", "false"))) {
            for (String set : properties.getProperty("ActiveShimeji", "").split("/"))
                if (!set.trim().isEmpty()) {
                    imageSets.add(set.trim());
                }
        }
        if (imageSets.isEmpty()) {
            imageSets = new ImageSetChooser(frame, true).display();
            if (imageSets == null) {
                exit();
            }
        }

        // Load mascot configurations
        for (int index = 0; index < imageSets.size(); index++) {
            if (!loadConfiguration(imageSets.get(index))) {
                // failed validation
                configurations.remove(imageSets.get(index));
                imageSets.remove(imageSets.get(index));
                index--;
            }
        }
        if (imageSets.isEmpty()) {
            exit();
        }

        // Create the tray icon
        createTrayIcon();

        // Create mascots
        for (String imageSet : imageSets) {
            String informationAlreadySeen = properties.getProperty("InformationDismissed", "");
            if (configurations.get(imageSet).containsInformationKey("SplashImage") &&
                    (Boolean.parseBoolean(properties.getProperty("AlwaysShowInformationScreen", "false")) ||
                            !informationAlreadySeen.contains(imageSet))) {
                InformationWindow info = new InformationWindow();
                info.init(imageSet, configurations.get(imageSet));
                info.display();
                setMascotInformationDismissed(imageSet);
                updateConfigFile();
            }
            createMascot(imageSet);
        }

        getManager().start();
    }

    /**
     * Loads the configuration files for the given image set.
     *
     * @param imageSet the image set to load
     */
    private boolean loadConfiguration(final String imageSet) {
        try {
            // try to load in the correct xml files
            Path filePath = CONFIG_DIRECTORY;
            Path actionsFile = filePath.resolve("actions.xml");
            if (Files.exists(filePath.resolve("\u52D5\u4F5C.xml"))) {
                actionsFile = filePath.resolve("\u52D5\u4F5C.xml");
            }

            filePath = CONFIG_DIRECTORY.resolve(imageSet);
            if (Files.exists(filePath.resolve("actions.xml"))) {
                actionsFile = filePath.resolve("actions.xml");
            } else if (Files.exists(filePath.resolve("\u52D5\u4F5C.xml"))) {
                actionsFile = filePath.resolve("\u52D5\u4F5C.xml");
            } else if (Files.exists(filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml"))) {
                actionsFile = filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml");
            } else if (Files.exists(filePath.resolve("\u00A6-\u00BA@.xml"))) {
                actionsFile = filePath.resolve("\u00A6-\u00BA@.xml");
            } else if (Files.exists(filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml"))) {
                actionsFile = filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml");
            } else if (Files.exists(filePath.resolve("one.xml"))) {
                actionsFile = filePath.resolve("one.xml");
            } else if (Files.exists(filePath.resolve("1.xml"))) {
                actionsFile = filePath.resolve("1.xml");
            }

            filePath = IMAGE_DIRECTORY.resolve(imageSet).resolve(CONFIG_DIRECTORY);
            if (Files.exists(filePath.resolve("actions.xml"))) {
                actionsFile = filePath.resolve("actions.xml");
            } else if (Files.exists(filePath.resolve("\u52D5\u4F5C.xml"))) {
                actionsFile = filePath.resolve("\u52D5\u4F5C.xml");
            } else if (Files.exists(filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml"))) {
                actionsFile = filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml");
            } else if (Files.exists(filePath.resolve("\u00A6-\u00BA@.xml"))) {
                actionsFile = filePath.resolve("\u00A6-\u00BA@.xml");
            } else if (Files.exists(filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml"))) {
                actionsFile = filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml");
            } else if (Files.exists(filePath.resolve("one.xml"))) {
                actionsFile = filePath.resolve("one.xml");
            } else if (Files.exists(filePath.resolve("1.xml"))) {
                actionsFile = filePath.resolve("1.xml");
            }

            log.log(Level.INFO, "Reading action file \"{0}\" for image set \"{1}\"", new Object[]{actionsFile, imageSet});

            final Document actions = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    Files.newInputStream(actionsFile));

            Configuration configuration = new Configuration();

            configuration.load(new Entry(actions.getDocumentElement()), imageSet);

            filePath = CONFIG_DIRECTORY;
            Path behaviorsFile = filePath.resolve("behaviors.xml");
            if (Files.exists(filePath.resolve("\u884C\u52D5.xml"))) {
                behaviorsFile = filePath.resolve("\u884C\u52D5.xml");
            }

            filePath = CONFIG_DIRECTORY.resolve(imageSet);
            if (Files.exists(filePath.resolve("behaviors.xml"))) {
                behaviorsFile = filePath.resolve("behaviors.xml");
            } else if (Files.exists(filePath.resolve("behavior.xml"))) {
                behaviorsFile = filePath.resolve("behavior.xml");
            } else if (Files.exists(filePath.resolve("\u884C\u52D5.xml"))) {
                behaviorsFile = filePath.resolve("\u884C\u52D5.xml");
            } else if (Files.exists(filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml"))) {
                behaviorsFile = filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml");
            } else if (Files.exists(filePath.resolve("\u00AA\u00B5\u00A6-.xml"))) {
                behaviorsFile = filePath.resolve("\u00AA\u00B5\u00A6-.xml");
            } else if (Files.exists(filePath.resolve("\u00ECs\u00F4\u00AB.xml"))) {
                behaviorsFile = filePath.resolve("\u00ECs\u00F4\u00AB.xml");
            } else if (Files.exists(filePath.resolve("two.xml"))) {
                behaviorsFile = filePath.resolve("two.xml");
            } else if (Files.exists(filePath.resolve("2.xml"))) {
                behaviorsFile = filePath.resolve("2.xml");
            }

            filePath = IMAGE_DIRECTORY.resolve(imageSet).resolve(CONFIG_DIRECTORY);
            if (Files.exists(filePath.resolve("behaviors.xml"))) {
                behaviorsFile = filePath.resolve("behaviors.xml");
            } else if (Files.exists(filePath.resolve("behavior.xml"))) {
                behaviorsFile = filePath.resolve("behavior.xml");
            } else if (Files.exists(filePath.resolve("\u884C\u52D5.xml"))) {
                behaviorsFile = filePath.resolve("\u884C\u52D5.xml");
            } else if (Files.exists(filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml"))) {
                behaviorsFile = filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml");
            } else if (Files.exists(filePath.resolve("\u00AA\u00B5\u00A6-.xml"))) {
                behaviorsFile = filePath.resolve("\u00AA\u00B5\u00A6-.xml");
            } else if (Files.exists(filePath.resolve("\u00ECs\u00F4\u00AB.xml"))) {
                behaviorsFile = filePath.resolve("\u00ECs\u00F4\u00AB.xml");
            } else if (Files.exists(filePath.resolve("two.xml"))) {
                behaviorsFile = filePath.resolve("two.xml");
            } else if (Files.exists(filePath.resolve("2.xml"))) {
                behaviorsFile = filePath.resolve("2.xml");
            }

            log.log(Level.INFO, "Reading behavior file \"{0}\" for image set \"{1}\"", new Object[]{behaviorsFile, imageSet});

            final Document behaviors = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    Files.newInputStream(behaviorsFile));

            configuration.load(new Entry(behaviors.getDocumentElement()), imageSet);

            filePath = CONFIG_DIRECTORY;
            Path infoFile = filePath.resolve("info.xml");

            filePath = CONFIG_DIRECTORY.resolve(imageSet);
            if (Files.exists(filePath.resolve("info.xml"))) {
                infoFile = filePath.resolve("info.xml");
            }

            filePath = IMAGE_DIRECTORY.resolve(imageSet).resolve(CONFIG_DIRECTORY);
            if (Files.exists(filePath.resolve("info.xml"))) {
                infoFile = filePath.resolve("info.xml");
            }

            if (Files.exists(infoFile)) {
                log.log(Level.INFO, "Reading information file \"{0}\" for image set \"{1}\"", new Object[]{infoFile, imageSet});

                final Document information = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Files.newInputStream(infoFile));

                configuration.load(new Entry(information.getDocumentElement()), imageSet);
            }

            configuration.validate();

            configurations.put(imageSet, configuration);

            ArrayList<String> childMascots = new ArrayList<>();

            // born mascot bit goes here...
            // TODO Make these use the proper language's schema tag names instead of hardcoding them as the English ones
            for (final Entry list : new Entry(actions.getDocumentElement()).selectChildren("ActionList")) {
                for (final Entry node : list.selectChildren("Action")) {
                    if (node.getAttributes().containsKey("BornMascot")) {
                        String set = node.getAttribute("BornMascot");
                        if (!childMascots.contains(set)) {
                            childMascots.add(set);
                        }
                        if (!configurations.containsKey(set)) {
                            loadConfiguration(set);
                        }
                    }
                    if (node.getAttributes().containsKey("TransformMascot")) {
                        String set = node.getAttribute("TransformMascot");
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
        } catch (ConfigurationException | IOException | ParserConfigurationException | SAXException e) {
            log.log(Level.SEVERE, "Failed to load configuration files", e);
            showError(languageBundle.getString("FailedLoadConfigErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getString("SeeLogForDetails"));
        }

        return false;
    }

    /**
     * Creates a tray icon.
     */
    private void createTrayIcon() {
        if (!SystemTray.isSupported()) {
            // TODO Make an alternative way to access the tray icon's menu in case the system tray is not supported
            log.log(Level.INFO, "System tray not supported");
            return;
        }

        log.log(Level.INFO, "Creating tray icon");

        // get the tray icon image
        BufferedImage image = null;
        try {
            image = ImageIO.read(ICON_FILE.toFile());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to create tray icon", e);
            showError(languageBundle.getString("FailedDisplaySystemTrayErrorMessage") + "\n" + languageBundle.getString("SeeLogForDetails"));
        } finally {
            if (image == null) {
                image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
            }
        }

        try {
            // Create the tray icon
            final TrayIcon icon = new TrayIcon(image, languageBundle.getString("ShimejiEE"));

            // attach menu
            icon.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
                        // Create a mascot when the icon is left-clicked
                        createMascot();
                    } else if (SwingUtilities.isMiddleMouseButton(e) && e.getClickCount() == 2) {
                        // When the icon is double-middle-clicked, dispose of all mascots, but do not close the program
                        // FIXME Java seems to think the middle mouse button is the left mouse button, so this code never gets executed
                        if (getManager().isExitOnLastRemoved()) {
                            getManager().setExitOnLastRemoved(false);
                            getManager().disposeAll();
                        } else {
                            // If the mascots are already gone, recreate one mascot for each active image set
                            for (String imageSet : imageSets) {
                                createMascot(imageSet);
                                getManager().setExitOnLastRemoved(true);
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
                    // close the form if it's open
                    if (form != null) {
                        form.dispose();
                    }

                    // create the form and border
                    form = new JDialog(frame, false);
                    final JPanel panel = new JPanel();
                    panel.setBorder(new EmptyBorder(5, 5, 5, 5));
                    form.add(panel);

                    // buttons and action handling
                    JButton btnCallShimeji = new JButton(languageBundle.getString("CallShimeji"));
                    btnCallShimeji.addActionListener(event17 -> {
                        createMascot();
                        form.dispose();
                    });

                    JButton btnFollowCursor = new JButton(languageBundle.getString("FollowCursor"));
                    btnFollowCursor.addActionListener(event16 -> {
                        getManager().setBehaviorAll(BEHAVIOR_GATHER);
                        form.dispose();
                    });

                    JButton btnReduceToOne = new JButton(languageBundle.getString("ReduceToOne"));
                    btnReduceToOne.addActionListener(event15 -> {
                        getManager().remainOne();
                        form.dispose();
                    });

                    JButton btnRestoreWindows = new JButton(languageBundle.getString("RestoreWindows"));
                    btnRestoreWindows.addActionListener(event14 -> {
                        NativeFactory.getInstance().getEnvironment().restoreIE();
                        form.dispose();
                    });

                    final JButton btnAllowedBehaviours = new JButton(languageBundle.getString("AllowedBehaviours"));
                    btnAllowedBehaviours.addMouseListener(new MouseListener() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                        }

                        @Override
                        public void mousePressed(MouseEvent e) {
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            btnAllowedBehaviours.setEnabled(true);
                        }

                        @Override
                        public void mouseEntered(MouseEvent e) {
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                        }
                    });
                    btnAllowedBehaviours.addActionListener(event13 -> {
                        // "Disable Breeding" menu item
                        final JCheckBoxMenuItem breedingMenu = new JCheckBoxMenuItem(languageBundle.getString("BreedingCloning"), Boolean.parseBoolean(properties.getProperty("Breeding", "true")));
                        breedingMenu.addItemListener(e -> {
                            breedingMenu.setState(toggleBooleanSetting("Breeding", true));
                            updateConfigFile();
                            btnAllowedBehaviours.setEnabled(true);
                        });

                        // "Disable Breeding Transient" menu item
                        final JCheckBoxMenuItem transientMenu = new JCheckBoxMenuItem(languageBundle.getString("BreedingTransient"), Boolean.parseBoolean(properties.getProperty("Transients", "true")));
                        transientMenu.addItemListener(e -> {
                            transientMenu.setState(toggleBooleanSetting("Transients", true));
                            updateConfigFile();
                            btnAllowedBehaviours.setEnabled(true);
                        });

                        // "Disable Transformations" menu item
                        final JCheckBoxMenuItem transformationMenu = new JCheckBoxMenuItem(languageBundle.getString("Transformation"), Boolean.parseBoolean(properties.getProperty("Transformation", "true")));
                        transformationMenu.addItemListener(e -> {
                            transformationMenu.setState(toggleBooleanSetting("Transformation", true));
                            updateConfigFile();
                            btnAllowedBehaviours.setEnabled(true);
                        });

                        // "Throwing Windows" menu item
                        final JCheckBoxMenuItem throwingMenu = new JCheckBoxMenuItem(languageBundle.getString("ThrowingWindows"), Boolean.parseBoolean(properties.getProperty("Throwing", "true")));
                        throwingMenu.addItemListener(e -> {
                            throwingMenu.setState(toggleBooleanSetting("Throwing", true));
                            updateConfigFile();
                            btnAllowedBehaviours.setEnabled(true);
                        });

                        // "Mute Sounds" menu item
                        final JCheckBoxMenuItem soundsMenu = new JCheckBoxMenuItem(languageBundle.getString("SoundEffects"), Boolean.parseBoolean(properties.getProperty("Sounds", "true")));
                        soundsMenu.addItemListener(e -> {
                            boolean result = toggleBooleanSetting("Sounds", true);
                            soundsMenu.setState(result);
                            Sounds.setMuted(!result);
                            updateConfigFile();
                            btnAllowedBehaviours.setEnabled(true);
                        });

                        // "Multiscreen" menu item
                        final JCheckBoxMenuItem multiscreenMenu = new JCheckBoxMenuItem(languageBundle.getString("Multiscreen"), Boolean.parseBoolean(properties.getProperty("Multiscreen", "true")));
                        multiscreenMenu.addItemListener(e -> {
                            multiscreenMenu.setState(toggleBooleanSetting("Multiscreen", true));
                            updateConfigFile();
                            btnAllowedBehaviours.setEnabled(true);
                        });

                        JPopupMenu behaviourPopup = new JPopupMenu();
                        behaviourPopup.add(breedingMenu);
                        behaviourPopup.add(transientMenu);
                        behaviourPopup.add(transformationMenu);
                        behaviourPopup.add(throwingMenu);
                        behaviourPopup.add(soundsMenu);
                        behaviourPopup.add(multiscreenMenu);
                        behaviourPopup.addPopupMenuListener(new PopupMenuListener() {
                            @Override
                            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                            }

                            @Override
                            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                                if (panel.getMousePosition() != null) {
                                    btnAllowedBehaviours.setEnabled(!(panel.getMousePosition().x > btnAllowedBehaviours.getX() &&
                                            panel.getMousePosition().x < btnAllowedBehaviours.getX() + btnAllowedBehaviours.getWidth() &&
                                            panel.getMousePosition().y > btnAllowedBehaviours.getY() &&
                                            panel.getMousePosition().y < btnAllowedBehaviours.getY() + btnAllowedBehaviours.getHeight()));
                                } else {
                                    btnAllowedBehaviours.setEnabled(true);
                                }
                            }

                            @Override
                            public void popupMenuCanceled(PopupMenuEvent e) {
                            }
                        });
                        behaviourPopup.show(btnAllowedBehaviours, 0, btnAllowedBehaviours.getHeight());
                        btnAllowedBehaviours.requestFocusInWindow();
                    });

                    final JButton btnChooseShimeji = new JButton(languageBundle.getString("ChooseShimeji"));
                    btnChooseShimeji.addActionListener(event12 -> {
                        form.dispose();
                        ImageSetChooser chooser = new ImageSetChooser(frame, true);
                        chooser.setIconImage(icon.getImage());
                        setActiveImageSets(chooser.display());
                    });

                    final JButton btnSettings = new JButton(languageBundle.getString("Settings"));
                    btnSettings.addActionListener(event1 -> {
                        form.dispose();
                        SettingsWindow dialog = new SettingsWindow(frame, true);
                        dialog.setIconImage(icon.getImage());
                        dialog.init();
                        dialog.display();

                        if (dialog.getEnvironmentReloadRequired()) {
                            NativeFactory.getInstance().getEnvironment().dispose();
                            NativeFactory.resetInstance();
                        }
                        if (dialog.getEnvironmentReloadRequired() || dialog.getImageReloadRequired()) {
                            // need to reload the shimeji as the images have rescaled
                            boolean isExit = getManager().isExitOnLastRemoved();
                            getManager().setExitOnLastRemoved(false);
                            getManager().disposeAll();

                            // Wipe all loaded data
                            ImagePairs.clear();
                            configurations.clear();

                            // Load settings
                            for (String imageSet : imageSets) {
                                loadConfiguration(imageSet);
                            }

                            // Create the first mascot
                            for (String imageSet : imageSets) {
                                createMascot(imageSet);
                            }

                            getManager().setExitOnLastRemoved(isExit);
                        }
                        if (dialog.getInteractiveWindowReloadRequired()) {
                            NativeFactory.getInstance().getEnvironment().refreshCache();
                        }
                    });

                    final JButton btnLanguage = new JButton(languageBundle.getString("Language"));
                    btnLanguage.addMouseListener(new MouseListener() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                        }

                        @Override
                        public void mousePressed(MouseEvent e) {
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            btnLanguage.setEnabled(true);
                        }

                        @Override
                        public void mouseEntered(MouseEvent e) {
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                        }
                    });
                    btnLanguage.addActionListener(e -> {
                        // English menu item
                        final JMenuItem englishMenu = new JMenuItem("English");
                        englishMenu.addActionListener(e121 -> {
                            form.dispose();
                            updateLanguage(Locale.UK);
                            updateConfigFile();
                        });

                        // Arabic menu item
                        final JMenuItem arabicMenu = new JMenuItem("\u0639\u0631\u0628\u064A");
                        arabicMenu.addActionListener(e120 -> {
                            form.dispose();
                            updateLanguage("ar-SA");
                            updateConfigFile();
                        });

                        // Catalan menu item
                        final JMenuItem catalanMenu = new JMenuItem("Catal\u00E0");
                        catalanMenu.addActionListener(e119 -> {
                            form.dispose();
                            updateLanguage("ca-ES");
                            updateConfigFile();
                        });

                        // German menu item
                        final JMenuItem germanMenu = new JMenuItem("Deutsch");
                        germanMenu.addActionListener(e118 -> {
                            form.dispose();
                            updateLanguage(Locale.GERMANY);
                            updateConfigFile();
                        });

                        // Spanish menu item
                        final JMenuItem spanishMenu = new JMenuItem("Espa\u00F1ol");
                        spanishMenu.addActionListener(e117 -> {
                            form.dispose();
                            updateLanguage("es-ES");
                            updateConfigFile();
                        });

                        // French menu item
                        final JMenuItem frenchMenu = new JMenuItem("Fran\u00E7ais");
                        frenchMenu.addActionListener(e116 -> {
                            form.dispose();
                            updateLanguage(Locale.FRANCE);
                            updateConfigFile();
                        });

                        // Croatian menu item
                        final JMenuItem croatianMenu = new JMenuItem("Hrvatski");
                        croatianMenu.addActionListener(e115 -> {
                            form.dispose();
                            updateLanguage("hr-HR");
                            updateConfigFile();
                        });

                        // Italian menu item
                        final JMenuItem italianMenu = new JMenuItem("Italiano");
                        italianMenu.addActionListener(e114 -> {
                            form.dispose();
                            updateLanguage(Locale.ITALY);
                            updateConfigFile();
                        });

                        // Dutch menu item
                        final JMenuItem dutchMenu = new JMenuItem("Nederlands");
                        dutchMenu.addActionListener(e113 -> {
                            form.dispose();
                            updateLanguage("nl-NL");
                            updateConfigFile();
                        });

                        // Polish menu item
                        final JMenuItem polishMenu = new JMenuItem("Polski");
                        polishMenu.addActionListener(e112 -> {
                            form.dispose();
                            updateLanguage("pl-PL");
                            updateConfigFile();
                        });

                        // Brazilian Portuguese menu item
                        final JMenuItem brazilianPortugueseMenu = new JMenuItem("Portugu\u00eas Brasileiro");
                        brazilianPortugueseMenu.addActionListener(e111 -> {
                            form.dispose();
                            updateLanguage("pt-BR");
                            updateConfigFile();
                        });

                        // Portuguese menu item
                        final JMenuItem portugueseMenu = new JMenuItem("Portugu\u00eas");
                        portugueseMenu.addActionListener(e110 -> {
                            form.dispose();
                            updateLanguage("pt-PT");
                            updateConfigFile();
                        });

                        // Russian menu item
                        final JMenuItem russianMenu = new JMenuItem("\u0440\u0443\u0301\u0441\u0441\u043a\u0438\u0439 \u044f\u0437\u044b\u0301\u043a");
                        russianMenu.addActionListener(e19 -> {
                            form.dispose();
                            updateLanguage("ru-RU");
                            updateConfigFile();
                        });

                        // Romanian menu item
                        final JMenuItem romanianMenu = new JMenuItem("Rom\u00e2n\u0103");
                        romanianMenu.addActionListener(e18 -> {
                            form.dispose();
                            updateLanguage("ro-RO");
                            updateConfigFile();
                        });

                        // Serbian menu item
                        final JMenuItem serbianMenu = new JMenuItem("Srpski");
                        serbianMenu.addActionListener(e17 -> {
                            form.dispose();
                            updateLanguage("sr-RS");
                            updateConfigFile();
                        });

                        // Finnish menu item
                        final JMenuItem finnishMenu = new JMenuItem("Suomi");
                        finnishMenu.addActionListener(e16 -> {
                            form.dispose();
                            updateLanguage("fi-FI");
                            updateConfigFile();
                        });

                        // Vietnamese menu item
                        final JMenuItem vietnameseMenu = new JMenuItem("ti\u1ebfng Vi\u1ec7t");
                        vietnameseMenu.addActionListener(e15 -> {
                            form.dispose();
                            updateLanguage("vi-VN");
                            updateConfigFile();
                        });

                        // Chinese menu item
                        final JMenuItem chineseMenu = new JMenuItem("\u7b80\u4f53\u4e2d\u6587");
                        chineseMenu.addActionListener(e14 -> {
                            form.dispose();
                            updateLanguage(Locale.SIMPLIFIED_CHINESE);
                            updateConfigFile();
                        });

                        // Chinese (Traditional) menu item
                        final JMenuItem chineseTraditionalMenu = new JMenuItem("\u7E41\u9AD4\u4E2D\u6587");
                        chineseTraditionalMenu.addActionListener(e13 -> {
                            form.dispose();
                            updateLanguage(Locale.TRADITIONAL_CHINESE);
                            updateConfigFile();
                        });

                        // Korean menu item
                        final JMenuItem koreanMenu = new JMenuItem("\ud55c\uad6d\uc5b4");
                        koreanMenu.addActionListener(e12 -> {
                            form.dispose();
                            updateLanguage(Locale.KOREA);
                            updateConfigFile();
                        });

                        // Japanese menu item
                        final JMenuItem japaneseMenu = new JMenuItem("\u65E5\u672C\u8A9E");
                        japaneseMenu.addActionListener(e1 -> {
                            form.dispose();
                            updateLanguage(Locale.JAPAN);
                            updateConfigFile();
                        });

                        JPopupMenu languagePopup = new JPopupMenu();
                        languagePopup.add(englishMenu);
                        languagePopup.addSeparator();
                        languagePopup.add(arabicMenu);
                        languagePopup.add(catalanMenu);
                        languagePopup.add(germanMenu);
                        languagePopup.add(spanishMenu);
                        languagePopup.add(frenchMenu);
                        languagePopup.add(croatianMenu);
                        languagePopup.add(italianMenu);
                        languagePopup.add(dutchMenu);
                        languagePopup.add(polishMenu);
                        languagePopup.add(portugueseMenu);
                        languagePopup.add(brazilianPortugueseMenu);
                        languagePopup.add(russianMenu);
                        languagePopup.add(romanianMenu);
                        languagePopup.add(serbianMenu);
                        languagePopup.add(finnishMenu);
                        languagePopup.add(vietnameseMenu);
                        languagePopup.add(chineseMenu);
                        languagePopup.add(chineseTraditionalMenu);
                        languagePopup.add(koreanMenu);
                        languagePopup.add(japaneseMenu);
                        languagePopup.addPopupMenuListener(new PopupMenuListener() {
                            @Override
                            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                            }

                            @Override
                            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                                if (panel.getMousePosition() != null) {
                                    btnLanguage.setEnabled(!(panel.getMousePosition().x > btnLanguage.getX() &&
                                            panel.getMousePosition().x < btnLanguage.getX() + btnLanguage.getWidth() &&
                                            panel.getMousePosition().y > btnLanguage.getY() &&
                                            panel.getMousePosition().y < btnLanguage.getY() + btnLanguage.getHeight()));
                                } else {
                                    btnLanguage.setEnabled(true);
                                }
                            }

                            @Override
                            public void popupMenuCanceled(PopupMenuEvent e) {
                            }
                        });
                        languagePopup.show(btnLanguage, 0, btnLanguage.getHeight());
                        btnLanguage.requestFocusInWindow();
                    });

                    JButton btnPauseAll = new JButton(getManager().isPaused() ? languageBundle.getString("ResumeAnimations") : languageBundle.getString("PauseAnimations"));
                    btnPauseAll.addActionListener(e -> {
                        form.dispose();
                        getManager().togglePauseAll();
                    });

                    JButton btnDismissAll = new JButton(languageBundle.getString("DismissAll"));
                    btnDismissAll.addActionListener(e -> exit());

                    // layout
                    panel.setLayout(new GridBagLayout());
                    GridBagConstraints gridBag = new GridBagConstraints();
                    gridBag.fill = GridBagConstraints.HORIZONTAL;
                    gridBag.gridx = 0;
                    gridBag.gridy = 0;
                    panel.add(btnCallShimeji, gridBag);
                    gridBag.insets = new Insets(5, 0, 0, 0);
                    gridBag.gridy++;
                    panel.add(btnFollowCursor, gridBag);
                    gridBag.gridy++;
                    panel.add(btnReduceToOne, gridBag);
                    gridBag.gridy++;
                    panel.add(btnRestoreWindows, gridBag);
                    gridBag.gridy++;
                    panel.add(new JSeparator(), gridBag);
                    gridBag.gridy++;
                    panel.add(btnAllowedBehaviours, gridBag);
                    gridBag.gridy++;
                    panel.add(btnChooseShimeji, gridBag);
                    gridBag.gridy++;
                    panel.add(btnSettings, gridBag);
                    gridBag.gridy++;
                    panel.add(btnLanguage, gridBag);
                    gridBag.gridy++;
                    panel.add(new JSeparator(), gridBag);
                    gridBag.gridy++;
                    panel.add(btnPauseAll, gridBag);
                    gridBag.gridy++;
                    panel.add(btnDismissAll, gridBag);

                    form.setIconImage(icon.getImage());
                    form.setTitle(languageBundle.getString("ShimejiEE"));
                    form.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    form.setAlwaysOnTop(true);

                    // set the form dimensions
                    FontMetrics metrics = btnCallShimeji.getFontMetrics(btnCallShimeji.getFont());
                    int width = metrics.stringWidth(btnCallShimeji.getText());
                    width = Math.max(metrics.stringWidth(btnFollowCursor.getText()), width);
                    width = Math.max(metrics.stringWidth(btnReduceToOne.getText()), width);
                    width = Math.max(metrics.stringWidth(btnRestoreWindows.getText()), width);
                    width = Math.max(metrics.stringWidth(btnAllowedBehaviours.getText()), width);
                    width = Math.max(metrics.stringWidth(btnChooseShimeji.getText()), width);
                    width = Math.max(metrics.stringWidth(btnSettings.getText()), width);
                    width = Math.max(metrics.stringWidth(btnLanguage.getText()), width);
                    width = Math.max(metrics.stringWidth(btnPauseAll.getText()), width);
                    width = Math.max(metrics.stringWidth(btnDismissAll.getText()), width);
                    panel.setPreferredSize(new Dimension(width + 64,
                            24 + // 12 padding on top and bottom
                                    75 + // 13 insets of 5 height normally
                                    10 * metrics.getHeight() + // 10 button faces
                                    84));
                    form.pack();
                    form.setMinimumSize(form.getSize());

                    // get the DPI of the screen, and divide 96 by it to get a ratio
                    double dpiScaleInverse = 96.0 / Toolkit.getDefaultToolkit().getScreenResolution();

                    // setting location of the form
                    form.setLocation((int) Math.round(event.getX() * dpiScaleInverse) - form.getWidth(), (int) Math.round(event.getY() * dpiScaleInverse) - form.getHeight());

                    // make sure that it is on the screen if people are using exotic taskbar locations
                    Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                    if (form.getX() < screen.getX()) {
                        form.setLocation((int) Math.round(event.getX() * dpiScaleInverse), form.getY());
                    }
                    if (form.getY() < screen.getY()) {
                        form.setLocation(form.getX(), (int) Math.round(event.getY() * dpiScaleInverse));
                    }

                    form.setVisible(true);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }
            });

            // Show tray icon
            SystemTray.getSystemTray().add(icon);
        } catch (final AWTException e) {
            log.log(Level.SEVERE, "Failed to create tray icon", e);
            showError(languageBundle.getString("FailedDisplaySystemTrayErrorMessage") + "\n" + languageBundle.getString("SeeLogForDetails"));
            exit();
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
        log.log(Level.INFO, "Creating mascot with image set \"{0}\"", imageSet);

        // Create one mascot
        final Mascot mascot = new Mascot(imageSet);

        // Create it outside the bounds of the screen
        mascot.setAnchor(new Point(-4000, -4000));

        // Randomize the initial orientation
        mascot.setLookRight(Math.random() < 0.5);

        try {
            mascot.setBehavior(getConfiguration(imageSet).buildNextBehavior(null, mascot));
            getManager().add(mascot);
        } catch (final BehaviorInstantiationException e) {
            // Not sure why this says "first action" instead of "first behavior", but changing it would require changing all of the translations, so...
            log.log(Level.SEVERE, "Failed to initialize the first action for mascot \"" + mascot + "\"", e);
            showError(languageBundle.getString("FailedInitialiseFirstActionErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getString("SeeLogForDetails"));
            mascot.dispose();
        } catch (final CantBeAliveException e) {
            log.log(Level.SEVERE, "Could not create mascot \"" + mascot + "\"", e);
            showError(languageBundle.getString("FailedInitialiseFirstActionErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getString("SeeLogForDetails"));
            mascot.dispose();
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Could not create mascot \"" + mascot + "\"", e);
            showError(languageBundle.getString("CouldNotCreateShimejiErrorMessage") + " " + imageSet + ".\n" + e.getMessage() + "\n" + languageBundle.getString("SeeLogForDetails"));
            mascot.dispose();
        }
    }

    private void refreshLanguage(Locale locale) {
        try {
            URL[] urls = {CONFIG_DIRECTORY.toUri().toURL()};
            try (URLClassLoader loader = new URLClassLoader(urls)) {
                // ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl(false);
                // languageBundle = ResourceBundle.getBundle("language", locale, loader, utf8Control);
                languageBundle = ResourceBundle.getBundle("language", locale, loader);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to load language file for locale " + locale.toLanguageTag(), e);
            showError("The language file for locale " + locale.toLanguageTag() + " could not be loaded. Ensure that you have the latest Shimeji language.properties in your conf directory.");
            exit();
        }

        boolean isExit = getManager().isExitOnLastRemoved();
        getManager().setExitOnLastRemoved(false);
        getManager().disposeAll();

        // Load mascot configurations
        for (String imageSet : imageSets) {
            loadConfiguration(imageSet);
        }

        // Create mascots
        for (String imageSet : imageSets) {
            createMascot(imageSet);
        }

        getManager().setExitOnLastRemoved(isExit);
    }

    private void updateLanguage(Locale locale) {
        if (!properties.getProperty("Language", Locale.UK.toLanguageTag()).equals(locale.toLanguageTag())) {
            properties.setProperty("Language", locale.toLanguageTag());
            refreshLanguage(locale);
        }
    }

    private void updateLanguage(String languageTag) {
        if (!properties.getProperty("Language", Locale.UK.toLanguageTag()).equals(languageTag)) {
            properties.setProperty("Language", languageTag);
            refreshLanguage(Locale.forLanguageTag(languageTag));
        }
    }

    private boolean toggleBooleanSetting(String propertyName, boolean defaultValue) {
        if (Boolean.parseBoolean(properties.getProperty(propertyName, String.valueOf(defaultValue)))) {
            properties.setProperty(propertyName, "false");
            return false;
        } else {
            properties.setProperty(propertyName, "true");
            return true;
        }
    }

    private void setMascotInformationDismissed(final String imageSet) {
        ArrayList<String> list = new ArrayList<>();
        String[] data = properties.getProperty("InformationDismissed", "").split("/");

        if (data.length > 0 && !data[0].isEmpty()) {
            list.addAll(Arrays.asList(data));
        }
        if (!list.contains(imageSet)) {
            list.add(imageSet);
        }

        properties.setProperty("InformationDismissed", list.toString().replace("[", "").replace("]", "").replace(", ", "/"));
    }

    public void setMascotBehaviorEnabled(final String name, final Mascot mascot, boolean enabled) {
        ArrayList<String> list = new ArrayList<>();
        String[] data = properties.getProperty("DisabledBehaviours." + mascot.getImageSet(), "").split("/");

        if (data.length > 0 && !data[0].isEmpty()) {
            list.addAll(Arrays.asList(data));
        }

        if (list.contains(name) && enabled) {
            list.remove(name);
        } else if (!list.contains(name) && !enabled) {
            list.add(name);
        }

        if (list.isEmpty()) {
            properties.remove("DisabledBehaviours." + mascot.getImageSet());
        } else {
            properties.setProperty("DisabledBehaviours." + mascot.getImageSet(), list.toString().replace("[", "").replace("]", "").replace(", ", "/"));
        }

        updateConfigFile();
    }

    private void updateConfigFile() {
        try (OutputStream output = Files.newOutputStream(SETTINGS_FILE)) {
            properties.store(output, "Shimeji-ee Configuration Options");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to save settings", e);
        }
    }

    /**
     * Replaces the current set of active image sets without modifying
     * valid image sets that are already active. Does nothing if {@code newImageSets == null}.
     *
     * @param newImageSets all the image sets that should now be active
     * @author snek
     * @author Kilkakon (did some tweaks)
     */
    private void setActiveImageSets(Collection<String> newImageSets) {
        if (newImageSets == null) {
            return;
        }

        // I don't think there would be enough image sets chosen at any given
        // time for it to be worth using HashSet, but I might be wrong
        Collection<String> toRemove = new ArrayList<>(imageSets);
        toRemove.removeAll(newImageSets);

        Collection<String> toAdd = new ArrayList<>();
        ArrayList<String> toRetain = new ArrayList<>();
        for (String set : newImageSets) {
            if (!imageSets.contains(set)) {
                toAdd.add(set);
            }
            if (!toRetain.contains(set)) {
                toRetain.add(set);
            }
            populateArrayListWithChildSets(set, toRetain);
        }

        boolean isExit = getManager().isExitOnLastRemoved();
        getManager().setExitOnLastRemoved(false);

        for (String r : toRemove)
            removeLoadedImageSet(r, toRetain);

        for (String a : toAdd)
            addImageSet(a);

        getManager().setExitOnLastRemoved(isExit);
    }

    private void populateArrayListWithChildSets(String imageSet, ArrayList<String> childList) {
        if (childImageSets.containsKey(imageSet)) {
            for (String set : childImageSets.get(imageSet)) {
                if (!childList.contains(set)) {
                    populateArrayListWithChildSets(set, childList);
                    childList.add(set);
                }
            }
        }
    }

    private void removeLoadedImageSet(String imageSet, ArrayList<String> setsToIgnore) {
        /*
         * If a mascot "Mascot1" has the ability to transform into another mascot type "Mascot2", Mascot2 is stored as a child image set of Mascot1.
         * If Mascot2 is unchecked in the Shimeji chooser, the existing Mascot2 mascots will only be removed if no Mascot1 instances exist, because Mascot2 is a child of Mascot1.
         * It's confusing, but it prevents errors.
         */
        // TODO Change this to remove the mascots of the provided image set ALWAYS, but hold off on unloading the actual image set until all mascots which have it as a child image set have also been unloaded.
        if (childImageSets.containsKey(imageSet)) {
            for (String set : childImageSets.get(imageSet)) {
                if (!setsToIgnore.contains(set)) {
                    setsToIgnore.add(set);
                    imageSets.remove(imageSet);
                    getManager().remainNone(imageSet);
                    configurations.remove(imageSet);
                    ImagePairs.removeAll(imageSet);
                    removeLoadedImageSet(set, setsToIgnore);
                }
            }
        }

        if (!setsToIgnore.contains(imageSet)) {
            imageSets.remove(imageSet);
            getManager().remainNone(imageSet);
            configurations.remove(imageSet);
            ImagePairs.removeAll(imageSet);
        }
    }

    private void addImageSet(String imageSet) {
        if (configurations.containsKey(imageSet)) {
            imageSets.add(imageSet);
            createMascot(imageSet);
        } else {
            if (loadConfiguration(imageSet)) {
                imageSets.add(imageSet);
                String informationAlreadySeen = properties.getProperty("InformationDismissed", "");
                if (configurations.get(imageSet).containsInformationKey("SplashImage") &&
                        (Boolean.parseBoolean(properties.getProperty("AlwaysShowInformationScreen", "false")) ||
                                !informationAlreadySeen.contains(imageSet))) {
                    InformationWindow info = new InformationWindow();
                    info.init(imageSet, configurations.get(imageSet));
                    info.display();
                    setMascotInformationDismissed(imageSet);
                    updateConfigFile();
                }
                createMascot(imageSet);
            } else {
                // conf failed
                configurations.remove(imageSet); // maybe move this to the loadConfig catch
            }
        }
    }

    public Configuration getConfiguration(String imageSet) {
        return configurations.get(imageSet);
    }

    private Manager getManager() {
        return manager;
    }

    public Properties getProperties() {
        return properties;
    }

    public ResourceBundle getLanguageBundle() {
        return languageBundle;
    }

    public void exit() {
        getManager().disposeAll();
        getManager().stop();
        System.exit(0);
    }
}
