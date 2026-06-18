package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Location;
import com.group_finity.mascot.script.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Action for being dragged.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Dragged extends ActionBase {
    private static final Logger log = LoggerFactory.getLogger(Dragged.class);

    private static final String PARAMETER_OFFSETX = "OffsetX";
    private static final int DEFAULT_OFFSETX = 0;

    private static final String PARAMETER_OFFSETY = "OffsetY";
    private static final int DEFAULT_OFFSETY = 120;

    private static final String PARAMETER_OFFSETTYPE = "OffsetType";
    private static final String DEFAULT_OFFSETTYPE = "ImageAnchor";

    private static final String VARIABLE_FOOTX = "FootX";

    private static final String VARIABLE_FOOTDX = "FootDX";

    private double footX;

    private double footDx;

    private int timeToResist;

    private double scaling;

    public Dragged(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        scaling = Main.getInstance().getSettings().scaling;

        footX = getEnvironment().getCursor().getX() + (int) Math.round(getOffsetX() * scaling);
        timeToResist = 250;
    }

    @Override
    public boolean hasNext() throws VariableException {
        return super.hasNext() && getTime() < timeToResist;
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

        if (Math.abs(cursor.getX() + offsetX - getMascot().getAnchor().x) >= 5) {
            setTime(0);
        }

        final int newX = cursor.getX();

        /*
        By calculating footDx like this instead of simply setting it to newX - footX, we can make it oscillate between
        positive and negative values as it approaches 0. This is useful for mascot scripts that may use this variable
        in animation conditions to make the mascot sway back and forth when the cursor stops moving.
        */
        footDx = (footDx + (newX - footX) * 0.1) * 0.8;
        footX += footDx;

        // Put FootX and FootDX in the variables so they may be used in animation conditions
        putVariable(getSchema().getString(VARIABLE_FOOTDX), footDx);
        putVariable(getSchema().getString(VARIABLE_FOOTX), footX);

        // Animate
        getAnimation().apply(getMascot(), getTime());

        // Align the mascot position to the mouse cursor
        getMascot().getAnchor().setLocation(cursor.getX() + offsetX, cursor.getY() + offsetY);

        // If the action's time is one less than timeToResist, there is a 90% chance that
        // we increase timeToResist by 1, which will make the next hasNext() call not return false.
        // This recreates the old lukewarm behavior while also keeping hasNext() deterministic.
        if (getTime() == timeToResist - 1 && Math.random() >= 0.1) {
            timeToResist++;
        }
    }

    @Override
    protected void refreshHotspots() {
        ReadWriteLock lock = getMascot().getHotspotLock();
        lock.writeLock().lock();
        try {
            // action does not support hotspots
            getMascot().clearHotspots();
        } finally {
            lock.writeLock().unlock();
        }
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
