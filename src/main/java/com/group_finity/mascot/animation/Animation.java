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
     * The condition that must evaluate to {@code true} for this animation to become active.
     * If this is {@code null}, the condition will be treated as if it evaluates to {@code true},
     * meaning this animation will always be able to become active.
     *
     * @see #init()
     * @see #resetCondition()
     * @see #isEffective(VariableMap)
     */
    private final Variable condition;

    /**
     * A sequence of poses through which this animation will iterate when it is active. This must not be empty.
     *
     * @see #getPoseAt(int)
     */
    private final List<Pose> poses;

    /**
     * The hotspots that this animation will apply to a mascot when it is active.
     *
     * @see #getHotspots()
     */
    private final List<Hotspot> hotspots;

    /**
     * Whether this animation is used for when a mascot changes walking direction.
     *
     * @see #isTurn()
     */
    private final boolean turn;

    /**
     * The duration of this animation. This value will equal the sum of the durations in {@link #poses}.
     *
     * @see #getDuration()
     */
    private final int duration;

    /**
     * Creates a new Animation.
     *
     * @param condition the condition that must evaluate to {@code true} for this animation to become active
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
     *
     * @see Variable#init()
     */
    public void init() {
        if (condition == null) {
            return;
        }
        condition.init();
    }

    /**
     * Clears the cached value of this animation's condition so it may be reevaluated when
     * {@link #isEffective(VariableMap)} is next called.
     * Called at the start of each frame.
     *
     * @see #isEffective(VariableMap)
     * @see Variable#resetValue()
     */
    public void resetCondition() {
        if (condition == null) {
            return;
        }
        condition.resetValue();
    }

    /**
     * Checks whether this animation's condition evaluates to {@code true} using the supplied context.
     *
     * @param variables the context to use when evaluating this animation's condition
     * @return {@code true} if the condition evaluated to {@code true}, otherwise {@code false}
     * @throws VariableException if the condition fails to be evaluated
     * @see Variable#get(VariableMap)
     */
    public boolean isEffective(final VariableMap variables) throws VariableException {
        if (condition == null) {
            // Always allow the animation to be applied to a mascot if it has no condition
            return true;
        }
        return (Boolean) condition.get(variables);
    }

    /**
     * Retrieves the pose corresponding to the given time and applies it to the given mascot.
     *
     * @param mascot the mascot to which to apply the pose
     * @param time the number of ticks since this animation began
     * @see Pose#apply(Mascot)
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
     * @see Pose#duration()
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
