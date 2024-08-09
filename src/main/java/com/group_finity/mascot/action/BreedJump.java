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
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kilkakon
 * @since 1.0.18
 */
public class BreedJump extends Jump {
    private static final Logger log = Logger.getLogger(BreedJump.class.getName());

    public static final String PARAMETER_BORNX = "BornX";

    private static final int DEFAULT_BORNX = 0;

    public static final String PARAMETER_BORNY = "BornY";

    private static final int DEFAULT_BORNY = 0;

    public static final String PARAMETER_BORNBEHAVIOUR = "BornBehaviour";

    private static final String DEFAULT_BORNBEHAVIOUR = "";

    public static final String PARAMETER_BORNMASCOT = "BornMascot";

    private static final String DEFAULT_BORNMASCOT = "";

    public static final String PARAMETER_BORNINTERVAL = "BornInterval";

    private static final int DEFAULT_BORNINTERVAL = 1;

    public static final String PARAMETER_BORNTRANSIENT = "BornTransient";

    private static final boolean DEFAULT_BORNTRANSIENT = false;

    public static final String PARAMETER_BORNCOUNT = "BornCount";

    private static final int DEFAULT_BORNCOUNT = 1;

    private double scaling;

    public BreedJump(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        if (getBornCount() < 1) {
            throw new VariableException("BornCount must be positive");
        }
        if (getBornInterval() < 1) {
            throw new VariableException("BornInterval must be positive");
        }
        scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        if (getTime() % getBornInterval() == 0 &&
                (getBornTransient() ?
                        Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Transients", "true")) :
                        Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Breeding", "true")))) {
            breed();
        }
    }

    private void breed() throws VariableException {
        String childType = Main.getInstance().getConfiguration(getBornMascot()) != null ? getBornMascot() : getMascot().getImageSet();

        for (int index = 0; index < getBornCount(); index++) {
            // Create a mascot
            final Mascot mascot = new Mascot(childType);

            log.log(Level.INFO, "Breeding mascot ({0}, {1}, {2})", new Object[]{getMascot(), this, mascot});

            // Start outside the range
            if (getMascot().isLookRight()) {
                mascot.setAnchor(new Point(getMascot().getAnchor().x - (int) Math.round(getBornX() * scaling),
                        getMascot().getAnchor().y + (int) Math.round(getBornY() * scaling)));
            } else {
                mascot.setAnchor(new Point(getMascot().getAnchor().x + (int) Math.round(getBornX() * scaling),
                        getMascot().getAnchor().y + (int) Math.round(getBornY() * scaling)));
            }
            mascot.setLookRight(getMascot().isLookRight());

            try {
                mascot.setBehavior(Main.getInstance().getConfiguration(childType).buildBehavior(getBornBehaviour(), getMascot()));
                getMascot().getManager().add(mascot);
            } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                log.log(Level.SEVERE, "Failed to create mascot \"" + mascot + "\" with behavior \"" + getBornBehaviour() + "\"", e);
                Main.showError(Main.getInstance().getLanguageBundle().getString("FailedCreateNewShimejiErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                mascot.dispose();
            }
        }
    }

    private int getBornX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_BORNX), Number.class, DEFAULT_BORNX).intValue();
    }

    private int getBornY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_BORNY), Number.class, DEFAULT_BORNY).intValue();
    }

    private String getBornBehaviour() throws VariableException {
        return eval(getSchema().getString(PARAMETER_BORNBEHAVIOUR), String.class, DEFAULT_BORNBEHAVIOUR);
    }

    private String getBornMascot() throws VariableException {
        return eval(getSchema().getString(PARAMETER_BORNMASCOT), String.class, DEFAULT_BORNMASCOT);
    }

    private boolean getBornTransient() throws VariableException {
        return eval(getSchema().getString(PARAMETER_BORNTRANSIENT), Boolean.class, DEFAULT_BORNTRANSIENT);
    }

    private int getBornInterval() throws VariableException {
        return eval(getSchema().getString(PARAMETER_BORNINTERVAL), Number.class, DEFAULT_BORNINTERVAL).intValue();
    }

    private int getBornCount() throws VariableException {
        return eval(getSchema().getString(PARAMETER_BORNCOUNT), Number.class, DEFAULT_BORNCOUNT).intValue();
    }
}
