/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.group_finity.mascot;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Kilkakon
 */
public class SettingsWindow extends JDialog {
    private static final Logger log = Logger.getLogger(SettingsWindow.class.getName());
    private final ArrayList<String> listData = new ArrayList<>();
    private final ArrayList<String> blacklistData = new ArrayList<>();
    private Boolean alwaysShowShimejiChooser = false;
    private Boolean alwaysShowInformationScreen = false;
    private String filter = "nearest";
    private double scaling = 1.0;
    private double opacity = 1.0;
    private Boolean windowedMode = false;
    private Dimension windowSize = new Dimension(600, 500);
    private Color backgroundColour = new Color(0, 255, 0);
    private String backgroundMode = "centre";
    private String backgroundImage = null;
    private final String[] backgroundModes = {"centre", "fill", "fit", "stretch"};

    private Boolean suppressTextChanged = true;
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

    public void init() {
        // initialise controls
        setLocationRelativeTo(null);
        grpFilter.add(radFilterNearest);
        grpFilter.add(radFilterBicubic);
        grpFilter.add(radFilterHqx);
        Dictionary<Integer, JLabel> labelTable = IntStream.range(0, 9).boxed().collect(Collectors.toMap(index -> index * 10, index -> new JLabel(index + "x"), (a, b) -> b, Hashtable::new));
        sldScaling.setLabelTable(labelTable);
        sldScaling.setPaintLabels(true);
        sldScaling.setSnapToTicks(true);

        // load existing settings
        Properties properties = Main.getInstance().getProperties();
        alwaysShowShimejiChooser = Boolean.parseBoolean(properties.getProperty("AlwaysShowShimejiChooser", "false"));
        alwaysShowInformationScreen = Boolean.parseBoolean(properties.getProperty("AlwaysShowInformationScreen", "false"));
        String filterText = Main.getInstance().getProperties().getProperty("Filter", "false");
        filter = "nearest";
        if (filterText.equalsIgnoreCase("true") || filterText.equalsIgnoreCase("hqx")) {
            filter = "hqx";
        } else if (filterText.equalsIgnoreCase("bicubic")) {
            filter = "bicubic";
        }
        opacity = Double.parseDouble(properties.getProperty("Opacity", "1.0"));
        scaling = Double.parseDouble(properties.getProperty("Scaling", "1.0"));
        windowedMode = properties.getProperty("Environment", "generic").equals("virtual");
        String[] windowArray = properties.getProperty("WindowSize", "600x500").split("x");
        windowSize = new Dimension(Integer.parseInt(windowArray[0]), Integer.parseInt(windowArray[1]));
        backgroundColour = Color.decode(properties.getProperty("Background", "#00FF00"));
        backgroundImage = properties.getProperty("BackgroundImage", "");
        backgroundMode = properties.getProperty("BackgroundMode", "centre");
        chkAlwaysShowShimejiChooser.setSelected(alwaysShowShimejiChooser);
        chkAlwaysShowInformationScreen.setSelected(alwaysShowInformationScreen);
        if (filter.equals("bicubic")) {
            radFilterBicubic.setSelected(true);
        } else if (filter.equals("hqx")) {
            radFilterHqx.setSelected(true);
        } else {
            radFilterNearest.setSelected(true);
        }
        sldOpacity.setValue((int) (opacity * 100));
        sldScaling.setValue((int) (scaling * 10));

        for (String item : properties.getProperty("InteractiveWindows", "").split("/"))
            if (!item.trim().isEmpty()) {
                listData.add(item);
            }
        lstInteractiveWindows.setListData(listData.toArray(new String[0]));
        for (String item : properties.getProperty("InteractiveWindowsBlacklist", "").split("/"))
            if (!item.trim().isEmpty()) {
                blacklistData.add(item);
            }
        lstInteractiveWindowsBlacklist.setListData(blacklistData.toArray(new String[0]));

        chkWindowModeEnabled.setSelected(windowedMode);
        spnWindowWidth.setBackground(txtBackgroundColour.getBackground());
        spnWindowHeight.setBackground(txtBackgroundColour.getBackground());
        spnWindowWidth.setEnabled(windowedMode);
        spnWindowHeight.setEnabled(windowedMode);
        spnWindowWidth.setValue(windowSize.width);
        spnWindowHeight.setValue(windowSize.height);
        txtBackgroundColour.setText(String.format("#%02X%02X%02X", backgroundColour.getRed(), backgroundColour.getGreen(), backgroundColour.getBlue()));
        btnBackgroundColourChange.setEnabled(windowedMode);
        btnBackgroundImageChange.setEnabled(windowedMode);
        pnlBackgroundPreview.setBackground(backgroundColour);
        if (backgroundImage != null) {
            try {
                Dimension size = pnlBackgroundImage.getPreferredSize();
                refreshBackgroundImage();
                pnlBackgroundImage.setPreferredSize(size);
            } catch (RuntimeException e) {
                backgroundImage = null;
                lblBackgroundImage.setIcon(null);
            }
        }
        cmbBackgroundImageMode.setEnabled(windowedMode && backgroundImage != null);
        btnBackgroundImageRemove.setEnabled(windowedMode && backgroundImage != null);

        // localisation
        ResourceBundle language = Main.getInstance().getLanguageBundle();
        setTitle(language.getString("Settings"));
        pnlTabs.setTitleAt(0, language.getString("General"));
        pnlTabs.setTitleAt(1, language.getString("InteractiveWindows"));
        pnlTabs.setTitleAt(2, language.getString("WindowMode"));
        pnlTabs.setTitleAt(3, language.getString("About"));
        lblShimejiEE.setText(language.getString("ShimejiEE"));
        lblDevelopedBy.setText(language.getString("DevelopedBy"));
        chkAlwaysShowShimejiChooser.setText(language.getString("AlwaysShowShimejiChooser"));
        chkAlwaysShowInformationScreen.setText(language.getString("AlwaysShowInformationScreen"));
        lblOpacity.setText(language.getString("Opacity"));
        lblScaling.setText(language.getString("Scaling"));
        lblFilter.setText(language.getString("FilterOptions"));
        radFilterNearest.setText(language.getString("NearestNeighbour"));
        radFilterHqx.setText(language.getString("Filter"));
        radFilterBicubic.setText(language.getString("BicubicFilter"));
        pnlInteractiveTabs.setTitleAt(0, language.getString("Whitelist"));
        pnlInteractiveTabs.setTitleAt(1, language.getString("Blacklist"));
        btnAddInteractiveWindow.setText(language.getString("Add"));
        btnRemoveInteractiveWindow.setText(language.getString("Remove"));
        lblPrimaryColour1.setText(language.getString("PrimaryColour1"));
        lblPrimaryColour2.setText(language.getString("PrimaryColour2"));
        lblSecondaryColour1.setText(language.getString("SecondaryColour1"));
        lblSecondaryColour2.setText(language.getString("SecondaryColour2"));
        lblSecondaryColour3.setText(language.getString("SecondaryColour3"));
        lblBlackColour.setText(language.getString("BlackColour"));
        lblWhiteColour.setText(language.getString("WhiteColour"));
        lblMenuOpacity.setText(language.getString("MenuOpacity"));
        btnChangeFont.setText(language.getString("ChangeFont"));
        btnReset.setText(language.getString("Reset"));
        chkWindowModeEnabled.setText(language.getString("WindowedModeEnabled"));
        lblDimensions.setText(language.getString("Dimensions"));
        lblBackground.setText(language.getString("Background"));
        btnBackgroundColourChange.setText(language.getString("Change"));
        btnBackgroundImageChange.setText(language.getString("Change"));
        cmbBackgroundImageMode.addItem(language.getString("BackgroundModeCentre"));
        cmbBackgroundImageMode.addItem(language.getString("BackgroundModeFill"));
        cmbBackgroundImageMode.addItem(language.getString("BackgroundModeFit"));
        cmbBackgroundImageMode.addItem(language.getString("BackgroundModeStretch"));
        btnBackgroundImageRemove.setText(language.getString("Remove"));
        lblShimejiEE.setText(language.getString("ShimejiEE"));
        lblDevelopedBy.setText(language.getString("DevelopedBy"));
        btnWebsite.setText(language.getString("Website"));
        btnDone.setText(language.getString("Done"));
        btnCancel.setText(language.getString("Cancel"));

        // come back around to this one now that the dropdown is populated
        IntStream.range(0, backgroundModes.length).filter(index -> backgroundMode.equals(backgroundModes[index])).findFirst().ifPresent(index -> cmbBackgroundImageMode.setSelectedIndex(index));
    }

    public boolean display() {
        getContentPane().setPreferredSize(new Dimension(600, 497));
        pnlInteractiveButtons.setPreferredSize(new Dimension(pnlInteractiveButtons.getPreferredSize().width, btnAddInteractiveWindow.getPreferredSize().height + 6));
        cmbBackgroundImageMode.setPreferredSize(btnBackgroundImageRemove.getPreferredSize());
        pnlBackgroundImage.setMaximumSize(pnlBackgroundImage.getPreferredSize());
        lblIcon.setMaximumSize(lblIcon.getPreferredSize());
        if (!getIconImages().isEmpty()) {
            lblIcon.setIcon(new ImageIcon(getIconImages().get(0).getScaledInstance(lblIcon.getPreferredSize().width, lblIcon.getPreferredSize().height, Image.SCALE_DEFAULT)));
        }
        pnlAboutButtons.setPreferredSize(new Dimension(pnlAboutButtons.getPreferredSize().width, btnWebsite.getPreferredSize().height + 6));
        pnlFooter.setPreferredSize(new Dimension(pnlFooter.getPreferredSize().width, btnDone.getPreferredSize().height + 6));
        pack();
        suppressTextChanged = false;
        setVisible(true);
        suppressTextChanged = true;

        return true;
    }

    private void browseToUrl(String url) {
        try {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(url));
            } else {
                if (desktop == null) {
                    log.log(Level.WARNING, "Can not open URL \"" + url + "\", as desktop operations are not supported on this platform");
                } else {
                    log.log(Level.WARNING, "Can not open URL \"" + url + "\", as the desktop browse operation is not supported on this platform");
                }
                JOptionPane.showMessageDialog(this, Main.getInstance().getLanguageBundle().getString("FailedOpenWebBrowserErrorMessage") + " " + url, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException | UnsupportedOperationException | URISyntaxException e) {
            log.log(Level.SEVERE, "Failed to open URL \"" + url + "\"", e);
            JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
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
        jScrollPane1 = new JScrollPane();
        jTextArea1 = new JTextArea();
        pnlTabs = new JTabbedPane();
        pnlGeneral = new JPanel();
        chkAlwaysShowShimejiChooser = new JCheckBox();
        lblScaling = new JLabel();
        sldScaling = new JSlider();
        lblFilter = new JLabel();
        radFilterNearest = new JRadioButton();
        radFilterBicubic = new JRadioButton();
        radFilterHqx = new JRadioButton();
        sldOpacity = new JSlider();
        lblOpacity = new JLabel();
        chkAlwaysShowInformationScreen = new JCheckBox();
        pnlInteractiveWindows = new JPanel();
        pnlInteractiveTabs = new JTabbedPane();
        pnlWhitelistTab = new JPanel();
        jScrollPane2 = new JScrollPane();
        lstInteractiveWindows = new JList<>();
        pnlBlacklistTab = new JPanel();
        jScrollPane3 = new JScrollPane();
        lstInteractiveWindowsBlacklist = new JList<>();
        pnlInteractiveButtons = new JPanel();
        btnAddInteractiveWindow = new JButton();
        btnRemoveInteractiveWindow = new JButton();
        btnChangeFont = new JButton();
        btnReset = new JButton();
        lblPrimaryColour1 = new JLabel();
        txtPrimaryColour1 = new JTextField();
        pnlPrimaryColour1PreviewContainer = new JPanel();
        gluePrimaryColour1a = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlPrimaryColour1Preview = new JPanel();
        gluePrimaryColour1b = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnPrimaryColour1Change = new JButton();
        lblPrimaryColour2 = new JLabel();
        txtPrimaryColour2 = new JTextField();
        pnlPrimaryColour2PreviewContainer = new JPanel();
        gluePrimaryColour2a = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlPrimaryColour2Preview = new JPanel();
        gluePrimaryColour2b = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnPrimaryColour2Change = new JButton();
        lblSecondaryColour1 = new JLabel();
        txtSecondaryColour1 = new JTextField();
        pnlSecondaryColour1PreviewContainer = new JPanel();
        glueSecondaryColour1a = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlSecondaryColour1Preview = new JPanel();
        glueSecondaryColour1b = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnSecondaryColour1Change = new JButton();
        lblSecondaryColour2 = new JLabel();
        txtSecondaryColour2 = new JTextField();
        pnlSecondaryColour2PreviewContainer = new JPanel();
        glueSecondaryColour2a = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlSecondaryColour2Preview = new JPanel();
        glueSecondaryColour2b = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnSecondaryColour2Change = new JButton();
        lblSecondaryColour3 = new JLabel();
        txtSecondaryColour3 = new JTextField();
        pnlSecondaryColour3PreviewContainer = new JPanel();
        glueSecondaryColour3a = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlSecondaryColour3Preview = new JPanel();
        glueSecondaryColour3b = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnSecondaryColour3Change = new JButton();
        lblMenuOpacity = new JLabel();
        sldMenuOpacity = new JSlider();
        lblBlackColour = new JLabel();
        txtBlackColour = new JTextField();
        pnlBlackColourPreviewContainer = new JPanel();
        glueBlackColoura = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlBlackColourPreview = new JPanel();
        glueBlackColourb = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnBlackColourChange = new JButton();
        lblWhiteColour = new JLabel();
        txtWhiteColour = new JTextField();
        pnlWhiteColourPreviewContainer = new JPanel();
        glueWhiteColoura = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlWhiteColourPreview = new JPanel();
        glueWhiteColourb = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnWhiteColourChange = new JButton();
        pnlScrollPane = new JScrollPane();
        pnlWindowMode = new JPanel();
        chkWindowModeEnabled = new JCheckBox();
        lblDimensions = new JLabel();
        lblDimensionsX = new JLabel();
        lblBackground = new JLabel();
        txtBackgroundColour = new JTextField();
        btnBackgroundColourChange = new JButton();
        spnWindowWidth = new JSpinner();
        spnWindowHeight = new JSpinner();
        lblBackgroundColour = new JLabel();
        pnlBackgroundPreviewContainer = new JPanel();
        glueBackground = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlBackgroundPreview = new JPanel();
        glueBackground2 = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        lblBackgroundImageCaption = new JLabel();
        pnlBackgroundImage = new JPanel();
        lblBackgroundImage = new JLabel();
        btnBackgroundImageChange = new JButton();
        btnBackgroundImageRemove = new JButton();
        cmbBackgroundImageMode = new JComboBox<>();
        pnlAbout = new JPanel();
        glue1 = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        lblIcon = new JLabel();
        rigid1 = new Box.Filler(new Dimension(0, 15), new Dimension(0, 15), new Dimension(0, 15));
        lblShimejiEE = new JLabel();
        rigid2 = new Box.Filler(new Dimension(0, 10), new Dimension(0, 5), new Dimension(0, 10));
        lblVersion = new JLabel();
        rigid3 = new Box.Filler(new Dimension(0, 15), new Dimension(0, 15), new Dimension(0, 15));
        lblDevelopedBy = new JLabel();
        lblKilkakon = new JLabel();
        rigid4 = new Box.Filler(new Dimension(0, 30), new Dimension(0, 30), new Dimension(0, 30));
        pnlAboutButtons = new JPanel();
        btnWebsite = new JButton();
        btnDiscord = new JButton();
        btnPatreon = new JButton();
        glue2 = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlFooter = new JPanel();
        btnDone = new JButton();
        btnCancel = new JButton();

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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

        sldOpacity.setMajorTickSpacing(10);
        sldOpacity.setMinorTickSpacing(5);
        sldOpacity.setPaintLabels(true);
        sldOpacity.setPaintTicks(true);
        sldOpacity.setSnapToTicks(true);
        sldOpacity.setValue(10);
        sldOpacity.setPreferredSize(new Dimension(300, 45));
        sldOpacity.addChangeListener(this::sldOpacityStateChanged);

        lblOpacity.setText("Opacity");

        chkAlwaysShowInformationScreen.setText("Always Show Information Screen");
        chkAlwaysShowInformationScreen.addItemListener(this::chkAlwaysShowInformationScreenItemStateChanged);

        GroupLayout pnlGeneralLayout = new GroupLayout(pnlGeneral);
        pnlGeneral.setLayout(pnlGeneralLayout);
        pnlGeneralLayout.setHorizontalGroup(
                pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlGeneralLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(chkAlwaysShowShimejiChooser)
                                        .addComponent(lblFilter)
                                        .addComponent(lblScaling)
                                        .addGroup(pnlGeneralLayout.createSequentialGroup()
                                                .addGap(10, 10, 10)
                                                .addGroup(pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(sldOpacity, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                .addComponent(radFilterNearest)
                                                                .addComponent(sldScaling, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(radFilterBicubic)
                                                                .addComponent(radFilterHqx))))
                                        .addComponent(lblOpacity)
                                        .addComponent(chkAlwaysShowInformationScreen))
                                .addContainerGap(26, Short.MAX_VALUE))
        );
        pnlGeneralLayout.setVerticalGroup(
                pnlGeneralLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlGeneralLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(chkAlwaysShowShimejiChooser)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(chkAlwaysShowInformationScreen)
                                .addGap(18, 18, 18)
                                .addComponent(lblOpacity)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sldOpacity, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
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
                                .addContainerGap(40, Short.MAX_VALUE))
        );

        pnlTabs.addTab("General", pnlGeneral);

        lstInteractiveWindows.setModel(new AbstractListModel<>() {
            final String[] strings = {"Item 1", "Item 2", "Item 3", "Item 4", "Item 5"};

            @Override
            public int getSize() {
                return strings.length;
            }

            @Override
            public String getElementAt(int index) {
                return strings[index];
            }
        });
        jScrollPane2.setViewportView(lstInteractiveWindows);

        GroupLayout pnlWhitelistTabLayout = new GroupLayout(pnlWhitelistTab);
        pnlWhitelistTab.setLayout(pnlWhitelistTabLayout);
        pnlWhitelistTabLayout.setHorizontalGroup(
                pnlWhitelistTabLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlWhitelistTabLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                                .addContainerGap())
        );
        pnlWhitelistTabLayout.setVerticalGroup(
                pnlWhitelistTabLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlWhitelistTabLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                                .addContainerGap())
        );

        pnlInteractiveTabs.addTab("Whitelist", pnlWhitelistTab);

        lstInteractiveWindowsBlacklist.setModel(new AbstractListModel<>() {
            final String[] strings = {"Item 1", "Item 2", "Item 3", "Item 4", "Item 5"};

            @Override
            public int getSize() {
                return strings.length;
            }

            @Override
            public String getElementAt(int index) {
                return strings[index];
            }
        });
        jScrollPane3.setViewportView(lstInteractiveWindowsBlacklist);

        GroupLayout pnlBlacklistTabLayout = new GroupLayout(pnlBlacklistTab);
        pnlBlacklistTab.setLayout(pnlBlacklistTabLayout);
        pnlBlacklistTabLayout.setHorizontalGroup(
                pnlBlacklistTabLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlBlacklistTabLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane3, GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                                .addContainerGap())
        );
        pnlBlacklistTabLayout.setVerticalGroup(
                pnlBlacklistTabLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlBlacklistTabLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane3, GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                                .addContainerGap())
        );

        pnlInteractiveTabs.addTab("Blacklist", pnlBlacklistTab);

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

        GroupLayout pnlInteractiveWindowsLayout = new GroupLayout(pnlInteractiveWindows);
        pnlInteractiveWindows.setLayout(pnlInteractiveWindowsLayout);
        pnlInteractiveWindowsLayout.setHorizontalGroup(
                pnlInteractiveWindowsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlInteractiveWindowsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(pnlInteractiveWindowsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(pnlInteractiveButtons, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                        .addComponent(pnlInteractiveTabs))
                                .addContainerGap())
        );
        pnlInteractiveWindowsLayout.setVerticalGroup(
                pnlInteractiveWindowsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, pnlInteractiveWindowsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(pnlInteractiveTabs)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pnlInteractiveButtons, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        pnlTabs.addTab("InteractiveWindows", pnlInteractiveWindows);

        chkWindowModeEnabled.setText("Enable Windowed Mode");
        chkWindowModeEnabled.addItemListener(this::chkWindowModeEnabledItemStateChanged);

        lblDimensions.setText("Dimensions");

        lblDimensionsX.setText("x");

        lblBackground.setText("Background");

        txtBackgroundColour.setText("#00FF00");
        txtBackgroundColour.setPreferredSize(new Dimension(70, 24));
        txtBackgroundColour.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                colourTextChanged(txtBackgroundColour);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                colourTextChanged(txtBackgroundColour);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                colourTextChanged(txtBackgroundColour);
            }
        });
        ((AbstractDocument) txtBackgroundColour.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass filterBypass, int offset, String text, AttributeSet attributes) throws BadLocationException {
                if (text != null && text.matches("[#0-9a-fA-F]*") && filterBypass.getDocument().getLength() + text.length() <= 7) {
                    super.insertString(filterBypass, offset, text, attributes);
                }
            }

            @Override
            public void replace(FilterBypass filterBypass, int offset, int length, String text, AttributeSet attributes) throws BadLocationException {
                if (text != null && text.matches("[#0-9a-fA-F]*") && filterBypass.getDocument().getLength() - length + text.length() <= 7) {
                    super.replace(filterBypass, offset, length, text, attributes);
                }
            }
        });

        btnBackgroundColourChange.setText("Change");
        btnBackgroundColourChange.addActionListener(this::btnBackgroundColourChangeActionPerformed);

        spnWindowWidth.setModel(new SpinnerNumberModel(0, 0, 10000, 1));
        spnWindowWidth.setPreferredSize(new Dimension(60, 24));
        spnWindowWidth.addChangeListener(this::spnWindowWidthStateChanged);

        spnWindowHeight.setModel(new SpinnerNumberModel(0, 0, 10000, 1));
        spnWindowHeight.setMinimumSize(new Dimension(30, 20));
        spnWindowHeight.setPreferredSize(new Dimension(60, 24));
        spnWindowHeight.addChangeListener(this::spnWindowHeightStateChanged);

        lblBackgroundColour.setText("Colour");

        pnlBackgroundPreviewContainer.setLayout(new BoxLayout(pnlBackgroundPreviewContainer, BoxLayout.Y_AXIS));
        pnlBackgroundPreviewContainer.add(glueBackground);

        pnlBackgroundPreview.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        pnlBackgroundPreview.setPreferredSize(new Dimension(20, 20));

        GroupLayout pnlBackgroundPreviewLayout = new GroupLayout(pnlBackgroundPreview);
        pnlBackgroundPreview.setLayout(pnlBackgroundPreviewLayout);
        pnlBackgroundPreviewLayout.setHorizontalGroup(
                pnlBackgroundPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlBackgroundPreviewLayout.setVerticalGroup(
                pnlBackgroundPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        pnlBackgroundPreviewContainer.add(pnlBackgroundPreview);
        pnlBackgroundPreviewContainer.add(glueBackground2);

        lblBackgroundImageCaption.setText("Image");

        pnlBackgroundImage.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        pnlBackgroundImage.setPreferredSize(new Dimension(96, 96));
        pnlBackgroundImage.setLayout(new BorderLayout());

        lblBackgroundImage.setHorizontalAlignment(SwingConstants.CENTER);
        pnlBackgroundImage.add(lblBackgroundImage, BorderLayout.CENTER);

        btnBackgroundImageChange.setText("Change");
        btnBackgroundImageChange.addActionListener(this::btnBackgroundImageChangeActionPerformed);

        btnBackgroundImageRemove.setText("Remove");
        btnBackgroundImageRemove.addActionListener(this::btnBackgroundImageRemoveActionPerformed);

        cmbBackgroundImageMode.setModel(new DefaultComboBoxModel<>());
        cmbBackgroundImageMode.addActionListener(this::cmbBackgroundImageModeActionPerformed);

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
                                                        .addComponent(lblBackground)
                                                        .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                .addGap(10, 10, 10)
                                                                .addGroup(pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                        .addComponent(lblBackgroundImageCaption)
                                                                        .addComponent(lblBackgroundColour))))
                                                .addGap(18, 18, 18)
                                                .addGroup(pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                        .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                .addComponent(spnWindowWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblDimensionsX)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(spnWindowHeight, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                .addComponent(txtBackgroundColour, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(pnlBackgroundPreviewContainer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(btnBackgroundColourChange, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                        .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                .addComponent(pnlBackgroundImage, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(btnBackgroundImageRemove, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(btnBackgroundImageChange, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(cmbBackgroundImageMode, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))
                                .addContainerGap(85, Short.MAX_VALUE))
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
                                .addGap(18, 18, 18)
                                .addComponent(lblBackground)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblBackgroundColour)
                                                .addComponent(txtBackgroundColour, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnBackgroundColourChange))
                                        .addComponent(pnlBackgroundPreviewContainer, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlWindowModeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                .addComponent(btnBackgroundImageChange)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cmbBackgroundImageMode, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnBackgroundImageRemove))
                                        .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                .addGap(4, 4, 4)
                                                .addComponent(lblBackgroundImageCaption))
                                        .addComponent(pnlBackgroundImage, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(115, Short.MAX_VALUE))
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
        lblVersion.setText("1.0.22");
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

        pnlAboutButtons.setMaximumSize(new Dimension(32767, 36));
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
        pnlAbout.add(glue2);

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
                                        .addComponent(pnlFooter, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                        .addComponent(pnlTabs))
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
        Properties properties = Main.getInstance().getProperties();
        String interactiveWindows = listData.toString().replace("[", "").replace("]", "").replace(", ", "/");
        String interactiveWindowsBlacklist = blacklistData.toString().replace("[", "").replace("]", "").replace(", ", "/");
        String[] windowArray = properties.getProperty("WindowSize", "600x500").split("x");
        Dimension window = new Dimension(Integer.parseInt(windowArray[0]), Integer.parseInt(windowArray[1]));

        environmentReloadRequired = properties.getProperty("Environment", "generic").equals("virtual") != windowedMode ||
                !window.equals(windowSize) ||
                !Color.decode(properties.getProperty("Background", "#00FF00")).equals(backgroundColour) ||
                !properties.getProperty("BackgroundMode", "centre").equals(backgroundMode) ||
                !properties.getProperty("BackgroundImage", "").equalsIgnoreCase(backgroundImage == null ? "" : backgroundImage);
        imageReloadRequired = !properties.getProperty("Filter", "false").equalsIgnoreCase(filter) ||
                Double.parseDouble(properties.getProperty("Scaling", "1.0")) != scaling ||
                Double.parseDouble(properties.getProperty("Opacity", "1.0")) != opacity;
        interactiveWindowReloadRequired = !properties.getProperty("InteractiveWindows", "").equals(interactiveWindows) ||
                !properties.getProperty("InteractiveWindowsBlacklist", "").equals(interactiveWindowsBlacklist);

        try (OutputStream output = Files.newOutputStream(Main.SETTINGS_FILE)) {
            properties.setProperty("AlwaysShowShimejiChooser", alwaysShowShimejiChooser.toString());
            properties.setProperty("AlwaysShowInformationScreen", alwaysShowInformationScreen.toString());
            properties.setProperty("Opacity", Double.toString(opacity));
            properties.setProperty("Scaling", Double.toString(scaling));
            properties.setProperty("Filter", filter);
            properties.setProperty("InteractiveWindows", interactiveWindows);
            properties.setProperty("Environment", windowedMode ? "virtual" : "generic");
            if (windowedMode) {
                properties.setProperty("WindowSize", windowSize.width + "x" + windowSize.height);
                properties.setProperty("Background", String.format("#%02X%02X%02X", backgroundColour.getRed(), backgroundColour.getGreen(), backgroundColour.getBlue()));
                properties.setProperty("BackgroundMode", backgroundMode);
                properties.setProperty("BackgroundImage", backgroundImage == null ? "" : backgroundImage);
            }

            properties.store(output, "Shimeji-ee Configuration Options");
        } catch (IOException e) {
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
        String inputValue = JOptionPane.showInputDialog(rootPane, Main.getInstance().getLanguageBundle().getString("InteractiveWindowHintMessage"), Main.getInstance().getLanguageBundle().getString(pnlInteractiveTabs.getSelectedIndex() == 0 ? "AddInteractiveWindow" : "BlacklistInteractiveWindow"), JOptionPane.QUESTION_MESSAGE);
        if (inputValue != null && !inputValue.trim().isEmpty() && !inputValue.contains("/")) {
            if (pnlInteractiveTabs.getSelectedIndex() == 0) {
                listData.add(inputValue.trim());
                lstInteractiveWindows.setListData(listData.toArray(new String[0]));
            } else {
                blacklistData.add(inputValue.trim());
                lstInteractiveWindowsBlacklist.setListData(blacklistData.toArray(new String[0]));
            }
        }
    }// GEN-LAST:event_btnAddInteractiveWindowActionPerformed

    private void btnRemoveInteractiveWindowActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnRemoveInteractiveWindowActionPerformed
    {// GEN-HEADEREND:event_btnRemoveInteractiveWindowActionPerformed
        // delete button
        if (pnlInteractiveTabs.getSelectedIndex() == 0) {
            if (lstInteractiveWindows.getSelectedIndex() != -1) {
                listData.remove(lstInteractiveWindows.getSelectedIndex());
                lstInteractiveWindows.setListData(listData.toArray(new String[0]));
            }
        } else {
            if (lstInteractiveWindowsBlacklist.getSelectedIndex() != -1) {
                blacklistData.remove(lstInteractiveWindowsBlacklist.getSelectedIndex());
                lstInteractiveWindowsBlacklist.setListData(blacklistData.toArray(new String[0]));
            }
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

    private void btnWebsiteActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnWebsiteActionPerformed
    {// GEN-HEADEREND:event_btnWebsiteActionPerformed
        browseToUrl("https://kilkakon.com/");
    }// GEN-LAST:event_btnWebsiteActionPerformed

    private void btnDiscordActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnDiscordActionPerformed
    {// GEN-HEADEREND:event_btnDiscordActionPerformed
        browseToUrl("https://discord.gg/NBq3zqfA2B");
    }// GEN-LAST:event_btnDiscordActionPerformed

    private void btnPatreonActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnPatreonActionPerformed
    {// GEN-HEADEREND:event_btnPatreonActionPerformed
        browseToUrl("https://patreon.com/kilkakon");
    }// GEN-LAST:event_btnPatreonActionPerformed

    private void sldOpacityStateChanged(ChangeEvent evt)// GEN-FIRST:event_sldOpacityStateChanged
    {// GEN-HEADEREND:event_sldOpacityStateChanged
        if (!sldOpacity.getValueIsAdjusting()) {
            if (sldOpacity.getValue() == 0) {
                sldOpacity.setValue(5);
            } else {
                opacity = sldOpacity.getValue() / 100.0;
            }
        }
    }// GEN-LAST:event_sldOpacityStateChanged

    private void spnWindowHeightStateChanged(ChangeEvent evt)// GEN-FIRST:event_spnWindowHeightStateChanged
    {// GEN-HEADEREND:event_spnWindowHeightStateChanged
        windowSize.height = ((SpinnerNumberModel) spnWindowHeight.getModel()).getNumber().intValue();
    }// GEN-LAST:event_spnWindowHeightStateChanged

    private void spnWindowWidthStateChanged(ChangeEvent evt)// GEN-FIRST:event_spnWindowWidthStateChanged
    {// GEN-HEADEREND:event_spnWindowWidthStateChanged
        windowSize.width = ((SpinnerNumberModel) spnWindowWidth.getModel()).getNumber().intValue();
    }// GEN-LAST:event_spnWindowWidthStateChanged

    private void btnBackgroundColourChangeActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnBackgroundColourChangeActionPerformed
    {// GEN-HEADEREND:event_btnBackgroundColourChangeActionPerformed
        backgroundColour = chooseColour(backgroundColour, txtBackgroundColour, pnlBackgroundPreview, "ChooseBackgroundColour");
    }// GEN-LAST:event_btnBackgroundColourChangeActionPerformed

    private void chkWindowModeEnabledItemStateChanged(ItemEvent evt)// GEN-FIRST:event_chkWindowModeEnabledItemStateChanged
    {// GEN-HEADEREND:event_chkWindowModeEnabledItemStateChanged
        windowedMode = evt.getStateChange() == ItemEvent.SELECTED;
        spnWindowWidth.setEnabled(windowedMode);
        spnWindowHeight.setEnabled(windowedMode);
        btnBackgroundColourChange.setEnabled(windowedMode);
        btnBackgroundImageChange.setEnabled(windowedMode);
        cmbBackgroundImageMode.setEnabled(windowedMode && backgroundImage != null);
        btnBackgroundImageRemove.setEnabled(windowedMode && backgroundImage != null);
    }// GEN-LAST:event_chkWindowModeEnabledItemStateChanged

    private void btnBackgroundImageChangeActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnBackgroundImageChangeActionPerformed
    {// GEN-HEADEREND:event_btnBackgroundImageChangeActionPerformed
        final JFileChooser dialog = new JFileChooser();
        dialog.setDialogTitle(Main.getInstance().getLanguageBundle().getString("ChooseBackgroundImage"));
        // dialog.setFileFilter(  );

        if (dialog.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                backgroundImage = dialog.getSelectedFile().getCanonicalPath();
                refreshBackgroundImage();
            } catch (IOException e) {
                backgroundImage = null;
                lblBackgroundImage.setIcon(null);
            }
            cmbBackgroundImageMode.setEnabled(windowedMode && backgroundImage != null);
            btnBackgroundImageRemove.setEnabled(windowedMode && backgroundImage != null);
        }
    }// GEN-LAST:event_btnBackgroundImageChangeActionPerformed

    private void btnBackgroundImageRemoveActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnBackgroundImageRemoveActionPerformed
    {// GEN-HEADEREND:event_btnBackgroundImageRemoveActionPerformed
        backgroundImage = null;
        lblBackgroundImage.setIcon(null);
        cmbBackgroundImageMode.setEnabled(false);
        btnBackgroundImageRemove.setEnabled(false);
    }// GEN-LAST:event_btnBackgroundImageRemoveActionPerformed

    private void cmbBackgroundImageModeActionPerformed(ActionEvent evt)// GEN-FIRST:event_cmbBackgroundImageModeActionPerformed
    {// GEN-HEADEREND:event_cmbBackgroundImageModeActionPerformed
        if (cmbBackgroundImageMode.getSelectedIndex() > -1) {
            backgroundMode = backgroundModes[cmbBackgroundImageMode.getSelectedIndex()];
        }
        refreshBackgroundImage();
    }// GEN-LAST:event_cmbBackgroundImageModeActionPerformed

    private void chkAlwaysShowInformationScreenItemStateChanged(ItemEvent evt)// GEN-FIRST:event_chkAlwaysShowInformationScreenItemStateChanged
    {// GEN-HEADEREND:event_chkAlwaysShowInformationScreenItemStateChanged
        alwaysShowInformationScreen = evt.getStateChange() == ItemEvent.SELECTED;
    }// GEN-LAST:event_chkAlwaysShowInformationScreenItemStateChanged

    private void colourTextChanged(JTextField field) {
        if (suppressTextChanged || !isVisible()) {
            return;
        }

        String text = field.getText();
        if (text == null) {
            return;
        }
        if (text.length() != 7 || !text.matches("#[0-9a-fA-F]{6}")) {
            return;
        }

        Color newColour = Color.decode(text);

        if (field.equals(txtBackgroundColour)) {
            if (!newColour.equals(backgroundColour)) {
                backgroundColour = Color.decode(text);
                pnlBackgroundPreview.setBackground(backgroundColour);
            }
        }
    }

    private Color chooseColour(Color colour, JTextField field, JPanel preview, String title) {
        Color newColour = JColorChooser.showDialog(this, Main.getInstance().getLanguageBundle().getString(title), colour);

        if (newColour != null) {
            colour = newColour;
            field.setText(String.format("#%02X%02X%02X", colour.getRed(), colour.getGreen(), colour.getBlue()));
            preview.setBackground(colour);
        }

        return colour;
    }

    private void refreshBackgroundImage() {
        Dimension size = pnlBackgroundImage.getPreferredSize();
        Image image = new ImageIcon(backgroundImage).getImage();

        if (backgroundMode.equals("stretch")) {
            image = image.getScaledInstance(size.width,
                    size.height,
                    Image.SCALE_SMOOTH);

        } else if (!backgroundMode.equals("centre")) {
            double factor = backgroundMode.equals("fit") ?
                    Math.min(size.width / (double) image.getWidth(null), size.height / (double) image.getHeight(null)) :
                    Math.max(size.width / (double) image.getWidth(null), size.height / (double) image.getHeight(null));
            image = image.getScaledInstance((int) (factor * image.getWidth(null)),
                    (int) (factor * image.getHeight(null)),
                    Image.SCALE_SMOOTH);
        }

        lblBackgroundImage.setIcon(new ImageIcon(image));
        lblBackgroundImage.setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnAddInteractiveWindow;
    private JButton btnBackgroundColourChange;
    private JButton btnBackgroundImageChange;
    private JButton btnBackgroundImageRemove;
    private JButton btnBlackColourChange;
    private JButton btnCancel;
    private JButton btnChangeFont;
    private JButton btnDiscord;
    private JButton btnDone;
    private JButton btnPatreon;
    private JButton btnPrimaryColour1Change;
    private JButton btnPrimaryColour2Change;
    private JButton btnRemoveInteractiveWindow;
    private JButton btnReset;
    private JButton btnSecondaryColour1Change;
    private JButton btnSecondaryColour2Change;
    private JButton btnSecondaryColour3Change;
    private JButton btnWebsite;
    private JButton btnWhiteColourChange;
    private JCheckBox chkAlwaysShowInformationScreen;
    private JCheckBox chkAlwaysShowShimejiChooser;
    private JCheckBox chkWindowModeEnabled;
    private JComboBox<String> cmbBackgroundImageMode;
    private Box.Filler glue1;
    private Box.Filler glue2;
    private Box.Filler glueBackground;
    private Box.Filler glueBackground2;
    private Box.Filler glueBlackColoura;
    private Box.Filler glueBlackColourb;
    private Box.Filler gluePrimaryColour1a;
    private Box.Filler gluePrimaryColour1b;
    private Box.Filler gluePrimaryColour2a;
    private Box.Filler gluePrimaryColour2b;
    private Box.Filler glueSecondaryColour1a;
    private Box.Filler glueSecondaryColour1b;
    private Box.Filler glueSecondaryColour2a;
    private Box.Filler glueSecondaryColour2b;
    private Box.Filler glueSecondaryColour3a;
    private Box.Filler glueSecondaryColour3b;
    private Box.Filler glueWhiteColoura;
    private Box.Filler glueWhiteColourb;
    private ButtonGroup grpFilter;
    private JScrollPane jScrollPane1;
    private JScrollPane jScrollPane2;
    private JScrollPane jScrollPane3;
    private JTextArea jTextArea1;
    private JLabel lblBackground;
    private JLabel lblBackgroundColour;
    private JLabel lblBackgroundImage;
    private JLabel lblBackgroundImageCaption;
    private JLabel lblBlackColour;
    private JLabel lblDevelopedBy;
    private JLabel lblDimensions;
    private JLabel lblDimensionsX;
    private JLabel lblFilter;
    private JLabel lblIcon;
    private JLabel lblKilkakon;
    private JLabel lblMenuOpacity;
    private JLabel lblOpacity;
    private JLabel lblPrimaryColour1;
    private JLabel lblPrimaryColour2;
    private JLabel lblScaling;
    private JLabel lblSecondaryColour1;
    private JLabel lblSecondaryColour2;
    private JLabel lblSecondaryColour3;
    private JLabel lblShimejiEE;
    private JLabel lblVersion;
    private JLabel lblWhiteColour;
    private JList<String> lstInteractiveWindows;
    private JList<String> lstInteractiveWindowsBlacklist;
    private JPanel pnlAbout;
    private JPanel pnlAboutButtons;
    private JPanel pnlBackgroundImage;
    private JPanel pnlBackgroundPreview;
    private JPanel pnlBackgroundPreviewContainer;
    private JPanel pnlBlackColourPreview;
    private JPanel pnlBlackColourPreviewContainer;
    private JPanel pnlBlacklistTab;
    private JPanel pnlFooter;
    private JPanel pnlGeneral;
    private JPanel pnlInteractiveButtons;
    private JTabbedPane pnlInteractiveTabs;
    private JPanel pnlInteractiveWindows;
    private JPanel pnlPrimaryColour1Preview;
    private JPanel pnlPrimaryColour1PreviewContainer;
    private JPanel pnlPrimaryColour2Preview;
    private JPanel pnlPrimaryColour2PreviewContainer;
    private JScrollPane pnlScrollPane;
    private JPanel pnlSecondaryColour1Preview;
    private JPanel pnlSecondaryColour1PreviewContainer;
    private JPanel pnlSecondaryColour2Preview;
    private JPanel pnlSecondaryColour2PreviewContainer;
    private JPanel pnlSecondaryColour3Preview;
    private JPanel pnlSecondaryColour3PreviewContainer;
    private JTabbedPane pnlTabs;
    private JPanel pnlWhiteColourPreview;
    private JPanel pnlWhiteColourPreviewContainer;
    private JPanel pnlWhitelistTab;
    private JPanel pnlWindowMode;
    private JRadioButton radFilterBicubic;
    private JRadioButton radFilterHqx;
    private JRadioButton radFilterNearest;
    private Box.Filler rigid1;
    private Box.Filler rigid2;
    private Box.Filler rigid3;
    private Box.Filler rigid4;
    private JSlider sldMenuOpacity;
    private JSlider sldOpacity;
    private JSlider sldScaling;
    private JSpinner spnWindowHeight;
    private JSpinner spnWindowWidth;
    private JTextField txtBackgroundColour;
    private JTextField txtBlackColour;
    private JTextField txtPrimaryColour1;
    private JTextField txtPrimaryColour2;
    private JTextField txtSecondaryColour1;
    private JTextField txtSecondaryColour2;
    private JTextField txtSecondaryColour3;
    private JTextField txtWhiteColour;
    // End of variables declaration//GEN-END:variables
}
