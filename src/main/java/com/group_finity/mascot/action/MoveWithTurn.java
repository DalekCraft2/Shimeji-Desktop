package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * @author Kilkakon
 * @since 1.0.8
 * @deprecated As of 1.0.21, integrated into {@link Move}
 */
@Deprecated
public class MoveWithTurn extends Move {
    private static final Logger log = Logger.getLogger(MoveWithTurn.class.getName());

    public MoveWithTurn(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);

        if (animations.size() < 2) {
            throw new IllegalArgumentException("animations.size<2");
        }
    }

    @Override
    protected Animation getAnimation() throws VariableException {
        // force to last animation if turning
        if (turning) {
            return getAnimations().get(getAnimations().size() - 1);
        } else {
            List<Animation> animations = getAnimations();
            for (int index = 0; index < animations.size() - 1; index++) {
                if (animations.get(index).isEffective(getVariables())) {
                    return animations.get(index);
                }
            }
        }

        return null;
    }

    @Override
    protected boolean hasTurningAnimation() {
        return true;
    }
}
