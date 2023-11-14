package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public class Fall extends ActionBase {

    private static final Logger log = Logger.getLogger(Fall.class.getName());

    public static final String PARAMETER_INITIALVX = "InitialVX";

    private static final int DEFAULT_INITIALVX = 0;

    private static final String PARAMETER_INITIALVY = "InitialVY";

    private static final int DEFAULT_INITIALVY = 0;

    public static final String PARAMETER_RESISTANCEX = "ResistanceX";

    private static final double DEFAULT_RESISTANCEX = 0.05;

    public static final String PARAMETER_RESISTANCEY = "ResistanceY";

    private static final double DEFAULT_RESISTANCEY = 0.1;

    public static final String PARAMETER_GRAVITY = "Gravity";

    private static final double DEFAULT_GRAVITY = 2;

    public static final String VARIABLE_VELOCITYX = "VelocityX";

    public static final String VARIABLE_VELOCITYY = "VelocityY";

    private double velocityX;

    private double velocityY;

    private double modX;

    private double modY;

    public Fall(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        setVelocityX(getInitialVx());
        setVelocityY(getInitialVy());
    }

    @Override
    public boolean hasNext() throws VariableException {
        Point pos = getMascot().getAnchor();
        boolean onBorder = getEnvironment().getFloor().isOn(pos) || getEnvironment().getWall().isOn(pos);
        return super.hasNext() && !onBorder;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        if (getVelocityX() != 0) {
            getMascot().setLookRight(getVelocityX() > 0);
        }

        setVelocityX(getVelocityX() - getVelocityX() * getResistanceX());
        setVelocityY(getVelocityY() - getVelocityY() * getResistanceY() + getGravity());

        putVariable(getSchema().getString(VARIABLE_VELOCITYX), getVelocityX());
        putVariable(getSchema().getString(VARIABLE_VELOCITYY), getVelocityY());

        setModX(getModX() + getVelocityX() % 1);
        setModY(getModY() + getVelocityY() % 1);

        int dx = (int) getVelocityX() + (int) getModX();
        int dy = (int) getVelocityY() + (int) getModY();

        setModX(getModX() % 1);
        setModY(getModY() % 1);

        int dev = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));

        Point start = getMascot().getAnchor();

        OUTER:
        for (int i = 0; i <= dev; ++i) {
            int x = start.x + dx * i / dev;
            int y = start.y + dy * i / dev;

            getMascot().setAnchor(new Point(x, y));
            if (dy > 0) {
                // HACK IE
                for (int j = -80; j <= 0; ++j) {
                    getMascot().setAnchor(new Point(x, y + j));
                    if (getEnvironment().getFloor(true).isOn(getMascot().getAnchor())) {
                        break OUTER;
                    }
                }
            }
            if (getEnvironment().getWall(true).isOn(getMascot().getAnchor())) {
                break;
            }
        }

        getAnimation().next(getMascot(), getTime());
    }

    private int getInitialVx() throws VariableException {
        return eval(getSchema().getString(PARAMETER_INITIALVX), Number.class, DEFAULT_INITIALVX).intValue();
    }

    private int getInitialVy() throws VariableException {
        return eval(getSchema().getString(PARAMETER_INITIALVY), Number.class, DEFAULT_INITIALVY).intValue();
    }

    private double getGravity() throws VariableException {
        return eval(getSchema().getString(PARAMETER_GRAVITY), Number.class, DEFAULT_GRAVITY).doubleValue();
    }

    private double getResistanceX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_RESISTANCEX), Number.class, DEFAULT_RESISTANCEX).doubleValue();
    }

    private double getResistanceY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_RESISTANCEY), Number.class, DEFAULT_RESISTANCEY).doubleValue();
    }

    private void setVelocityY(final double velocityY) {
        this.velocityY = velocityY;
    }

    private double getVelocityY() {
        return velocityY;
    }

    private void setVelocityX(final double velocityX) {
        this.velocityX = velocityX;
    }

    private double getVelocityX() {
        return velocityX;
    }

    private void setModX(final double modX) {
        this.modX = modX;
    }

    private double getModX() {
        return modX;
    }

    private void setModY(final double modY) {
        this.modY = modY;
    }

    private double getModY() {
        return modY;
    }

}
