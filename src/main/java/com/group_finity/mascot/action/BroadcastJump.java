package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * @author Kilkakon
 * @since 1.0.20
 */
public class BroadcastJump extends Jump {
    private static final Logger log = Logger.getLogger(BroadcastJump.class.getName());

    public static final String PARAMETER_AFFORDANCE = "Affordance";

    private static final String DEFAULT_AFFORDANCE = "";

    public BroadcastJump(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        getMascot().getAffordances().add(getAffordance());
    }

    private String getAffordance() throws VariableException {
        return eval(getSchema().getString(PARAMETER_AFFORDANCE), String.class, DEFAULT_AFFORDANCE);
    }
}
