/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.group_finity.mascot;

import com.group_finity.mascot.image.Filter;
import com.group_finity.mascot.image.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Kilkakon
 */
public class SettingsWindow extends JDialog {
    private static final Logger log = LoggerFactory.getLogger(SettingsWindow.class);
    private final ArrayList<String> listData = new ArrayList<>();
    private final ArrayList<String> blacklistData = new ArrayList<>();
    private boolean showTrayIcon = true;
    private boolean alwaysShowShimejiChooser = false;
    private boolean alwaysShowInformationScreen = false;
    private boolean drawShimejiBounds = false;
    private Filter filter = Filter.NEAREST_NEIGHBOUR;
    private double scaling = 1.0;
    private double opacity = 1.0;
    private boolean windowedMode = false;
    private Dimension windowSize = new Dimension(600, 500);
    private Color backgroundColour = new Color(0, 255, 0);
    private String backgroundMode = "centre";
    private Path backgroundImage = null;
    private final String[] backgroundModes = {"centre", "fill", "fit", "stretch"};

    private boolean suppressTextChanged = true;
    private boolean environmentReloadRequired = false;
    private boolean imageReloadRequired = false;
    private boolean interactiveWindowReloadRequired = false;
    private boolean trayMenuReloadRequired = false;

    /**
     * Creates new form SettingsWindow
     *
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param modal specifies whether dialog blocks user input to other top-level
     * windows when shown. If {@code true}, the modality type property is set to
     * {@code DEFAULT_MODALITY_TYPE} otherwise the dialog is modeless
     */
    public SettingsWindow(Frame owner, boolean modal) {
        super(owner, modal);
        initComponents();
    }

    public void init() {
        // initialise controls
        Dictionary<Integer, JLabel> labelTable = IntStream.range(0, 9).boxed().collect(Collectors.toMap(index -> index * 10, index -> new JLabel(index + "x"), (a, b) -> b, Hashtable::new));
        sldScaling.setLabelTable(labelTable);

        if (!getIconImages().isEmpty()) {
            lblIcon.setIcon(new ImageIcon(getIconImages().get(0).getScaledInstance(lblIcon.getPreferredSize().width, lblIcon.getPreferredSize().height, Image.SCALE_DEFAULT)));
        }

        // load existing settings
        Settings settings = Main.getInstance().getSettings();
        showTrayIcon = settings.showTrayIcon;
        alwaysShowShimejiChooser = settings.alwaysShowShimejiChooser;
        alwaysShowInformationScreen = settings.alwaysShowInformationScreen;
        drawShimejiBounds = settings.drawShimejiBounds;
        filter = settings.filter;
        opacity = settings.opacity;
        scaling = settings.scaling;
        windowedMode = settings.windowedMode;
        windowSize = new Dimension(settings.windowSize);
        backgroundColour = settings.backgroundColor;
        backgroundImage = settings.backgroundImage;
        backgroundMode = settings.backgroundMode;
        chkShowTrayIcon.setSelected(showTrayIcon);
        chkAlwaysShowShimejiChooser.setSelected(alwaysShowShimejiChooser);
        chkAlwaysShowInformationScreen.setSelected(alwaysShowInformationScreen);
        chkDrawShimejiBounds.setSelected(drawShimejiBounds);
        radFilterHqx.setEnabled(scaling == 2 || scaling == 3 || scaling == 4 || scaling == 6 || scaling == 8);
        if (filter == Filter.BICUBIC) {
            radFilterBicubic.setSelected(true);
        } else if (filter == Filter.HQX && radFilterHqx.isEnabled()) {
            radFilterHqx.setSelected(true);
        } else {
            radFilterNearest.setSelected(true);
        }
        sldOpacity.setValue((int) (opacity * 100));
        sldScaling.setValue((int) (scaling * 10));

        for (String item : settings.interactiveWindows)
            if (!item.trim().isEmpty()) {
                listData.add(item);
            }
        lstInteractiveWindows.setListData(listData.toArray(new String[0]));
        for (String item : settings.interactiveWindowsBlacklist)
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
                refreshBackgroundImage();
            } catch (IOException | RuntimeException e) {
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
        chkShowTrayIcon.setText(language.getString("ShowTrayIcon"));
        chkAlwaysShowShimejiChooser.setText(language.getString("AlwaysShowShimejiChooser"));
        chkAlwaysShowInformationScreen.setText(language.getString("AlwaysShowInformationScreen"));
        chkDrawShimejiBounds.setText(language.getString("DrawShimejiBounds"));
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
        chkWindowModeEnabled.setText(language.getString("WindowedModeEnabled"));
        lblDimensions.setText(language.getString("Dimensions"));
        lblBackground.setText(language.getString("Background"));
        lblBackgroundColour.setText(language.getString("Colour"));
        lblBackgroundImageCaption.setText(language.getString("Image"));
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

    public void display() {
        setLocationRelativeTo(null);
        suppressTextChanged = false;
        setVisible(true);
        suppressTextChanged = true;
    }

    private void browseToUrl(String url) {
        try {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(url));
            } else {
                if (desktop == null) {
                    log.warn("Can not open URL \"{}\", as desktop operations are not supported on this platform", url);
                } else {
                    log.warn("Can not open URL \"{}\", as the desktop browse operation is not supported on this platform", url);
                }
                JOptionPane.showMessageDialog(this, String.format(Main.getInstance().getLanguageBundle().getString("FailedOpenWebBrowserErrorMessage"), url), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException | UnsupportedOperationException | URISyntaxException e) {
            log.error("Failed to open URL \"{}\"", url, e);
            JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isEnvironmentReloadRequired() {
        return environmentReloadRequired;
    }

    public boolean isImageReloadRequired() {
        return imageReloadRequired;
    }

    public boolean isInteractiveWindowReloadRequired() {
        return interactiveWindowReloadRequired;
    }

    public boolean isTrayMenuReloadRequired() {
        return trayMenuReloadRequired;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        grpFilter = new javax.swing.ButtonGroup();
        pnlTabs = new javax.swing.JTabbedPane();
        pnlGeneral = new javax.swing.JPanel();
        chkAlwaysShowShimejiChooser = new javax.swing.JCheckBox();
        lblScaling = new javax.swing.JLabel();
        sldScaling = new javax.swing.JSlider();
        lblFilter = new javax.swing.JLabel();
        radFilterNearest = new javax.swing.JRadioButton();
        radFilterBicubic = new javax.swing.JRadioButton();
        radFilterHqx = new javax.swing.JRadioButton();
        sldOpacity = new javax.swing.JSlider();
        lblOpacity = new javax.swing.JLabel();
        chkAlwaysShowInformationScreen = new javax.swing.JCheckBox();
        chkShowTrayIcon = new javax.swing.JCheckBox();
        chkDrawShimejiBounds = new javax.swing.JCheckBox();
        pnlInteractiveWindows = new javax.swing.JPanel();
        pnlInteractiveTabs = new javax.swing.JTabbedPane();
        pnlWhitelistTab = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstInteractiveWindows = new javax.swing.JList<>();
        pnlBlacklistTab = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        lstInteractiveWindowsBlacklist = new javax.swing.JList<>();
        pnlInteractiveButtons = new javax.swing.JPanel();
        btnAddInteractiveWindow = new javax.swing.JButton();
        btnRemoveInteractiveWindow = new javax.swing.JButton();
        pnlWindowMode = new javax.swing.JPanel();
        chkWindowModeEnabled = new javax.swing.JCheckBox();
        lblDimensions = new javax.swing.JLabel();
        lblDimensionsX = new javax.swing.JLabel();
        lblBackground = new javax.swing.JLabel();
        txtBackgroundColour = new javax.swing.JTextField();
        btnBackgroundColourChange = new javax.swing.JButton();
        spnWindowWidth = new javax.swing.JSpinner();
        spnWindowHeight = new javax.swing.JSpinner();
        lblBackgroundColour = new javax.swing.JLabel();
        pnlBackgroundPreviewContainer = new javax.swing.JPanel();
        glueBackground = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        pnlBackgroundPreview = new javax.swing.JPanel();
        glueBackground2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0));
        lblBackgroundImageCaption = new javax.swing.JLabel();
        pnlBackgroundImage = new javax.swing.JPanel();
        lblBackgroundImage = new javax.swing.JLabel();
        btnBackgroundImageChange = new javax.swing.JButton();
        btnBackgroundImageRemove = new javax.swing.JButton();
        cmbBackgroundImageMode = new javax.swing.JComboBox<String>();
        pnlAbout = new javax.swing.JPanel();
        glue1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        lblIcon = new javax.swing.JLabel();
        rigid1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 15), new java.awt.Dimension(0, 15), new java.awt.Dimension(0, 15));
        lblShimejiEE = new javax.swing.JLabel();
        rigid2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 10), new java.awt.Dimension(0, 5), new java.awt.Dimension(0, 10));
        lblVersion = new javax.swing.JLabel();
        rigid3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 15), new java.awt.Dimension(0, 15), new java.awt.Dimension(0, 15));
        lblDevelopedBy = new javax.swing.JLabel();
        lblKilkakon = new javax.swing.JLabel();
        rigid4 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 30), new java.awt.Dimension(0, 30), new java.awt.Dimension(0, 30));
        pnlAboutButtons = new javax.swing.JPanel();
        btnWebsite = new javax.swing.JButton();
        btnDiscord = new javax.swing.JButton();
        btnPatreon = new javax.swing.JButton();
        glue2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        pnlFooter = new javax.swing.JPanel();
        btnDone = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");

        chkAlwaysShowShimejiChooser.setText("Always Show Choose Shimeji");
        chkAlwaysShowShimejiChooser.addItemListener(this::chkAlwaysShowShimejiChooserItemStateChanged);

        lblScaling.setText("Scaling");

        sldScaling.setMajorTickSpacing(10);
        sldScaling.setMaximum(80);
        sldScaling.setMinorTickSpacing(5);
        sldScaling.setPaintLabels(true);
        sldScaling.setPaintTicks(true);
        sldScaling.setSnapToTicks(true);
        sldScaling.setValue(10);
        sldScaling.setPreferredSize(new java.awt.Dimension(300, 45));
        sldScaling.addChangeListener(this::sldScalingStateChanged);

        lblFilter.setText("Filter");

        grpFilter.add(radFilterNearest);
        radFilterNearest.setText("Nearest Neighbour (No filter)");
        radFilterNearest.addItemListener(this::radFilterItemStateChanged);

        grpFilter.add(radFilterBicubic);
        radFilterBicubic.setText("Bicubic Filter");
        radFilterBicubic.addItemListener(this::radFilterItemStateChanged);

        grpFilter.add(radFilterHqx);
        radFilterHqx.setText("hqx Filter");
        radFilterHqx.addItemListener(this::radFilterItemStateChanged);

        sldOpacity.setMajorTickSpacing(10);
        sldOpacity.setMinorTickSpacing(5);
        sldOpacity.setPaintLabels(true);
        sldOpacity.setPaintTicks(true);
        sldOpacity.setSnapToTicks(true);
        sldOpacity.setValue(10);
        sldOpacity.setPreferredSize(new java.awt.Dimension(300, 45));
        sldOpacity.addChangeListener(this::sldOpacityStateChanged);

        lblOpacity.setText("Opacity");

        chkAlwaysShowInformationScreen.setText("Always Show Information Screen");
        chkAlwaysShowInformationScreen.addItemListener(this::chkAlwaysShowInformationScreenItemStateChanged);

        chkShowTrayIcon.setText("Show Tray Icon");
        chkShowTrayIcon.addItemListener(this::chkShowTrayIconItemStateChanged);

        chkDrawShimejiBounds.setText("Draw Shimeji Bounds");
        chkDrawShimejiBounds.addItemListener(this::chkDrawShimejiBoundsItemStateChanged);

        javax.swing.GroupLayout pnlGeneralLayout = new javax.swing.GroupLayout(pnlGeneral);
        pnlGeneral.setLayout(pnlGeneralLayout);
        pnlGeneralLayout.setHorizontalGroup(
            pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlGeneralLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkAlwaysShowShimejiChooser)
                    .addComponent(lblFilter)
                    .addComponent(lblScaling)
                    .addGroup(pnlGeneralLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(sldOpacity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(radFilterNearest)
                                .addComponent(sldScaling, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(radFilterBicubic)
                                .addComponent(radFilterHqx))))
                    .addComponent(lblOpacity)
                    .addComponent(chkAlwaysShowInformationScreen)
                    .addComponent(chkShowTrayIcon)
                    .addComponent(chkDrawShimejiBounds))
                .addContainerGap(52, Short.MAX_VALUE))
        );
        pnlGeneralLayout.setVerticalGroup(
            pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlGeneralLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chkShowTrayIcon)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkAlwaysShowShimejiChooser)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkAlwaysShowInformationScreen)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkDrawShimejiBounds)
                .addGap(18, 18, 18)
                .addComponent(lblOpacity)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sldOpacity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblScaling)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sldScaling, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblFilter)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radFilterNearest)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radFilterBicubic)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radFilterHqx)
                .addContainerGap(70, Short.MAX_VALUE))
        );

        pnlTabs.addTab("General", pnlGeneral);

        jScrollPane2.setViewportView(lstInteractiveWindows);

        javax.swing.GroupLayout pnlWhitelistTabLayout = new javax.swing.GroupLayout(pnlWhitelistTab);
        pnlWhitelistTab.setLayout(pnlWhitelistTabLayout);
        pnlWhitelistTabLayout.setHorizontalGroup(
            pnlWhitelistTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlWhitelistTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)
                .addContainerGap())
        );
        pnlWhitelistTabLayout.setVerticalGroup(
            pnlWhitelistTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlWhitelistTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 364, Short.MAX_VALUE)
                .addContainerGap())
        );

        pnlInteractiveTabs.addTab("Whitelist", pnlWhitelistTab);

        jScrollPane3.setViewportView(lstInteractiveWindowsBlacklist);

        javax.swing.GroupLayout pnlBlacklistTabLayout = new javax.swing.GroupLayout(pnlBlacklistTab);
        pnlBlacklistTab.setLayout(pnlBlacklistTabLayout);
        pnlBlacklistTabLayout.setHorizontalGroup(
            pnlBlacklistTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlBlacklistTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)
                .addContainerGap())
        );
        pnlBlacklistTabLayout.setVerticalGroup(
            pnlBlacklistTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlBlacklistTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 364, Short.MAX_VALUE)
                .addContainerGap())
        );

        pnlInteractiveTabs.addTab("Blacklist", pnlBlacklistTab);

        pnlInteractiveButtons.setPreferredSize(new java.awt.Dimension(380, 36));
        pnlInteractiveButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

        btnAddInteractiveWindow.setText("Add");
        btnAddInteractiveWindow.setMaximumSize(new java.awt.Dimension(130, 26));
        btnAddInteractiveWindow.setMinimumSize(new java.awt.Dimension(95, 23));
        btnAddInteractiveWindow.setName(""); // NOI18N
        btnAddInteractiveWindow.setPreferredSize(new java.awt.Dimension(130, 26));
        btnAddInteractiveWindow.addActionListener(this::btnAddInteractiveWindowActionPerformed);
        pnlInteractiveButtons.add(btnAddInteractiveWindow);

        btnRemoveInteractiveWindow.setText("Remove");
        btnRemoveInteractiveWindow.setMaximumSize(new java.awt.Dimension(130, 26));
        btnRemoveInteractiveWindow.setMinimumSize(new java.awt.Dimension(95, 23));
        btnRemoveInteractiveWindow.setPreferredSize(new java.awt.Dimension(130, 26));
        btnRemoveInteractiveWindow.addActionListener(this::btnRemoveInteractiveWindowActionPerformed);
        pnlInteractiveButtons.add(btnRemoveInteractiveWindow);

        javax.swing.GroupLayout pnlInteractiveWindowsLayout = new javax.swing.GroupLayout(pnlInteractiveWindows);
        pnlInteractiveWindows.setLayout(pnlInteractiveWindowsLayout);
        pnlInteractiveWindowsLayout.setHorizontalGroup(
            pnlInteractiveWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlInteractiveWindowsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlInteractiveWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlInteractiveButtons, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(pnlInteractiveTabs))
                .addContainerGap())
        );
        pnlInteractiveWindowsLayout.setVerticalGroup(
            pnlInteractiveWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlInteractiveWindowsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlInteractiveTabs)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlInteractiveButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pnlTabs.addTab("Interactive Windows", pnlInteractiveWindows);

        chkWindowModeEnabled.setText("Enable Windowed Mode");
        chkWindowModeEnabled.addItemListener(this::chkWindowModeEnabledItemStateChanged);

        lblDimensions.setText("Dimensions");

        lblDimensionsX.setText("x");

        lblBackground.setText("Background");

        txtBackgroundColour.setText("#00FF00");
        txtBackgroundColour.setPreferredSize(new java.awt.Dimension(70, 24));
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

        spnWindowWidth.setModel(new javax.swing.SpinnerNumberModel(0, 0, 10000, 1));
        spnWindowWidth.setPreferredSize(new java.awt.Dimension(60, 24));
        spnWindowWidth.addChangeListener(this::spnWindowWidthStateChanged);

        spnWindowHeight.setModel(new javax.swing.SpinnerNumberModel(0, 0, 10000, 1));
        spnWindowHeight.setMinimumSize(new java.awt.Dimension(30, 20));
        spnWindowHeight.setPreferredSize(new java.awt.Dimension(60, 24));
        spnWindowHeight.addChangeListener(this::spnWindowHeightStateChanged);

        lblBackgroundColour.setText("Colour");

        pnlBackgroundPreviewContainer.setLayout(new javax.swing.BoxLayout(pnlBackgroundPreviewContainer, javax.swing.BoxLayout.Y_AXIS));
        pnlBackgroundPreviewContainer.add(glueBackground);

        pnlBackgroundPreview.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        pnlBackgroundPreview.setPreferredSize(new java.awt.Dimension(20, 20));

        javax.swing.GroupLayout pnlBackgroundPreviewLayout = new javax.swing.GroupLayout(pnlBackgroundPreview);
        pnlBackgroundPreview.setLayout(pnlBackgroundPreviewLayout);
        pnlBackgroundPreviewLayout.setHorizontalGroup(
            pnlBackgroundPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlBackgroundPreviewLayout.setVerticalGroup(
            pnlBackgroundPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        pnlBackgroundPreviewContainer.add(pnlBackgroundPreview);
        pnlBackgroundPreviewContainer.add(glueBackground2);

        lblBackgroundImageCaption.setText("Image");

        pnlBackgroundImage.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        pnlBackgroundImage.setMaximumSize(new java.awt.Dimension(96, 96));
        pnlBackgroundImage.setPreferredSize(new java.awt.Dimension(96, 96));
        pnlBackgroundImage.setLayout(new java.awt.BorderLayout());

        lblBackgroundImage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pnlBackgroundImage.add(lblBackgroundImage, java.awt.BorderLayout.CENTER);

        btnBackgroundImageChange.setText("Change");
        btnBackgroundImageChange.addActionListener(this::btnBackgroundImageChangeActionPerformed);

        btnBackgroundImageRemove.setText("Remove");
        btnBackgroundImageRemove.addActionListener(this::btnBackgroundImageRemoveActionPerformed);

        cmbBackgroundImageMode.setModel(new javax.swing.DefaultComboBoxModel<String>());
        cmbBackgroundImageMode.addActionListener(this::cmbBackgroundImageModeActionPerformed);

        javax.swing.GroupLayout pnlWindowModeLayout = new javax.swing.GroupLayout(pnlWindowMode);
        pnlWindowMode.setLayout(pnlWindowModeLayout);
        pnlWindowModeLayout.setHorizontalGroup(
            pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkWindowModeEnabled)
                    .addGroup(pnlWindowModeLayout.createSequentialGroup()
                        .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblDimensions)
                            .addComponent(lblBackground)
                            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblBackgroundImageCaption)
                                    .addComponent(lblBackgroundColour))))
                        .addGap(18, 18, 18)
                        .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                .addComponent(spnWindowWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblDimensionsX)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnWindowHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                .addComponent(txtBackgroundColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pnlBackgroundPreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnBackgroundColourChange, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                .addComponent(pnlBackgroundImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(btnBackgroundImageRemove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnBackgroundImageChange, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(cmbBackgroundImageMode, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))
                .addContainerGap(100, Short.MAX_VALUE))
        );
        pnlWindowModeLayout.setVerticalGroup(
            pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chkWindowModeEnabled)
                .addGap(18, 18, 18)
                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblDimensions)
                    .addComponent(lblDimensionsX)
                    .addComponent(spnWindowWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spnWindowHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(lblBackground)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblBackgroundColour)
                        .addComponent(txtBackgroundColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnBackgroundColourChange))
                    .addComponent(pnlBackgroundPreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlWindowModeLayout.createSequentialGroup()
                        .addComponent(btnBackgroundImageChange)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbBackgroundImageMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBackgroundImageRemove))
                    .addGroup(pnlWindowModeLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(lblBackgroundImageCaption))
                    .addComponent(pnlBackgroundImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(231, Short.MAX_VALUE))
        );

        pnlTabs.addTab("Window Mode", pnlWindowMode);

        pnlAbout.setLayout(new javax.swing.BoxLayout(pnlAbout, javax.swing.BoxLayout.Y_AXIS));
        pnlAbout.add(glue1);

        lblIcon.setAlignmentX(0.5F);
        lblIcon.setMaximumSize(new java.awt.Dimension(64, 64));
        lblIcon.setMinimumSize(new java.awt.Dimension(64, 64));
        lblIcon.setPreferredSize(new java.awt.Dimension(64, 64));
        pnlAbout.add(lblIcon);
        pnlAbout.add(rigid1);

        lblShimejiEE.setFont(lblShimejiEE.getFont().deriveFont(lblShimejiEE.getFont().getStyle() | java.awt.Font.BOLD, lblShimejiEE.getFont().getSize()+10));
        lblShimejiEE.setText("Shimeji-ee");
        lblShimejiEE.setAlignmentX(0.5F);
        pnlAbout.add(lblShimejiEE);
        pnlAbout.add(rigid2);

        lblVersion.setFont(lblVersion.getFont().deriveFont(lblVersion.getFont().getSize()+4f));
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

        pnlAboutButtons.setMaximumSize(new java.awt.Dimension(32767, 36));
        pnlAboutButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

        btnWebsite.setText("Website");
        btnWebsite.setAlignmentX(0.5F);
        btnWebsite.setMaximumSize(new java.awt.Dimension(130, 26));
        btnWebsite.setPreferredSize(new java.awt.Dimension(100, 26));
        btnWebsite.addActionListener(this::btnWebsiteActionPerformed);
        pnlAboutButtons.add(btnWebsite);

        btnDiscord.setText("Discord");
        btnDiscord.setAlignmentX(0.5F);
        btnDiscord.setMaximumSize(new java.awt.Dimension(130, 26));
        btnDiscord.setPreferredSize(new java.awt.Dimension(100, 26));
        btnDiscord.addActionListener(this::btnDiscordActionPerformed);
        pnlAboutButtons.add(btnDiscord);

        btnPatreon.setText("Patreon");
        btnPatreon.setAlignmentX(0.5F);
        btnPatreon.setMaximumSize(new java.awt.Dimension(130, 26));
        btnPatreon.setPreferredSize(new java.awt.Dimension(100, 26));
        btnPatreon.addActionListener(this::btnPatreonActionPerformed);
        pnlAboutButtons.add(btnPatreon);

        pnlAbout.add(pnlAboutButtons);
        pnlAbout.add(glue2);

        pnlTabs.addTab("About", pnlAbout);

        pnlFooter.setPreferredSize(new java.awt.Dimension(380, 36));
        pnlFooter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

        btnDone.setText("Done");
        btnDone.setMaximumSize(new java.awt.Dimension(130, 26));
        btnDone.setMinimumSize(new java.awt.Dimension(95, 23));
        btnDone.setName(""); // NOI18N
        btnDone.setPreferredSize(new java.awt.Dimension(130, 26));
        btnDone.addActionListener(this::btnDoneActionPerformed);
        pnlFooter.add(btnDone);

        btnCancel.setText("Cancel");
        btnCancel.setMaximumSize(new java.awt.Dimension(130, 26));
        btnCancel.setMinimumSize(new java.awt.Dimension(95, 23));
        btnCancel.setPreferredSize(new java.awt.Dimension(130, 26));
        btnCancel.addActionListener(this::btnCancelActionPerformed);
        pnlFooter.add(btnCancel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlFooter, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(pnlTabs))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlTabs)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlFooter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnDoneActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnDoneActionPerformed
        // done button
        Settings settings = Main.getInstance().getSettings();

        environmentReloadRequired = settings.windowedMode != windowedMode ||
                !settings.windowSize.equals(windowSize) ||
                !settings.backgroundColor.equals(backgroundColour) ||
                !settings.backgroundMode.equals(backgroundMode) ||
                !Objects.equals(settings.backgroundImage, backgroundImage);
        imageReloadRequired = settings.filter != filter || settings.scaling != scaling || settings.opacity != opacity;
        interactiveWindowReloadRequired = !settings.interactiveWindows.equals(listData) ||
                !settings.interactiveWindowsBlacklist.equals(blacklistData);
        trayMenuReloadRequired = settings.showTrayIcon != showTrayIcon;

        settings.showTrayIcon = showTrayIcon;
        settings.alwaysShowShimejiChooser = alwaysShowShimejiChooser;
        settings.alwaysShowInformationScreen = alwaysShowInformationScreen;
        settings.opacity = opacity;
        settings.scaling = scaling;
        settings.filter = filter;
        settings.drawShimejiBounds = drawShimejiBounds;
        settings.interactiveWindows = listData;
        settings.interactiveWindowsBlacklist = blacklistData;
        settings.windowedMode = windowedMode;
        if (windowedMode) {
            settings.windowSize = new Dimension(windowSize);
            settings.backgroundColor = backgroundColour;
            settings.backgroundMode = backgroundMode;
            settings.backgroundImage = backgroundImage;
        }

        settings.saveUserSettings();

        dispose();
    }//GEN-LAST:event_btnDoneActionPerformed

    private void btnCancelActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnAddInteractiveWindowActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnAddInteractiveWindowActionPerformed
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
    }//GEN-LAST:event_btnAddInteractiveWindowActionPerformed

    private void btnRemoveInteractiveWindowActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnRemoveInteractiveWindowActionPerformed
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
    }//GEN-LAST:event_btnRemoveInteractiveWindowActionPerformed

    private void chkShowTrayIconItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_chkShowTrayIconItemStateChanged
        showTrayIcon = evt.getStateChange() == ItemEvent.SELECTED;
    }//GEN-LAST:event_chkShowTrayIconItemStateChanged

    private void chkAlwaysShowShimejiChooserItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_chkAlwaysShowShimejiChooserItemStateChanged
        alwaysShowShimejiChooser = evt.getStateChange() == ItemEvent.SELECTED;
    }//GEN-LAST:event_chkAlwaysShowShimejiChooserItemStateChanged

    private void chkDrawShimejiBoundsItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_chkDrawShimejiBoundsItemStateChanged
        drawShimejiBounds = evt.getStateChange() == ItemEvent.SELECTED;
    }//GEN-LAST:event_chkDrawShimejiBoundsItemStateChanged

    private void radFilterItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_radFilterItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            Object source = evt.getItemSelectable();

            if (source == radFilterNearest) {
                filter = Filter.NEAREST_NEIGHBOUR;
            } else if (source == radFilterHqx) {
                filter = Filter.HQX;
            } else {
                filter = Filter.BICUBIC;
            }
        }
    }//GEN-LAST:event_radFilterItemStateChanged

    private void sldScalingStateChanged(ChangeEvent evt) {//GEN-FIRST:event_sldScalingStateChanged
        if (!sldScaling.getValueIsAdjusting()) {
            if (sldScaling.getValue() == 0) {
                sldScaling.setValue(5);
            } else {
                scaling = sldScaling.getValue() / 10.0;
                if (scaling == 2 || scaling == 3 || scaling == 4 || scaling == 6 || scaling == 8) {
                    radFilterHqx.setEnabled(true);
                } else {
                    radFilterHqx.setEnabled(false);
                    if (filter == Filter.HQX) {
                        radFilterNearest.setSelected(true);
                    }
                }
            }
        }
    }//GEN-LAST:event_sldScalingStateChanged

    private void btnWebsiteActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnWebsiteActionPerformed
        browseToUrl("https://kilkakon.com/");
    }//GEN-LAST:event_btnWebsiteActionPerformed

    private void btnDiscordActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnDiscordActionPerformed
        browseToUrl("https://discord.gg/NBq3zqfA2B");
    }//GEN-LAST:event_btnDiscordActionPerformed

    private void btnPatreonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnPatreonActionPerformed
        browseToUrl("https://patreon.com/kilkakon");
    }//GEN-LAST:event_btnPatreonActionPerformed

    private void sldOpacityStateChanged(ChangeEvent evt) {//GEN-FIRST:event_sldOpacityStateChanged
        if (!sldOpacity.getValueIsAdjusting()) {
            if (sldOpacity.getValue() == 0) {
                sldOpacity.setValue(5);
            } else {
                opacity = sldOpacity.getValue() / 100.0;
            }
        }
    }//GEN-LAST:event_sldOpacityStateChanged

    private void spnWindowHeightStateChanged(ChangeEvent evt) {//GEN-FIRST:event_spnWindowHeightStateChanged
        windowSize.height = ((SpinnerNumberModel) spnWindowHeight.getModel()).getNumber().intValue();
    }//GEN-LAST:event_spnWindowHeightStateChanged

    private void spnWindowWidthStateChanged(ChangeEvent evt) {//GEN-FIRST:event_spnWindowWidthStateChanged
        windowSize.width = ((SpinnerNumberModel) spnWindowWidth.getModel()).getNumber().intValue();
    }//GEN-LAST:event_spnWindowWidthStateChanged

    private void btnBackgroundColourChangeActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnBackgroundColourChangeActionPerformed
        backgroundColour = chooseColour(backgroundColour, txtBackgroundColour, pnlBackgroundPreview, "ChooseBackgroundColour");
    }//GEN-LAST:event_btnBackgroundColourChangeActionPerformed

    private void chkWindowModeEnabledItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_chkWindowModeEnabledItemStateChanged
        windowedMode = evt.getStateChange() == ItemEvent.SELECTED;
        spnWindowWidth.setEnabled(windowedMode);
        spnWindowHeight.setEnabled(windowedMode);
        btnBackgroundColourChange.setEnabled(windowedMode);
        btnBackgroundImageChange.setEnabled(windowedMode);
        cmbBackgroundImageMode.setEnabled(windowedMode && backgroundImage != null);
        btnBackgroundImageRemove.setEnabled(windowedMode && backgroundImage != null);
    }//GEN-LAST:event_chkWindowModeEnabledItemStateChanged

    private void btnBackgroundImageChangeActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnBackgroundImageChangeActionPerformed
        final JFileChooser dialog = new JFileChooser();
        dialog.setDialogTitle(Main.getInstance().getLanguageBundle().getString("ChooseBackgroundImage"));
        // dialog.setFileFilter();

        if (dialog.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                backgroundImage = dialog.getSelectedFile().toPath().toRealPath();
                refreshBackgroundImage();
            } catch (IOException e) {
                backgroundImage = null;
                lblBackgroundImage.setIcon(null);
            }
            cmbBackgroundImageMode.setEnabled(windowedMode && backgroundImage != null);
            btnBackgroundImageRemove.setEnabled(windowedMode && backgroundImage != null);
        }
    }//GEN-LAST:event_btnBackgroundImageChangeActionPerformed

    private void btnBackgroundImageRemoveActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnBackgroundImageRemoveActionPerformed
        backgroundImage = null;
        lblBackgroundImage.setIcon(null);
        cmbBackgroundImageMode.setEnabled(false);
        btnBackgroundImageRemove.setEnabled(false);
    }//GEN-LAST:event_btnBackgroundImageRemoveActionPerformed

    private void cmbBackgroundImageModeActionPerformed(ActionEvent evt) {//GEN-FIRST:event_cmbBackgroundImageModeActionPerformed
        if (cmbBackgroundImageMode.getSelectedIndex() > -1) {
            backgroundMode = backgroundModes[cmbBackgroundImageMode.getSelectedIndex()];
        }
        try {
            refreshBackgroundImage();
        } catch (IOException | RuntimeException e) {
            backgroundImage = null;
            lblBackgroundImage.setIcon(null);
        }
    }//GEN-LAST:event_cmbBackgroundImageModeActionPerformed

    private void chkAlwaysShowInformationScreenItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_chkAlwaysShowInformationScreenItemStateChanged
        alwaysShowInformationScreen = evt.getStateChange() == ItemEvent.SELECTED;
    }//GEN-LAST:event_chkAlwaysShowInformationScreenItemStateChanged

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

    private void refreshBackgroundImage() throws IOException {
        if (backgroundImage == null) {
            lblBackgroundImage.setIcon(null);
            return;
        }

        Dimension size = pnlBackgroundImage.getPreferredSize();
        Image image = ImageUtils.toCompatibleImage(ImageIO.read(Files.newInputStream(backgroundImage)));

        if (image == null) {
            lblBackgroundImage.setIcon(null);
            return;
        }

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
    private javax.swing.JButton btnAddInteractiveWindow;
    private javax.swing.JButton btnBackgroundColourChange;
    private javax.swing.JButton btnBackgroundImageChange;
    private javax.swing.JButton btnBackgroundImageRemove;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnDiscord;
    private javax.swing.JButton btnDone;
    private javax.swing.JButton btnPatreon;
    private javax.swing.JButton btnRemoveInteractiveWindow;
    private javax.swing.JButton btnWebsite;
    private javax.swing.JCheckBox chkAlwaysShowInformationScreen;
    private javax.swing.JCheckBox chkAlwaysShowShimejiChooser;
    private javax.swing.JCheckBox chkDrawShimejiBounds;
    private javax.swing.JCheckBox chkShowTrayIcon;
    private javax.swing.JCheckBox chkWindowModeEnabled;
    private javax.swing.JComboBox<String> cmbBackgroundImageMode;
    private javax.swing.Box.Filler glue1;
    private javax.swing.Box.Filler glue2;
    private javax.swing.Box.Filler glueBackground;
    private javax.swing.Box.Filler glueBackground2;
    private javax.swing.ButtonGroup grpFilter;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lblBackground;
    private javax.swing.JLabel lblBackgroundColour;
    private javax.swing.JLabel lblBackgroundImage;
    private javax.swing.JLabel lblBackgroundImageCaption;
    private javax.swing.JLabel lblDevelopedBy;
    private javax.swing.JLabel lblDimensions;
    private javax.swing.JLabel lblDimensionsX;
    private javax.swing.JLabel lblFilter;
    private javax.swing.JLabel lblIcon;
    private javax.swing.JLabel lblKilkakon;
    private javax.swing.JLabel lblOpacity;
    private javax.swing.JLabel lblScaling;
    private javax.swing.JLabel lblShimejiEE;
    private javax.swing.JLabel lblVersion;
    private javax.swing.JList<String> lstInteractiveWindows;
    private javax.swing.JList<String> lstInteractiveWindowsBlacklist;
    private javax.swing.JPanel pnlAbout;
    private javax.swing.JPanel pnlAboutButtons;
    private javax.swing.JPanel pnlBackgroundImage;
    private javax.swing.JPanel pnlBackgroundPreview;
    private javax.swing.JPanel pnlBackgroundPreviewContainer;
    private javax.swing.JPanel pnlBlacklistTab;
    private javax.swing.JPanel pnlFooter;
    private javax.swing.JPanel pnlGeneral;
    private javax.swing.JPanel pnlInteractiveButtons;
    private javax.swing.JTabbedPane pnlInteractiveTabs;
    private javax.swing.JPanel pnlInteractiveWindows;
    private javax.swing.JTabbedPane pnlTabs;
    private javax.swing.JPanel pnlWhitelistTab;
    private javax.swing.JPanel pnlWindowMode;
    private javax.swing.JRadioButton radFilterBicubic;
    private javax.swing.JRadioButton radFilterHqx;
    private javax.swing.JRadioButton radFilterNearest;
    private javax.swing.Box.Filler rigid1;
    private javax.swing.Box.Filler rigid2;
    private javax.swing.Box.Filler rigid3;
    private javax.swing.Box.Filler rigid4;
    private javax.swing.JSlider sldOpacity;
    private javax.swing.JSlider sldScaling;
    private javax.swing.JSpinner spnWindowHeight;
    private javax.swing.JSpinner spnWindowWidth;
    private javax.swing.JTextField txtBackgroundColour;
    // End of variables declaration//GEN-END:variables
}
