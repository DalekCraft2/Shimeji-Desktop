package com.group_finity.mascot.image;

import com.group_finity.mascot.Main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and stores image pairs.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public final class ImagePairs {
    /**
     * A map of keys and image pairs.
     * An image pair's key is returned by {@link #load} when that image pair is loaded.
     */
    private static final Map<String, ImagePair> imagePairs = new ConcurrentHashMap<>();

    /**
     * A map that stores which image pairs are used by the given image set.
     */
    private static final Map<String, List<String>> IMAGESETS_TO_IMAGEPAIRS = new ConcurrentHashMap<>();

    private ImagePairs() {
        throw new UnsupportedOperationException("ImagePairs is a static class and cannot be instantiated");
    }

    /**
     * Loads an image pair.
     *
     * @param path file path of left-facing image to load
     * @param rightPath file path of right-facing image to load. If {@code null}, the left-facing image will be copied
     * and horizontally flipped.
     * @param anchorX the x-coordinate of the point on the image that aligns with the mascot's anchor
     * @param anchorY the y-coordinate of the point on the image that aligns with the mascot's anchor
     * @param scaling the scale factor of the image
     * @param filter the type of filter to use to generate the image
     * @param opacity the opacity of the image
     * @return a key to access the loaded image pair
     * @throws IOException if an error occurs when reading the image files or when creating an {@code InputStream}
     */
    public static String load(final Path path, final Path rightPath, final int anchorX, final int anchorY,
                              final double scaling, final Filter filter, final double opacity) throws IOException {
        String key = anchorX + "," + anchorY + ":" + path.toString() + (rightPath == null ? "" : ":" + rightPath);
        if (imagePairs.containsKey(key)) {
            return key;
        }

        BufferedImage leftImage;
        try (InputStream input = Files.newInputStream(Main.IMAGE_DIRECTORY.resolve(path))) {
            leftImage = ImageUtils.toCompatibleImage(ImageIO.read(input));
        }
        // Premultiply after scaling to prevent artifacts when using the hqx filter with an opacity less than 1.0
        leftImage = ImageUtils.premultiply(ImageUtils.scale(leftImage, scaling, filter), opacity);

        BufferedImage rightImage;
        if (rightPath == null) {
            rightImage = ImageUtils.flip(leftImage);
        } else {
            try (InputStream input = Files.newInputStream(Main.IMAGE_DIRECTORY.resolve(rightPath))) {
                rightImage = ImageUtils.toCompatibleImage(ImageIO.read(input));
            }
            rightImage = ImageUtils.premultiply(ImageUtils.scale(rightImage, scaling, filter), opacity);
        }

        int scaledAnchorX = (int) Math.round(anchorX * scaling);
        int scaledAnchorY = (int) Math.round(anchorY * scaling);
        ImagePair ip = new ImagePair(
                new MascotImage(leftImage, new Point(scaledAnchorX, scaledAnchorY)),
                new MascotImage(rightImage, new Point(rightImage.getWidth() - scaledAnchorX, scaledAnchorY))
        );
        imagePairs.put(key, ip);

        return key;
    }

    /**
     * Checks whether there is an image pair associated with the given key.
     *
     * @param key the key whose presence is to be checked
     * @return whether the key has an associated image pair
     */
    public static boolean contains(String key) {
        return imagePairs.containsKey(key);
    }

    /**
     * Gets the image pair associated with the given key.
     *
     * @param key the key whose associated image pair is to be returned
     * @return the key's associated image pair
     */
    public static ImagePair get(String key) {
        return key == null ? null : imagePairs.get(key);
    }

    /**
     * Marks an image pair as being used by the given image set.
     *
     * @param imagePair the key of an image pair
     * @param imageSet the name of the image set that uses the image pair
     */
    public static void addUsage(String imagePair, String imageSet) {
        if (!IMAGESETS_TO_IMAGEPAIRS.containsKey(imageSet)) {
            IMAGESETS_TO_IMAGEPAIRS.put(imageSet, new ArrayList<>());
        }
        if (!IMAGESETS_TO_IMAGEPAIRS.get(imageSet).contains(imagePair)) {
            IMAGESETS_TO_IMAGEPAIRS.get(imageSet).add(imagePair);
        }
    }

    /**
     * Removes all image pairs attributed to the given image set.
     *
     * @param imageSet the image set whose image pairs should be removed
     */
    public static void removeAll(String imageSet) {
        if (!IMAGESETS_TO_IMAGEPAIRS.containsKey(imageSet)) {
            return;
        }

        for (String imagePair : IMAGESETS_TO_IMAGEPAIRS.get(imageSet)) {
            imagePairs.remove(imagePair);
        }

        IMAGESETS_TO_IMAGEPAIRS.remove(imageSet);
    }

    /**
     * Clears all currently loaded image pairs.
     */
    public static void clear() {
        imagePairs.clear();
    }
}