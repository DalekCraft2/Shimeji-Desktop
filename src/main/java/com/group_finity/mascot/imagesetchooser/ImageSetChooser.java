package com.group_finity.mascot.imagesetchooser;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.ConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Chooser used to select the Shimeji image sets in use.
 *
 * @author Shimeji-ee Group
 * @since 1.0.2
 */
public class ImageSetChooser extends JDialog {
    private static final Logger log = Logger.getLogger(ImageSetChooser.class.getName());
    private final ArrayList<String> imageSets = new ArrayList<>();
    private boolean closeProgram = true; // Whether the program closes on dispose
    private boolean selectAllSets = false; // Default all to selected

    public ImageSetChooser(Frame owner, boolean modal) {
        super(owner, modal);
        initComponents();
        setLocationRelativeTo(null);

        List<String> activeImageSets = readConfigFile();

        List<ImageSetChooserPanel> data1 = new ArrayList<>();
        List<ImageSetChooserPanel> data2 = new ArrayList<>();
        Collection<Integer> si1 = new ArrayList<>();
        Collection<Integer> si2 = new ArrayList<>();

        // Get list of image sets (directories under img)
        DirectoryStream.Filter<Path> filter = entry -> {
            String fileName = entry.getFileName().toString();
            if (fileName.equalsIgnoreCase("unused") || fileName.startsWith(".")) {
                return false;
            }
            return Files.isDirectory(entry);
        };
        try (DirectoryStream<Path> imageSetDirs = Files.newDirectoryStream(Main.IMAGE_DIRECTORY, filter)) {
            // Create ImageSetChooserPanels for ShimejiList
            boolean onList1 = true;    // Toggle adding between the two lists
            int row = 0;    // Current row
            for (Path imageSetDir : imageSetDirs) {
                String imageSet = imageSetDir.getFileName().toString();

                // Determine actions file
                Path filePath = Main.CONFIG_DIRECTORY;
                Path actionsFile = filePath.resolve("actions.xml");
                if (Files.exists(filePath.resolve("\u52D5\u4F5C.xml"))) {
                    actionsFile = filePath.resolve("\u52D5\u4F5C.xml");
                }

                filePath = Main.CONFIG_DIRECTORY.resolve(imageSet);
                if (Files.exists(filePath.resolve("actions.xml"))) {
                    actionsFile = filePath.resolve("actions.xml");
                } else if (Files.exists(filePath.resolve("\u52D5\u4F5C.xml"))) {
                    actionsFile = filePath.resolve("\u52D5\u4F5C.xml");
                } else if (Files.exists(filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml"))) {
                    actionsFile = filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml");
                } else if (Files.exists(filePath.resolve("\u00A6-\u00BA@.xml"))) {
                    actionsFile = filePath.resolve("\u00A6-\u00BA@.xml");
                } else if (Files.exists(filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml"))) {
                    actionsFile = filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml");
                } else if (Files.exists(filePath.resolve("one.xml"))) {
                    actionsFile = filePath.resolve("one.xml");
                } else if (Files.exists(filePath.resolve("1.xml"))) {
                    actionsFile = filePath.resolve("1.xml");
                }

                filePath = imageSetDir.resolve(Main.CONFIG_DIRECTORY);
                if (Files.exists(filePath.resolve("actions.xml"))) {
                    actionsFile = filePath.resolve("actions.xml");
                } else if (Files.exists(filePath.resolve("\u52D5\u4F5C.xml"))) {
                    actionsFile = filePath.resolve("\u52D5\u4F5C.xml");
                } else if (Files.exists(filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml"))) {
                    actionsFile = filePath.resolve("\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml");
                } else if (Files.exists(filePath.resolve("\u00A6-\u00BA@.xml"))) {
                    actionsFile = filePath.resolve("\u00A6-\u00BA@.xml");
                } else if (Files.exists(filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml"))) {
                    actionsFile = filePath.resolve("\u00F4\u00AB\u00EC\u00FD.xml");
                } else if (Files.exists(filePath.resolve("one.xml"))) {
                    actionsFile = filePath.resolve("one.xml");
                } else if (Files.exists(filePath.resolve("1.xml"))) {
                    actionsFile = filePath.resolve("1.xml");
                }

                // Determine behaviours file
                filePath = Main.CONFIG_DIRECTORY;
                Path behaviorsFile = filePath.resolve("behaviors.xml");
                if (Files.exists(filePath.resolve("\u884C\u52D5.xml"))) {
                    behaviorsFile = filePath.resolve("\u884C\u52D5.xml");
                }

                filePath = Main.CONFIG_DIRECTORY.resolve(imageSet);
                if (Files.exists(filePath.resolve("behaviors.xml"))) {
                    behaviorsFile = filePath.resolve("behaviors.xml");
                } else if (Files.exists(filePath.resolve("behavior.xml"))) {
                    behaviorsFile = filePath.resolve("behavior.xml");
                } else if (Files.exists(filePath.resolve("\u884C\u52D5.xml"))) {
                    behaviorsFile = filePath.resolve("\u884C\u52D5.xml");
                } else if (Files.exists(filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml"))) {
                    behaviorsFile = filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml");
                } else if (Files.exists(filePath.resolve("\u00AA\u00B5\u00A6-.xml"))) {
                    behaviorsFile = filePath.resolve("\u00AA\u00B5\u00A6-.xml");
                } else if (Files.exists(filePath.resolve("\u00ECs\u00F4\u00AB.xml"))) {
                    behaviorsFile = filePath.resolve("\u00ECs\u00F4\u00AB.xml");
                } else if (Files.exists(filePath.resolve("two.xml"))) {
                    behaviorsFile = filePath.resolve("two.xml");
                } else if (Files.exists(filePath.resolve("2.xml"))) {
                    behaviorsFile = filePath.resolve("2.xml");
                }

                filePath = imageSetDir.resolve(Main.CONFIG_DIRECTORY);
                if (Files.exists(filePath.resolve("behaviors.xml"))) {
                    behaviorsFile = filePath.resolve("behaviors.xml");
                } else if (Files.exists(filePath.resolve("behavior.xml"))) {
                    behaviorsFile = filePath.resolve("behavior.xml");
                } else if (Files.exists(filePath.resolve("\u884C\u52D5.xml"))) {
                    behaviorsFile = filePath.resolve("\u884C\u52D5.xml");
                } else if (Files.exists(filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml"))) {
                    behaviorsFile = filePath.resolve("\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml");
                } else if (Files.exists(filePath.resolve("\u00AA\u00B5\u00A6-.xml"))) {
                    behaviorsFile = filePath.resolve("\u00AA\u00B5\u00A6-.xml");
                } else if (Files.exists(filePath.resolve("\u00ECs\u00F4\u00AB.xml"))) {
                    behaviorsFile = filePath.resolve("\u00ECs\u00F4\u00AB.xml");
                } else if (Files.exists(filePath.resolve("two.xml"))) {
                    behaviorsFile = filePath.resolve("two.xml");
                } else if (Files.exists(filePath.resolve("2.xml"))) {
                    behaviorsFile = filePath.resolve("2.xml");
                }

                // Determine information file
                filePath = Main.CONFIG_DIRECTORY;
                Path infoFile = filePath.resolve("info.xml");

                filePath = Main.CONFIG_DIRECTORY.resolve(imageSet);
                if (Files.exists(filePath.resolve("info.xml"))) {
                    infoFile = filePath.resolve("info.xml");
                }

                filePath = imageSetDir.resolve(Main.CONFIG_DIRECTORY);
                if (Files.exists(filePath.resolve("info.xml"))) {
                    infoFile = filePath.resolve("info.xml");
                }

                Path imageFile = imageSetDir.resolve("shime1.png");
                String caption = imageSet;
                try {
                    if (Files.exists(infoFile)) {
                        Configuration configuration = new Configuration();

                        final Document information = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Files.newInputStream(infoFile));

                        configuration.load(new Entry(information.getDocumentElement()), imageSet);

                        if (configuration.containsInformationKey(configuration.getSchema().getString("Name"))) {
                            caption = configuration.getInformation(configuration.getSchema().getString("Name"));
                        }
                        if (configuration.containsInformationKey(configuration.getSchema().getString("PreviewImage"))) {
                            imageFile = imageSetDir.resolve(configuration.getInformation(configuration.getSchema().getString("PreviewImage")));
                        }
                    }

                } catch (ConfigurationException | ParserConfigurationException | IOException | SAXException ex) {
                    imageFile = imageSetDir.resolve("shime1.png");
                    caption = imageSet;
                }

                if (onList1) {
                    onList1 = false;
                    data1.add(new ImageSetChooserPanel(imageSet, actionsFile.toString(),
                            behaviorsFile.toString(), imageFile, caption));
                    // Is this set initially selected?
                    if (activeImageSets.contains(imageSet) || selectAllSets) {
                        si1.add(row);
                    }
                } else {
                    onList1 = true;
                    data2.add(new ImageSetChooserPanel(imageSet, actionsFile.toString(),
                            behaviorsFile.toString(), imageFile, caption));
                    // Is this set initially selected?
                    if (activeImageSets.contains(imageSet) || selectAllSets) {
                        si2.add(row);
                    }
                    row++; // Only increment the row number after the second column
                }
                imageSets.add(imageSet);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to read image sets", e);
        }

        setUpList(jList1);
        jList1.setListData(data1.toArray(new ImageSetChooserPanel[0]));
        jList1.setSelectedIndices(convertIntegers(si1));

        setUpList(jList2);
        jList2.setListData(data2.toArray(new ImageSetChooserPanel[0]));
        jList2.setSelectedIndices(convertIntegers(si2));
    }

    public ArrayList<String> display() {
        setTitle(Main.getInstance().getLanguageBundle().getString("ShimejiImageSetChooser"));
        jLabel1.setText(Main.getInstance().getLanguageBundle().getString("SelectImageSetsToUse"));
        useSelectedButton.setText(Main.getInstance().getLanguageBundle().getString("UseSelected"));
        useAllButton.setText(Main.getInstance().getLanguageBundle().getString("UseAll"));
        cancelButton.setText(Main.getInstance().getLanguageBundle().getString("Cancel"));
        clearAllLabel.setText(Main.getInstance().getLanguageBundle().getString("ClearAll"));
        selectAllLabel.setText(Main.getInstance().getLanguageBundle().getString("SelectAll"));
        setVisible(true);
        if (closeProgram) {
            return null;
        }
        return imageSets;
    }

    private List<String> readConfigFile() {
        // now with properties style loading!
        List<String> activeImageSets = new ArrayList<>(Arrays.asList(Main.getInstance().getProperties().getProperty("ActiveShimeji", "").split("/")));
        selectAllSets = activeImageSets.get(0).trim().isEmpty(); // if no active ones, activate them all!
        return activeImageSets;
    }

    private void updateConfigFile() {
        try (OutputStream output = Files.newOutputStream(Main.SETTINGS_FILE)) {
            Main.getInstance().getProperties().setProperty("ActiveShimeji", imageSets.toString().replace("[", "").replace("]", "").replace(", ", "/"));
            Main.getInstance().getProperties().store(output, "Shimeji-ee Configuration Options");
        } catch (IOException e) {
            // Doesn't matter at all
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        pnlLists = new javax.swing.JPanel();
        jList1 = new ShimejiList();
        jList2 = new ShimejiList();
        jLabel1 = new javax.swing.JLabel();
        pnlFooter = new javax.swing.JPanel();
        useSelectedButton = new javax.swing.JButton();
        useAllButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        pnlLabels = new javax.swing.JPanel();
        clearAllLabel = new javax.swing.JLabel();
        slashLabel = new javax.swing.JLabel();
        selectAllLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Shimeji-ee Image Set Chooser");
        setMinimumSize(new java.awt.Dimension(670, 495));

        jScrollPane1.setPreferredSize(new java.awt.Dimension(518, 100));
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(9);

        javax.swing.GroupLayout pnlListsLayout = new javax.swing.GroupLayout(pnlLists);
        pnlLists.setLayout(pnlListsLayout);
        pnlListsLayout.setHorizontalGroup(
            pnlListsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlListsLayout.createSequentialGroup()
                .addComponent(jList1, javax.swing.GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jList2, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
        );
        pnlListsLayout.setVerticalGroup(
            pnlListsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jList2, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
            .addComponent(jList1, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(pnlLists);

        jLabel1.setText("Select Image Sets to Use:");

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

        clearAllLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        clearAllLabel.setForeground(javax.swing.UIManager.getDefaults().getColor("Button.default.focusColor"));
        clearAllLabel.setText("Clear All");
        clearAllLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        clearAllLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                clearAllLabelMouseClicked(evt);
            }
        });
        pnlLabels.add(clearAllLabel);

        slashLabel.setText(" / ");
        pnlLabels.add(slashLabel);

        selectAllLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        selectAllLabel.setForeground(javax.swing.UIManager.getDefaults().getColor("Button.default.focusColor"));
        selectAllLabel.setText("Select All");
        selectAllLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        selectAllLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectAllLabelMouseClicked(evt);
            }
        });
        pnlLabels.add(selectAllLabel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                    .addComponent(pnlFooter, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 382, Short.MAX_VALUE)
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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 378, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlFooter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void clearAllLabelMouseClicked(MouseEvent evt) {//GEN-FIRST:event_clearAllLabelMouseClicked
        jList1.clearSelection();
        jList2.clearSelection();
    }//GEN-LAST:event_clearAllLabelMouseClicked

    private void selectAllLabelMouseClicked(MouseEvent evt) {//GEN-FIRST:event_selectAllLabelMouseClicked
        jList1.setSelectionInterval(0, jList1.getModel().getSize() - 1);
        jList2.setSelectionInterval(0, jList2.getModel().getSize() - 1);
    }//GEN-LAST:event_selectAllLabelMouseClicked

    private void useSelectedButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_useSelectedButtonActionPerformed
        imageSets.clear();

        for (ImageSetChooserPanel obj : jList1.getSelectedValuesList()) {
            if (obj != null) {
                imageSets.add(obj.getImageSetName());
            }
        }

        for (ImageSetChooserPanel obj : jList2.getSelectedValuesList()) {
            if (obj != null) {
                imageSets.add(obj.getImageSetName());
            }
        }

        updateConfigFile();
        closeProgram = false;
        dispose();
    }//GEN-LAST:event_useSelectedButtonActionPerformed

    private void useAllButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_useAllButtonActionPerformed
        updateConfigFile();
        closeProgram = false;
        dispose();
    }//GEN-LAST:event_useAllButtonActionPerformed

    private void cancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private int[] convertIntegers(Collection<Integer> integers) {
        return integers.stream().mapToInt(Integer::intValue).toArray();
    }

    private void setUpList(JList<?> list) {
        list.setSelectionModel(new DefaultListSelectionModel() {
            private int i0 = -1;
            private int i1 = -1;

            @Override
            public void setSelectionInterval(int index0, int index1) {
                // These statements ensure that the buttons do not flicker whenever the cursor is dragged over them
                // This code was made by Francisco on StackOverflow (https://stackoverflow.com/a/5831609)
                if (i0 == index0 && i1 == index1) {
                    if (getValueIsAdjusting()) {
                        setValueIsAdjusting(false);
                        setSelection(index0, index1);
                    }
                } else {
                    i0 = index0;
                    i1 = index1;
                    setValueIsAdjusting(false);
                    setSelection(index0, index1);
                }
            }

            private void setSelection(int index0, int index1) {
                if (isSelectedIndex(index0)) {
                    removeSelectionInterval(index0, index1);
                } else {
                    addSelectionInterval(index0, index1);
                }
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            new ImageSetChooser(new JFrame(), true).display();
            System.exit(0);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel clearAllLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList<ImageSetChooserPanel> jList1;
    private javax.swing.JList<ImageSetChooserPanel> jList2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel pnlFooter;
    private javax.swing.JPanel pnlLabels;
    private javax.swing.JPanel pnlLists;
    private javax.swing.JLabel selectAllLabel;
    private javax.swing.JLabel slashLabel;
    private javax.swing.JButton useAllButton;
    private javax.swing.JButton useSelectedButton;
    // End of variables declaration//GEN-END:variables
}
