package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kilkakon
 * @since 1.0.20
 */
public class ScanJump extends ActionBase {
    private static final Logger log = Logger.getLogger(ScanJump.class.getName());

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

        scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));

        // cannot broadcast while scanning for an affordance
        getMascot().getAffordances().clear();

        if (getMascot().getManager() != null) {
            target = getMascot().getManager().getMascotWithAffordance(getAffordance());
        }
        putVariable("target", target != null ? target.get() : null);
    }

    @Override
    public boolean hasNext() throws VariableException {
        if (getMascot().getManager() == null) {
            return super.hasNext();
        }

        return super.hasNext() && target != null && target.get() != null && target.get().getAffordances().contains(getAffordance());
    }

    @Override
    protected void tick() throws VariableException {
        // cannot broadcast while scanning for an affordance
        getMascot().getAffordances().clear();

        int targetX = target.get().getAnchor().x;
        int targetY = target.get().getAnchor().y;

        if (getMascot().getAnchor().x != targetX) {
            getMascot().setLookRight(getMascot().getAnchor().x < targetX);
        }

        double distanceX = targetX - getMascot().getAnchor().x;
        double distanceY = targetY - getMascot().getAnchor().y - Math.abs(distanceX) / 2;

        double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);

        double velocity = getVelocity() * scaling;

        if (distance != 0) {
            int velocityX = (int) Math.round(velocity * distanceX / distance);
            int velocityY = (int) Math.round(velocity * distanceY / distance);

            putVariable(getSchema().getString(VARIABLE_VELOCITYX), velocityX);
            putVariable(getSchema().getString(VARIABLE_VELOCITYY), velocityY);

            getMascot().setAnchor(new Point(getMascot().getAnchor().x + velocityX,
                    getMascot().getAnchor().y + velocityY));
            getAnimation().next(getMascot(), getTime());
        }

        if (distance <= velocity) {
            getMascot().setAnchor(new Point(targetX, targetY));

            boolean setFirstBehavior = false;
            try {
                getMascot().setBehavior(Main.getInstance().getConfiguration(getMascot().getImageSet()).buildBehavior(getBehavior(), getMascot()));
                setFirstBehavior = true;
                Mascot targetMascot = target.get();
                if (targetMascot != null) {
                    targetMascot.setBehavior(Main.getInstance().getConfiguration(targetMascot.getImageSet()).buildBehavior(getTargetBehavior(), targetMascot));
                    if (getTargetLook() && targetMascot.isLookRight() == getMascot().isLookRight()) {
                        targetMascot.setLookRight(!getMascot().isLookRight());
                    }
                }
            } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                log.log(Level.SEVERE, "Failed to set behavior to \"" + (setFirstBehavior ? getTargetBehavior() : getBehavior()) + "\" for mascot \"" + (setFirstBehavior ? target.get() : getMascot()) + "\"", e);
                Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
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
