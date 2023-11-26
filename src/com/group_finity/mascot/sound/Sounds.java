package com.group_finity.mascot.sound;

import com.group_finity.mascot.Main;

import javax.sound.sampled.Clip;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This static class contains all the sounds loaded by Shimeji-ee.
 * <p>
 * Visit <a href="https://kilkakon.com/shimeji">kilkakon.com/shimeji</a> for updates
 *
 * @author Kilkakon
 */
public class Sounds {
    private final static ConcurrentHashMap<String, Clip> SOUNDS = new ConcurrentHashMap<>();

    public static void load(final String filename, final Clip clip) {
        if (!SOUNDS.containsKey(filename)) {
            SOUNDS.put(filename, clip);
        }
    }

    public static boolean contains(String filename) {
        return SOUNDS.containsKey(filename);
    }

    public static Clip getSound(String filename) {
        if (!SOUNDS.containsKey(filename)) {
            return null;
        }
        return SOUNDS.get(filename);
    }

    public static List<Clip> getSoundsIgnoringVolume(String filename) {
        List<Clip> sounds = new ArrayList<>(5);
        for (Map.Entry<String, Clip> entry : SOUNDS.entrySet()) {
            String soundName = entry.getKey();
            Clip soundClip = entry.getValue();
            if (soundName.startsWith(filename)) {
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
