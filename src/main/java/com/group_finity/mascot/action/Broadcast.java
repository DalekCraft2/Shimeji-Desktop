package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Kilkakon
 * @since 1.0.14
 * @deprecated As of 1.0.21, integrated into {@link ActionBase} and replaced by {@link Animate}
 */
@Deprecated
public class Broadcast extends Animate {
    private static final Logger log = LoggerFactory.getLogger(Broadcast.class);

    public Broadcast(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }
}
