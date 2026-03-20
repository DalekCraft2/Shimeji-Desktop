package com.group_finity.mascot.sound;

import com.group_finity.mascot.Main;

import javax.sound.sampled.Clip;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This static class contains all the sounds loaded by Shimeji-ee.
 *
 * @author Kilkakon
 * @since 1.0.9
 */
public class Sounds {
    /**
     * A map that maps keys with the format "fileName:volume" to sound clips.
     */
    private static final Map<String, Clip> SOUNDS = new ConcurrentHashMap<>();

    /**
     * A map that maps sound file names to lists of all sound clips associated with that sound file.
     */
    private static final Map<String, List<String>> FILE_NAME_MAP = new ConcurrentHashMap<>();

    public static boolean contains(String key) {
        return SOUNDS.containsKey(key);
    }

    public static Clip getSound(String key) {
        if (!SOUNDS.containsKey(key)) {
            return null;
        }
        return SOUNDS.get(key);
    }

    public static List<Clip> getSoundsIgnoringVolume(String fileName) {
        return FILE_NAME_MAP.get(fileName).stream().map(SOUNDS::get).collect(Collectors.toList());
    }

    public static void put(final String key, final Clip clip) {
        if (!SOUNDS.containsKey(key)) {
            SOUNDS.put(key, clip);
            String fileName = key.substring(0, key.lastIndexOf(':'));
            FILE_NAME_MAP.putIfAbsent(fileName, new ArrayList<>(4));
            FILE_NAME_MAP.get(fileName).add(key);
        }
    }

    public static void clear() {
        for (Clip clip : SOUNDS.values()) {
            clip.close();
        }
        SOUNDS.clear();
        FILE_NAME_MAP.clear();
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Sounds", "true"));
    }

    public static void stopAll() {
        // mute everything
        for (Clip clip : SOUNDS.values()) {
            clip.stop();
        }
    }
}
