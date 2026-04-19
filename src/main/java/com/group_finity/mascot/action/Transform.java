package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Kilkakon
 * @since 1.0.12
 */
public class Transform extends Animate {
    private static final Logger log = LoggerFactory.getLogger(Transform.class);

    public static final String PARAMETER_TRANSFORMBEHAVIOR = "TransformBehaviour";

    private static final String DEFAULT_TRANSFORMBEHAVIOR = "";

    public static final String PARAMETER_TRANSFORMMASCOT = "TransformMascot";

    private static final String DEFAULT_TRANSFORMMASCOT = "";

    public Transform(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        if (Main.getInstance().getSettings().transformation) {
            Animation animation = getAnimation();
            if (getTime() == animation.getDuration() - 1 || animation.getDuration() == 1) {
                transform();
            }
        }
    }

    private void transform() throws VariableException {
        String childType = Main.getInstance().getConfiguration(getTransformMascot()) != null ? getTransformMascot() : getMascot().getImageSet();

        try {
            getMascot().setImageSet(childType);
            getMascot().setBehavior(Main.getInstance().getConfiguration(childType).buildBehavior(getTransformBehavior(), getMascot()));
        } catch (final BehaviorInstantiationException | CantBeAliveException e) {
            log.error("Failed to set behavior to \"{}\" for mascot \"{}\"", getTransformBehavior(), getMascot(), e);
            Main.showError(String.format(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), getTransformBehavior(), getMascot()), e);
        }
    }

    private String getTransformBehavior() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TRANSFORMBEHAVIOR), String.class, DEFAULT_TRANSFORMBEHAVIOR);
    }

    private String getTransformMascot() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TRANSFORMMASCOT), String.class, DEFAULT_TRANSFORMMASCOT);
    }
}
