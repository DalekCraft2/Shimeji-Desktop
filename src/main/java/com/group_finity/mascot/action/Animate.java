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
 * An action that simply executes an animation.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Animate extends BorderedAction {

    private static final Logger log = LoggerFactory.getLogger(Animate.class);

    public Animate(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        if (getBorder() != null && !getBorder().isOn(getMascot().getAnchor())) {
            // Mascot is off the ground
            throw new LostGroundException();
        }

        // Animate
        getAnimation().next(getMascot(), getTime());
    }

    @Override
    public boolean hasNext() throws VariableException {
        final boolean inTime = getTime() < getAnimation().getDuration();

        return super.hasNext() && inTime;
    }
}
