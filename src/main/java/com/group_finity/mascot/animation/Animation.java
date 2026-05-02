package com.group_finity.mascot.animation;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;

import java.util.List;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Animation {
    private final Variable condition;
    private final List<Pose> poses;
    private final List<Hotspot> hotspots;
    private final boolean turn;
    private Integer duration = null;

    public Animation(final Variable condition, final List<Pose> poses, final List<Hotspot> hotspots, final boolean turn) {
        if (poses.isEmpty()) {
            throw new IllegalArgumentException("poses.size==0");
        }

        this.condition = condition;
        this.poses = poses;
        this.hotspots = hotspots;
        this.turn = turn;
    }

    public void init() {
        condition.init();
    }

    public void initFrame() {
        condition.initFrame();
    }

    public boolean isEffective(final VariableMap variables) throws VariableException {
        return (Boolean) condition.get(variables);
    }

    public void next(final Mascot mascot, final int time) {
        getPoseAt(time).next(mascot);
    }

    public Pose getPoseAt(int time) {
        time %= getDuration();

        for (final Pose pose : poses) {
            time -= pose.getDuration();
            if (time < 0) {
                return pose;
            }
        }

        return null;
    }

    public int getDuration() {
        if (duration == null)
            duration = poses.stream().mapToInt(Pose::getDuration).sum();
        return duration;
    }

    public List<Hotspot> getHotspots() {
        return hotspots;
    }

    public boolean isTurn() {
        return turn;
    }
}
