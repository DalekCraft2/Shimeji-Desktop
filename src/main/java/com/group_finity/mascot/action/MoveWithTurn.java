package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
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
        } else {
            animations.get(animations.size() - 1).setTurn(true);
        }
    }
}
