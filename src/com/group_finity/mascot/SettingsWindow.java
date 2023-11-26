/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.group_finity.mascot;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Monolith
 */
public class SettingsWindow extends JDialog {
    private static final Logger log = Logger.getLogger(SettingsWindow.class.getName());
    private static final String configFile = "./conf/settings.properties";    // Config file name
    private final ArrayList<String> listData = new ArrayList<>();
    private Boolean alwaysShowShimejiChooser = false;
    private String filter = "nearest";
    private double scaling = 1.0;
    private Boolean windowedMode = false;
    private Dimension windowSize = new Dimension(600, 500);
    private Color background = new Color(0, 255, 0);

    private Boolean imageReloadRequired = false;
    private Boolean interactiveWindowReloadRequired = false;
    private Boolean environmentReloadRequired = false;

    /**
     * Creates new form SettingsWindow
     */
    public SettingsWindow(Frame owner, boolean modal) {
        super(owner, modal);
        initComponents();
    }

    public boolean display() {
        // initialise controls
        setLocationRelativeTo(null);
        grpFilter.add(radFilterNearest);
        grpFilter.add(radFilterBicubic);
        grpFilter.add(radFilterHqx);
        // TODO Try to convert this to a Map because Dictionary is obsolete
        Dictionary<Integer, JLabel> labelTable = IntStream.range(0, 9).boxed().collect(Collectors.toMap(index -> index * 10, index -> new JLabel(index + "x"), (a, b) -> b, Hashtable::new));
        sldScaling.setLabelTable(labelTable);
        sldScaling.setPaintLabels(true);
        sldScaling.setSnapToTicks(true);

        // load existing settings
        Properties properties = Main.getInstance().getProperties();
        alwaysShowShimejiChooser = Boolean.parseBoolean(properties.getProperty("AlwaysShowShimejiChooser", "false"));
        String filterText = Main.getInstance().getProperties().getProperty("Filter", "false");
        filter = "nearest";
        if (filterText.equalsIgnoreCase("true") || filterText.equalsIgnoreCase("hqx")) {
            filter = "hqx";
        } else if (filterText.equalsIgnoreCase("bicubic")) {
            filter = "bicubic";
        }
        scaling = Double.parseDouble(properties.getProperty("Scaling", "1.0"));
        windowedMode = properties.getProperty("Environment", "generic").equals("virtual");
        String[] windowArray = properties.getProperty("WindowSize", "600x500").split("x");
        windowSize = new Dimension(Integer.parseInt(windowArray[0]), Integer.parseInt(windowArray[1]));
        background = Color.decode(properties.getProperty("Background", "#00FF00"));

        chkAlwaysShowShimejiChooser.setSelected(alwaysShowShimejiChooser);
        if (filter.equals("bicubic")) {
            radFilterBicubic.setSelected(true);
        } else if (filter.equals("hqx")) {
            radFilterHqx.setSelected(true);
        } else {
            radFilterNearest.setSelected(true);
        }
        sldScaling.setValue((int) (scaling * 10));
        listData.addAll(Arrays.asList(properties.getProperty("InteractiveWindows", "").split("/")));
        lstInteractiveWindows.setListData(listData.toArray(new String[0]));
        chkWindowModeEnabled.setSelected(windowedMode);
        spnWindowWidth.setBackground(txtBackground.getBackground());
        spnWindowHeight.setBackground(txtBackground.getBackground());
        spnWindowWidth.setEnabled(windowedMode);
        spnWindowHeight.setEnabled(windowedMode);
        spnWindowWidth.setValue(windowSize.width);
        spnWindowHeight.setValue(windowSize.height);
        txtBackground.setText(String.format("#%02X%02X%02X", background.getRed(), background.getGreen(), background.getBlue()));
        btnBackgroundChange.setEnabled(windowedMode);
        pnlBackgroundPreview.setBackground(background);

        // localisation
        ResourceBundle language = Main.getInstance().getLanguageBundle();
        setTitle(language.getString("Settings"));
        pnlTabs.setTitleAt(0, language.getString("General"));
        pnlTabs.setTitleAt(1, language.getString("InteractiveWindows"));
        pnlTabs.setTitleAt(2, language.getString("WindowMode"));
        pnlTabs.setTitleAt(3, language.getString("About"));
        chkAlwaysShowShimejiChooser.setText(language.getString("AlwaysShowShimejiChooser"));
        lblScaling.setText(language.getString("Scaling"));
        lblFilter.setText(language.getString("FilterOptions"));
        radFilterNearest.setText(language.getString("NearestNeighbour"));
        radFilterHqx.setText(language.getString("Filter"));
        radFilterBicubic.setText(language.getString("BicubicFilter"));
        btnAddInteractiveWindow.setText(language.getString("Add"));
        btnRemoveInteractiveWindow.setText(language.getString("Remove"));
        chkWindowModeEnabled.setText(language.getString("WindowedModeEnabled"));
        lblDimensions.setText(language.getString("Dimensions"));
        lblBackground.setText(language.getString("Background"));
        btnBackgroundChange.setText(language.getString("Change"));
        lblShimejiEE.setText(language.getString("ShimejiEE"));
        lblDevelopedBy.setText(language.getString("DevelopedBy"));
        btnWebsite.setText(language.getString("Website"));
        btnDone.setText(language.getString("Done"));
        btnCancel.setText(language.getString("Cancel"));

        // scale controls to fit
        getContentPane().setPreferredSize(new Dimension(500, 360));
        sldScaling.setPreferredSize(new Dimension(sldScaling.getPreferredSize().width, sldScaling.getPreferredSize().height));
        btnAddInteractiveWindow.setPreferredSize(new Dimension(btnAddInteractiveWindow.getPreferredSize().width, btnAddInteractiveWindow.getPreferredSize().height));
        btnRemoveInteractiveWindow.setPreferredSize(new Dimension(btnRemoveInteractiveWindow.getPreferredSize().width, btnRemoveInteractiveWindow.getPreferredSize().height));
        pnlInteractiveButtons.setPreferredSize(new Dimension(pnlInteractiveButtons.getPreferredSize().width, btnAddInteractiveWindow.getPreferredSize().height + 6));
        spnWindowWidth.setPreferredSize(new Dimension(spnWindowWidth.getPreferredSize().width, spnWindowWidth.getPreferredSize().height));
        spnWindowHeight.setPreferredSize(new Dimension(spnWindowHeight.getPreferredSize().width, spnWindowHeight.getPreferredSize().height));
        txtBackground.setPreferredSize(new Dimension(txtBackground.getPreferredSize().width, txtBackground.getPreferredSize().height));
        pnlBackgroundPreview.setPreferredSize(new Dimension(pnlBackgroundPreview.getPreferredSize().width, pnlBackgroundPreview.getPreferredSize().height));
        lblIcon.setPreferredSize(new Dimension(lblIcon.getPreferredSize().width, lblIcon.getPreferredSize().height));
        lblIcon.setMaximumSize(lblIcon.getPreferredSize());
        if (!getIconImages().isEmpty()) {
            lblIcon.setIcon(new ImageIcon(getIconImages().get(0).getScaledInstance(lblIcon.getPreferredSize().width, lblIcon.getPreferredSize().height, Image.SCALE_DEFAULT)));
        }
        btnWebsite.setPreferredSize(new Dimension(btnWebsite.getPreferredSize().width, btnWebsite.getPreferredSize().height));
        btnDiscord.setPreferredSize(new Dimension(btnDiscord.getPreferredSize().width, btnDiscord.getPreferredSize().height));
        btnPatreon.setPreferredSize(new Dimension(btnPatreon.getPreferredSize().width, btnPatreon.getPreferredSize().height));
        pnlAboutButtons.setPreferredSize(new Dimension(pnlAboutButtons.getPreferredSize().width, btnWebsite.getPreferredSize().height + 6));
        btnDone.setPreferredSize(new Dimension(btnDone.getPreferredSize().width, btnDone.getPreferredSize().height));
        btnCancel.setPreferredSize(new Dimension(btnCancel.getPreferredSize().width, btnCancel.getPreferredSize().height));
        pnlFooter.setPreferredSize(new Dimension(pnlFooter.getPreferredSize().width, btnDone.getPreferredSize().height + 6));
        pack();
        setVisible(true);

        return true;
    }

    private void browseToUrl(String url) {
        try {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(url));
            } else {
                throw new UnsupportedOperationException(Main.getInstance().getLanguageBundle().getString("FailedOpenWebBrowserErrorMessage") + " " + url);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.PLAIN_MESSAGE);
        }
    }

    public boolean getEnvironmentReloadRequired() {
        return environmentReloadRequired;
    }

    public boolean getImageReloadRequired() {
        return imageReloadRequired;
    }

    public boolean getInteractiveWindowReloadRequired() {
        return interactiveWindowReloadRequired;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        grpFilter = new ButtonGroup();
        pnlTabs = new JTabbedPane();
        pnlGeneral = new JPanel();
        chkAlwaysShowShimejiChooser = new JCheckBox();
        lblScaling = new JLabel();
        sldScaling = new JSlider();
        lblFilter = new JLabel();
        radFilterNearest = new JRadioButton();
        radFilterBicubic = new JRadioButton();
        radFilterHqx = new JRadioButton();
        pnlInteractiveWindows = new JPanel();
        pnlInteractiveButtons = new JPanel();
        btnAddInteractiveWindow = new JButton();
        btnRemoveInteractiveWindow = new JButton();
        jScrollPane1 = new JScrollPane();
        lstInteractiveWindows = new JList<>();
        pnlWindowMode = new JPanel();
        chkWindowModeEnabled = new JCheckBox();
        pnlBackgroundPreview = new JPanel();
        lblDimensions = new JLabel();
        lblDimensionsX = new JLabel();
        lblBackground = new JLabel();
        txtBackground = new JTextField();
        btnBackgroundChange = new JButton();
        spnWindowWidth = new JSpinner();
        spnWindowHeight = new JSpinner();
        pnlAbout = new JPanel();
        glue1 = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        lblIcon = new JLabel();
        rigid1 = new Box.Filler(new Dimension(0, 10), new Dimension(0, 10), new Dimension(0, 10));
        lblShimejiEE = new JLabel();
        rigid2 = new Box.Filler(new Dimension(0, 5), new Dimension(0, 5), new Dimension(0, 5));
        lblVersion = new JLabel();
        rigid3 = new Box.Filler(new Dimension(0, 15), new Dimension(0, 15), new Dimension(0, 15));
        lblDevelopedBy = new JLabel();
        lblKilkakon = new JLabel();
        rigid4 = new Box.Filler(new Dimension(0, 15), new Dimension(0, 15), new Dimension(0, 15));
        pnlAboutButtons = new JPanel();
        btnWebsite = new JButton();
        btnDiscord = new JButton();
        btnPatreon = new JButton();
        pnlFooter = new JPanel();
        btnDone = new JButton();
        btnCancel = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        chkAlwaysShowShimejiChooser.setText("Always Show Shimeji Chooser");
        chkAlwaysShowShimejiChooser.addItemListener(this::chkAlwaysShowShimejiChooserItemStateChanged);

        lblScaling.setText("Scaling");

        sldScaling.setMajorTickSpacing(10);
        sldScaling.setMaximum(80);
        sldScaling.setMinorTickSpacing(5);
        sldScaling.setPaintLabels(true);
        sldScaling.setPaintTicks(true);
        sldScaling.setSnapToTicks(true);
        sldScaling.setValue(10);
        sldScaling.setPreferredSize(new Dimension(300, 45));
        sldScaling.addChangeListener(this::sldScalingStateChanged);

        lblFilter.setText("Filter");

        radFilterNearest.setText("Nearest");
        radFilterNearest.addItemListener(this::radFilterItemStateChanged);

        radFilterBicubic.setText("Bicubic");
        radFilterBicubic.addItemListener(this::radFilterItemStateChanged);

        radFilterHqx.setText("hqx");
        radFilterHqx.addItemListener(this::radFilterItemStateChanged);

        GroupLayout pnlGeneralLayout = new GroupLayout(pnlGeneral);
        pnlGeneral.setLayout(pnlGeneralLayout);
        pnlGeneralLayout.setHorizontalGroup(
                pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlGeneralLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(chkAlwaysShowShimejiChooser)
                                        .addComponent(lblScaling)
                                        .addComponent(lblFilter)
                                        .addGroup(pnlGeneralLayout.createSequentialGroup()
                                                .addGap(10, 10, 10)
                                                .addGroup(pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(radFilterNearest)
                                                        .addComponent(sldScaling, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(radFilterBicubic)
                                                        .addComponent(radFilterHqx))))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlGeneralLayout.setVerticalGroup(
                pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlGeneralLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(chkAlwaysShowShimejiChooser)
                                .addGap(18, 18, 18)
                                .addComponent(lblScaling)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sldScaling, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblFilter)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radFilterNearest)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radFilterBicubic)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radFilterHqx)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pnlTabs.addTab("General", pnlGeneral);

        pnlInteractiveButtons.setPreferredSize(new Dimension(380, 36));
        pnlInteractiveButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));

        btnAddInteractiveWindow.setText("Add");
        btnAddInteractiveWindow.setMaximumSize(new Dimension(130, 26));
        btnAddInteractiveWindow.setMinimumSize(new Dimension(95, 23));
        btnAddInteractiveWindow.setName(""); // NOI18N
        btnAddInteractiveWindow.setPreferredSize(new Dimension(130, 26));
        btnAddInteractiveWindow.addActionListener(this::btnAddInteractiveWindowActionPerformed);
        pnlInteractiveButtons.add(btnAddInteractiveWindow);

        btnRemoveInteractiveWindow.setText("Remove");
        btnRemoveInteractiveWindow.setMaximumSize(new Dimension(130, 26));
        btnRemoveInteractiveWindow.setMinimumSize(new Dimension(95, 23));
        btnRemoveInteractiveWindow.setPreferredSize(new Dimension(130, 26));
        btnRemoveInteractiveWindow.addActionListener(this::btnRemoveInteractiveWindowActionPerformed);
        pnlInteractiveButtons.add(btnRemoveInteractiveWindow);

        lstInteractiveWindows.setModel(new AbstractListModel<String>() {
            String[] strings = {"Item 1", "Item 2", "Item 3", "Item 4", "Item 5"};

            @Override
            public int getSize() {
                return strings.length;
            }

            @Override
            public String getElementAt(int index) {
                return strings[index];
            }
        });
        jScrollPane1.setViewportView(lstInteractiveWindows);

        GroupLayout pnlInteractiveWindowsLayout = new GroupLayout(pnlInteractiveWindows);
        pnlInteractiveWindows.setLayout(pnlInteractiveWindowsLayout);
        pnlInteractiveWindowsLayout.setHorizontalGroup(
                pnlInteractiveWindowsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, pnlInteractiveWindowsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(pnlInteractiveWindowsLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(jScrollPane1)
                                        .addComponent(pnlInteractiveButtons, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        pnlInteractiveWindowsLayout.setVerticalGroup(
                pnlInteractiveWindowsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, pnlInteractiveWindowsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pnlInteractiveButtons, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        pnlTabs.addTab("InteractiveWindows", pnlInteractiveWindows);

        chkWindowModeEnabled.setText("Enable Windowed Mode");
        chkWindowModeEnabled.addItemListener(this::chkWindowModeEnabledItemStateChanged);

        pnlBackgroundPreview.setBorder(BorderFactory.createEtchedBorder());
        pnlBackgroundPreview.setPreferredSize(new Dimension(40, 40));

        GroupLayout pnlBackgroundPreviewLayout = new GroupLayout(pnlBackgroundPreview);
        pnlBackgroundPreview.setLayout(pnlBackgroundPreviewLayout);
        pnlBackgroundPreviewLayout.setHorizontalGroup(
                pnlBackgroundPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 36, Short.MAX_VALUE)
        );
        pnlBackgroundPreviewLayout.setVerticalGroup(
                pnlBackgroundPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 36, Short.MAX_VALUE)
        );

        lblDimensions.setText("Dimensions");

        lblDimensionsX.setText("x");

        lblBackground.setText("Background");

        txtBackground.setEditable(false);
        txtBackground.setText("#00FF00");
        txtBackground.setPreferredSize(new Dimension(70, 24));

        btnBackgroundChange.setText("Change");
        btnBackgroundChange.addActionListener(this::btnBackgroundChangeActionPerformed);

        spnWindowWidth.setModel(new SpinnerNumberModel(0, 0, 10000, 1));
        spnWindowWidth.setPreferredSize(new Dimension(60, 24));
        spnWindowWidth.addChangeListener(this::spnWindowWidthStateChanged);

        spnWindowHeight.setModel(new SpinnerNumberModel(0, 0, 10000, 1));
        spnWindowHeight.setMinimumSize(new Dimension(30, 20));
        spnWindowHeight.setPreferredSize(new Dimension(60, 24));
        spnWindowHeight.addChangeListener(this::spnWindowHeightStateChanged);

        GroupLayout pnlWindowModeLayout = new GroupLayout(pnlWindowMode);
        pnlWindowMode.setLayout(pnlWindowModeLayout);
        pnlWindowModeLayout.setHorizontalGroup(
                pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(chkWindowModeEnabled)
                                        .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                .addGroup(pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(lblDimensions)
                                                        .addComponent(lblBackground))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(pnlBackgroundPreview, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                .addComponent(spnWindowWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblDimensionsX)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(spnWindowHeight, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                .addComponent(txtBackground, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnBackgroundChange)))))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlWindowModeLayout.setVerticalGroup(
                pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(chkWindowModeEnabled)
                                .addGap(18, 18, 18)
                                .addGroup(pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblDimensions)
                                        .addComponent(lblDimensionsX)
                                        .addComponent(spnWindowWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(spnWindowHeight, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtBackground, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblBackground)
                                        .addComponent(btnBackgroundChange))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pnlBackgroundPreview, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pnlTabs.addTab("WindowMode", pnlWindowMode);

        pnlAbout.setLayout(new BoxLayout(pnlAbout, BoxLayout.Y_AXIS));
        pnlAbout.add(glue1);

        lblIcon.setAlignmentX(0.5F);
        lblIcon.setMinimumSize(new Dimension(64, 64));
        lblIcon.setPreferredSize(new Dimension(64, 64));
        pnlAbout.add(lblIcon);
        pnlAbout.add(rigid1);

        lblShimejiEE.setFont(lblShimejiEE.getFont().deriveFont(lblShimejiEE.getFont().getStyle() | Font.BOLD, lblShimejiEE.getFont().getSize() + 10));
        lblShimejiEE.setText("Shimeji");
        lblShimejiEE.setAlignmentX(0.5F);
        pnlAbout.add(lblShimejiEE);
        pnlAbout.add(rigid2);

        lblVersion.setFont(lblVersion.getFont().deriveFont(lblVersion.getFont().getSize() + 4f));
        lblVersion.setText("1.0.20");
        lblVersion.setAlignmentX(0.5F);
        pnlAbout.add(lblVersion);
        pnlAbout.add(rigid3);

        lblDevelopedBy.setText("developed by");
        lblDevelopedBy.setAlignmentX(0.5F);
        pnlAbout.add(lblDevelopedBy);

        lblKilkakon.setText("Kilkakon");
        lblKilkakon.setAlignmentX(0.5F);
        pnlAbout.add(lblKilkakon);
        pnlAbout.add(rigid4);

        pnlAboutButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));

        btnWebsite.setText("Website");
        btnWebsite.setAlignmentX(0.5F);
        btnWebsite.setMaximumSize(new Dimension(130, 26));
        btnWebsite.setPreferredSize(new Dimension(100, 26));
        btnWebsite.addActionListener(this::btnWebsiteActionPerformed);
        pnlAboutButtons.add(btnWebsite);

        btnDiscord.setText("Discord");
        btnDiscord.setAlignmentX(0.5F);
        btnDiscord.setMaximumSize(new Dimension(130, 26));
        btnDiscord.setPreferredSize(new Dimension(100, 26));
        btnDiscord.addActionListener(this::btnDiscordActionPerformed);
        pnlAboutButtons.add(btnDiscord);

        btnPatreon.setText("Patreon");
        btnPatreon.setAlignmentX(0.5F);
        btnPatreon.setMaximumSize(new Dimension(130, 26));
        btnPatreon.setPreferredSize(new Dimension(100, 26));
        btnPatreon.addActionListener(this::btnPatreonActionPerformed);
        pnlAboutButtons.add(btnPatreon);

        pnlAbout.add(pnlAboutButtons);

        pnlTabs.addTab("About", pnlAbout);

        pnlFooter.setPreferredSize(new Dimension(380, 36));
        pnlFooter.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));

        btnDone.setText("Done");
        btnDone.setMaximumSize(new Dimension(130, 26));
        btnDone.setMinimumSize(new Dimension(95, 23));
        btnDone.setName(""); // NOI18N
        btnDone.setPreferredSize(new Dimension(130, 26));
        btnDone.addActionListener(this::btnDoneActionPerformed);
        pnlFooter.add(btnDone);

        btnCancel.setText("Cancel");
        btnCancel.setMaximumSize(new Dimension(130, 26));
        btnCancel.setMinimumSize(new Dimension(95, 23));
        btnCancel.setPreferredSize(new Dimension(130, 26));
        btnCancel.addActionListener(this::btnCancelActionPerformed);
        pnlFooter.add(btnCancel);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(pnlTabs)
                                        .addComponent(pnlFooter, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(pnlTabs)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pnlFooter, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnDoneActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnDoneActionPerformed
    {// GEN-HEADEREND:event_btnDoneActionPerformed
        // done button
        try (FileOutputStream output = new FileOutputStream(configFile)) {
            Properties properties = Main.getInstance().getProperties();
            String interactiveWindows = listData.toString().replace("[", "").replace("]", "").replace(", ", "/");
            String[] windowArray = properties.getProperty("WindowSize", "600x500").split("x");
            Dimension window = new Dimension(Integer.parseInt(windowArray[0]), Integer.parseInt(windowArray[1]));

            environmentReloadRequired = properties.getProperty("Environment", "generic").equals("virtual") != windowedMode ||
                    !window.equals(windowSize) ||
                    !Color.decode(properties.getProperty("Background", "#00FF00")).equals(background);
            imageReloadRequired = !properties.getProperty("Filter", "false").equalsIgnoreCase(filter) ||
                    Double.parseDouble(properties.getProperty("Scaling", "1.0")) != scaling;
            interactiveWindowReloadRequired = !properties.getProperty("InteractiveWindows", "").equals(interactiveWindows);

            properties.setProperty("AlwaysShowShimejiChooser", alwaysShowShimejiChooser.toString());
            properties.setProperty("Scaling", Double.toString(scaling));
            properties.setProperty("Filter", filter);
            properties.setProperty("InteractiveWindows", interactiveWindows);
            properties.setProperty("Environment", windowedMode ? "virtual" : "generic");
            if (windowedMode) {
                properties.setProperty("WindowSize", windowSize.width + "x" + windowSize.height);
                properties.setProperty("Background", String.format("#%02X%02X%02X", background.getRed(), background.getGreen(), background.getBlue()));
            }

            properties.store(output, "Shimeji-ee Configuration Options");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to save settings", e);
        }
        dispose();
    }// GEN-LAST:event_btnDoneActionPerformed

    private void btnCancelActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnCancelActionPerformed
    {// GEN-HEADEREND:event_btnCancelActionPerformed
        dispose();
    }// GEN-LAST:event_btnCancelActionPerformed

    private void btnAddInteractiveWindowActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnAddInteractiveWindowActionPerformed
    {// GEN-HEADEREND:event_btnAddInteractiveWindowActionPerformed
        // add button
        String inputValue = JOptionPane.showInputDialog(rootPane, Main.getInstance().getLanguageBundle().getString("InteractiveWindowHintMessage"), Main.getInstance().getLanguageBundle().getString("AddInteractiveWindow"), JOptionPane.QUESTION_MESSAGE).trim();
        if (!inputValue.isEmpty() && !inputValue.contains("/")) {
            listData.add(inputValue);
            lstInteractiveWindows.setListData(listData.toArray(new String[0]));
        }
    }// GEN-LAST:event_btnAddInteractiveWindowActionPerformed

    private void btnRemoveInteractiveWindowActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnRemoveInteractiveWindowActionPerformed
    {// GEN-HEADEREND:event_btnRemoveInteractiveWindowActionPerformed
        // delete button
        if (lstInteractiveWindows.getSelectedIndex() != -1) {
            listData.remove(lstInteractiveWindows.getSelectedIndex());
            lstInteractiveWindows.setListData(listData.toArray(new String[0]));
        }
    }// GEN-LAST:event_btnRemoveInteractiveWindowActionPerformed

    private void chkAlwaysShowShimejiChooserItemStateChanged(ItemEvent evt)// GEN-FIRST:event_chkAlwaysShowShimejiChooserItemStateChanged
    {// GEN-HEADEREND:event_chkAlwaysShowShimejiChooserItemStateChanged
        alwaysShowShimejiChooser = evt.getStateChange() == ItemEvent.SELECTED;
    }// GEN-LAST:event_chkAlwaysShowShimejiChooserItemStateChanged

    private void radFilterItemStateChanged(ItemEvent evt)// GEN-FIRST:event_radFilterItemStateChanged
    {// GEN-HEADEREND:event_radFilterItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            Object source = evt.getItemSelectable();

            if (source == radFilterNearest) {
                filter = "nearest";
            } else if (source == radFilterHqx) {
                filter = "hqx";
            } else {
                filter = "bicubic";
            }
        }
    }// GEN-LAST:event_radFilterItemStateChanged

    private void sldScalingStateChanged(ChangeEvent evt)// GEN-FIRST:event_sldScalingStateChanged
    {// GEN-HEADEREND:event_sldScalingStateChanged
        if (!sldScaling.getValueIsAdjusting()) {
            if (sldScaling.getValue() == 0) {
                sldScaling.setValue(5);
            } else {
                scaling = sldScaling.getValue() / 10.0;
                if (scaling == 2 || scaling == 3 || scaling == 4 || scaling == 6 || scaling == 8) {
                    radFilterHqx.setEnabled(true);
                } else {
                    radFilterHqx.setEnabled(false);
                    if (filter.equals("hqx")) {
                        radFilterNearest.setSelected(true);
                    }
                }
            }
        }
    }// GEN-LAST:event_sldScalingStateChanged

    private void btnBackgroundChangeActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnBackgroundChangeActionPerformed
    {// GEN-HEADEREND:event_btnBackgroundChangeActionPerformed
        Color color = JColorChooser.showDialog(this, "Choose Background Color", background);

        if (color != null) {
            background = color;
            txtBackground.setText(String.format("#%02X%02X%02X", background.getRed(), background.getGreen(), background.getBlue()));
            pnlBackgroundPreview.setBackground(background);
        }
    }// GEN-LAST:event_btnBackgroundChangeActionPerformed

    private void chkWindowModeEnabledItemStateChanged(ItemEvent evt)// GEN-FIRST:event_chkWindowModeEnabledItemStateChanged
    {// GEN-HEADEREND:event_chkWindowModeEnabledItemStateChanged
        windowedMode = evt.getStateChange() == ItemEvent.SELECTED;

        spnWindowWidth.setEnabled(windowedMode);
        spnWindowHeight.setEnabled(windowedMode);
        btnBackgroundChange.setEnabled(windowedMode);
    }// GEN-LAST:event_chkWindowModeEnabledItemStateChanged

    private void spnWindowHeightStateChanged(ChangeEvent evt)// GEN-FIRST:event_spnWindowHeightStateChanged
    {// GEN-HEADEREND:event_spnWindowHeightStateChanged
        windowSize.height = ((SpinnerNumberModel) spnWindowHeight.getModel()).getNumber().intValue();
    }// GEN-LAST:event_spnWindowHeightStateChanged

    private void spnWindowWidthStateChanged(ChangeEvent evt)// GEN-FIRST:event_spnWindowWidthStateChanged
    {// GEN-HEADEREND:event_spnWindowWidthStateChanged
        windowSize.width = ((SpinnerNumberModel) spnWindowWidth.getModel()).getNumber().intValue();
    }// GEN-LAST:event_spnWindowWidthStateChanged

    private void btnWebsiteActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnWebsiteActionPerformed
    {// GEN-HEADEREND:event_btnWebsiteActionPerformed
        browseToUrl("http://kilkakon.com/");
    }// GEN-LAST:event_btnWebsiteActionPerformed

    private void btnDiscordActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnDiscordActionPerformed
    {// GEN-HEADEREND:event_btnDiscordActionPerformed
        browseToUrl("https://discord.gg/NBq3zqfA2B");
    }// GEN-LAST:event_btnDiscordActionPerformed

    private void btnPatreonActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnPatreonActionPerformed
    {// GEN-HEADEREND:event_btnPatreonActionPerformed
        browseToUrl("https://patreon.com/kilkakon");
    }// GEN-LAST:event_btnPatreonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Create and display the form */
        EventQueue.invokeLater(() -> new SettingsWindow(new JFrame(), true).display());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnAddInteractiveWindow;
    private JButton btnBackgroundChange;
    private JButton btnCancel;
    private JButton btnDiscord;
    private JButton btnDone;
    private JButton btnPatreon;
    private JButton btnRemoveInteractiveWindow;
    private JButton btnWebsite;
    private JCheckBox chkAlwaysShowShimejiChooser;
    private JCheckBox chkWindowModeEnabled;
    private Box.Filler glue1;
    private ButtonGroup grpFilter;
    private JScrollPane jScrollPane1;
    private JLabel lblBackground;
    private JLabel lblDevelopedBy;
    private JLabel lblDimensions;
    private JLabel lblDimensionsX;
    private JLabel lblFilter;
    private JLabel lblIcon;
    private JLabel lblKilkakon;
    private JLabel lblScaling;
    private JLabel lblShimejiEE;
    private JLabel lblVersion;
    private JList<String> lstInteractiveWindows;
    private JPanel pnlAbout;
    private JPanel pnlAboutButtons;
    private JPanel pnlBackgroundPreview;
    private JPanel pnlFooter;
    private JPanel pnlGeneral;
    private JPanel pnlInteractiveButtons;
    private JPanel pnlInteractiveWindows;
    private JTabbedPane pnlTabs;
    private JPanel pnlWindowMode;
    private JRadioButton radFilterBicubic;
    private JRadioButton radFilterHqx;
    private JRadioButton radFilterNearest;
    private Box.Filler rigid1;
    private Box.Filler rigid2;
    private Box.Filler rigid3;
    private Box.Filler rigid4;
    private JSlider sldScaling;
    private JSpinner spnWindowHeight;
    private JSpinner spnWindowWidth;
    private JTextField txtBackground;
    // End of variables declaration//GEN-END:variables
}
