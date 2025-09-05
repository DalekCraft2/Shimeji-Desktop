package com.group_finity.mascot.image;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shimeji-ee Group
 */
public class ImagePairs {
    private static final ConcurrentHashMap<String, ImagePair> imagePairs = new ConcurrentHashMap<>();

    public static void put(final String filename, final ImagePair imagepair) {
        if (!imagePairs.containsKey(filename)) {
            imagePairs.put(filename, imagepair);
        }
    }

    public static ImagePair getImagePair(String filename) {
        if (!imagePairs.containsKey(filename)) {
            return null;
        }
        return imagePairs.get(filename);
    }

    public static boolean contains(String filename) {
        return imagePairs.containsKey(filename);
    }

    public static void clear() {
        imagePairs.clear();
    }

    public static void removeAll(String searchTerm) {
        if (imagePairs.isEmpty()) {
            return;
        }

        imagePairs.keySet().removeIf(key -> searchTerm.equals(Path.of(key).getParent().toString()));
    }

    public static MascotImage getImage(String filename, boolean isLookRight) {
        if (!imagePairs.containsKey(filename)) {
            return null;
        }
        return imagePairs.get(filename).getImage(isLookRight);
    }
}