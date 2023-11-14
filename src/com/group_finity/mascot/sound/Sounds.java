package com.group_finity.mascot.sound;

import com.group_finity.mascot.Main;

import javax.sound.sampled.Clip;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.stream.Collectors;

/**
 * This static class contains all the sounds loaded by Shimeji-ee.
 * <p>
 * Visit <a href="https://kilkakon.com/shimeji">kilkakon.com/shimeji</a> for updates
 *
 * @author Kilkakon
 */
public class Sounds {
    private final static Hashtable<String, Clip> SOUNDS = new Hashtable<>();

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

    public static ArrayList<Clip> getSoundsIgnoringVolume(String filename) {
        return SOUNDS.keySet().stream().filter(soundName -> soundName.startsWith(filename)).map(SOUNDS::get).collect(Collectors.toCollection(() -> new ArrayList<>(5)));
    }

    public static boolean isMuted() {
        return !Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Sounds", "true"));
    }

    public static void setMuted(boolean mutedFlag) {
        if (mutedFlag) {
            // mute everything
            for (String s : SOUNDS.keySet()) {
                SOUNDS.get(s).stop();
            }
        }
    }
}
