package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.script.VariableMap;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * @author Kilkakon
 * @since 1.0.14
 */
// TODO Add @deprecated tags to the newly deprecated classes' Javadocs after finishing the 1.0.21/1.0.21.1 merge
@Deprecated
public class Broadcast extends Animate {
    private static final Logger log = Logger.getLogger(Broadcast.class.getName());

    public Broadcast(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }
}
