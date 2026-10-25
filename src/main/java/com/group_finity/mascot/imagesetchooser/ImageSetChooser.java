package com.group_finity.mascot.imagesetchooser;

import com.group_finity.mascot.Localizable;
import com.group_finity.mascot.Main;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.ConfigurationException;
import com.group_finity.mascot.config.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Chooser used to select the Shimeji image sets in use.
 *
 * @author Shimeji-ee Group
 * @since 1.0.2
 */
public class ImageSetChooser extends JDialog implements Localizable {
    private static final Logger log = LoggerFactory.getLogger(ImageSetChooser.class);

    /**
     * Constant for an empty array of ImageSetPanels.
     * This is used to save memory by only allocating one empty array.
     */
    private static final ImageSetPanel[] EMPTY_PANEL_ARRAY = new ImageSetPanel[0];

    /**
     * The currently selected image sets.
     */
    private final List<String> imageSets = new ArrayList<>();

    /**
     * Whether the "Use Selected" or "Use All" buttons were pressed instead of the "Cancel" or "Close" buttons.
     */
    private boolean selectionConfirmed = false;

    /**
     * Creates new form ImageSetChooser
     *
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param modal specifies whether dialog blocks user input to other top-level
     * windows when shown. If {@code true}, the modality type property is set to
     * {@code DEFAULT_MODALITY_TYPE} otherwise the dialog is modeless
     */
    public ImageSetChooser(Frame owner, boolean modal) {
        super(owner, modal);
        initComponents();
        localize(Main.getInstance().getLanguageBundle());

        // Load settings
        List<String> activeImageSets = new ArrayList<>(Main.getInstance().getSettings().activeImageSets);
        boolean selectAllSets = activeImageSets.isEmpty(); // if no active ones, activate them all!

        List<ImageSetPanel> listData = new ArrayList<>();
        Collection<Integer> selectedIndices = new ArrayList<>();

        // Get list of image sets (directories under img)
        DirectoryStream.Filter<Path> filter = entry -> {
            String fileName = entry.getFileName().toString();
            if (fileName.equalsIgnoreCase("unused") || fileName.startsWith(".")) {
                return false;
            }
            return Files.isDirectory(entry);
        };
        try (DirectoryStream<Path> imageSetDirs = Files.newDirectoryStream(Main.IMAGE_DIRECTORY, filter)) {
            // Create ImageSetPanels for ImageSetPanelList
            int index = 0;
            for (Path imageSetDir : imageSetDirs) {
                String imageSet = imageSetDir.getFileName().toString();

                Path actionsFile = Main.getActionsFilePath(imageSet);

                Path behaviorsFile = Main.getBehaviorsFilePath(imageSet);

                Path imageFile;
                String title;
                try {
                    // Determine information file
                    Path infoFile = Main.getInfoFilePath(imageSet);

                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    builderFactory.setIgnoringComments(true);
                    DocumentBuilder builder = builderFactory.newDocumentBuilder();

                    final Document infoDocument;
                    try (InputStream input = Files.newInputStream(infoFile)) {
                        infoDocument = builder.parse(input);
                    }

                    Configuration configuration = new Configuration();
                    configuration.load(new Entry(infoDocument.getDocumentElement()), imageSet, true);

                    if (configuration.getDisplayName() != null) {
                        title = configuration.getDisplayName();
                    } else {
                        title = imageSet;
                    }
                    if (configuration.getPreviewImagePath() != null) {
                        imageFile = imageSetDir.resolve(configuration.getPreviewImagePath());
                    } else {
                        imageFile = imageSetDir.resolve("shime1.png");
                    }
                } catch (IOException | ParserConfigurationException | SAXException | ConfigurationException |
                         RuntimeException ex) {
                    imageFile = imageSetDir.resolve("shime1.png");
                    title = imageSet;
                }

                listData.add(new ImageSetPanel(imageSet, actionsFile.toString(),
                        behaviorsFile.toString(), imageFile, title));
                // Is this set initially selected?
                if (activeImageSets.contains(imageSet) || selectAllSets) {
                    selectedIndices.add(index);
                }
                imageSets.add(imageSet);
                index++;
            }
        } catch (IOException e) {
            log.error("Failed to read image sets", e);
        }

        lstImageSets.setListData(listData.toArray(EMPTY_PANEL_ARRAY));
        lstImageSets.setSelectedIndices(convertIntegers(selectedIndices));
        /*
        Set the visible row count to the minimum amount that is required for two columns,
        to ensure that there are always two cells for every row (except the last row, which can have either one or two)
         */
        int numRows = listData.size() % 2 == 0 ? listData.size() / 2 : (listData.size() + 1) / 2;
        lstImageSets.setVisibleRowCount(Math.min(numRows, lstImageSets.getVisibleRowCount()));
    }

    /**
     * Converts the specified collection of Integers into an array of ints.
     *
     * @param integers the collection of Integers to convert
     * @return the resulting int array
     */
    private int[] convertIntegers(Collection<Integer> integers) {
        return integers.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public void localize(ResourceBundle languageBundle) {
        setTitle(languageBundle.getString("ShimejiImageSetChooser"));
        jLabel1.setText(languageBundle.getString("SelectImageSetsToUse"));
        useSelectedButton.setText(languageBundle.getString("UseSelected"));
        useAllButton.setText(languageBundle.getString("UseAll"));
        cancelButton.setText(languageBundle.getString("Cancel"));
        clearAllButton.setText(languageBundle.getString("ClearAll"));
        selectAllButton.setText(languageBundle.getString("SelectAll"));
    }

    /**
     * Displays this ImageSetChooser to the user and returns the image sets that were selected.
     * This method will not return until the window has been hidden.
     *
     * @return the selected image sets, or {@code null} if the selection was canceled
     */
    public List<String> display() {
        setLocationRelativeTo(null);
        setVisible(true);
        if (selectionConfirmed) {
            return imageSets;
        }
        return null;
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        pnlList = new javax.swing.JPanel();
        lstImageSets = new javax.swing.JList<>();
        pnlFooter = new javax.swing.JPanel();
        useSelectedButton = new javax.swing.JButton();
        useAllButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        pnlLabels = new javax.swing.JPanel();
        clearAllButton = new javax.swing.JButton();
        slashLabel = new javax.swing.JLabel();
        selectAllButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Shimeji-ee Image Set Chooser");
        setMinimumSize(getPreferredSize());

        jLabel1.setText("Select Image Sets to Use:");

        jScrollPane1.getVerticalScrollBar().setUnitIncrement(9);

        lstImageSets.setCellRenderer(new CustomListCellRenderer());
        lstImageSets.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);
        lstImageSets.setSelectionModel(new CustomListSelectionModel());

        javax.swing.GroupLayout pnlListLayout = new javax.swing.GroupLayout(pnlList);
        pnlList.setLayout(pnlListLayout);
        pnlListLayout.setHorizontalGroup(
            pnlListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lstImageSets, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pnlListLayout.setVerticalGroup(
            pnlListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lstImageSets, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(pnlList);

        pnlFooter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

        useSelectedButton.setText("Use Selected");
        useSelectedButton.setMaximumSize(new java.awt.Dimension(130, 26));
        useSelectedButton.setPreferredSize(new java.awt.Dimension(130, 26));
        useSelectedButton.addActionListener(this::useSelectedButtonActionPerformed);
        pnlFooter.add(useSelectedButton);

        useAllButton.setText("Use All");
        useAllButton.setMaximumSize(new java.awt.Dimension(95, 23));
        useAllButton.setMinimumSize(new java.awt.Dimension(95, 23));
        useAllButton.setPreferredSize(new java.awt.Dimension(130, 26));
        useAllButton.addActionListener(this::useAllButtonActionPerformed);
        pnlFooter.add(useAllButton);

        cancelButton.setText("Cancel");
        cancelButton.setMaximumSize(new java.awt.Dimension(95, 23));
        cancelButton.setMinimumSize(new java.awt.Dimension(95, 23));
        cancelButton.setPreferredSize(new java.awt.Dimension(130, 26));
        cancelButton.addActionListener(this::cancelButtonActionPerformed);
        pnlFooter.add(cancelButton);

        pnlLabels.setLayout(new javax.swing.BoxLayout(pnlLabels, javax.swing.BoxLayout.LINE_AXIS));

        clearAllButton.setFont(clearAllButton.getFont().deriveFont(clearAllButton.getFont().getStyle() | java.awt.Font.BOLD));
        clearAllButton.setForeground(javax.swing.UIManager.getDefaults().getColor("Button.default.focusColor"));
        clearAllButton.setText("Clear All");
        clearAllButton.setBorder(null);
        clearAllButton.setBorderPainted(false);
        clearAllButton.setContentAreaFilled(false);
        clearAllButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        clearAllButton.addActionListener(this::clearAllButtonActionPerformed);
        pnlLabels.add(clearAllButton);

        slashLabel.setText(" / ");
        pnlLabels.add(slashLabel);

        selectAllButton.setFont(selectAllButton.getFont().deriveFont(selectAllButton.getFont().getStyle() | java.awt.Font.BOLD));
        selectAllButton.setForeground(javax.swing.UIManager.getDefaults().getColor("Button.default.focusColor"));
        selectAllButton.setText("Select All");
        selectAllButton.setBorder(null);
        selectAllButton.setBorderPainted(false);
        selectAllButton.setContentAreaFilled(false);
        selectAllButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        selectAllButton.addActionListener(this::selectAllButtonActionPerformed);
        pnlLabels.add(selectAllButton);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 610, Short.MAX_VALUE)
                    .addComponent(pnlFooter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(pnlLabels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1)
                    .addComponent(pnlLabels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlFooter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void clearAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearAllButtonActionPerformed
        lstImageSets.clearSelection();
    }//GEN-LAST:event_clearAllButtonActionPerformed

    private void selectAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllButtonActionPerformed
        // We can't call setSelectionInterval because overriding it with CustomListSelectionModel causes it to
        // not behave as intended when called manually, so we need to call addSelectionInterval instead
        lstImageSets.addSelectionInterval(0, lstImageSets.getModel().getSize() - 1);
        // lstImageSets.setSelectionInterval(0, lstImageSets.getModel().getSize() - 1);
    }//GEN-LAST:event_selectAllButtonActionPerformed

    private void useSelectedButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_useSelectedButtonActionPerformed
        imageSets.clear();

        List<ImageSetPanel> selectedValues = lstImageSets.getSelectedValuesList();
        if (!selectedValues.isEmpty()) {
            for (ImageSetPanel obj : selectedValues) {
                if (obj != null) {
                    imageSets.add(obj.getImageSetName());
                }
            }
        }

        Main.getInstance().getSettings().activeImageSets = imageSets;
        selectionConfirmed = true;
        dispose();
    }//GEN-LAST:event_useSelectedButtonActionPerformed

    private void useAllButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_useAllButtonActionPerformed
        Main.getInstance().getSettings().activeImageSets = imageSets;
        selectionConfirmed = true;
        dispose();
    }//GEN-LAST:event_useAllButtonActionPerformed

    private void cancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Custom list selection model that allows multiple entries in the list to be selected when the mouse
     * is dragged over them.
     * <p>
     * This class was made by <a href="https://stackoverflow.com/a/5831609">Francisco on StackOverflow</a>.
     */
    static class CustomListSelectionModel extends DefaultListSelectionModel {
        /**
         * The previous index of the first end of the selection interval, inclusive.
         */
        private int i0 = -1;
        /**
         * The previous index of the second end of the selection interval, inclusive.
         */
        private int i1 = -1;

        @Override
        public void setSelectionInterval(int index0, int index1) {
            if (i0 == index0 && i1 == index1) {
                // The selection indices are the same as they last were, meaning the mouse
                // is being dragged over the same list entry as it was when this method was last called
                if (getValueIsAdjusting()) {
                    // Set valueIsAdjusting to false to prevent the selection from being changed if we
                    // reach the above conditional statement again, as that would cause flickering
                    // wheneber the mouse is dragged over the current list entry
                    setValueIsAdjusting(false);
                    setSelection(index0, index1);
                }
            } else {
                // The selection indices are not the same as they last were,
                // meaning the mouse is being dragged over a new list entry
                i0 = index0;
                i1 = index1;
                // Set valueIsAdjusting to false to prevent the "getValueIsAdjusting" check above from passing if
                // the mouse is dragged over the currently pressed list entry, because that would cause the selection
                // of the list entry to be toggled as soon as the mouse is dragged over it
                setValueIsAdjusting(false);
                setSelection(index0, index1);
            }
        }

        /**
         * An alternative to calling {@code super.setSelectionInterval()} that delegates to
         * {@link #addSelectionInterval(int, int)} and {@link #removeSelectionInterval(int, int)}
         * to allow selecting multiple list entries without needing to hold down the CTRL key.
         *
         * @param index0 one end of the interval
         * @param index1 other end of the interval
         */
        private void setSelection(int index0, int index1) {
            if (isSelectedIndex(index0)) {
                removeSelectionInterval(index0, index1);
            } else {
                addSelectionInterval(index0, index1);
            }
        }
    }

    /**
     * Custom list cell renderer that only calls the superclass's {@link DefaultListCellRenderer#getListCellRendererComponent
     * getListCellRendererComponent} method if the value passed to the method isn't an instance of {@link ImageSetPanel}.
     */
    static class CustomListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof ImageSetPanel component) {
                component.setSelected(isSelected);
                return component;
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

    /**
     * Program entry point used for quickly testing the ImageSetChooser.
     */
    static void main() {
        EventQueue.invokeLater(() -> {
            new ImageSetChooser(new JFrame(), true).display();
            System.exit(0);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton clearAllButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList<ImageSetPanel> lstImageSets;
    private javax.swing.JPanel pnlFooter;
    private javax.swing.JPanel pnlLabels;
    private javax.swing.JPanel pnlList;
    private javax.swing.JButton selectAllButton;
    private javax.swing.JLabel slashLabel;
    private javax.swing.JButton useAllButton;
    private javax.swing.JButton useSelectedButton;
    // End of variables declaration//GEN-END:variables
}
