package com.group_finity.mascot.hotspot;

import com.group_finity.mascot.Mascot;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
@AllArgsConstructor
@Getter
public class Hotspot {
    private final String behaviour;
    private final Shape shape;

    public boolean contains(Mascot mascot, Point point) {
        // flip if facing right
        if (mascot.isLookRight()) {
            point = new Point(mascot.getBounds().width - point.x, point.y);
        }

        return shape.contains(point);
    }
}
