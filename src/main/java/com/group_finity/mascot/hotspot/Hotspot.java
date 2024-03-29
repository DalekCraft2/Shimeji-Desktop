package com.group_finity.mascot.hotspot;

import com.group_finity.mascot.Mascot;

import java.awt.*;

/**
 * Hotspot.
 * <p>
 * Represents a clickable area on a shimeji, along with the behaviour to execute
 * when the user interacts with the area.
 *
 * @author Kilkakon
 * @since 1.0.19
 */
public class Hotspot {
    private final String behaviour;

    private final Shape shape;

    public Hotspot(String behaviour, Shape shape) {
        this.behaviour = behaviour;
        this.shape = shape;
    }

    public boolean contains(Mascot mascot, Point point) {
        // flip if facing right
        if (mascot.isLookRight()) {
            point = new Point(mascot.getBounds().width - point.x, point.y);
        }

        return shape.contains(point);
    }

    public String getBehaviour() {
        return behaviour;
    }

    public Shape getShape() {
        return shape;
    }
}
