package com.group_finity.mascot.animation;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.image.ImagePairs;

/**
 * Represents a pose used in mascot animations.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 * @param imageKey the key of the {@link com.group_finity.mascot.image.ImagePair ImagePair} used for this pose
 * @param dx the x-component of the velocity the mascot should have when this pose is active
 * @param dy the y-component of the velocity the mascot should have when this pose is active
 * @param duration the duration of this pose, in ticks
 * @param soundKey the key of an optional sound to play when this pose becomes active
 */
public record Pose(String imageKey, int dx, int dy, int duration, String soundKey) {
    /**
     * Applies this pose to the given mascot. The mascot will be moved by the amount specified
     * in the pose's {@link #dx} and {@link #dy} values, its image will be set to the
     * {@link com.group_finity.mascot.image.ImagePair ImagePair} that corresponds to the pose's {@link #imageKey}, and
     * its sound will be set to the sound corresponding to the pose's {@link #soundKey}.
     *
     * @param mascot the mascot to which to apply this pose
     */
    public void next(final Mascot mascot) {
        mascot.getAnchor().translate(mascot.isLookRight() ? -dx : dx, dy);
        mascot.setImage(imageKey == null || !ImagePairs.contains(imageKey) ? null :
                ImagePairs.get(imageKey).getImage(mascot.isLookRight()));
        mascot.setSound(soundKey);
    }
}