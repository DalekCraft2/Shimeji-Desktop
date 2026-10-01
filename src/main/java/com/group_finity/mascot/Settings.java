package com.group_finity.mascot;

import com.group_finity.mascot.image.Filter;
import com.group_finity.mascot.platform.virtual.VirtualContentPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A container for the user settings data read from the program's settings file.
 *
 * @author DalekCraft
 * @see SettingsWindow
 */
public class Settings {
    private static final Logger log = LoggerFactory.getLogger(Settings.class);

    /**
     * The property list containing the raw data read from the settings file.
     * This is read from when loading data, and written to when writing data to the settings file.
     */
    private final Properties properties = new Properties();

    // Miscellaneous settings

    /**
     * An override for the program's name, used in places like window titles and the tray icon if set.
     * If left empty, the default program name ("Shimeji-ee") is used.
     */
    public String shimejiEeNameOverride = "";

    /**
     * The currently selected image sets.
     */
    public List<String> activeImageSets = new ArrayList<>();

    /**
     * A list of image sets whose information windows have been dismissed.
     * Information windows that have been dismissed will not appear when starting the program.
     * This is ignored if {@link #alwaysShowInformationScreen} is {@code true}.
     */
    public List<String> informationDismissed = new ArrayList<>();

    // Settings in tray menu and mascot context menu

    /**
     * The language to use for localizing the UI.
     */
    public Locale language = Locale.getDefault();

    /**
     * Maps an image set to a list of behaviors that are disabled for that image set.
     */
    public Map<String, List<String>> disabledBehaviors = new HashMap<>();

    /**
     * Whether mascots are allowed to perform breeding actions, which create new mascots.
     * This is only used if a breeding action's {@code BornTransient} parameter is set to {@code false}.
     *
     * @see com.group_finity.mascot.action.Breed
     * @see com.group_finity.mascot.action.BreedJump
     * @see com.group_finity.mascot.action.BreedMove
     */
    public boolean breeding = true;

    /**
     * Whether mascots are allowed to create transient mascots, which disappear shortly after creation.
     * This is only used if a breeding action's {@code BornTransient} parameter is set to {@code true}.
     *
     * @see com.group_finity.mascot.action.Breed
     * @see com.group_finity.mascot.action.BreedJump
     * @see com.group_finity.mascot.action.BreedMove
     */
    public boolean transients = true;

    /**
     * Whether mascots are allowed to change which image set they are using.
     *
     * @see com.group_finity.mascot.action.Transform
     */
    public boolean transformation = true;

    /**
     * Whether mascots are allowed to carry and throw windows.
     *
     * @see com.group_finity.mascot.action.FallWithIE
     * @see com.group_finity.mascot.action.WalkWithIE
     * @see com.group_finity.mascot.action.ThrowIE
     */
    public boolean throwing = true;

    /**
     * Whether sound effects are enabled.
     */
    public boolean sounds = true;

    /**
     * Whether mascots are allowed to walk between multiple displays on their own,
     * without needing the user to drag them to another display.
     */
    public boolean multiscreen = true;

    // General settings

    /**
     * Whether to use the system tray for the tray menu.
     * If set to {@code true}, the tray menu will act as a popup menu for the tray icon.
     * If set to {@code false}, the tray menu will always be visible, and will exit the program if closed.
     *
     * @see TrayMenu
     */
    public boolean showTrayIcon = true;

    /**
     * Whether to always show the {@linkplain com.group_finity.mascot.imagesetchooser.ImageSetChooser image set chooser}
     * upon starting the program.
     *
     * @see com.group_finity.mascot.imagesetchooser.ImageSetChooser
     */
    public boolean alwaysShowShimejiChooser = false;

    /**
     * Whether to always show the {@linkplain InformationWindow information windows} for all
     * image sets upon starting the program.
     *
     * @see InformationWindow
     */
    public boolean alwaysShowInformationScreen = false;

    /**
     * Whether to draw geometry information for mascots, for debugging purposes.
     * The geometry information of a mascot includes its bounds, anchor point, and hotspots.
     */
    public boolean drawShimejiBounds = false;

    /**
     * The type of filter to use when scaling mascot images.
     */
    public Filter filter = Filter.NEAREST_NEIGHBOUR;

    /**
     * The opacity factor by which to premultiply mascot images.
     * 0 makes the images completely transparent, and 1 leaves the images' opacity unchanged.
     */
    public double opacity = 1.0;

    /**
     * The factor by which to scale mascot images.
     */
    public double scaling = 1.0;

    // Interactive window settings

    /**
     * A list of all whitelisted window titles, for determining whether mascots can interact with a window
     * with a given title. A window can only be interactive if its title is valid.
     * <p>
     * If a window title contains any of the strings in this list, the window title may be valid.
     * However, because the {@linkplain #interactiveWindowsBlacklist window title blacklist} takes priority over
     * the whitelist, it is not guaranteed that the window title will be valid if it contains a string in this list.
     * <p>
     * If this list is empty, a window title can still be valid if the blacklist is not empty and does not contain
     * any substrings of the window title.
     *
     * @see #interactiveWindowsBlacklist
     */
    public List<String> interactiveWindows = new ArrayList<>();

    /**
     * A list of all blacklisted window titles, for determining whether mascots can interact with a window
     * with a given title. A window can only be interactive if its title is valid.
     * <p>
     * If a window title contains any of the strings in this list, the window title is invalid.
     *
     * @see #interactiveWindows
     */
    public List<String> interactiveWindowsBlacklist = new ArrayList<>();

    // Window mode settings

    /**
     * Whether to create mascots in a virtual environment in a window, instead of on the desktop itself.
     */
    public boolean windowedMode = false;

    /**
     * The initial dimensions of the virtual environment window.
     */
    public Dimension windowSize = new Dimension(600, 500);

    /**
     * The background color of the virtual environment window.
     */
    public Color backgroundColor = Color.GREEN;

    /**
     * An optional image to display in the background of the virtual environment window.
     */
    public Path backgroundImage = null;

    /**
     * The type of behavior to use when scaling the virtual environment window's background image.
     */
    public VirtualContentPanel.ResizeMode backgroundMode = VirtualContentPanel.ResizeMode.CENTRE;

    /**
     * Reads settings from the given path.
     *
     * @param path the path from which to load the settings
     */
    public void load(Path path) {
        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException e) {
                log.error("Failed to load settings", e);
            }
        }

        // Miscellaneous settings
        shimejiEeNameOverride = properties.getProperty("ShimejiEENameOverride", "").trim();
        activeImageSets = getStringListProperty(properties, "ActiveShimeji", "/", new ArrayList<>());
        informationDismissed = getStringListProperty(properties, "InformationDismissed", "/", new ArrayList<>());

        // Settings in tray menu and mascot context menu
        language = Locale.forLanguageTag(properties.getProperty("Language", Locale.getDefault().toLanguageTag()));
        disabledBehaviors.clear();
        if (!properties.isEmpty()) {
            for (String key : properties.stringPropertyNames()) {
                // Make sure the key's length is longer than "DisabledBehaviours." to prevent an IndexOutOfBoundsException
                if (key.startsWith("DisabledBehaviours.") && key.length() > "DisabledBehaviours.".length()) {
                    String imageSet = key.substring(key.indexOf('.') + 1);
                    List<String> list = getStringListProperty(properties, key, "/");
                    if (!list.isEmpty())
                        disabledBehaviors.put(imageSet, list);
                }
            }
        }
        breeding = getBooleanProperty(properties, "Breeding", true);
        transients = getBooleanProperty(properties, "Transients", true);
        transformation = getBooleanProperty(properties, "Transformation", true);
        throwing = getBooleanProperty(properties, "Throwing", true);
        sounds = getBooleanProperty(properties, "Sounds", true);
        multiscreen = getBooleanProperty(properties, "Multiscreen", true);

        // General settings
        showTrayIcon = getBooleanProperty(properties, "ShowTrayIcon", true);
        alwaysShowShimejiChooser = getBooleanProperty(properties, "AlwaysShowShimejiChooser", false);
        alwaysShowInformationScreen = getBooleanProperty(properties, "AlwaysShowInformationScreen", false);
        drawShimejiBounds = getBooleanProperty(properties, "DrawShimejiBounds", false);
        String filterText = properties.getProperty("Filter", "false");
        if (filterText.equalsIgnoreCase("true") || filterText.equalsIgnoreCase("hqx")) {
            filter = Filter.HQX;
        } else if (filterText.equalsIgnoreCase("bicubic")) {
            filter = Filter.BICUBIC;
        } else {
            filter = Filter.NEAREST_NEIGHBOUR;
        }
        opacity = getDoubleProperty(properties, "Opacity", 1.0);
        scaling = getDoubleProperty(properties, "Scaling", 1.0);

        // Interactive window settings
        interactiveWindows = getStringListProperty(properties, "InteractiveWindows", "/", new ArrayList<>());
        interactiveWindowsBlacklist = getStringListProperty(properties, "InteractiveWindowsBlacklist", "/", new ArrayList<>());

        // Window mode settings
        windowedMode = properties.getProperty("Environment", "generic").equals("virtual");
        try {
            String[] windowSizeArray = properties.getProperty("WindowSize", "600x500").split("x");
            if (windowSizeArray.length >= 2)
                windowSize = new Dimension(Integer.parseInt(windowSizeArray[0]), Integer.parseInt(windowSizeArray[1]));
            else
                windowSize = new Dimension(600, 500);
        } catch (NumberFormatException e) {
            windowSize = new Dimension(600, 500);
        }
        backgroundColor = new Color(getIntProperty(properties, "Background", 0x00FF00));
        String backgroundImageText = properties.getProperty("BackgroundImage");
        backgroundImage = backgroundImageText == null || backgroundImageText.isEmpty() ? null : Path.of(properties.getProperty("BackgroundImage"));
        String backgroundModeText = properties.getProperty("BackgroundMode", "centre");
        switch (backgroundModeText) {
            case "fill" -> backgroundMode = VirtualContentPanel.ResizeMode.FILL;
            case "fit" -> backgroundMode = VirtualContentPanel.ResizeMode.FIT;
            case "stretch" -> backgroundMode = VirtualContentPanel.ResizeMode.STRETCH;
            default -> backgroundMode = VirtualContentPanel.ResizeMode.CENTRE;
        }
    }

    /**
     * Parses a boolean from the value with the specified key in the specified property list.
     *
     * @param properties the property list from which to read the value
     * @param key the key of the property
     * @param defaultValue a default value if the specified key does not exist in the property list
     * @return a boolean parsed from the property with the specified key, or the default value if the specified
     * key does not exist in the property list
     */
    private boolean getBooleanProperty(Properties properties, String key, boolean defaultValue) {
        if (properties.containsKey(key)) {
            return Boolean.parseBoolean(properties.getProperty(key));
        } else {
            return defaultValue;
        }
    }

    /**
     * Parses an integer from the value with the specified key in the specified property list.
     *
     * @param properties the property list from which to read the value
     * @param key the key of the property
     * @param defaultValue a default value if the specified key does not exist in the property list, or if the
     * value with the specified key cannot be parsed as an integer
     * @return an integer parsed from the value with the specified key, or the default value if either the specified
     * key does not exist in the property list or the value with the specified key cannot be parsed as an integer
     */
    private int getIntProperty(Properties properties, String key, int defaultValue) {
        if (properties.containsKey(key)) {
            try {
                return Integer.parseInt(properties.getProperty(key));
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Parses a double from the value with the specified key in the specified property list.
     *
     * @param properties the property list from which to read the value
     * @param key the key of the property
     * @param defaultValue a default value if the specified key does not exist in the property list, or if the
     * value with the specified key cannot be parsed as a double
     * @return a double parsed from the value with the specified key, or the default value if either the specified
     * key does not exist in the property list or the value with the specified key cannot be parsed as a double
     */
    private double getDoubleProperty(Properties properties, String key, double defaultValue) {
        if (properties.containsKey(key)) {
            try {
                return Double.parseDouble(properties.getProperty(key));
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Parses an array of integers from the value with the specified key in the specified property list.
     *
     * @param properties the property list from which to read the value
     * @param key the key of the property
     * @param separator the delimiter to use when splitting the single string property into an array of integers
     * @param defaultValue a default value if the specified key does not exist in the property list, or if one of the
     * values in the array cannot be parsed as an integer
     * @return an array of integers parsed from the value with the specified key, or the default value if either the
     * specified key does not exist in the property list or one of the values in the array cannot be parsed as an integer
     */
    private int[] getIntArrayProperty(Properties properties, String key, String separator, int[] defaultValue) {
        if (properties.containsKey(key)) {
            String[] splitArray = properties.getProperty(key).split(separator);
            try {
                return Arrays.stream(splitArray).mapToInt(Integer::parseInt).toArray();
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Parses a list of strings from the value with the specified key in the specified property list.
     *
     * @param properties the property list from which to read the value
     * @param key the key of the property
     * @param separator the delimiter to use when splitting the single string property into a list of strings
     * @return a list of strings parsed from the value with the specified key
     */
    private List<String> getStringListProperty(Properties properties, String key, String separator) {
        return Arrays.stream(properties.getProperty(key).split(separator)).filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());
    }

    /**
     * Parses a list of strings from the value with the specified key in the specified property list.
     *
     * @param properties the property list from which to read the value
     * @param key the key of the property
     * @param separator the delimiter to use when splitting the single string property into a list of strings
     * @param defaultValue a default value if the specified key does not exist in the property list
     * @return a list of strings parsed from the value with the specified key, or the default value if the specified
     * key does not exist in the property list
     */
    private List<String> getStringListProperty(Properties properties, String key, String separator, List<String> defaultValue) {
        if (properties.containsKey(key)) {
            return Arrays.stream(properties.getProperty(key).split(separator)).filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());
        }
        return defaultValue;
    }

    /**
     * Writes settings to the given path.
     *
     * @param path the path to which to write the settings
     */
    public void save(Path path) {
        // Miscellaneous settings
        properties.setProperty("ShimejiEENameOverride", shimejiEeNameOverride.trim());
        properties.setProperty("ActiveShimeji", String.join("/", activeImageSets));
        properties.setProperty("InformationDismissed", String.join("/", informationDismissed));

        // Settings in tray menu and mascot context menu
        properties.setProperty("Language", language.toLanguageTag());
        if (!disabledBehaviors.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : disabledBehaviors.entrySet()) {
                properties.setProperty("DisabledBehaviours." + entry.getKey(), String.join("/", entry.getValue()));
            }
        }
        for (String key : properties.stringPropertyNames()) {
            // Make sure the key's length is longer than "DisabledBehaviours." to prevent an IndexOutOfBoundsException
            if (key.startsWith("DisabledBehaviours.") && key.length() > "DisabledBehaviours.".length()) {
                String imageSet = key.substring(key.indexOf('.') + 1);
                if (!disabledBehaviors.containsKey(imageSet)) {
                    properties.remove(key);
                }
            }
        }
        properties.setProperty("Breeding", String.valueOf(breeding));
        properties.setProperty("Transients", String.valueOf(transients));
        properties.setProperty("Transformation", String.valueOf(transformation));
        properties.setProperty("Throwing", String.valueOf(throwing));
        properties.setProperty("Sounds", String.valueOf(sounds));
        properties.setProperty("Multiscreen", String.valueOf(multiscreen));

        // General settings
        properties.setProperty("ShowTrayIcon", String.valueOf(showTrayIcon));
        properties.setProperty("AlwaysShowShimejiChooser", String.valueOf(alwaysShowShimejiChooser));
        properties.setProperty("AlwaysShowInformationScreen", String.valueOf(alwaysShowInformationScreen));
        properties.setProperty("DrawShimejiBounds", String.valueOf(drawShimejiBounds));
        /*
        BUG: Using switch statements in this method seem to sometimes cause NoClassDefFoundError (likely related to the
        fact that this method is only called when the program is shutting down), so we have to use if statements instead.
        https://stackoverflow.com/questions/24473247/java-enum-noclassdeffounderror
         */
        if (filter == Filter.NEAREST_NEIGHBOUR) {
            properties.setProperty("Filter", "nearest");
        } else if (filter == Filter.BICUBIC) {
            properties.setProperty("Filter", "bicubic");
        } else if (filter == Filter.HQX) {
            properties.setProperty("Filter", "hqx");
        }
        properties.setProperty("Opacity", String.valueOf(opacity));
        properties.setProperty("Scaling", String.valueOf(scaling));

        // Interactive window settings
        properties.setProperty("InteractiveWindows", String.join("/", interactiveWindows));
        properties.setProperty("InteractiveWindowsBlacklist", String.join("/", interactiveWindowsBlacklist));

        // Window mode settings
        properties.setProperty("Environment", windowedMode ? "virtual" : "generic");
        properties.setProperty("WindowSize", windowSize.width + "x" + windowSize.height);
        properties.setProperty("Background", String.format("#%02X%02X%02X", backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue()));
        if (backgroundMode == VirtualContentPanel.ResizeMode.CENTRE) {
            properties.setProperty("BackgroundMode", "centre");
        } else if (backgroundMode == VirtualContentPanel.ResizeMode.FILL) {
            properties.setProperty("BackgroundMode", "fill");
        } else if (backgroundMode == VirtualContentPanel.ResizeMode.FIT) {
            properties.setProperty("BackgroundMode", "fit");
        } else if (backgroundMode == VirtualContentPanel.ResizeMode.STRETCH) {
            properties.setProperty("BackgroundMode", "stretch");
        }
        properties.setProperty("BackgroundImage", backgroundImage == null ? "" : backgroundImage.toString());

        try (OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "Shimeji-ee Configuration Options");
        } catch (IOException e) {
            log.error("Failed to save settings", e);
        }
    }
}
