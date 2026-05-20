package com.group_finity.mascot.animation;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.util.List;

/**
 * Represents a sequence of {@link Pose Poses} that can be applied to a mascot when a certain condition is met.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Animation {
    /**
     * The condition that must pass for this animation to become active.
     */
    private final Variable condition;

    /**
     * A sequence of poses through which this animation will iterate when it is active. Must not be empty.
     */
    private final List<Pose> poses;

    /**
     * The hotspots that this animation will apply to a mascot when it is active.
     */
    private final List<Hotspot> hotspots;

    /**
     * Whether this animation is used for when a mascot changes walking direction.
     */
    private final boolean turn;

    /**
     * The duration of this animation. This value will equal the sum of the durations in {@link #poses}.
     */
    private final int duration;

    /**
     * Creates a new Animation.
     *
     * @param condition the condition that must pass for this animation to become active
     * @param poses a sequence of poses through which this animation will iterate when it is active. Must not be empty.
     * @param hotspots the hotspots that this animation will apply to a mascot when it is active
     * @param turn whether this animation is used for when a mascot changes walking direction
     * @param duration the duration of this animation. This value is expected to equal the sum of the durations in
     * {@code poses}.
     * @throws IllegalArgumentException if {@code poses} is empty
     */
    public Animation(final Variable condition, final List<Pose> poses, final List<Hotspot> hotspots, final boolean turn, final int duration) {
        if (poses.isEmpty()) {
            throw new IllegalArgumentException("poses.size==0");
        }

        this.condition = condition;
        this.poses = poses;
        this.hotspots = hotspots;
        this.turn = turn;
        this.duration = duration;
    }

    /**
     * Initializes this animation's condition.
     */
    public void init() {
        condition.init();
    }

    /**
     * Clears the cached value of whether this animation's condition passed.
     * Called at the start of each frame.
     */
    public void resetCondition() {
        condition.resetValue();
    }

    /**
     * Checks whether this animation's condition passes using the supplied context.
     *
     * @param variables the context to use when evaluating this animation's condition
     * @return {@code true} if the condition passed, otherwise {@code false}
     * @throws VariableException if the condition fails to be evaluated
     */
    public boolean isEffective(final VariableMap variables) throws VariableException {
        return (Boolean) condition.get(variables);
    }

    /**
     * Retrieves the pose corresponding to the given time and applies it to the given mascot.
     *
     * @param mascot the mascot to which to apply the pose
     * @param time the number of ticks since this animation began
     */
    public void apply(final Mascot mascot, final int time) {
        getPoseAt(time).apply(mascot);
    }

    /**
     * Gets the pose corresponding to the given time.
     *
     * @param time the number of ticks since this animation began
     * @return the pose corresponding to the given time
     */
    public Pose getPoseAt(int time) {
        time %= duration;

        for (final Pose pose : poses) {
            time -= pose.duration();
            if (time < 0) {
                return pose;
            }
        }

        return null;
    }

    /**
     * Gets the duration of this animation. This value will equal the sum of the durations in this animation's poses.
     *
     * @return the duration of this animation
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Gets the hotspots that this animation will apply to a mascot when it is active.
     *
     * @return the hotspots that this animation will apply to a mascot
     */
    public List<Hotspot> getHotspots() {
        return hotspots;
    }

    /**
     * Gets whether this animation is used for when a mascot changes walking direction.
     *
     * @return {@code true} if this animation is used for turning, otherwise {@code false}
     */
    public boolean isTurn() {
        return turn;
    }
}
