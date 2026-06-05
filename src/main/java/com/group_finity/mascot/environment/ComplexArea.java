package com.group_finity.mascot.environment;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a region of space comprised of multiple {@link Area} objects.
 * Mainly used by {@link Environment} to represent the areas of all active displays.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 * @see Area
 */
public class ComplexArea {

    /**
     * Maps from an arbitrary string to the corresponding {@link Area} object.
     */
    private final Map<String, Area> areas = new HashMap<>(2);

    /**
     * Sets the bounds of each {@link Area} to match the {@link Rectangle} with the same name in the specified map.
     * Any {@code Area} objects whose names are not present in the key set of {@code rectangles} are removed from this
     * {@code ComplexArea}.
     *
     * @param rectangles a collection of mappings from {@code Area} names
     * to the new bounds of the corresponding {@code Area} objects
     */
    public void set(Map<String, Rectangle> rectangles) {
        // Retain the areas with the same names
        retain(rectangles.keySet());
        for (Map.Entry<String, Rectangle> e : rectangles.entrySet()) {
            set(e.getKey(), e.getValue());
        }
    }

    /**
     * Sets the bounds of the {@link Area} with the specified name to match the specified {@link Rectangle}.
     * If this {@code ComplexArea} does not contain an {@code Area} with the specified name, a new one is created.
     *
     * @param name the name of the {@code Area} to update
     * @param value the new bounds of the {@code Area}
     * @see Area#set(Rectangle)
     */
    public void set(String name, final Rectangle value) {
        // Should I exclude an area which matches exactly?
        // This seems to occur when mirroring
        for (Area area : areas.values()) {
            if (area.getLeft() == value.x &&
                    area.getTop() == value.y &&
                    area.getWidth() == value.width &&
                    area.getHeight() == value.height) {
                return;
            }
        }

        Area area = areas.get(name);
        if (area == null) {
            // This class is only used for screen areas, so set calcDeltas to false for the Area objects
            area = new Area(false);
            areas.put(name, area);
        }
        area.set(value);
    }

    /**
     * Retains only the {@link Area} objects in this {@code ComplexArea}
     * that are mapped to the specified names. In other words, removes from
     * this {@code ComplexArea} all {@code Area} objects that are not mapped
     * to any of the specified names.
     *
     * @param areaNames the names of the {@code Area} objects to retain
     * @see java.util.Set#retainAll(Collection)
     */
    public void retain(Collection<String> areaNames) {
        areas.keySet().retainAll(areaNames);
    }

    /**
     * Iterates through the {@link Area} objects in this {@code ComplexArea} and returns
     * the last one on whose bottom border the specified {@link Point} lies.
     * If the specified {@code Point} is on the top border of any {@code Area}, the method returns {@code null}.
     *
     * @param location the specified {@code Point}
     * @return the last instance of a bottom border on which {@code location} lies;
     * {@code null} if {@code location} lies on the top border of any {@code Area} in this {@code ComplexArea}
     * or if {@code location} does not lie on any bottom borders
     * @see Area#getBottomBorder()
     */
    public FloorCeiling getBottomBorder(Point location) {
        FloorCeiling ret = null;

        for (Area area : areas.values()) {
            if (area.getTopBorder().isOn(location)) {
                return null;
            } else if (area.getBottomBorder().isOn(location)) {
                ret = area.getBottomBorder();
            }
        }

        return ret;
    }

    /**
     * Iterates through the {@link Area} objects in this {@code ComplexArea} and returns
     * the last one on whose top border the specified {@link Point} lies.
     * If the specified {@code Point} is on the bottom border of any {@code Area}, the method returns {@code null}.
     *
     * @param location the specified {@code Point}
     * @return the last instance of a top border on which {@code location} lies;
     * {@code null} if {@code location} lies on the bottom border of any {@code Area} in this {@code ComplexArea}
     * or if {@code location} does not lie on any top borders
     * @see Area#getTopBorder()
     */
    public FloorCeiling getTopBorder(Point location) {
        FloorCeiling ret = null;

        for (Area area : areas.values()) {
            if (area.getBottomBorder().isOn(location)) {
                return null;
            } else if (area.getTopBorder().isOn(location)) {
                ret = area.getTopBorder();
            }
        }

        return ret;
    }

    /**
     * Iterates through the {@link Area} objects in this {@code ComplexArea} and returns
     * the last one on whose left border the specified {@link Point} lies.
     * If the specified {@code Point} is on the right border of any {@code Area}, the method returns {@code null}.
     *
     * @param location the specified {@code Point}
     * @return the last instance of a left border on which {@code location} lies;
     * {@code null} if {@code location} lies on the right border of any {@code Area} in this {@code ComplexArea}
     * or if {@code location} does not lie on any left borders
     * @see Area#getLeftBorder()
     */
    public Wall getLeftBorder(Point location) {
        Wall ret = null;

        for (Area area : areas.values()) {
            if (area.getRightBorder().isOn(location)) {
                return null;
            } else if (area.getLeftBorder().isOn(location)) {
                ret = area.getLeftBorder();
            }
        }

        return ret;
    }

    /**
     * Iterates through the {@link Area} objects in this {@code ComplexArea} and returns
     * the last one on whose right border the specified {@link Point} lies.
     * If the specified {@code Point} is on the left border of any {@code Area}, the method returns {@code null}.
     *
     * @param location the specified {@code Point}
     * @return the last instance of a right border on which {@code location} lies;
     * {@code null} if {@code location} lies on the left border of any {@code Area} in this {@code ComplexArea}
     * or if {@code location} does not lie on any right borders
     * @see Area#getRightBorder()
     */
    public Wall getRightBorder(Point location) {
        Wall ret = null;

        for (Area area : areas.values()) {
            if (area.getLeftBorder().isOn(location)) {
                return null;
            } else if (area.getRightBorder().isOn(location)) {
                ret = area.getRightBorder();
            }
        }

        return ret;
    }

    /**
     * Gets all {@link Area} objects that comprise this {@code ComplexArea}.
     *
     * @return all {@link Area} objects in this {@code ComplexArea}
     */
    public Collection<Area> getAreas() {
        return areas.values();
    }
}
