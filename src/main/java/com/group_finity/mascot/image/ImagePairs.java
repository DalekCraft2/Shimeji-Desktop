package com.group_finity.mascot.image;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shimeji-ee Group
 */
public class ImagePairs {
    private static final Map<String, ImagePair> imagePairs = new ConcurrentHashMap<>();

    public static boolean contains(String fileName) {
        return imagePairs.containsKey(fileName);
    }

    public static ImagePair getImagePair(String fileName) {
        if (!imagePairs.containsKey(fileName)) {
            return null;
        }
        return imagePairs.get(fileName);
    }

    public static MascotImage getImage(String fileName, boolean isLookRight) {
        if (!imagePairs.containsKey(fileName)) {
            return null;
        }
        return imagePairs.get(fileName).getImage(isLookRight);
    }

    public static void put(final String fileName, final ImagePair imagePair) {
        if (!imagePairs.containsKey(fileName)) {
            imagePairs.put(fileName, imagePair);
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