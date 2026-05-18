package com.group_finity.mascot.animation;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.image.ImagePairs;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public record Pose(String imageKey, int dx, int dy, int duration, String soundKey) {
    public void next(final Mascot mascot) {
        mascot.getAnchor().translate(mascot.isLookRight() ? -dx : dx, dy);
        mascot.setImage(imageKey == null || !ImagePairs.contains(imageKey) ? null :
                ImagePairs.get(imageKey).getImage(mascot.isLookRight()));
        mascot.setSound(soundKey);
    }
}