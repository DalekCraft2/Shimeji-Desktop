package com.group_finity.mascot.animation;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.image.ImagePair;
import com.group_finity.mascot.image.ImagePairs;

import java.awt.*;
import java.nio.file.Path;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Pose {
    private final Path image;
    private final Path rightImage;
    private final int dx;
    private final int dy;
    private final int duration;
    private final String sound;

    public Pose(final Path image) {
        this(image, null, 0, 0, 1);
    }

    public Pose(final Path image, final int duration) {
        this(image, null, 0, 0, duration);
    }

    public Pose(final Path image, final int dx, final int dy, final int duration) {
        this(image, null, dx, dy, duration);
    }

    public Pose(final Path image, final Path rightImage) {
        this(image, rightImage, 0, 0, 1);
    }

    public Pose(final Path image, final Path rightImage, final int duration) {
        this(image, rightImage, 0, 0, duration);
    }

    public Pose(final Path image, final Path rightImage, final int dx, final int dy, final int duration) {
        this(image, rightImage, dx, dy, duration, null);
    }

    public Pose(final Path image, final Path rightImage, final int dx, final int dy, final int duration, final String sound) {
        this.image = image;
        this.rightImage = rightImage;
        this.dx = dx;
        this.dy = dy;
        this.duration = duration;
        this.sound = sound;
    }

    @Override
    public String toString() {
        return "Pose(" + (getImage() == null ? "" : getImage()) + "," + dx + "," + dy + "," + duration + ", " + sound + ")";
    }

    public void next(final Mascot mascot) {
        mascot.setAnchor(new Point(mascot.getAnchor().x + (mascot.isLookRight() ? -dx : dx),
                mascot.getAnchor().y + dy));
        mascot.setImage(ImagePairs.getImage(getImageName(), mascot.isLookRight()));
        mascot.setSound(sound);
    }

    public String getImageName() {
        return (image == null ? "" : image.toString()) + (rightImage == null ? "" : rightImage.toString());
    }

    public ImagePair getImage() {
        return ImagePairs.getImagePair(getImageName());
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public int getDuration() {
        return duration;
    }

    public String getSoundName() {
        return sound;
    }
}