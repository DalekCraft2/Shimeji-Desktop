package com.group_finity.mascot.imagesetchooser;

import com.group_finity.mascot.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chooser used to select the Shimeji image sets in use.
 */
public class ImageSetChooser extends JDialog {
    private static final String configFile = "./conf/settings.properties"; // Config file name
    private static final String topDir = "./img"; // Top Level Directory
    private final ArrayList<String> imageSets = new ArrayList<>();
    private boolean closeProgram = true; // Whether the program closes on dispose
    private boolean selectAllSets = false; // Default all to selected

    public ImageSetChooser(Frame owner, boolean modal) {
        super(owner, modal);
        initComponents();
        setLocationRelativeTo(null);

        ArrayList<String> activeImageSets = readConfigFile();

        ArrayList<ImageSetChooserPanel> data1 = new ArrayList<>();
        ArrayList<ImageSetChooserPanel> data2 = new ArrayList<>();
        ArrayList<Integer> si1 = new ArrayList<>();
        ArrayList<Integer> si2 = new ArrayList<>();

        // Get list of imagesets (directories under img)
        FilenameFilter fileFilter = (dir, name) -> {
            if (name.equalsIgnoreCase("unused") || name.startsWith(".")) {
                return false;
            }
            return new File(dir + "/" + name).isDirectory();
        };
        File dir = new File(topDir);
        String[] children = dir.list(fileFilter);

        // Create ImageSetChooserPanels for ShimejiList
        boolean onList1 = true;    // Toggle adding between the two lists
        int row = 0;    // Current row
        if (children != null) {
            for (String imageSet : children) {
                String imageFile = topDir + "/" + imageSet + "/shime1.png";

                // Determine actions file
                String filePath = "./conf/";
                String actionsFile = filePath + "actions.xml";
                if (new File(filePath + "\u52D5\u4F5C.xml").exists()) {
                    actionsFile = filePath + "\u52D5\u4F5C.xml";
                }

                filePath = "./conf/" + imageSet + "/";
                if (new File(filePath + "actions.xml").exists()) {
                    actionsFile = filePath + "actions.xml";
                }
                if (new File(filePath + "\u52D5\u4F5C.xml").exists()) {
                    actionsFile = filePath + "\u52D5\u4F5C.xml";
                }
                if (new File(filePath + "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml").exists()) {
                    actionsFile = filePath + "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml";
                }
                if (new File(filePath + "\u00A6-\u00BA@.xml").exists()) {
                    actionsFile = filePath + "\u00A6-\u00BA@.xml";
                }
                if (new File(filePath + "\u00F4\u00AB\u00EC\u00FD.xml").exists()) {
                    actionsFile = filePath + "\u00F4\u00AB\u00EC\u00FD.xml";
                }
                if (new File(filePath + "one.xml").exists()) {
                    actionsFile = filePath + "one.xml";
                }
                if (new File(filePath + "1.xml").exists()) {
                    actionsFile = filePath + "1.xml";
                }

                filePath = "./img/" + imageSet + "/conf/";
                if (new File(filePath + "actions.xml").exists()) {
                    actionsFile = filePath + "actions.xml";
                }
                if (new File(filePath + "\u52D5\u4F5C.xml").exists()) {
                    actionsFile = filePath + "\u52D5\u4F5C.xml";
                }
                if (new File(filePath + "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml").exists()) {
                    actionsFile = filePath + "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml";
                }
                if (new File(filePath + "\u00A6-\u00BA@.xml").exists()) {
                    actionsFile = filePath + "\u00A6-\u00BA@.xml";
                }
                if (new File(filePath + "\u00F4\u00AB\u00EC\u00FD.xml").exists()) {
                    actionsFile = filePath + "\u00F4\u00AB\u00EC\u00FD.xml";
                }
                if (new File(filePath + "one.xml").exists()) {
                    actionsFile = filePath + "one.xml";
                }
                if (new File(filePath + "1.xml").exists()) {
                    actionsFile = filePath + "1.xml";
                }

                // Determine behaviours file
                filePath = "./conf/";
                String behaviorsFile = filePath + "behaviors.xml";
                if (new File(filePath + "\u884C\u52D5.xml").exists()) {
                    behaviorsFile = filePath + "\u884C\u52D5.xml";
                }

                filePath = "./conf/" + imageSet + "/";
                if (new File(filePath + "behaviors.xml").exists()) {
                    behaviorsFile = filePath + "behaviors.xml";
                }
                if (new File(filePath + "behavior.xml").exists()) {
                    behaviorsFile = filePath + "behavior.xml";
                }
                if (new File(filePath + "\u884C\u52D5.xml").exists()) {
                    behaviorsFile = filePath + "\u884C\u52D5.xml";
                }
                if (new File(filePath + "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml").exists()) {
                    behaviorsFile = filePath + "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml";
                }
                if (new File(filePath + "\u00AA\u00B5\u00A6-.xml").exists()) {
                    behaviorsFile = filePath + "\u00AA\u00B5\u00A6-.xml";
                }
                if (new File(filePath + "\u00ECs\u00F4\u00AB.xml").exists()) {
                    behaviorsFile = filePath + "\u00ECs\u00F4\u00AB.xml";
                }
                if (new File(filePath + "two.xml").exists()) {
                    behaviorsFile = filePath + "two.xml";
                }
                if (new File(filePath + "2.xml").exists()) {
                    behaviorsFile = filePath + "2.xml";
                }

                filePath = "./img/" + imageSet + "/conf/";
                if (new File(filePath + "behaviors.xml").exists()) {
                    behaviorsFile = filePath + "behaviors.xml";
                }
                if (new File(filePath + "behavior.xml").exists()) {
                    behaviorsFile = filePath + "behavior.xml";
                }
                if (new File(filePath + "\u884C\u52D5.xml").exists()) {
                    behaviorsFile = filePath + "\u884C\u52D5.xml";
                }
                if (new File(filePath + "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml").exists()) {
                    behaviorsFile = filePath + "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml";
                }
                if (new File(filePath + "\u00AA\u00B5\u00A6-.xml").exists()) {
                    behaviorsFile = filePath + "\u00AA\u00B5\u00A6-.xml";
                }
                if (new File(filePath + "\u00ECs\u00F4\u00AB.xml").exists()) {
                    behaviorsFile = filePath + "\u00ECs\u00F4\u00AB.xml";
                }
                if (new File(filePath + "two.xml").exists()) {
                    behaviorsFile = filePath + "two.xml";
                }
                if (new File(filePath + "2.xml").exists()) {
                    behaviorsFile = filePath + "2.xml";
                }

                if (onList1) {
                    onList1 = false;
                    data1.add(new ImageSetChooserPanel(imageSet, actionsFile,
                            behaviorsFile, imageFile));
                    // Is this set initially selected?
                    if (activeImageSets.contains(imageSet) || selectAllSets) {
                        si1.add(row);
                    }
                } else {
                    onList1 = true;
                    data2.add(new ImageSetChooserPanel(imageSet, actionsFile,
                            behaviorsFile, imageFile));
                    // Is this set initially selected?
                    if (activeImageSets.contains(imageSet) || selectAllSets) {
                        si2.add(row);
                    }
                    row++; // Only increment the row number after the second column
                }
                imageSets.add(imageSet);
            }
        }

        setUpList1();
        jList1.setListData(data1.toArray(new ImageSetChooserPanel[0]));
        jList1.setSelectedIndices(convertIntegers(si1));

        setUpList2();
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

    private ArrayList<String> readConfigFile() {
        // now with properties style loading!
        ArrayList<String> activeImageSets = new ArrayList<>(Arrays.asList(Main.getInstance().getProperties().getProperty("ActiveShimeji", "").split("/")));
        selectAllSets = activeImageSets.get(0).trim().isEmpty(); // if no active ones, activate them all!
        return activeImageSets;
    }

    private void updateConfigFile() {
        try (FileOutputStream output = new FileOutputStream(configFile)) {
            Main.getInstance().getProperties().setProperty("ActiveShimeji", imageSets.toString().replace("[", "").replace("]", "").replace(", ", "/"));
            Main.getInstance().getProperties().store(output, "Shimeji-ee Configuration Options");
        } catch (Exception e) {
            // Doesn't matter at all
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        jScrollPane1 = new JScrollPane();
        jPanel2 = new JPanel();
        jList1 = new ShimejiList();
        jList2 = new ShimejiList();
        jLabel1 = new JLabel();
        jPanel1 = new JPanel();
        useSelectedButton = new JButton();
        useAllButton = new JButton();
        cancelButton = new JButton();
        jPanel4 = new JPanel();
        clearAllLabel = new JLabel();
        slashLabel = new JLabel();
        selectAllLabel = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Shimeji-ee Image Set Chooser");
        setMinimumSize(new Dimension(670, 495));

        jScrollPane1.setPreferredSize(new Dimension(518, 100));

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jList1, GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
                                .addGap(0, 0, 0)
                                .addComponent(jList2, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)));
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jList2, GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                        .addComponent(jList1, GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE));

        jScrollPane1.setViewportView(jPanel2);

        jLabel1.setText("Select Image Sets to Use:");

        jPanel1.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));

        useSelectedButton.setText("Use Selected");
        useSelectedButton.setMaximumSize(new Dimension(130, 26));
        useSelectedButton.setPreferredSize(new Dimension(130, 26));
        useSelectedButton.addActionListener(this::useSelectedButtonActionPerformed);
        jPanel1.add(useSelectedButton);

        useAllButton.setText("Use All");
        useAllButton.setMaximumSize(new Dimension(95, 23));
        useAllButton.setMinimumSize(new Dimension(95, 23));
        useAllButton.setPreferredSize(new Dimension(130, 26));
        useAllButton.addActionListener(this::useAllButtonActionPerformed);
        jPanel1.add(useAllButton);

        cancelButton.setText("Cancel");
        cancelButton.setMaximumSize(new Dimension(95, 23));
        cancelButton.setMinimumSize(new Dimension(95, 23));
        cancelButton.setPreferredSize(new Dimension(130, 26));
        cancelButton.addActionListener(this::cancelButtonActionPerformed);
        jPanel1.add(cancelButton);

        jPanel4.setLayout(new BoxLayout(jPanel4, BoxLayout.LINE_AXIS));

        clearAllLabel.setForeground(new Color(0, 0, 204));
        clearAllLabel.setText("Clear All");
        clearAllLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearAllLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clearAllLabelMouseClicked(e);
            }
        });
        jPanel4.add(clearAllLabel);

        slashLabel.setText(" / ");
        jPanel4.add(slashLabel);

        selectAllLabel.setForeground(new Color(0, 0, 204));
        selectAllLabel.setText("Select All");
        selectAllLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selectAllLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectAllLabelMouseClicked(e);
            }
        });
        jPanel4.add(selectAllLabel);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 384, Short.MAX_VALUE)
                                                .addComponent(jPanel4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE))
                                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(jLabel1)
                                        .addComponent(jPanel4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(11, 11, 11)));

        pack();
    }// </editor-fold>

    private void clearAllLabelMouseClicked(MouseEvent evt) {
        jList1.clearSelection();
        jList2.clearSelection();
    }

    private void selectAllLabelMouseClicked(MouseEvent evt) {
        jList1.setSelectionInterval(0, jList1.getModel().getSize() - 1);
        jList2.setSelectionInterval(0, jList2.getModel().getSize() - 1);
    }

    private void useSelectedButtonActionPerformed(ActionEvent evt) {
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
    }

    private void useAllButtonActionPerformed(ActionEvent evt) {
        updateConfigFile();
        closeProgram = false;
        dispose();
    }

    private void cancelButtonActionPerformed(ActionEvent evt) {
        dispose();
    }

    private int[] convertIntegers(List<Integer> integers) {
        return integers.stream().mapToInt(integer -> integer).toArray();
    }

    private void setUpList1() {
        jList1.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (isSelectedIndex(index0)) {
                    removeSelectionInterval(index0, index1);
                } else {
                    addSelectionInterval(index0, index1);
                }
            }
        });
    }

    private void setUpList2() {
        jList2.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
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

    // Variables declaration - do not modify
    private JButton cancelButton;
    private JLabel clearAllLabel;
    private JLabel jLabel1;
    private JList<ImageSetChooserPanel> jList1;
    private JList<ImageSetChooserPanel> jList2;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JPanel jPanel4;
    private JScrollPane jScrollPane1;
    private JLabel selectAllLabel;
    private JLabel slashLabel;
    private JButton useAllButton;
    private JButton useSelectedButton;
    // End of variables declaration
}
