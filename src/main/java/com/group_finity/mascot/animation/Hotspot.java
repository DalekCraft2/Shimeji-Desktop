package com.group_finity.mascot.animation;

import com.group_finity.mascot.Mascot;

import java.awt.*;

/**
 * Represents a clickable area on a mascot, along with the behavior to execute
 * when the user interacts with the area.
 *
 * @author Kilkakon
 * @since 1.0.19
 */
public class Hotspot {
    /**
     * The name of the behavior to which to set the mascot when this hotspot is activated.
     */
    private final String behaviour;

    /**
     * The shape of this hotspot's clickable area.
     */
    private final Shape shape;

    /**
     * Creates a new Hotspot.
     *
     * @param behaviour the name of the behavior to which to set the mascot when this hotspot is activated
     * @param shape the shape of this hotspot's clickable area
     */
    public Hotspot(String behaviour, Shape shape) {
        this.behaviour = behaviour;
        this.shape = shape;
    }

    /**
     * Checks whether the given point is within this hotspot's shape on the given mascot.
     *
     * @param mascot the mascot to use when performing this check
     * @param point the point to check, relative to mascot's bounds
     * @return {@code true} if the point is within this hotspot's shape, otherwise {@code false}
     */
    public boolean contains(Mascot mascot, Point point) {
        // Flip if facing right
        if (mascot.isLookRight()) {
            point = new Point(mascot.getBounds().width - point.x, point.y);
        }

        return shape.contains(point);
    }

    /**
     * Gets the name of the behavior to which to set the mascot when this hotspot is activated.
     *
     * @return the name of this hotspot's behavior
     */
    public String getBehaviour() {
        return behaviour;
    }

    /**
     * Gets the shape of this hotspot's clickable area.
     *
     * @return the shape of this hotspot's clickable area
     */
    public Shape getShape() {
        return shape;
    }
}
