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
    private final String imageKey;
    private final int dx;
    private final int dy;
    private final int duration;
    private final String soundKey;

    public Pose(final String imageKey, final int dx, final int dy, final int duration, final String soundKey) {
        this.imageKey = imageKey;
        this.dx = dx;
        this.dy = dy;
        this.duration = duration;
        this.soundKey = soundKey;
    }

    @Override
    public String toString() {
        return "Pose[imageKey=" + imageKey + ",dx=" + dx + ",dy=" + dy + ",duration=" + duration + ",soundKey=" + soundKey + "]";
    }

    public void next(final Mascot mascot) {
        mascot.setAnchor(new Point(mascot.getAnchor().x + (mascot.isLookRight() ? -dx : dx),
                mascot.getAnchor().y + dy));
        mascot.setImage(ImagePairs.getImage(getImageKey(), mascot.isLookRight()));
        mascot.setSound(soundKey);
    }

    public String getImageKey() {
        return imageKey;
    }

    public ImagePair getImage() {
        return ImagePairs.getImagePair(getImageKey());
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

    public String getSoundKey() {
        return soundKey;
    }
}