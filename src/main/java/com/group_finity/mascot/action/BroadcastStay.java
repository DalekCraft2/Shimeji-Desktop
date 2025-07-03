package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.script.VariableMap;

import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Kilkakon
 * @since 1.0.14
 * @deprecated As of 1.0.21, integrated into {@link ActionBase} and replaced by {@link Stay}
 */
@Deprecated
public class BroadcastStay extends Stay {
    public BroadcastStay(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }
}
