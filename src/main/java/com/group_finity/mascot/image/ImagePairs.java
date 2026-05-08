package com.group_finity.mascot.image;

import com.group_finity.mascot.Main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and stores image pairs.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public final class ImagePairs {
    private static final Map<String, ImagePair> imagePairs = new ConcurrentHashMap<>();

    private ImagePairs() {
        throw new UnsupportedOperationException("ImagePairs is a static class and cannot be instantiated");
    }

    /**
     * Loads an image pair.
     *
     * @param path file path of left-facing image to load
     * @param rightPath file path of right-facing image to load. If {@code null}, the left-facing image will be copied
     * and horizontally flipped.
     * @param center image center coordinate
     * @param scaling the scale factor of the image
     * @param filter the type of filter to use to generate the image
     * @param opacity the opacity of the image
     * @return a key to access the loaded image pair
     * @throws IOException if an error occurs when reading the image files or when creating an {@code InputStream}
     */
    public static String load(final Path path, final Path rightPath, final Point center, final double scaling, final Filter filter, final double opacity) throws IOException {
        String key = center.x + "," + center.y + ":" + path.toString() + (rightPath == null ? "" : ":" + rightPath);
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

        ImagePair ip = new ImagePair(new MascotImage(leftImage, new Point((int) Math.round(center.x * scaling), (int) Math.round(center.y * scaling))),
                new MascotImage(rightImage, new Point(rightImage.getWidth() - (int) Math.round(center.x * scaling), (int) Math.round(center.y * scaling))));
        imagePairs.put(key, ip);

        return key;
    }

    public static boolean contains(String key) {
        return imagePairs.containsKey(key);
    }

    public static ImagePair get(String key) {
        return key == null ? null : imagePairs.get(key);
    }

    public static void removeAll(String searchTerm) {
        if (imagePairs.isEmpty()) {
            return;
        }

        imagePairs.keySet().removeIf(key -> searchTerm.equals(Path.of(key.split(":")[1]).getParent().toString()));
    }

    public static void clear() {
        imagePairs.clear();
    }
}