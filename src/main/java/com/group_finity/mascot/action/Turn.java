package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Kilkakon
 * @since 1.0.8
 */
public class Turn extends BorderedAction {
    private static final Logger log = LoggerFactory.getLogger(Turn.class);

    private static final String PARAMETER_LOOKRIGHT = "LookRight";

    private boolean turning = false;

    public Turn(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public boolean hasNext() throws VariableException {
        turning = turning || isLookRight() != getMascot().isLookRight();
        final boolean inTime = getTime() < getAnimation().getDuration();

        return super.hasNext() && inTime && turning;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        getMascot().setLookRight(isLookRight());

        super.tick();

        if (getBorder() != null && !getBorder().isOn(getMascot().getAnchor())) {
            throw new LostGroundException();
        }

        getAnimation().next(getMascot(), getTime());
    }

    private boolean isLookRight() throws VariableException {
        return eval(getSchema().getString(PARAMETER_LOOKRIGHT), Boolean.class, !getMascot().isLookRight());
    }
}