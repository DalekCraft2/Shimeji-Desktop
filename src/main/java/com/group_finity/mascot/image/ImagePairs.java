package com.group_finity.mascot.image;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shimeji-ee Group
 */
public class ImagePairs {
    private static final Map<String, ImagePair> imagePairs = new ConcurrentHashMap<>();

    public static boolean contains(String key) {
        return imagePairs.containsKey(key);
    }

    public static ImagePair getImagePair(String key) {
        if (!imagePairs.containsKey(key)) {
            return null;
        }
        return imagePairs.get(key);
    }

    public static MascotImage getImage(String key, boolean isLookRight) {
        if (!imagePairs.containsKey(key)) {
            return null;
        }
        return imagePairs.get(key).getImage(isLookRight);
    }

    public static void put(final String key, final ImagePair imagePair) {
        if (!imagePairs.containsKey(key)) {
            imagePairs.put(key, imagePair);
        }
    }

    public static void removeAll(String searchTerm) {
        if (imagePairs.isEmpty()) {
            return;
        }

        imagePairs.keySet().removeIf(key -> searchTerm.equals(Path.of(key).getParent().toString()));
    }

    public static void clear() {
        imagePairs.clear();
    }
}