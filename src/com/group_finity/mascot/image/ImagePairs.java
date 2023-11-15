package com.group_finity.mascot.image;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public class ImagePairs {
    private static ConcurrentHashMap<String, ImagePair> imagePairs = new ConcurrentHashMap<>();

    public static void load(final String filename, final ImagePair imagepair) {
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

        // TODO Confirm that these two function identically
        // for (Enumeration<String> key = imagePairs.keys(); key.hasMoreElements(); ) {
        //     String filename = key.nextElement();
        //     if (searchTerm.equals(filename.split("/")[1])) {
        //         imagePairs.remove(filename);
        //     }
        // }
        imagePairs.keySet().removeIf(key -> searchTerm.equals(key.split("/")[1]));
    }

    public static MascotImage getImage(String filename, boolean isLookRight) {
        if (!imagePairs.containsKey(filename)) {
            return null;
        }
        return imagePairs.get(filename).getImage(isLookRight);
    }
}