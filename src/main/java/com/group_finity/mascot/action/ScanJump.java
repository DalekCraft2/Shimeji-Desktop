package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Kilkakon
 * @since 1.0.20
 */
public class ScanJump extends ActionBase {
    private static final Logger log = LoggerFactory.getLogger(ScanJump.class);

    public static final String PARAMETER_BEHAVIOUR = "Behaviour";

    private static final String DEFAULT_BEHAVIOUR = "";

    public static final String PARAMETER_TARGETBEHAVIOUR = "TargetBehaviour";

    private static final String DEFAULT_TARGETBEHAVIOUR = "";

    public static final String PARAMETER_TARGETLOOK = "TargetLook";

    private static final boolean DEFAULT_TARGETLOOK = false;

    // A Pose attribute is already named "Velocity", so this is named "VelocityParam" instead
    public static final String PARAMETER_VELOCITY = "VelocityParam";

    private static final double DEFAULT_VELOCITY = 20.0;

    public static final String VARIABLE_VELOCITYX = "VelocityX";

    public static final String VARIABLE_VELOCITYY = "VelocityY";

    private WeakReference<Mascot> target;

    private double scaling;

    public ScanJump(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        scaling = Main.getInstance().getSettings().scaling;

        // cannot broadcast while scanning for an affordance
        getMascot().getAffordances().clear();

        if (getMascot().getManager() != null) {
            target = getMascot().getManager().getMascotWithAffordance(getAffordance());
        }
        Mascot targetMascot = target == null ? null : target.get();
        putVariable(getSchema().getString("TargetX"), targetMascot != null ? targetMascot.getAnchor().x : null);
        putVariable(getSchema().getString("TargetY"), targetMascot != null ? targetMascot.getAnchor().y : null);
    }

    @Override
    public boolean hasNext() throws VariableException {
        if (getMascot().getManager() == null) {
            return super.hasNext();
        }

        Mascot targetMascot = target == null ? null : target.get();
        return super.hasNext() && targetMascot != null && targetMascot.getAffordances().contains(getAffordance());
    }

    @Override
    protected void tick() throws VariableException {
        // cannot broadcast while scanning for an affordance
        getMascot().getAffordances().clear();

        Mascot targetMascot = target == null ? null : target.get();
        // TODO: Figure out how to handle targetMascot possibly being null here and in other "Scan"/"Complex" action classes
        int targetX = targetMascot.getAnchor().x;
        int targetY = targetMascot.getAnchor().y;

        putVariable(getSchema().getString("TargetX"), targetX);
        putVariable(getSchema().getString("TargetY"), targetY);

        if (getMascot().getAnchor().x != targetX) {
            getMascot().setLookRight(getMascot().getAnchor().x < targetX);
        }

        double distanceX = targetX - getMascot().getAnchor().x;
        double distanceY = targetY - getMascot().getAnchor().y - Math.abs(distanceX) / 2;

        double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);

        double velocity = getVelocity() * scaling;

        if (distance != 0) {
            double velocityX = velocity * distanceX / distance;
            double velocityY = velocity * distanceY / distance;

            putVariable(getSchema().getString(VARIABLE_VELOCITYX), velocityX);
            putVariable(getSchema().getString(VARIABLE_VELOCITYY), velocityY);

            getMascot().setAnchor(new Point(getMascot().getAnchor().x + (int) Math.round(velocityX),
                    getMascot().getAnchor().y + (int) Math.round(velocityY)));
            getAnimation().next(getMascot(), getTime());
        }

        if (distance <= velocity) {
            getMascot().setAnchor(new Point(targetX, targetY));

            boolean setFirstBehavior = false;
            try {
                getMascot().setBehavior(Main.getInstance().getConfiguration(getMascot().getImageSet()).buildBehavior(getBehavior(), getMascot()));
                setFirstBehavior = true;
                targetMascot.setBehavior(Main.getInstance().getConfiguration(targetMascot.getImageSet()).buildBehavior(getTargetBehavior(), targetMascot));
                if (getTargetLook() && targetMascot.isLookRight() == getMascot().isLookRight()) {
                    targetMascot.setLookRight(!getMascot().isLookRight());
                }
            } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                String behaviorParam = setFirstBehavior ? getTargetBehavior() : getBehavior();
                Mascot mascotParam = setFirstBehavior ? targetMascot : getMascot();
                log.error("Failed to set behavior to \"{}\" for mascot \"{}\"", behaviorParam, mascotParam, e);
                Main.showError(String.format(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), behaviorParam, mascotParam), e);
            }
        }
    }

    private String getBehavior() throws VariableException {
        return eval(getSchema().getString(PARAMETER_BEHAVIOUR), String.class, DEFAULT_BEHAVIOUR);
    }

    private String getTargetBehavior() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETBEHAVIOUR), String.class, DEFAULT_TARGETBEHAVIOUR);
    }

    private boolean getTargetLook() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETLOOK), Boolean.class, DEFAULT_TARGETLOOK);
    }

    private double getVelocity() throws VariableException {
        return eval(getSchema().getString(PARAMETER_VELOCITY), Number.class, DEFAULT_VELOCITY).doubleValue();
    }
}
