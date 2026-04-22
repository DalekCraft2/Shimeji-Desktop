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
 * @since 1.0.14
 */
public class Interact extends Animate {
    private static final Logger log = LoggerFactory.getLogger(Interact.class);

    private static final String PARAMETER_BEHAVIOR = "Behaviour";
    private static final String DEFAULT_BEHAVIOR = "";

    public Interact(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public boolean hasNext() throws VariableException {
        return super.hasNext() && getMascot().getManager().hasOverlappingMascotsAtPoint(getMascot().getAnchor());
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        Animation animation = getAnimation();
        if ((getTime() == animation.getDuration() - 1 || animation.getDuration() == 1) && !getBehavior().trim().isEmpty()) {
            try {
                getMascot().setBehavior(Main.getInstance().getConfiguration(getMascot().getImageSet()).buildBehavior(getBehavior(), getMascot()));
            } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                log.error("Failed to set behavior to \"{}\" for mascot \"{}\"", getBehavior(), getMascot(), e);
                Main.showError(String.format(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), getBehavior(), getMascot()), e);
            }
        }
    }

    private String getBehavior() throws VariableException {
        return eval(getSchema().getString(PARAMETER_BEHAVIOR), String.class, DEFAULT_BEHAVIOR);
    }
}
