package com.group_finity.mascot.sound;

import com.group_finity.mascot.Main;

import javax.sound.sampled.Clip;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This static class contains all the sounds loaded by Shimeji-ee.
 *
 * @author Kilkakon
 * @since 1.0.9
 */
public class Sounds {
    private static final ConcurrentHashMap<String, Clip> SOUNDS = new ConcurrentHashMap<>();

    public static void load(final String fileName, final Clip clip) {
        if (!SOUNDS.containsKey(fileName)) {
            SOUNDS.put(fileName, clip);
        }
    }

    public static boolean contains(String fileName) {
        return SOUNDS.containsKey(fileName);
    }

    public static Clip getSound(String fileName) {
        if (!SOUNDS.containsKey(fileName)) {
            return null;
        }
        return SOUNDS.get(fileName);
    }

    public static List<Clip> getSoundsIgnoringVolume(String fileName) {
        List<Clip> sounds = new ArrayList<>(5);
        for (Map.Entry<String, Clip> entry : SOUNDS.entrySet()) {
            String soundName = entry.getKey();
            Clip soundClip = entry.getValue();
            if (soundName.startsWith(fileName)) {
                sounds.add(soundClip);
            }
        }
        return sounds;
    }

    public static boolean isMuted() {
        return !Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Sounds", "true"));
    }

    public static void setMuted(boolean mutedFlag) {
        if (mutedFlag) {
            // mute everything
            for (Clip clip : SOUNDS.values()) {
                clip.stop();
            }
        }
    }
}
