package com.group_finity.mascot;

import com.group_finity.mascot.image.ImagePairLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.group_finity.mascot.Main.SETTINGS_FILE;

/**
 * @author DalekCraft
 */
public class Settings {
    private static final Logger log = LoggerFactory.getLogger(Settings.class);

    private Properties properties = new Properties();

    public String shimejiEeNameOverride = "";
    public List<String> activeImageSets = new ArrayList<>();
    public List<String> informationDismissed = new ArrayList<>();

    public Locale language = Locale.UK;
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
    public ImagePairLoader.Filter filter = ImagePairLoader.Filter.NEAREST_NEIGHBOUR;
    public double opacity = 1.0;
    public double scaling = 1.0;

    public List<String> interactiveWindows = new ArrayList<>();
    public List<String> interactiveWindowsBlacklist = new ArrayList<>();

    public boolean windowedMode = false;
    public Dimension windowSize = new Dimension(600, 500);
    public Color backgroundColor = Color.GREEN;
    public String backgroundImage = "";
    public String backgroundMode = "centre";

    public void load() {
        if (Files.isRegularFile(SETTINGS_FILE)) {
            try (InputStream input = Files.newInputStream(SETTINGS_FILE)) {
                properties.load(input);
            } catch (IOException e) {
                log.error("Failed to load settings", e);
            }
        }

        // Miscellaneous settings
        shimejiEeNameOverride = properties.getProperty("ShimejiEENameOverride", "").trim();
        activeImageSets = Arrays.stream(properties.getProperty("ActiveShimeji", "").split("/")).filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());
        informationDismissed = Arrays.stream(properties.getProperty("InformationDismissed", "").split("/")).filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());

        // Settings in tray menu and mascot popup menu
        language = Locale.forLanguageTag(properties.getProperty("Language", Locale.UK.toLanguageTag()));
        disabledBehaviors.clear();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("DisabledBehaviours.")) {
                String imageSet = key.substring(key.indexOf('.') + 1);
                List<String> list = Arrays.stream(properties.getProperty(key, "").split("/")).filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());
                if (!list.isEmpty())
                    disabledBehaviors.put(imageSet, list);
            }
        }
        breeding = Boolean.parseBoolean(properties.getProperty("Breeding", "true"));
        transients = Boolean.parseBoolean(properties.getProperty("Transients", "true"));
        transformation = Boolean.parseBoolean(properties.getProperty("Transformation", "true"));
        throwing = Boolean.parseBoolean(properties.getProperty("Throwing", "true"));
        sounds = Boolean.parseBoolean(properties.getProperty("Sounds", "true"));
        multiscreen = Boolean.parseBoolean(properties.getProperty("Multiscreen", "true"));

        // General settings
        showTrayIcon = Boolean.parseBoolean(properties.getProperty("ShowTrayIcon", "true"));
        alwaysShowShimejiChooser = Boolean.parseBoolean(properties.getProperty("AlwaysShowShimejiChooser", "false"));
        alwaysShowInformationScreen = Boolean.parseBoolean(properties.getProperty("AlwaysShowInformationScreen", "false"));
        String filterText = properties.getProperty("Filter", "false");
        if (filterText.equalsIgnoreCase("true") || filterText.equalsIgnoreCase("hqx")) {
            filter = ImagePairLoader.Filter.HQX;
        } else if (filterText.equalsIgnoreCase("bicubic")) {
            filter = ImagePairLoader.Filter.BICUBIC;
        } else {
            filter = ImagePairLoader.Filter.NEAREST_NEIGHBOUR;
        }
        opacity = Double.parseDouble(properties.getProperty("Opacity", "1.0"));
        scaling = Double.parseDouble(properties.getProperty("Scaling", "1.0"));

        // Interactive window settings
        interactiveWindows = Arrays.stream(properties.getProperty("InteractiveWindows", "").split("/")).filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());
        interactiveWindowsBlacklist = Arrays.stream(properties.getProperty("InteractiveWindowsBlacklist", "").split("/")).filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());

        // Window mode settings
        windowedMode = properties.getProperty("Environment", "generic").equals("virtual");
        String[] windowArray = properties.getProperty("WindowSize", "600x500").split("x");
        windowSize = new Dimension(Integer.parseInt(windowArray[0]), Integer.parseInt(windowArray[1]));
        backgroundColor = Color.decode(properties.getProperty("Background", "#00FF00"));
        backgroundImage = properties.getProperty("BackgroundImage", "");
        backgroundMode = properties.getProperty("BackgroundMode", "centre");
    }

    private void saveImpl() {
        try (OutputStream output = Files.newOutputStream(SETTINGS_FILE)) {
            properties.store(output, "Shimeji-ee Configuration Options");
        } catch (IOException e) {
            log.error("Failed to save settings", e);
        }
    }

    /**
     * Saves all settings.
     */
    public void save() {
        // Miscellaneous settings
        properties.setProperty("ShimejiEENameOverride", shimejiEeNameOverride.trim());
        properties.setProperty("ActiveShimeji", String.join("/", activeImageSets));
        properties.setProperty("InformationDismissed", String.join("/", informationDismissed));

        savePopupSettingsImpl();

        saveUserSettingsImpl();

        saveImpl();
    }

    public void saveActiveImageSets() {
        properties.setProperty("ActiveShimeji", String.join("/", activeImageSets));

        saveImpl();
    }

    public void saveInformationDismissed() {
        properties.setProperty("InformationDismissed", String.join("/", informationDismissed));

        saveImpl();
    }

    /**
     * Saves the settings that are accessible through the tray icon menu and mascot popup menu.
     */
    public void savePopupSettings() {
        savePopupSettingsImpl();
        saveImpl();
    }

    private void savePopupSettingsImpl() {
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
    }

    /**
     * Saves the settings that are accessible through the settings window.
     * (I would have named this "saveSettingsSettings" if that didn't sound terrible.)
     */
    public void saveUserSettings() {
        saveUserSettingsImpl();
        saveImpl();
    }

    private void saveUserSettingsImpl() {
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
        properties.setProperty("BackgroundImage", backgroundImage == null ? "" : backgroundImage);
    }
}
