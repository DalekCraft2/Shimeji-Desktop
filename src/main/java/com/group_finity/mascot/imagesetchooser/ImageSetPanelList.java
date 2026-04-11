package com.group_finity.mascot.imagesetchooser;

import javax.swing.*;
import java.awt.*;

/**
 * A {@link JList} that can be populated with {@link ImageSetPanel} objects.
 *
 * @author Shimeji-ee Group
 * @since 1.0.2
 */
public class ImageSetPanelList extends JList<ImageSetPanel> {

    public ImageSetPanelList() {
        setCellRenderer(new CustomCellRenderer<>());
    }

    static class CustomCellRenderer<T> implements ListCellRenderer<T> {
        @Override
        public Component getListCellRendererComponent(JList<? extends T> list, T value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof ImageSetPanel) {
                ImageSetPanel component = (ImageSetPanel) value;
                component.setCheckbox(isSelected);
                return component;
            } else {
                return new JLabel("???");
            }
        }
    }
}