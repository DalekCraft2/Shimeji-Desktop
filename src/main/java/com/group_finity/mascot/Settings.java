package com.group_finity.mascot;

import com.group_finity.mascot.image.Filter;
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
 * @author DalekCraft
 */
public class Settings {
    private static final Logger log = LoggerFactory.getLogger(Settings.class);

    private final Properties properties = new Properties();

    public String shimejiEeNameOverride = "";
    public List<String> activeImageSets = new ArrayList<>();
    public List<String> informationDismissed = new ArrayList<>();

    public Locale language = Locale.getDefault();
    public Map<String, List<String>> disabledBehaviors = new HashMap<>();
    public boolean breeding = true;
    public boolean transients = true;
    public boolean transformation = true;
    public boolean throwing = true;
    public boolean sounds = true;
    public boolean multiscreen = true;

    public boolean showTrayIcon = true;
    public boolean alwaysShowShimejiChooser = false;
    public boolean alwaysShowInformationScreen = false;
    public boolean drawShimejiBounds = false;
    public Filter filter = Filter.NEAREST_NEIGHBOUR;
    public double opacity = 1.0;
    public double scaling = 1.0;

    public List<String> interactiveWindows = new ArrayList<>();
    public List<String> interactiveWindowsBlacklist = new ArrayList<>();

    public boolean windowedMode = false;
    public Dimension windowSize = new Dimension(600, 500);
    public Color backgroundColor = Color.GREEN;
    public Path backgroundImage = null;
    public String backgroundMode = "centre";

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

        // Settings in tray menu and mascot popup menu
        language = Locale.forLanguageTag(properties.getProperty("Language", Locale.getDefault().toLanguageTag()));
        disabledBehaviors.clear();
        for (String key : properties.stringPropertyNames()) {
            // Make sure the key's length is longer than "DisabledBehaviours." to prevent an IndexOutOfBoundsException
            if (key.startsWith("DisabledBehaviours.") && key.length() > "DisabledBehaviours.".length()) {
                String imageSet = key.substring(key.indexOf('.') + 1);
                List<String> list = getStringListProperty(properties, key, "/");
                if (!list.isEmpty())
                    disabledBehaviors.put(imageSet, list);
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
        String backgroundImageString = properties.getProperty("BackgroundImage");
        backgroundImage = backgroundImageString == null || backgroundImageString.isEmpty() ? null : Path.of(properties.getProperty("BackgroundImage"));
        backgroundMode = properties.getProperty("BackgroundMode", "centre");
    }

    private boolean getBooleanProperty(Properties properties, String key, boolean defaultValue) {
        if (properties.containsKey(key)) {
            return Boolean.parseBoolean(properties.getProperty(key));
        } else {
            return defaultValue;
        }
    }

    private int getIntProperty(Properties properties, String key, int defaultValue) {
        if (properties.containsKey(key)) {
            try {
                return Integer.parseInt(properties.getProperty(key));
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private double getDoubleProperty(Properties properties, String key, double defaultValue) {
        if (properties.containsKey(key)) {
            try {
                return Double.parseDouble(properties.getProperty(key));
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private int[] getIntArrayProperty(Properties properties, String key, String separator, int[] defaultValue) {
        if (properties.containsKey(key)) {
            try {
                String[] splitArray = properties.getProperty(key).split(separator);
                return Arrays.stream(splitArray).mapToInt(Integer::parseInt).toArray();
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private List<String> getStringListProperty(Properties properties, String key, String separator) {
        return Arrays.stream(properties.getProperty(key).split(separator)).filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());
    }

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

        // Settings in tray menu and mascot popup menu
        properties.setProperty("Language", language.toLanguageTag());
        for (Map.Entry<String, List<String>> entry : disabledBehaviors.entrySet()) {
            properties.setProperty("DisabledBehaviours." + entry.getKey(), String.join("/", entry.getValue()));
        }
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("DisabledBehaviours.") && !disabledBehaviors.containsKey(key)) {
                properties.remove(key);
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
        switch (filter) {
            case NEAREST_NEIGHBOUR:
                properties.setProperty("Filter", "nearest");
                break;
            case BICUBIC:
                properties.setProperty("Filter", "bicubic");
                break;
            case HQX:
                properties.setProperty("Filter", "hqx");
                break;
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
        properties.setProperty("BackgroundMode", backgroundMode);
        properties.setProperty("BackgroundImage", backgroundImage == null ? "" : backgroundImage.toString());

        try (OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "Shimeji-ee Configuration Options");
        } catch (IOException e) {
            log.error("Failed to save settings", e);
        }
    }
}
