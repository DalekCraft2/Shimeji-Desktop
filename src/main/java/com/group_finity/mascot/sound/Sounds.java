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
public final class Sounds {
    /**
     * A map of keys and sound clips.
     * A sound clip's key is returned by {@link #load} when that sound clip is loaded.
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

    private Sounds() {
        throw new UnsupportedOperationException("Sounds is a static class and cannot be instantiated");
    }

    /**
     * Loads a sound.
     *
     * @param fileName file path of the sound to load
     * @param volume the volume of the sound
     * @return a key to access the loaded sound
     * @throws LineUnavailableException if there is no {@link Clip} object available to use to load the sound
     * @throws UnsupportedAudioFileException if the sound file uses a format that is not recognized by the system
     * @throws IOException if an error occurs when creating an {@code AudioInputStream} or when reading the sound file
     */
    public static String load(final String fileName, final float volume) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        String key = fileName + ":" + volume;
        if (SOUNDS.containsKey(key)) {
            return key;
        }

        final Clip clip = AudioSystem.getClip();
        /* Normally, I would use a Path object here instead of a File object because I prefer java.nio over java.io,
        but AudioSystem.getAudioInputStream(InputStream) makes use of InputStream's reset() method, and InputStreams
        that are returned by Files.newInputStream(Path) do not support that method. Unfortunate! */
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(fileName))) {
            clip.open(audioInputStream);
            ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(volume);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    ((Clip) event.getLine()).stop();
                }
            });
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | RuntimeException e) {
            clip.close();
            throw e;
        }

        SOUNDS.put(key, clip);
        if (!FILE_NAME_MAP.containsKey(fileName)) {
            FILE_NAME_MAP.put(fileName, new ArrayList<>(4));
        }
        FILE_NAME_MAP.get(fileName).add(key);

        return key;
    }

    /**
     * Checks whether there is a sound associated with the given key.
     *
     * @param key the key whose presence is to be checked
     * @return whether the key has an associated sound
     */
    public static boolean contains(String key) {
        return SOUNDS.containsKey(key);
    }

    /**
     * Gets the sound associated with the given key.
     *
     * @param key the key whose associated sound is to be returned
     * @return the key's associated sound
     */
    public static Clip get(String key) {
        return key == null ? null : SOUNDS.get(key);
    }

    /**
     * Gets all sounds that were loaded from the specified file path.
     * These sounds will all have different volumes.
     *
     * @param fileName the file path whose associated sounds should be returned
     * @return all sounds that were loaded from the specified file path
     */
    public static List<Clip> getAllByFile(String fileName) {
        if (FILE_NAME_MAP.get(fileName).isEmpty()) {
            return List.of();
        }
        return FILE_NAME_MAP.get(fileName).stream().map(SOUNDS::get).collect(Collectors.toList());
    }

    /**
     * Marks a sound as being used by the given image set.
     *
     * @param sound the key of a sound clip
     * @param imageSet the name of the image set that uses the sound
     */
    public static void addUsage(String sound, String imageSet) {
        if (!SOUNDS_TO_IMAGESETS.containsKey(sound)) {
            SOUNDS_TO_IMAGESETS.put(sound, new ArrayList<>(4));
        }
        if (!IMAGESETS_TO_SOUNDS.containsKey(imageSet)) {
            IMAGESETS_TO_SOUNDS.put(imageSet, new ArrayList<>(4));
        }
        if (!SOUNDS_TO_IMAGESETS.get(sound).contains(imageSet)) {
            SOUNDS_TO_IMAGESETS.get(sound).add(imageSet);
            IMAGESETS_TO_SOUNDS.get(imageSet).add(sound);
        }
    }

    /**
     * Marks a sound as not being used by the given image set.
     * If the given sound is no longer used by any image sets after the operation, it is unloaded.
     *
     * @param sound the key of the sound clip
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

    /**
     * Removes all sounds attributed to the given image set.
     *
     * @param imageSet the image set whose sounds should be removed
     */
    public static void removeAll(String imageSet) {
        if (!IMAGESETS_TO_SOUNDS.containsKey(imageSet)) {
            return;
        }

        /* Because sounds can be loaded from a global sounds folder, it's possible that some image sets share common
         * sounds. This means we can't indiscriminately unload all sounds used by this image set like
         * ImagePairs.removeAll() does with image pairs; instead, we have to mark each sound as no longer being used by
         * the image set, and then unload them only if no other image sets are using them.
         */
        for (String sound : IMAGESETS_TO_SOUNDS.get(imageSet)) {
            removeUsage(sound, imageSet);
        }

        IMAGESETS_TO_SOUNDS.remove(imageSet);
    }

    /**
     * Removes all currently loaded sounds.
     */
    public static void clear() {
        if (!SOUNDS.isEmpty()) {
            for (Clip clip : SOUNDS.values()) {
                clip.close();
            }
            SOUNDS.clear();
        }
        FILE_NAME_MAP.clear();
        SOUNDS_TO_IMAGESETS.clear();
        IMAGESETS_TO_SOUNDS.clear();
    }

    /**
     * Checks whether sounds are enabled in the program settings.
     *
     * @return whether sounds are enabled
     */
    public static boolean isEnabled() {
        return Main.getInstance().getSettings().sounds;
    }

    /**
     * Stops all sounds that are currently playing.
     */
    public static void stopAll() {
        if (SOUNDS.isEmpty()) {
            return;
        }

        // mute everything
        for (Clip clip : SOUNDS.values()) {
            clip.stop();
        }
    }
}
