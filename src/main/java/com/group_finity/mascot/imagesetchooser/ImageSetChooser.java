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

        List<String> activeImageSets = readConfigFile();

        List<ImageSetChooserPanel> listData = new ArrayList<>();
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
            // Create ImageSetChooserPanels for ShimejiList
            int index = 0;
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

                listData.add(new ImageSetChooserPanel(imageSet, actionsFile.toString(),
                        behaviorsFile.toString(), imageFile, caption));
                // Is this set initially selected?
                if (activeImageSets.contains(imageSet) || selectAllSets) {
                    selectedIndices.add(index);
                }
                imageSets.add(imageSet);
                index++;
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to read image sets", e);
        }

        setUpList(lstImageSets);
        lstImageSets.setListData(listData.toArray(new ImageSetChooserPanel[0]));
        lstImageSets.setSelectedIndices(convertIntegers(selectedIndices));
    }

    public ArrayList<String> display() {
        setTitle(Main.getInstance().getLanguageBundle().getString("ShimejiImageSetChooser"));
        jLabel1.setText(Main.getInstance().getLanguageBundle().getString("SelectImageSetsToUse"));
        useSelectedButton.setText(Main.getInstance().getLanguageBundle().getString("UseSelected"));
        useAllButton.setText(Main.getInstance().getLanguageBundle().getString("UseAll"));
        cancelButton.setText(Main.getInstance().getLanguageBundle().getString("Cancel"));
        clearAllLabel.setText(Main.getInstance().getLanguageBundle().getString("ClearAll"));
        selectAllLabel.setText(Main.getInstance().getLanguageBundle().getString("SelectAll"));
        setLocationRelativeTo(null);
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
        pnlList = new javax.swing.JPanel();
        lstImageSets = new ShimejiList();
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
        setMinimumSize(getPreferredSize());

        jScrollPane1.getVerticalScrollBar().setUnitIncrement(9);

        lstImageSets.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);

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

    private void clearAllLabelMouseClicked(MouseEvent evt) {//GEN-FIRST:event_clearAllLabelMouseClicked
        lstImageSets.clearSelection();
    }//GEN-LAST:event_clearAllLabelMouseClicked

    private void selectAllLabelMouseClicked(MouseEvent evt) {//GEN-FIRST:event_selectAllLabelMouseClicked
        lstImageSets.addSelectionInterval(0, lstImageSets.getModel().getSize() - 1);
    }//GEN-LAST:event_selectAllLabelMouseClicked

    private void useSelectedButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_useSelectedButtonActionPerformed
        imageSets.clear();

        for (ImageSetChooserPanel obj : lstImageSets.getSelectedValuesList()) {
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
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList<ImageSetChooserPanel> lstImageSets;
    private javax.swing.JPanel pnlFooter;
    private javax.swing.JPanel pnlLabels;
    private javax.swing.JPanel pnlList;
    private javax.swing.JLabel selectAllLabel;
    private javax.swing.JLabel slashLabel;
    private javax.swing.JButton useAllButton;
    private javax.swing.JButton useSelectedButton;
    // End of variables declaration//GEN-END:variables
}
