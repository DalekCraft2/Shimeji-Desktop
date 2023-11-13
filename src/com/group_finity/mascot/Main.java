package com.group_finity.mascot;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.image.ImagePairs;
import com.group_finity.mascot.imagesetchooser.ImageSetChooser;
import com.group_finity.mascot.sound.Sounds;
import com.group_finity.mascot.win.WindowsInteractiveWindowForm;
import com.joconner.i18n.Utf8ResourceBundleControl;
import com.nilo.plaf.nimrod.NimRODLookAndFeel;
import com.nilo.plaf.nimrod.NimRODTheme;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Program entry point.
 * <p>
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());
    // Action that matches the "Gather Around Mouse!" context menu command
    static final String BEHAVIOR_GATHER = "ChaseMouse";

    static {
        try {
            LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
        } catch (final SecurityException | IOException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError err) {
            log.log(Level.SEVERE, "Out of Memory Exception.  There are probably have too many "
                    + "Shimeji mascots in the image folder for your computer to handle.  Select fewer"
                    + " image sets or move some to the img/unused folder and try again.", err);
            Main.showError("Out of Memory.  There are probably have too many \n"
                    + "Shimeji mascots for your computer to handle.\n"
                    + "Select fewer image sets or move some to the \n"
                    + "img/unused folder and try again.");
            System.exit(0);
        }
    }

    private final Manager manager = new Manager();
    private ArrayList<String> imageSets = new ArrayList<>();
    private Hashtable<String, Configuration> configurations = new Hashtable<>();
    private Hashtable<String, ArrayList<String>> childImageSets = new Hashtable<>();
    private static Main instance = new Main();
    private Properties properties = new Properties();
    private Platform platform;
    private ResourceBundle languageBundle;

    private JDialog form;

    public static Main getInstance() {
        return instance;
    }

    private static JFrame frame = new javax.swing.JFrame();

    public static void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(final String[] args) {
        try {
            getInstance().run();
        } catch (OutOfMemoryError err) {
            log.log(Level.SEVERE, "Out of Memory Exception.  There are probably have too many "
                    + "Shimeji mascots in the image folder for your computer to handle.  Select fewer"
                    + " image sets or move some to the img/unused folder and try again.", err);
            Main.showError("Out of Memory.  There are probably have too many \n"
                    + "Shimeji mascots for your computer to handle.\n"
                    + "Select fewer image sets or move some to the \n"
                    + "img/unused folder and try again.");
            System.exit(0);
        }
    }

    public void run() {
        // test operating system
        if (!System.getProperty("sun.arch.data.model").equals("64")) {
            platform = Platform.x86;
        } else {
            platform = Platform.x86_64;
        }

        // load properties
        properties = new Properties();
        try (FileInputStream input = new FileInputStream("./conf/settings.properties")) {
            properties.load(input);
        } catch (IOException ignored) {
        }

        // load langauges
        try {
            ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl(false);
            languageBundle = ResourceBundle.getBundle("language", Locale.forLanguageTag(properties.getProperty("Language", "en-GB")), utf8Control);
        } catch (Exception ex) {
            Main.showError("The default language file could not be loaded. Ensure that you have the latest shimeji language.properties in your conf directory.");
            exit();
        }

        // load theme
        try {
            // default light theme
            NimRODLookAndFeel lookAndFeel = new NimRODLookAndFeel();

            // check for theme properties
            NimRODTheme theme = null;
            try {
                if (new File("./conf/theme.properties").isFile()) {
                    theme = new NimRODTheme("./conf/theme.properties");
                }
            } catch (Exception exc) {
                theme = null;
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
                theme.setMenuOpacity(255);
                theme.setFrameOpacity(255);
            }

            // handle menu size
            if (!properties.containsKey("MenuDPI")) {
                properties.setProperty("MenuDPI", Math.max(java.awt.Toolkit.getDefaultToolkit().getScreenResolution(), 96) + "");
                updateConfigFile();
            }
            float menuScaling = Float.parseFloat(properties.getProperty("MenuDPI", "96")) / 96;
            java.awt.Font font = theme.getUserTextFont().deriveFont(theme.getUserTextFont().getSize() * menuScaling);
            theme.setFont(font);

            NimRODLookAndFeel.setCurrentTheme(theme);
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            // all done
            lookAndFeel.initialize();
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception ex) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex1) {
                log.log(Level.SEVERE, "Look & Feel unsupported.", ex1);
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

        // Load settings
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

        // Create the first mascot
        for (String imageSet : imageSets) {
            createMascot(imageSet);
        }

        getManager().start();
    }

    private boolean loadConfiguration(final String imageSet) {
        try {
            // try to load in the correct xml files
            String filePath = "./conf/";
            String actionsFile = filePath + "actions.xml";
            if (new File(filePath + "\u52D5\u4F5C.xml").exists()) {
                actionsFile = filePath + "\u52D5\u4F5C.xml";
            }

            filePath = "./conf/" + imageSet + "/";
            if (new File(filePath + "actions.xml").exists()) {
                actionsFile = filePath + "actions.xml";
            } else if (new File(filePath + "\u52D5\u4F5C.xml").exists()) {
                actionsFile = filePath + "\u52D5\u4F5C.xml";
            } else if (new File(filePath + "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml").exists()) {
                actionsFile = filePath + "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml";
            } else if (new File(filePath + "\u00A6-\u00BA@.xml").exists()) {
                actionsFile = filePath + "\u00A6-\u00BA@.xml";
            } else if (new File(filePath + "\u00F4\u00AB\u00EC\u00FD.xml").exists()) {
                actionsFile = filePath + "\u00F4\u00AB\u00EC\u00FD.xml";
            } else if (new File(filePath + "one.xml").exists()) {
                actionsFile = filePath + "one.xml";
            } else if (new File(filePath + "1.xml").exists()) {
                actionsFile = filePath + "1.xml";
            }

            filePath = "./img/" + imageSet + "/conf/";
            if (new File(filePath + "actions.xml").exists()) {
                actionsFile = filePath + "actions.xml";
            } else if (new File(filePath + "\u52D5\u4F5C.xml").exists()) {
                actionsFile = filePath + "\u52D5\u4F5C.xml";
            } else if (new File(filePath + "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml").exists()) {
                actionsFile = filePath + "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml";
            } else if (new File(filePath + "\u00A6-\u00BA@.xml").exists()) {
                actionsFile = filePath + "\u00A6-\u00BA@.xml";
            } else if (new File(filePath + "\u00F4\u00AB\u00EC\u00FD.xml").exists()) {
                actionsFile = filePath + "\u00F4\u00AB\u00EC\u00FD.xml";
            } else if (new File(filePath + "one.xml").exists()) {
                actionsFile = filePath + "one.xml";
            } else if (new File(filePath + "1.xml").exists()) {
                actionsFile = filePath + "1.xml";
            }

            log.log(Level.INFO, imageSet + " Read Action File ({0})", actionsFile);

            final Document actions = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    Files.newInputStream(Paths.get(actionsFile)));

            Configuration configuration = new Configuration();

            configuration.load(new Entry(actions.getDocumentElement()), imageSet);

            filePath = "./conf/";
            String behaviorsFile = filePath + "behaviors.xml";
            if (new File(filePath + "\u884C\u52D5.xml").exists()) {
                behaviorsFile = filePath + "\u884C\u52D5.xml";
            }

            filePath = "./conf/" + imageSet + "/";
            if (new File(filePath + "behaviors.xml").exists()) {
                behaviorsFile = filePath + "behaviors.xml";
            } else if (new File(filePath + "behavior.xml").exists()) {
                behaviorsFile = filePath + "behavior.xml";
            } else if (new File(filePath + "\u884C\u52D5.xml").exists()) {
                behaviorsFile = filePath + "\u884C\u52D5.xml";
            } else if (new File(filePath + "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml").exists()) {
                behaviorsFile = filePath + "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml";
            } else if (new File(filePath + "\u00AA\u00B5\u00A6-.xml").exists()) {
                behaviorsFile = filePath + "\u00AA\u00B5\u00A6-.xml";
            } else if (new File(filePath + "\u00ECs\u00F4\u00AB.xml").exists()) {
                behaviorsFile = filePath + "\u00ECs\u00F4\u00AB.xml";
            } else if (new File(filePath + "two.xml").exists()) {
                behaviorsFile = filePath + "two.xml";
            } else if (new File(filePath + "2.xml").exists()) {
                behaviorsFile = filePath + "2.xml";
            }

            filePath = "./img/" + imageSet + "/conf/";
            if (new File(filePath + "behaviors.xml").exists()) {
                behaviorsFile = filePath + "behaviors.xml";
            } else if (new File(filePath + "behavior.xml").exists()) {
                behaviorsFile = filePath + "behavior.xml";
            } else if (new File(filePath + "\u884C\u52D5.xml").exists()) {
                behaviorsFile = filePath + "\u884C\u52D5.xml";
            } else if (new File(filePath + "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml").exists()) {
                behaviorsFile = filePath + "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml";
            } else if (new File(filePath + "\u00AA\u00B5\u00A6-.xml").exists()) {
                behaviorsFile = filePath + "\u00AA\u00B5\u00A6-.xml";
            } else if (new File(filePath + "\u00ECs\u00F4\u00AB.xml").exists()) {
                behaviorsFile = filePath + "\u00ECs\u00F4\u00AB.xml";
            } else if (new File(filePath + "two.xml").exists()) {
                behaviorsFile = filePath + "two.xml";
            } else if (new File(filePath + "2.xml").exists()) {
                behaviorsFile = filePath + "2.xml";
            }

            log.log(Level.INFO, imageSet + " Read Behavior File ({0})", behaviorsFile);

            final Document behaviors = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    Files.newInputStream(Paths.get(behaviorsFile)));

            configuration.load(new Entry(behaviors.getDocumentElement()), imageSet);

            configuration.validate();

            configurations.put(imageSet, configuration);

            ArrayList<String> childMascots = new ArrayList<>();

            // born mascot bit goes here...
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
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Failed to load configuration files", e);
            Main.showError(languageBundle.getString("FailedLoadConfigErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getString("SeeLogForDetails"));
        }

        return false;
    }

    /**
     * Creates a tray icon.
     */
    private void createTrayIcon() {
        log.log(Level.INFO, "create a tray icon");

        // get the tray icon image
        BufferedImage image = null;
        try {
            image = ImageIO.read(Main.class.getResource("/icon.png"));
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Failed to create tray icon", e);
            Main.showError(languageBundle.getString("FailedDisplaySystemTrayErrorMessage") + "\n" + languageBundle.getString("SeeLogForDetails"));
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
                public void mouseClicked(MouseEvent event) {
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (event.isPopupTrigger()) {
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
                        btnCallShimeji.addActionListener(event15 -> {
                            createMascot();
                            form.dispose();
                        });

                        JButton btnFollowCursor = new JButton(languageBundle.getString("FollowCursor"));
                        btnFollowCursor.addActionListener(event14 -> {
                            getManager().setBehaviorAll(BEHAVIOR_GATHER);
                            form.dispose();
                        });

                        JButton btnReduceToOne = new JButton(languageBundle.getString("ReduceToOne"));
                        btnReduceToOne.addActionListener(event13 -> {
                            getManager().remainOne();
                            form.dispose();
                        });

                        JButton btnRestoreWindows = new JButton(languageBundle.getString("RestoreWindows"));
                        btnRestoreWindows.addActionListener(event12 -> {
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
                        btnAllowedBehaviours.addActionListener(event1 -> {
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

                        final JButton btnSettings = new JButton(languageBundle.getString("Settings"));
                        btnSettings.addMouseListener(new MouseListener() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                            }

                            @Override
                            public void mousePressed(MouseEvent e) {
                            }

                            @Override
                            public void mouseReleased(MouseEvent e) {
                                btnSettings.setEnabled(true);
                            }

                            @Override
                            public void mouseEntered(MouseEvent e) {
                            }

                            @Override
                            public void mouseExited(MouseEvent e) {
                            }
                        });
                        btnSettings.addActionListener(e -> {
                            JMenuItem chooseShimejiMenu = new JMenuItem(languageBundle.getString("ChooseShimeji"));
                            chooseShimejiMenu.addActionListener(e131 -> {
                                form.dispose();
                                setActiveImageSets(new ImageSetChooser(frame, true).display());
                            });

                            // "Interactive Windows" menu item
                            JMenuItem interactiveMenu = new JMenuItem(languageBundle.getString("ChooseInteractiveWindows"));
                            interactiveMenu.addActionListener(e130 -> {
                                form.dispose();
                                new WindowsInteractiveWindowForm(frame, true).display();
                                NativeFactory.getInstance().getEnvironment().refreshCache();
                            });

                            // "Always Show Shimeji Chooser" menu item
                            final JCheckBoxMenuItem shimejiChooserMenu = new JCheckBoxMenuItem(languageBundle.getString("AlwaysShowShimejiChooser"), Boolean.parseBoolean(properties.getProperty("AlwaysShowShimejiChooser", "false")));
                            shimejiChooserMenu.addItemListener(e129 -> {
                                shimejiChooserMenu.setState(toggleBooleanSetting("AlwaysShowShimejiChooser", false));
                                updateConfigFile();
                                btnAllowedBehaviours.setEnabled(true);
                            });

                            // "Scaling" menu
                            JMenu scalingMenu = new JMenu(languageBundle.getString("Scaling"));

                            // "hqx Filter" menu item
                            final JCheckBoxMenuItem filterMenu = new JCheckBoxMenuItem(languageBundle.getString("Filter"), Boolean.parseBoolean(properties.getProperty("Filter", "false")));
                            filterMenu.addActionListener(e128 -> {
                                form.dispose();
                                filterMenu.setState(toggleBooleanSetting("Filter", false));
                                updateConfigFile();

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

                                Main.this.getManager().setExitOnLastRemoved(isExit);
                            });

                            final JCheckBoxMenuItem scaling1x = new JCheckBoxMenuItem("1x", Integer.parseInt(properties.getProperty("Scaling", "1")) == 1);
                            scaling1x.addActionListener(e127 -> {
                                form.dispose();
                                properties.setProperty("Scaling", "1");
                                updateConfigFile();

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

                                Main.this.getManager().setExitOnLastRemoved(isExit);
                            });
                            final JCheckBoxMenuItem scaling2x = new JCheckBoxMenuItem("2x", Integer.parseInt(properties.getProperty("Scaling", "1")) == 2);
                            scaling2x.addActionListener(e126 -> {
                                form.dispose();
                                properties.setProperty("Scaling", "2");
                                updateConfigFile();

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

                                Main.this.getManager().setExitOnLastRemoved(isExit);
                            });
                            final JCheckBoxMenuItem scaling3x = new JCheckBoxMenuItem("3x", Integer.parseInt(properties.getProperty("Scaling", "1")) == 3);
                            scaling3x.addActionListener(e125 -> {
                                form.dispose();
                                properties.setProperty("Scaling", "3");
                                updateConfigFile();

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

                                Main.this.getManager().setExitOnLastRemoved(isExit);
                            });
                            final JCheckBoxMenuItem scaling4x = new JCheckBoxMenuItem("4x", Integer.parseInt(properties.getProperty("Scaling", "1")) == 4);
                            scaling4x.addActionListener(e124 -> {
                                form.dispose();
                                properties.setProperty("Scaling", "4");
                                updateConfigFile();

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

                                Main.this.getManager().setExitOnLastRemoved(isExit);
                            });
                            final JCheckBoxMenuItem scaling6x = new JCheckBoxMenuItem("6x", Integer.parseInt(properties.getProperty("Scaling", "1")) == 6);
                            scaling6x.addActionListener(e123 -> {
                                form.dispose();
                                properties.setProperty("Scaling", "6");
                                updateConfigFile();

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

                                Main.this.getManager().setExitOnLastRemoved(isExit);
                            });
                            final JCheckBoxMenuItem scaling8x = new JCheckBoxMenuItem("8x", Integer.parseInt(properties.getProperty("Scaling", "1")) == 8);
                            scaling8x.addActionListener(e122 -> {
                                form.dispose();
                                properties.setProperty("Scaling", "8");
                                updateConfigFile();

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

                                Main.this.getManager().setExitOnLastRemoved(isExit);
                            });
                            scalingMenu.add(filterMenu);
                            scalingMenu.add(new JSeparator());
                            scalingMenu.add(scaling1x);
                            scalingMenu.add(scaling2x);
                            scalingMenu.add(scaling3x);
                            scalingMenu.add(scaling4x);
                            scalingMenu.add(scaling6x);
                            scalingMenu.add(scaling8x);

                            JPopupMenu settingsPopup = new JPopupMenu();
                            settingsPopup.add(chooseShimejiMenu);
                            settingsPopup.add(interactiveMenu);
                            settingsPopup.add(new JSeparator());
                            settingsPopup.add(shimejiChooserMenu);
                            settingsPopup.add(new JSeparator());
                            settingsPopup.add(scalingMenu);
                            settingsPopup.addPopupMenuListener(new PopupMenuListener() {
                                @Override
                                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                                }

                                @Override
                                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                                    if (panel.getMousePosition() != null) {
                                        btnSettings.setEnabled(!(panel.getMousePosition().x > btnSettings.getX() &&
                                                panel.getMousePosition().x < btnSettings.getX() + btnSettings.getWidth() &&
                                                panel.getMousePosition().y > btnSettings.getY() &&
                                                panel.getMousePosition().y < btnSettings.getY() + btnSettings.getHeight()));
                                    } else {
                                        btnSettings.setEnabled(true);
                                    }
                                }

                                @Override
                                public void popupMenuCanceled(PopupMenuEvent e) {
                                }
                            });
                            settingsPopup.show(btnSettings, 0, btnSettings.getHeight());
                            btnSettings.requestFocusInWindow();
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
                                updateLanguage("en-GB");
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
                                updateLanguage("de-DE");
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
                                updateLanguage("fr-FR");
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
                                updateLanguage("it-IT");
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

                            // Srpski menu item
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
                                updateLanguage("zh-CN");
                                updateConfigFile();
                            });

                            // Chinese (Traditional) menu item
                            final JMenuItem chineseTraditionalMenu = new JMenuItem("\u7E41\u9AD4\u4E2D\u6587");
                            chineseTraditionalMenu.addActionListener(e13 -> {
                                form.dispose();
                                updateLanguage("zh-TW");
                                updateConfigFile();
                            });

                            // Korean menu item
                            final JMenuItem koreanMenu = new JMenuItem("\ud55c\uad6d\uc5b4");
                            koreanMenu.addActionListener(e12 -> {
                                form.dispose();
                                updateLanguage("ko-KR");
                                updateConfigFile();
                            });

                            // Japanese menu item
                            final JMenuItem japaneseMenu = new JMenuItem("\u65E5\u672C\u8A9E");
                            japaneseMenu.addActionListener(e1 -> {
                                form.dispose();
                                updateLanguage("ja-JP");
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
                        float scaling = Float.parseFloat(properties.getProperty("MenuDPI", "96")) / 96;
                        panel.setLayout(new java.awt.GridBagLayout());
                        GridBagConstraints gridBag = new GridBagConstraints();
                        gridBag.fill = GridBagConstraints.HORIZONTAL;
                        gridBag.gridx = 0;
                        gridBag.gridy = 0;
                        panel.add(btnCallShimeji, gridBag);
                        gridBag.insets = new Insets((int) (5 * scaling), 0, 0, 0);
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
                        form.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                        form.setAlwaysOnTop(true);

                        // set the form dimensions
                        java.awt.FontMetrics metrics = btnCallShimeji.getFontMetrics(btnCallShimeji.getFont());
                        int width = metrics.stringWidth(btnCallShimeji.getText());
                        width = Math.max(metrics.stringWidth(btnFollowCursor.getText()), width);
                        width = Math.max(metrics.stringWidth(btnReduceToOne.getText()), width);
                        width = Math.max(metrics.stringWidth(btnRestoreWindows.getText()), width);
                        width = Math.max(metrics.stringWidth(btnAllowedBehaviours.getText()), width);
                        width = Math.max(metrics.stringWidth(btnSettings.getText()), width);
                        width = Math.max(metrics.stringWidth(btnLanguage.getText()), width);
                        width = Math.max(metrics.stringWidth(btnPauseAll.getText()), width);
                        width = Math.max(metrics.stringWidth(btnDismissAll.getText()), width);
                        panel.setPreferredSize(new Dimension(width + 64,
                                (int) (24 * scaling) + // 12 padding on top and bottom
                                        (int) (60 * scaling) + // 10 insets of 5 height normally
                                        9 * metrics.getHeight() + // 9 button faces
                                        84));
                        form.pack();

                        // setting location of the form
                        form.setLocation(event.getPoint().x - form.getWidth(), event.getPoint().y - form.getHeight());

                        // make sure that it is on the screen if people are using exotic taskbar locations
                        Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                        if (form.getX() < screen.getX()) {
                            form.setLocation(event.getPoint().x, form.getY());
                        }
                        if (form.getY() < screen.getY()) {
                            form.setLocation(form.getX(), event.getPoint().y);
                        }
                        form.setVisible(true);
                        form.setMinimumSize(form.getSize());
                    } else if (event.getButton() == MouseEvent.BUTTON1) {
                        createMascot();
                    }
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
            Main.showError(languageBundle.getString("FailedDisplaySystemTrayErrorMessage") + "\n" + languageBundle.getString("SeeLogForDetails"));
            exit();
        }
    }

    // Randomly creates a mascot
    public void createMascot() {
        int length = imageSets.size();
        int random = (int) (length * Math.random());
        createMascot(imageSets.get(random));
    }

    /**
     * Creates a mascot.
     */
    public void createMascot(String imageSet) {
        log.log(Level.INFO, "create a mascot");

        // Create one mascot
        final Mascot mascot = new Mascot(imageSet);

        // Create it outside the bounds of the screen
        mascot.setAnchor(new Point(-4000, -4000));

        // Randomize the initial orientation
        mascot.setLookRight(Math.random() < 0.5);

        try {
            mascot.setBehavior(getConfiguration(imageSet).buildBehavior(null, mascot));
            this.getManager().add(mascot);
        } catch (final BehaviorInstantiationException e) {
            log.log(Level.SEVERE, "Failed to initialize the first action", e);
            Main.showError(languageBundle.getString("FailedInitialiseFirstActionErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getString("SeeLogForDetails"));
            mascot.dispose();
        } catch (final CantBeAliveException e) {
            log.log(Level.SEVERE, "Fatal Error", e);
            Main.showError(languageBundle.getString("FailedInitialiseFirstActionErrorMessage") + "\n" + e.getMessage() + "\n" + languageBundle.getString("SeeLogForDetails"));
            mascot.dispose();
        } catch (Exception e) {
            log.log(Level.SEVERE, imageSet + " fatal error, can not be started.", e);
            Main.showError(languageBundle.getString("CouldNotCreateShimejiErrorMessage") + imageSet + ".\n" + e.getMessage() + "\n" + languageBundle.getString("SeeLogForDetails"));
            mascot.dispose();
        }
    }

    private void refreshLanguage() {
        ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl(false);
        languageBundle = ResourceBundle.getBundle("language", Locale.forLanguageTag(properties.getProperty("Language", "en-GB")), utf8Control);

        boolean isExit = getManager().isExitOnLastRemoved();
        getManager().setExitOnLastRemoved(false);
        getManager().disposeAll();

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

    private void updateLanguage(String language) {
        if (!properties.getProperty("Language", "en-GB").equals(language)) {
            properties.setProperty("Language", language);
            refreshLanguage();
        }
    }

    private boolean toggleBooleanSetting(String propertyName, boolean defaultValue) {
        if (Boolean.parseBoolean(properties.getProperty(propertyName, defaultValue + ""))) {
            properties.setProperty(propertyName, "false");
            return false;
        } else {
            properties.setProperty(propertyName, "true");
            return true;
        }
    }

    private void updateConfigFile() {
        try (FileOutputStream output = new FileOutputStream("./conf/settings.properties")) {
            properties.store(output, "Shimeji-ee Configuration Options");
        } catch (Exception ignored) {
        }
    }

    /**
     * Replaces the current set of active imageSets without modifying
     * valid imageSets that are already active. Does nothing if newImageSets
     * are null
     *
     * @param newImageSets All the imageSets that should now be active
     * @author snek
     * @author Kilkakon (did some tweaks)
     */
    private void setActiveImageSets(ArrayList<String> newImageSets) {
        if (newImageSets == null) {
            return;
        }

        // I don't think there would be enough imageSets chosen at any given
        // time for it to be worth using HashSet but i might be wrong
        ArrayList<String> toRemove = new ArrayList<>(imageSets);
        toRemove.removeAll(newImageSets);

        ArrayList<String> toAdd = new ArrayList<>();
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

        boolean isExit = Main.this.getManager().isExitOnLastRemoved();
        Main.this.getManager().setExitOnLastRemoved(false);

        for (String r : toRemove)
            removeLoadedImageSet(r, toRetain);

        for (String a : toAdd)
            addImageSet(a);

        Main.this.getManager().setExitOnLastRemoved(isExit);
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
        return this.manager;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Properties getProperties() {
        return properties;
    }

    public ResourceBundle getLanguageBundle() {
        return languageBundle;
    }

    public void exit() {
        this.getManager().disposeAll();
        this.getManager().stop();
        System.exit(0);
    }
}
