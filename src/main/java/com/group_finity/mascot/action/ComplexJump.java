package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
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
 * @since 1.0.21.3
 */
public class ComplexJump extends ActionBase {
    private static final Logger log = Logger.getLogger(ComplexJump.class.getName());

    private final Breed.Delegate delegate = new Breed.Delegate(this);

    public static final String PARAMETER_CHARACTERISTICS = "Characteristics";

    private static final String DEFAULT_CHARACTERISTICS = "";

    public static final String PARAMETER_BEHAVIOUR = "Behaviour";

    private static final String DEFAULT_BEHAVIOUR = "";

    public static final String PARAMETER_TARGETBEHAVIOUR = "TargetBehaviour";

    private static final String DEFAULT_TARGETBEHAVIOUR = "";

    public static final String PARAMETER_TARGETLOOK = "TargetLook";

    private static final boolean DEFAULT_TARGETLOOK = false;

    // A Pose attribute is already named "Velocity", so this is named "VelocityParam" instead
    public static final String PARAMETER_VELOCITY = "VelocityParam";

    private static final double DEFAULT_VELOCITY = 20.0;

    public static final String PARAMETER_TARGETX = "TargetX";

    private static final int DEFAULT_TARGETX = 0;

    public static final String PARAMETER_TARGETY = "TargetY";

    private static final int DEFAULT_TARGETY = 0;

    public static final String VARIABLE_VELOCITYX = "VelocityX";

    public static final String VARIABLE_VELOCITYY = "VelocityY";

    private WeakReference<Mascot> target;

    private boolean breedEnabled = false;

    private boolean scanEnabled = false;

    private double scaling;

    public ComplexJump(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));

        for (String characteristic : getCharacteristics().split(",")) {
            if (characteristic.equals(getSchema().getString("Breed"))) {
                breedEnabled = true;
            }
            if (characteristic.equals(getSchema().getString("Scan"))) {
                scanEnabled = true;
            }
        }

        if (breedEnabled) {
            delegate.initScaling();
            delegate.validateBornCount();
            delegate.validateBornInterval();
        }
        if (scanEnabled) {
            // cannot broadcast while scanning for an affordance
            getMascot().getAffordances().clear();

            if (getMascot().getManager() != null) {
                target = getMascot().getManager().getMascotWithAffordance(getAffordance());
            }
            putVariable(getSchema().getString("TargetX"), target != null && target.get() != null ? target.get().getAnchor().x : null);
            putVariable(getSchema().getString("TargetY"), target != null && target.get() != null ? target.get().getAnchor().y : null);
        }
    }

    @Override
    public boolean hasNext() throws VariableException {
        if (scanEnabled) {
            if (getMascot().getManager() == null) {
                return super.hasNext();
            }

            return super.hasNext() && target != null && target.get() != null && target.get().getAffordances().contains(getAffordance());
        } else {
            final int targetX = getTargetX();
            final int targetY = getTargetY();

            final double distanceX = targetX - getMascot().getAnchor().x;
            final double distanceY = targetY - getMascot().getAnchor().y - Math.abs(distanceX) / 2;

            final double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);

            return super.hasNext() && distance != 0;
        }
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        int targetX;
        int targetY;

        if (scanEnabled) {
            // cannot broadcast while scanning for an affordance
            getMascot().getAffordances().clear();

            targetX = target.get().getAnchor().x;
            targetY = target.get().getAnchor().y;

            putVariable(getSchema().getString("TargetX"), targetX);
            putVariable(getSchema().getString("TargetY"), targetY);

            if (getMascot().getAnchor().x != targetX) {
                getMascot().setLookRight(getMascot().getAnchor().x < targetX);
            }
        } else {
            targetX = getTargetX();
            targetY = getTargetY();

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

            if (scanEnabled) {
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
                    Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), e);
                }
            }
        }

        if (breedEnabled && delegate.isIntervalFrame() && delegate.isEnabled()) {
            // Multiply
            delegate.breed();
        }
    }

    private String getCharacteristics() throws VariableException {
        return eval(getSchema().getString(PARAMETER_CHARACTERISTICS), String.class, DEFAULT_CHARACTERISTICS);
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

    private int getTargetX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETX), Number.class, DEFAULT_TARGETX).intValue();
    }

    private int getTargetY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETY), Number.class, DEFAULT_TARGETY).intValue();
    }
}
