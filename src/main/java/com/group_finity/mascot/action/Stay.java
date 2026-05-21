package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.script.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;

/**
 * An action that does not move.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Stay extends BorderedAction {
    private static final Logger log = LoggerFactory.getLogger(Stay.class);

    public Stay(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        if (getBorder() != null && !getBorder().isOn(getMascot().getAnchor())) {
            throw new LostGroundException("Mascot is not on border");
        }

        // Animate
        getAnimation().apply(getMascot(), getTime());
    }
}
