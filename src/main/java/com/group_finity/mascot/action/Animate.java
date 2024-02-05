package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * An action that simply executes an animation.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Animate extends BorderedAction {

    private static final Logger log = Logger.getLogger(Animate.class.getName());

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
