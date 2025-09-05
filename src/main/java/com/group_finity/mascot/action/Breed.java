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
 * Multiplying action.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Breed extends Animate {
    private static final Logger log = Logger.getLogger(Breed.class.getName());

    // thanks to LavenderSnek for the idea for this delegate, cleans up the breeding code nicely
    static class Delegate {
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

        private final ActionBase action;
        private double scaling;

        Delegate(ActionBase action) {
            this.action = action;
        }

        void initScaling() {
            scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));
        }

        boolean isEnabled() throws VariableException {
            return getBornTransient() ?
                    Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Transients", "true")) :
                    Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Breeding", "true"));
        }

        boolean isIntervalFrame() throws VariableException {
            return action.getTime() % getBornInterval() == 0;
        }

        boolean isPenultimateFrame() throws VariableException {
            return action.getTime() == action.getAnimation().getDuration() - 1;
        }

        void breed() throws VariableException {
            String childType = Main.getInstance().getConfiguration(getBornMascot()) != null ? getBornMascot() : action.getMascot().getImageSet();

            for (int index = 0; index < getBornCount(); index++) {
                // Create a mascot
                final Mascot mascot = new Mascot(childType);

                log.log(Level.INFO, "Breed Mascot ({0},{1},{2})", new Object[]{action.getMascot(), action, mascot});

                // Start outside the range
                if (action.getMascot().isLookRight()) {
                    mascot.setAnchor(new Point(action.getMascot().getAnchor().x - (int) Math.round(getBornX() * scaling),
                            action.getMascot().getAnchor().y + (int) Math.round(getBornY() * scaling)));
                } else {
                    mascot.setAnchor(new Point(action.getMascot().getAnchor().x + (int) Math.round(getBornX() * scaling),
                            action.getMascot().getAnchor().y + (int) Math.round(getBornY() * scaling)));
                }
                mascot.setLookRight(action.getMascot().isLookRight());

                try {
                    mascot.setBehavior(Main.getInstance().getConfiguration(childType).buildBehavior(getBornBehaviour(), action.getMascot()));
                    action.getMascot().getManager().add(mascot);
                } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                    log.log(Level.SEVERE, "Failed to create mascot \"" + mascot + "\" with behavior \"" + getBornBehaviour() + "\"", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("FailedCreateNewShimejiErrorMessage"), e);
                    mascot.dispose();
                }
            }
        }

        void validateBornCount() throws VariableException {
            if (getBornCount() < 1) {
                throw new VariableException("BornCount must be positive");
            }
        }

        void validateBornInterval() throws VariableException {
            if (getBornInterval() < 1) {
                throw new VariableException("BornInterval must be positive");
            }
        }

        private int getBornX() throws VariableException {
            return action.eval(action.getSchema().getString(PARAMETER_BORNX), Number.class, DEFAULT_BORNX).intValue();
        }

        private int getBornY() throws VariableException {
            return action.eval(action.getSchema().getString(PARAMETER_BORNY), Number.class, DEFAULT_BORNY).intValue();
        }

        private String getBornBehaviour() throws VariableException {
            return action.eval(action.getSchema().getString(PARAMETER_BORNBEHAVIOUR), String.class, DEFAULT_BORNBEHAVIOUR);
        }

        private String getBornMascot() throws VariableException {
            return action.eval(action.getSchema().getString(PARAMETER_BORNMASCOT), String.class, DEFAULT_BORNMASCOT);
        }

        private boolean getBornTransient() throws VariableException {
            return action.eval(action.getSchema().getString(PARAMETER_BORNTRANSIENT), Boolean.class, DEFAULT_BORNTRANSIENT);
        }

        private int getBornInterval() throws VariableException {
            return action.eval(action.getSchema().getString(PARAMETER_BORNINTERVAL), Number.class, DEFAULT_BORNINTERVAL).intValue();
        }

        private int getBornCount() throws VariableException {
            return action.eval(action.getSchema().getString(PARAMETER_BORNCOUNT), Number.class, DEFAULT_BORNCOUNT).intValue();
        }
    }

    private final Delegate delegate = new Delegate(this);

    public Breed(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        delegate.initScaling();
        delegate.validateBornCount();
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        if (delegate.isPenultimateFrame() && delegate.isEnabled()) {
            // Multiply
            delegate.breed();
        }
    }
}
