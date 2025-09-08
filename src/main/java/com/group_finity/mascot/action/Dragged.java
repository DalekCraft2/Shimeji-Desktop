package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Location;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Action for being dragged.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Dragged extends ActionBase {
    private static final Logger log = Logger.getLogger(Dragged.class.getName());

    private static final String VARIABLE_FOOTX = "FootX";

    private static final String VARIABLE_FOOTDX = "FootDX";

    public static final String PARAMETER_OFFSETX = "OffsetX";

    private static final int DEFAULT_OFFSETX = 0;

    public static final String PARAMETER_OFFSETY = "OffsetY";

    private static final int DEFAULT_OFFSETY = 120;

    public static final String PARAMETER_OFFSETTYPE = "OffsetType";

    private static final String DEFAULT_OFFSETTYPE = "ImageAnchor";

    private double footX;

    private double footDx;

    private int timeToRegist;

    private double scaling;

    public Dragged(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));

        footX = getEnvironment().getCursor().getX() + (int) Math.round(getOffsetX() * scaling);
        setTimeToRegist(250);
    }

    @Override
    public boolean hasNext() throws VariableException {
        return super.hasNext() && getTime() < getTimeToRegist();
    }

    @Override
    protected void tick() throws VariableException {
        getMascot().setLookRight(false);
        getMascot().setDragging(true);
        getEnvironment().refreshWorkArea();

        final Location cursor = getEnvironment().getCursor();

        int offsetX = (int) Math.round(getOffsetX() * scaling);
        int offsetY = (int) Math.round(getOffsetY() * scaling);
        if (getOffsetType().equals(getSchema().getString("Origin"))) {
            offsetX = getMascot().getImage().getCenter().x - offsetX;
            offsetY = getMascot().getImage().getCenter().y - offsetY;
        }

        if (Math.abs(cursor.getX() - getMascot().getAnchor().x + offsetX) >= 5) {
            setTime(0);
        }

        final int newX = cursor.getX();

        footDx = (footDx + (newX - footX) * 0.1) * 0.8;
        footX = footX + footDx;

        // Since the foot position and foot delta position may be included in the animation conditions, put them in variables
        putVariable(getSchema().getString(VARIABLE_FOOTDX), footDx);
        putVariable(getSchema().getString(VARIABLE_FOOTX), footX);

        // Animate
        getAnimation().next(getMascot(), getTime());

        // Align the mascot position to the mouse cursor
        getMascot().setAnchor(new Point(cursor.getX() + offsetX, cursor.getY() + offsetY));

        // recreates old lukewarm behaviour while keeping hasNext deterministic
        if (getTime() == getTimeToRegist() - 1 && Math.random() >= 0.1) {
            timeToRegist++;
        }
    }

    @Override
    protected void refreshHotspots() {
        synchronized (getMascot().getHotspots()) {
            // action does not support hotspots
            getMascot().getHotspots().clear();
        }
    }

    public void setTimeToRegist(final int timeToRegist) {
        this.timeToRegist = timeToRegist;
    }

    private int getTimeToRegist() {
        return timeToRegist;
    }

    private int getOffsetX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_OFFSETX), Number.class, DEFAULT_OFFSETX).intValue();
    }

    private int getOffsetY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_OFFSETY), Number.class, DEFAULT_OFFSETY).intValue();
    }

    private String getOffsetType() throws VariableException {
        return eval(getSchema().getString(PARAMETER_OFFSETTYPE), String.class, DEFAULT_OFFSETTYPE);
    }
}
