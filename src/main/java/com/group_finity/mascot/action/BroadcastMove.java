package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.script.VariableMap;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * @author Kilkakon
 * @since 1.0.14
 * @deprecated As of 1.0.21, integrated into {@link ActionBase} and replaced by {@link Move}
 */
@Deprecated
public class BroadcastMove extends Move {
    private static final Logger log = Logger.getLogger(BroadcastMove.class.getName());

    public BroadcastMove(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }
}
