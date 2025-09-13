package com.group_finity.mascot.display.view;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.display.controller.MascotInfoController;
import com.group_finity.mascot.display.model.MascotInfoType;
import com.valkryst.VMVC.view.View;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Objects;

public class MascotInfoView extends View<MascotInfoController> {
    /** {@link JTable} to display a {@link Mascot}'s properties in. */
    private final JTable propertiesTable;

    /**
     * Constructs a new {@link MascotInfoView}.
     *
     * @param controller Controller for this view.
     */
    public MascotInfoView(final MascotInfoController controller) {
        super(controller);

        final var tableModel = new DefaultTableModel(new Object[]{"Name", "Value"}, 0);
        propertiesTable = new JTable(tableModel);
        propertiesTable.setEnabled(false);
        propertiesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        propertiesTable.getTableHeader().setReorderingAllowed(false);

        // Add a row for each property.
        for (final var type : MascotInfoType.values()) {
            tableModel.addRow(new Object[]{type.localizedName(), "N/A"});
        }

        final var scrollPane = new JScrollPane(propertiesTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Updates the value of a property in the {@link #propertiesTable}.
     *
     * @param type {@link MascotInfoType} to update the value of.
     * @param value New value to set.
     */
    public void updateProperty(final MascotInfoType type, final String value) {
        Objects.requireNonNull(type);

        SwingUtilities.invokeLater(() -> {
            propertiesTable.getModel().setValueAt(value == null ? "N/A" : value, type.ordinal(), 1);
        });
    }

    /**
     * Updates the value of a property in the {@link #propertiesTable}.
     *
     * @param type {@link MascotInfoType} to update the value of.
     * @param value New value to set.
     */
    public void updateProperty(final MascotInfoType type, final int value) {
        this.updateProperty(type, String.valueOf(value));
    }
}
