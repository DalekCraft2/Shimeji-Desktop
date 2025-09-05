package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for resisting being dragged.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
// TODO Try to fix the typos of "resist" and "resistance" being "regist" and "registance" without breaking compatibility.
public class Regist extends ActionBase {
    private static final Logger log = Logger.getLogger(Regist.class.getName());

    public static final String PARAMETER_OFFSETX = "OffsetX";

    private static final int DEFAULT_OFFSETX = 0;

    public static final String PARAMETER_OFFSETTYPE = "OffsetType";

    private static final String DEFAULT_OFFSETTYPE = "ImageAnchor";

    private double scaling;

    public Regist(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));
    }

    @Override
    public boolean hasNext() throws VariableException {
        int offsetX = (int) Math.round(getOffsetX() * scaling);
        if (getOffsetType().equals(getSchema().getString("Origin"))) {
            offsetX = getMascot().getImage().getCenter().x - offsetX;
        }

        final boolean moved = Math.abs(getEnvironment().getCursor().getX() - getMascot().getAnchor().x + offsetX) >= 5;

        return super.hasNext() && !moved;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        getMascot().setDragging(true);

        // Animate
        final Animation animation = getAnimation();
        animation.next(getMascot(), getTime());

        if (getTime() + 1 >= getAnimation().getDuration()) {
            // Ended because the period has passed.

            getMascot().setLookRight(Math.random() < 0.5);

            log.log(Level.INFO, "Lost ground ({0}, {1})", new Object[]{getMascot(), this});
            throw new LostGroundException();
        }
    }

    @Override
    protected void refreshHotspots() {
        synchronized (getMascot().getHotspots()) {
            // action does not support hotspots
            getMascot().getHotspots().clear();
        }
    }

    private int getOffsetX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_OFFSETX), Number.class, DEFAULT_OFFSETX).intValue();
    }

    private String getOffsetType() throws VariableException {
        return eval(getSchema().getString(PARAMETER_OFFSETTYPE), String.class, DEFAULT_OFFSETTYPE);
    }
}
