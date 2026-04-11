package com.group_finity.mascot.sound;

import com.group_finity.mascot.Main;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Loads and stores sounds.
 *
 * @author Kilkakon
 * @since 1.0.9
 */
public class Sounds {
    /**
     * A map of strings (in the format "fileName:volume") and sound clips.
     */
    private static final Map<String, Clip> SOUNDS = new ConcurrentHashMap<>();

    /**
     * A map of sound file names and lists of all sound clips associated with that sound file.
     */
    private static final Map<String, List<String>> FILE_NAME_MAP = new ConcurrentHashMap<>();

    /**
     * A map that stores which image sets use the sound with the given key.
     */
    private static final Map<String, List<String>> SOUNDS_TO_IMAGESETS = new ConcurrentHashMap<>();

    /**
     * A map that stores which sounds are used by the given image set.
     */
    private static final Map<String, List<String>> IMAGESETS_TO_SOUNDS = new ConcurrentHashMap<>();

    /**
     * Loads a sound.
     *
     * @param fileName file path of the sound to load
     * @param volume the volume of the sound
     * @return a key to access the loaded sound
     * @throws IOException if an error occurs when reading the sound file or when creating an {@code AudioInputStream}
     * @throws UnsupportedAudioFileException if the sound file uses a format that is not recognized by the system
     * @throws LineUnavailableException if there is no {@link Clip} object available to use to load the sound
     */
    public static String load(final String fileName, final float volume) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        String key = fileName + ":" + volume;
        if (SOUNDS.containsKey(key)) {
            return key;
        }

        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(fileName));
        final Clip clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(volume);
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                ((Clip) event.getLine()).stop();
            }
        });

        SOUNDS.put(key, clip);
        FILE_NAME_MAP.putIfAbsent(fileName, new ArrayList<>(4));
        FILE_NAME_MAP.get(fileName).add(key);

        return key;
    }

    public static boolean contains(String key) {
        return SOUNDS.containsKey(key);
    }

    public static Clip get(String key) {
        return key == null ? null : SOUNDS.get(key);
    }

    public static List<Clip> getAllByFile(String fileName) {
        return FILE_NAME_MAP.get(fileName).stream().map(SOUNDS::get).collect(Collectors.toList());
    }

    /**
     * Marks a sound as being used by the given image set.
     *
     * @param sound the key of a sound clip, in the format "fileName:volume"
     * @param imageSet the name of the image set that uses the sound
     */
    public static void addUsage(String sound, String imageSet) {
        SOUNDS_TO_IMAGESETS.putIfAbsent(sound, new ArrayList<>(4));
        IMAGESETS_TO_SOUNDS.putIfAbsent(imageSet, new ArrayList<>(4));
        if (!SOUNDS_TO_IMAGESETS.get(sound).contains(imageSet)) {
            SOUNDS_TO_IMAGESETS.get(sound).add(imageSet);
            IMAGESETS_TO_SOUNDS.get(imageSet).add(sound);
        }
    }

    /**
     * Marks a sound as not being used by the given image set.
     * If the given sound is no longer used by any image sets after the operation, it is unloaded.
     *
     * @param sound the key of the sound clip, in the format "fileName:volume"
     * @param imageSet the name of the image set that no longer uses the sound
     */
    public static void removeUsage(String sound, String imageSet) {
        List<String> usages = SOUNDS_TO_IMAGESETS.get(sound);
        usages.remove(imageSet);
        if (usages.isEmpty()) {
            // If there are no more usages of this particular sound clip, remove it from the other two maps
            SOUNDS_TO_IMAGESETS.remove(sound);
            SOUNDS.remove(sound).close();
            String fileName = sound.substring(0, sound.lastIndexOf(':'));
            FILE_NAME_MAP.get(fileName).remove(sound);
            if (FILE_NAME_MAP.get(fileName).isEmpty()) {
                FILE_NAME_MAP.remove(fileName);
            }
        }
    }

    public static void removeAll(String searchTerm) {
        if (!IMAGESETS_TO_SOUNDS.containsKey(searchTerm)) {
            return;
        }

        /* Because sounds can be loaded from a global sounds folder, it's possible that some image sets share common
         * sounds. This means we can't indiscriminately unload all sounds used by this image set like
         * ImagePairs.removeAll() does with image pairs; instead, we have to mark each sound as no longer being used by
         * the image set, and then unload them only if no other image sets are using them.
         */
        for (String sound : IMAGESETS_TO_SOUNDS.get(searchTerm)) {
            removeUsage(sound, searchTerm);
        }

        IMAGESETS_TO_SOUNDS.remove(searchTerm);
    }

    public static void clear() {
        for (Clip clip : SOUNDS.values()) {
            clip.close();
        }
        SOUNDS.clear();
        FILE_NAME_MAP.clear();
        SOUNDS_TO_IMAGESETS.clear();
        IMAGESETS_TO_SOUNDS.clear();
    }

    public static boolean isEnabled() {
        return Main.getInstance().getSettings().sounds;
    }

    public static void stopAll() {
        // mute everything
        for (Clip clip : SOUNDS.values()) {
            clip.stop();
        }
    }
}
