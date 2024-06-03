/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.group_finity.mascot;

import com.nilo.plaf.nimrod.NimRODFontDialog;
import com.nilo.plaf.nimrod.NimRODLookAndFeel;
import com.nilo.plaf.nimrod.NimRODTheme;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.*;
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
    private NimRODTheme theme;
    private NimRODTheme oldTheme;
    private NimRODLookAndFeel lookAndFeel;
    private final ArrayList<String> listData = new ArrayList<>();
    private Boolean alwaysShowShimejiChooser = false;
    private Boolean alwaysShowInformationScreen = false;
    private String filter = "nearest";
    private double scaling = 1.0;
    private double opacity = 1.0;
    private Boolean windowedMode = false;
    private Dimension windowSize = new Dimension(600, 500);
    private Dimension buttonSize;
    private Dimension aboutButtonSize;
    private Color backgroundColour = new Color(0, 255, 0);
    private String backgroundMode = "centre";
    private String backgroundImage = null;
    private final String[] backgroundModes = {"centre", "fill", "fit", "stretch"};
    private Color primaryColour1;
    private Color primaryColour2;
    private Color primaryColour3;
    private Color secondaryColour1;
    private Color secondaryColour2;
    private Color secondaryColour3;
    private Color blackColour;
    private Color whiteColour;
    private Font font;
    private double menuOpacity = 1.0;

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
        // TODO Hashtable is described as obsolete, so consider switching to another class
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
        buttonSize = btnDone.getPreferredSize();
        aboutButtonSize = btnWebsite.getPreferredSize();
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

        listData.addAll(Arrays.asList(properties.getProperty("InteractiveWindows", "").split("/")));
        // This prevents the UI list from having empty entries,
        // and prevents those empty entries from being saved to settings.properties
        for (int i = 0; i < listData.size(); i++) {
            if (listData.get(i).trim().isEmpty()) {
                listData.remove(i);
                i--;
            }
        }
        lstInteractiveWindows.setListData(listData.toArray(new String[0]));

        Properties themeProperties = new Properties();
        try (InputStream input = Files.newInputStream(Main.THEME_FILE)) {
            themeProperties.load(input);
        } catch (IOException ignored) {
        }
        primaryColour1 = Color.decode(themeProperties.getProperty("nimrodlf.p1", "#1EA6EB"));
        primaryColour2 = Color.decode(themeProperties.getProperty("nimrodlf.p2", "#28B0F5"));
        primaryColour3 = Color.decode(themeProperties.getProperty("nimrodlf.p3", "#32BAFF"));
        secondaryColour1 = Color.decode(themeProperties.getProperty("nimrodlf.s1", "#BCBCBE"));
        secondaryColour2 = Color.decode(themeProperties.getProperty("nimrodlf.s2", "#C6C6C8"));
        secondaryColour3 = Color.decode(themeProperties.getProperty("nimrodlf.s3", "#D0D0D2"));
        blackColour = Color.decode(themeProperties.getProperty("nimrodlf.b", "#000000"));
        whiteColour = Color.decode(themeProperties.getProperty("nimrodlf.w", "#FFFFFF"));
        menuOpacity = Integer.parseInt(properties.getProperty("nimrodlf.menuOpacity", "255")) / 255.0;
        font = Font.decode(themeProperties.getProperty("nimrodlf.font", "SansSerif-PLAIN-12"));
        pnlPrimaryColour1Preview.setBackground(primaryColour1);
        txtPrimaryColour1.setText(String.format("#%02X%02X%02X", primaryColour1.getRed(), primaryColour1.getGreen(), primaryColour1.getBlue()));
        pnlPrimaryColour2Preview.setBackground(primaryColour2);
        txtPrimaryColour2.setText(String.format("#%02X%02X%02X", primaryColour2.getRed(), primaryColour2.getGreen(), primaryColour2.getBlue()));
        pnlPrimaryColour3Preview.setBackground(primaryColour3);
        txtPrimaryColour3.setText(String.format("#%02X%02X%02X", primaryColour3.getRed(), primaryColour2.getGreen(), primaryColour3.getBlue()));
        pnlSecondaryColour1Preview.setBackground(secondaryColour1);
        txtSecondaryColour1.setText(String.format("#%02X%02X%02X", secondaryColour1.getRed(), secondaryColour1.getGreen(), secondaryColour1.getBlue()));
        pnlSecondaryColour2Preview.setBackground(secondaryColour2);
        txtSecondaryColour2.setText(String.format("#%02X%02X%02X", secondaryColour2.getRed(), secondaryColour2.getGreen(), secondaryColour2.getBlue()));
        pnlSecondaryColour3Preview.setBackground(secondaryColour3);
        txtSecondaryColour3.setText(String.format("#%02X%02X%02X", secondaryColour3.getRed(), secondaryColour3.getGreen(), secondaryColour3.getBlue()));
        pnlBlackColourPreview.setBackground(blackColour);
        txtBlackColour.setText(String.format("#%02X%02X%02X", blackColour.getRed(), blackColour.getGreen(), blackColour.getBlue()));
        pnlWhiteColourPreview.setBackground(whiteColour);
        txtWhiteColour.setText(String.format("#%02X%02X%02X", whiteColour.getRed(), whiteColour.getGreen(), whiteColour.getBlue()));
        theme = new NimRODTheme();
        theme.setPrimary1(primaryColour1);
        theme.setPrimary2(primaryColour2);
        theme.setPrimary3(primaryColour3);
        theme.setSecondary1(secondaryColour1);
        theme.setSecondary2(secondaryColour2);
        theme.setSecondary3(secondaryColour3);
        theme.setBlack(blackColour);
        theme.setWhite(whiteColour);
        sldMenuOpacity.setValue((int) (menuOpacity * 100));
        theme.setFont(font);
        oldTheme = new NimRODTheme();
        oldTheme.setPrimary1(primaryColour1);
        oldTheme.setPrimary2(primaryColour2);
        oldTheme.setPrimary3(primaryColour3);
        oldTheme.setSecondary1(secondaryColour1);
        oldTheme.setSecondary2(secondaryColour2);
        oldTheme.setSecondary3(secondaryColour3);
        oldTheme.setBlack(blackColour);
        oldTheme.setWhite(whiteColour);
        oldTheme.setMenuOpacity((int) (menuOpacity * 255));
        oldTheme.setFont(font);
        lookAndFeel = (NimRODLookAndFeel) UIManager.getLookAndFeel();

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
        pnlTabs.setTitleAt(2, language.getString("Theme"));
        pnlTabs.setTitleAt(3, language.getString("WindowMode"));
        pnlTabs.setTitleAt(4, language.getString("About"));
        chkAlwaysShowShimejiChooser.setText(language.getString("AlwaysShowShimejiChooser"));
        chkAlwaysShowInformationScreen.setText(language.getString("AlwaysShowInformationScreen"));
        lblOpacity.setText(language.getString("Opacity"));
        lblScaling.setText(language.getString("Scaling"));
        lblFilter.setText(language.getString("FilterOptions"));
        radFilterNearest.setText(language.getString("NearestNeighbour"));
        radFilterHqx.setText(language.getString("Filter"));
        radFilterBicubic.setText(language.getString("BicubicFilter"));
        btnAddInteractiveWindow.setText(language.getString("Add"));
        btnRemoveInteractiveWindow.setText(language.getString("Remove"));
        lblPrimaryColour1.setText(language.getString("PrimaryColour1"));
        lblPrimaryColour2.setText(language.getString("PrimaryColour2"));
        lblPrimaryColour3.setText(language.getString("PrimaryColour3"));
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
        // scale controls to fit
        getContentPane().setPreferredSize(new Dimension(600, 497));
        sldOpacity.setPreferredSize(new Dimension(sldOpacity.getPreferredSize().width, sldOpacity.getPreferredSize().height));
        sldScaling.setPreferredSize(new Dimension(sldScaling.getPreferredSize().width, sldScaling.getPreferredSize().height));
        btnAddInteractiveWindow.setPreferredSize(new Dimension(btnAddInteractiveWindow.getPreferredSize().width, btnAddInteractiveWindow.getPreferredSize().height));
        btnRemoveInteractiveWindow.setPreferredSize(new Dimension(btnRemoveInteractiveWindow.getPreferredSize().width, btnRemoveInteractiveWindow.getPreferredSize().height));
        pnlInteractiveButtons.setPreferredSize(new Dimension(pnlInteractiveButtons.getPreferredSize().width, btnAddInteractiveWindow.getPreferredSize().height + 6));
        txtPrimaryColour1.setPreferredSize(new Dimension(txtPrimaryColour1.getPreferredSize().width, txtPrimaryColour1.getPreferredSize().height));
        txtPrimaryColour2.setPreferredSize(new Dimension(txtPrimaryColour2.getPreferredSize().width, txtPrimaryColour2.getPreferredSize().height));
        txtPrimaryColour3.setPreferredSize(new Dimension(txtPrimaryColour3.getPreferredSize().width, txtPrimaryColour3.getPreferredSize().height));
        txtSecondaryColour1.setPreferredSize(new Dimension(txtSecondaryColour1.getPreferredSize().width, txtSecondaryColour1.getPreferredSize().height));
        txtSecondaryColour2.setPreferredSize(new Dimension(txtSecondaryColour2.getPreferredSize().width, txtSecondaryColour2.getPreferredSize().height));
        txtSecondaryColour3.setPreferredSize(new Dimension(txtSecondaryColour3.getPreferredSize().width, txtSecondaryColour3.getPreferredSize().height));
        txtBlackColour.setPreferredSize(new Dimension(txtBlackColour.getPreferredSize().width, txtBlackColour.getPreferredSize().height));
        txtWhiteColour.setPreferredSize(new Dimension(txtWhiteColour.getPreferredSize().width, txtWhiteColour.getPreferredSize().height));
        pnlPrimaryColour1PreviewContainer.setPreferredSize(new Dimension(pnlPrimaryColour1PreviewContainer.getPreferredSize().width, pnlPrimaryColour1PreviewContainer.getPreferredSize().height));
        pnlPrimaryColour1Preview.setPreferredSize(new Dimension(pnlPrimaryColour1Preview.getPreferredSize().width, pnlPrimaryColour1Preview.getPreferredSize().height));
        pnlPrimaryColour2PreviewContainer.setPreferredSize(new Dimension(pnlPrimaryColour2PreviewContainer.getPreferredSize().width, pnlPrimaryColour2PreviewContainer.getPreferredSize().height));
        pnlPrimaryColour2Preview.setPreferredSize(new Dimension(pnlPrimaryColour2Preview.getPreferredSize().width, pnlPrimaryColour2Preview.getPreferredSize().height));
        pnlPrimaryColour3PreviewContainer.setPreferredSize(new Dimension(pnlPrimaryColour3PreviewContainer.getPreferredSize().width, pnlPrimaryColour3PreviewContainer.getPreferredSize().height));
        pnlPrimaryColour3Preview.setPreferredSize(new Dimension(pnlPrimaryColour3Preview.getPreferredSize().width, pnlPrimaryColour3Preview.getPreferredSize().height));
        pnlSecondaryColour1PreviewContainer.setPreferredSize(new Dimension(pnlSecondaryColour1PreviewContainer.getPreferredSize().width, pnlSecondaryColour1PreviewContainer.getPreferredSize().height));
        pnlSecondaryColour1Preview.setPreferredSize(new Dimension(pnlSecondaryColour1Preview.getPreferredSize().width, pnlSecondaryColour1Preview.getPreferredSize().height));
        pnlSecondaryColour2PreviewContainer.setPreferredSize(new Dimension(pnlSecondaryColour2PreviewContainer.getPreferredSize().width, pnlSecondaryColour2PreviewContainer.getPreferredSize().height));
        pnlSecondaryColour2Preview.setPreferredSize(new Dimension(pnlSecondaryColour2Preview.getPreferredSize().width, pnlSecondaryColour2Preview.getPreferredSize().height));
        pnlSecondaryColour3PreviewContainer.setPreferredSize(new Dimension(pnlSecondaryColour3PreviewContainer.getPreferredSize().width, pnlSecondaryColour3PreviewContainer.getPreferredSize().height));
        pnlSecondaryColour3Preview.setPreferredSize(new Dimension(pnlSecondaryColour3Preview.getPreferredSize().width, pnlSecondaryColour3Preview.getPreferredSize().height));
        pnlBlackColourPreviewContainer.setPreferredSize(new Dimension(pnlBlackColourPreviewContainer.getPreferredSize().width, pnlBlackColourPreviewContainer.getPreferredSize().height));
        pnlBlackColourPreview.setPreferredSize(new Dimension(pnlBlackColourPreview.getPreferredSize().width, pnlBlackColourPreview.getPreferredSize().height));
        pnlWhiteColourPreviewContainer.setPreferredSize(new Dimension(pnlWhiteColourPreviewContainer.getPreferredSize().width, pnlWhiteColourPreviewContainer.getPreferredSize().height));
        pnlWhiteColourPreview.setPreferredSize(new Dimension(pnlWhiteColourPreview.getPreferredSize().width, pnlWhiteColourPreview.getPreferredSize().height));
        btnPrimaryColour1Change.setPreferredSize(new Dimension(btnPrimaryColour1Change.getPreferredSize().width, btnPrimaryColour1Change.getPreferredSize().height));
        btnPrimaryColour2Change.setPreferredSize(new Dimension(btnPrimaryColour2Change.getPreferredSize().width, btnPrimaryColour2Change.getPreferredSize().height));
        btnPrimaryColour3Change.setPreferredSize(new Dimension(btnPrimaryColour3Change.getPreferredSize().width, btnPrimaryColour3Change.getPreferredSize().height));
        btnSecondaryColour1Change.setPreferredSize(new Dimension(btnSecondaryColour1Change.getPreferredSize().width, btnSecondaryColour1Change.getPreferredSize().height));
        btnSecondaryColour2Change.setPreferredSize(new Dimension(btnSecondaryColour2Change.getPreferredSize().width, btnSecondaryColour2Change.getPreferredSize().height));
        btnSecondaryColour3Change.setPreferredSize(new Dimension(btnSecondaryColour3Change.getPreferredSize().width, btnSecondaryColour3Change.getPreferredSize().height));
        btnBlackColourChange.setPreferredSize(new Dimension(btnBlackColourChange.getPreferredSize().width, btnBlackColourChange.getPreferredSize().height));
        btnWhiteColourChange.setPreferredSize(new Dimension(btnWhiteColourChange.getPreferredSize().width, btnWhiteColourChange.getPreferredSize().height));
        sldMenuOpacity.setPreferredSize(new Dimension(sldMenuOpacity.getPreferredSize().width, sldMenuOpacity.getPreferredSize().height));
        btnChangeFont.setPreferredSize(new Dimension(btnChangeFont.getPreferredSize().width, btnChangeFont.getPreferredSize().height));
        btnReset.setPreferredSize(new Dimension(btnReset.getPreferredSize().width, btnReset.getPreferredSize().height));
        pnlThemeButtons.setPreferredSize(new Dimension(pnlThemeButtons.getPreferredSize().width, btnReset.getPreferredSize().height + 6));
        spnWindowWidth.setPreferredSize(new Dimension(spnWindowWidth.getPreferredSize().width, spnWindowWidth.getPreferredSize().height));
        spnWindowHeight.setPreferredSize(new Dimension(spnWindowHeight.getPreferredSize().width, spnWindowHeight.getPreferredSize().height));
        txtBackgroundColour.setPreferredSize(new Dimension(txtBackgroundColour.getPreferredSize().width, txtBackgroundColour.getPreferredSize().height));
        pnlBackgroundPreviewContainer.setPreferredSize(new Dimension(pnlBackgroundPreviewContainer.getPreferredSize().width, pnlBackgroundPreviewContainer.getPreferredSize().height));
        pnlBackgroundPreview.setPreferredSize(new Dimension(pnlBackgroundPreview.getPreferredSize().width, pnlBackgroundPreview.getPreferredSize().height));
        btnBackgroundColourChange.setPreferredSize(new Dimension(btnBackgroundColourChange.getPreferredSize().width, btnBackgroundColourChange.getPreferredSize().height));
        btnBackgroundImageChange.setPreferredSize(new Dimension(btnBackgroundImageChange.getPreferredSize().width, btnBackgroundImageChange.getPreferredSize().height));
        btnBackgroundImageRemove.setPreferredSize(new Dimension(btnBackgroundImageRemove.getPreferredSize().width, btnBackgroundImageRemove.getPreferredSize().height));
        cmbBackgroundImageMode.setPreferredSize(btnBackgroundImageRemove.getPreferredSize());
        pnlBackgroundImage.setPreferredSize(new Dimension(pnlBackgroundImage.getPreferredSize().width, pnlBackgroundImage.getPreferredSize().height));
        pnlBackgroundImage.setMaximumSize(pnlBackgroundImage.getPreferredSize());
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
                if (desktop == null) {
                    log.log(Level.WARNING, "Can not open URL \"" + url + "\", as desktop operations are not supported on this platform");
                } else {
                    log.log(Level.WARNING, "Can not open URL \"" + url + "\", as the desktop browse operation is not supported on this platform");
                }
                JOptionPane.showMessageDialog(this, Main.getInstance().getLanguageBundle().getString("FailedOpenWebBrowserErrorMessage") + " " + url, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException | UnsupportedOperationException | URISyntaxException e) {
            log.log(Level.SEVERE, "Failed to open URL \"" + url + "\"", e);
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        sldOpacity = new JSlider();
        lblOpacity = new JLabel();
        chkAlwaysShowInformationScreen = new JCheckBox();
        pnlInteractiveWindows = new JPanel();
        pnlInteractiveButtons = new JPanel();
        btnAddInteractiveWindow = new JButton();
        btnRemoveInteractiveWindow = new JButton();
        jScrollPane1 = new JScrollPane();
        lstInteractiveWindows = new JList<>();
        pnlTheme = new JPanel();
        pnlThemeButtons = new JPanel();
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
        lblPrimaryColour3 = new JLabel();
        txtPrimaryColour3 = new JTextField();
        pnlPrimaryColour3PreviewContainer = new JPanel();
        gluePrimaryColour3a = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlPrimaryColour3Preview = new JPanel();
        gluePrimaryColour3b = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnPrimaryColour3Change = new JButton();
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
                                .addContainerGap(80, Short.MAX_VALUE))
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
                                .addContainerGap(75, Short.MAX_VALUE))
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

        pnlThemeButtons.setPreferredSize(new Dimension(380, 36));
        pnlThemeButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));

        btnChangeFont.setText("Change Font");
        btnChangeFont.setMaximumSize(new Dimension(130, 26));
        btnChangeFont.setName(""); // NOI18N
        btnChangeFont.setPreferredSize(new Dimension(130, 26));
        btnChangeFont.addActionListener(this::btnChangeFontActionPerformed);
        pnlThemeButtons.add(btnChangeFont);

        btnReset.setText("Reset");
        btnReset.setMaximumSize(new Dimension(130, 26));
        btnReset.setMinimumSize(new Dimension(95, 23));
        btnReset.setPreferredSize(new Dimension(130, 26));
        btnReset.addActionListener(this::btnResetActionPerformed);
        pnlThemeButtons.add(btnReset);

        lblPrimaryColour1.setText("Primary 1");

        txtPrimaryColour1.setEditable(false);
        txtPrimaryColour1.setText("#00FF00");
        txtPrimaryColour1.setPreferredSize(new Dimension(70, 24));

        pnlPrimaryColour1PreviewContainer.setLayout(new BoxLayout(pnlPrimaryColour1PreviewContainer, BoxLayout.Y_AXIS));
        pnlPrimaryColour1PreviewContainer.add(gluePrimaryColour1a);

        pnlPrimaryColour1Preview.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        pnlPrimaryColour1Preview.setPreferredSize(new Dimension(20, 20));

        GroupLayout pnlPrimaryColour1PreviewLayout = new GroupLayout(pnlPrimaryColour1Preview);
        pnlPrimaryColour1Preview.setLayout(pnlPrimaryColour1PreviewLayout);
        pnlPrimaryColour1PreviewLayout.setHorizontalGroup(
                pnlPrimaryColour1PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlPrimaryColour1PreviewLayout.setVerticalGroup(
                pnlPrimaryColour1PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        pnlPrimaryColour1PreviewContainer.add(pnlPrimaryColour1Preview);
        pnlPrimaryColour1PreviewContainer.add(gluePrimaryColour1b);

        btnPrimaryColour1Change.setText("Change");
        btnPrimaryColour1Change.addActionListener(this::btnPrimaryColour1ChangeActionPerformed);

        lblPrimaryColour2.setText("Primary 2");

        txtPrimaryColour2.setEditable(false);
        txtPrimaryColour2.setText("#00FF00");
        txtPrimaryColour2.setPreferredSize(new Dimension(70, 24));

        pnlPrimaryColour2PreviewContainer.setLayout(new BoxLayout(pnlPrimaryColour2PreviewContainer, BoxLayout.Y_AXIS));
        pnlPrimaryColour2PreviewContainer.add(gluePrimaryColour2a);

        pnlPrimaryColour2Preview.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        pnlPrimaryColour2Preview.setPreferredSize(new Dimension(20, 20));

        GroupLayout pnlPrimaryColour2PreviewLayout = new GroupLayout(pnlPrimaryColour2Preview);
        pnlPrimaryColour2Preview.setLayout(pnlPrimaryColour2PreviewLayout);
        pnlPrimaryColour2PreviewLayout.setHorizontalGroup(
                pnlPrimaryColour2PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlPrimaryColour2PreviewLayout.setVerticalGroup(
                pnlPrimaryColour2PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        pnlPrimaryColour2PreviewContainer.add(pnlPrimaryColour2Preview);
        pnlPrimaryColour2PreviewContainer.add(gluePrimaryColour2b);

        btnPrimaryColour2Change.setText("Change");
        btnPrimaryColour2Change.addActionListener(this::btnPrimaryColour2ChangeActionPerformed);

        lblPrimaryColour3.setText("Primary 3");

        txtPrimaryColour3.setEditable(false);
        txtPrimaryColour3.setText("#00FF00");
        txtPrimaryColour3.setPreferredSize(new Dimension(70, 24));

        pnlPrimaryColour3PreviewContainer.setLayout(new BoxLayout(pnlPrimaryColour3PreviewContainer, BoxLayout.Y_AXIS));
        pnlPrimaryColour3PreviewContainer.add(gluePrimaryColour3a);

        pnlPrimaryColour3Preview.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        pnlPrimaryColour3Preview.setPreferredSize(new Dimension(20, 20));

        GroupLayout pnlPrimaryColour3PreviewLayout = new GroupLayout(pnlPrimaryColour3Preview);
        pnlPrimaryColour3Preview.setLayout(pnlPrimaryColour3PreviewLayout);
        pnlPrimaryColour3PreviewLayout.setHorizontalGroup(
                pnlPrimaryColour3PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlPrimaryColour3PreviewLayout.setVerticalGroup(
                pnlPrimaryColour3PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        pnlPrimaryColour3PreviewContainer.add(pnlPrimaryColour3Preview);
        pnlPrimaryColour3PreviewContainer.add(gluePrimaryColour3b);

        btnPrimaryColour3Change.setText("Change");
        btnPrimaryColour3Change.addActionListener(this::btnPrimaryColour3ChangeActionPerformed);

        lblSecondaryColour1.setText("Secondary 1");

        txtSecondaryColour1.setEditable(false);
        txtSecondaryColour1.setText("#00FF00");
        txtSecondaryColour1.setPreferredSize(new Dimension(70, 24));

        pnlSecondaryColour1PreviewContainer.setLayout(new BoxLayout(pnlSecondaryColour1PreviewContainer, BoxLayout.Y_AXIS));
        pnlSecondaryColour1PreviewContainer.add(glueSecondaryColour1a);

        pnlSecondaryColour1Preview.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        pnlSecondaryColour1Preview.setPreferredSize(new Dimension(20, 20));

        GroupLayout pnlSecondaryColour1PreviewLayout = new GroupLayout(pnlSecondaryColour1Preview);
        pnlSecondaryColour1Preview.setLayout(pnlSecondaryColour1PreviewLayout);
        pnlSecondaryColour1PreviewLayout.setHorizontalGroup(
                pnlSecondaryColour1PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlSecondaryColour1PreviewLayout.setVerticalGroup(
                pnlSecondaryColour1PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        pnlSecondaryColour1PreviewContainer.add(pnlSecondaryColour1Preview);
        pnlSecondaryColour1PreviewContainer.add(glueSecondaryColour1b);

        btnSecondaryColour1Change.setText("Change");
        btnSecondaryColour1Change.addActionListener(this::btnSecondaryColour1ChangeActionPerformed);

        lblSecondaryColour2.setText("Secondary 2");

        txtSecondaryColour2.setEditable(false);
        txtSecondaryColour2.setText("#00FF00");
        txtSecondaryColour2.setPreferredSize(new Dimension(70, 24));

        pnlSecondaryColour2PreviewContainer.setLayout(new BoxLayout(pnlSecondaryColour2PreviewContainer, BoxLayout.Y_AXIS));
        pnlSecondaryColour2PreviewContainer.add(glueSecondaryColour2a);

        pnlSecondaryColour2Preview.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        pnlSecondaryColour2Preview.setPreferredSize(new Dimension(20, 20));

        GroupLayout pnlSecondaryColour2PreviewLayout = new GroupLayout(pnlSecondaryColour2Preview);
        pnlSecondaryColour2Preview.setLayout(pnlSecondaryColour2PreviewLayout);
        pnlSecondaryColour2PreviewLayout.setHorizontalGroup(
                pnlSecondaryColour2PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlSecondaryColour2PreviewLayout.setVerticalGroup(
                pnlSecondaryColour2PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        pnlSecondaryColour2PreviewContainer.add(pnlSecondaryColour2Preview);
        pnlSecondaryColour2PreviewContainer.add(glueSecondaryColour2b);

        btnSecondaryColour2Change.setText("Change");
        btnSecondaryColour2Change.addActionListener(this::btnSecondaryColour2ChangeActionPerformed);

        lblSecondaryColour3.setText("Secondary 3");

        txtSecondaryColour3.setEditable(false);
        txtSecondaryColour3.setText("#00FF00");
        txtSecondaryColour3.setPreferredSize(new Dimension(70, 24));

        pnlSecondaryColour3PreviewContainer.setLayout(new BoxLayout(pnlSecondaryColour3PreviewContainer, BoxLayout.Y_AXIS));
        pnlSecondaryColour3PreviewContainer.add(glueSecondaryColour3a);

        pnlSecondaryColour3Preview.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        pnlSecondaryColour3Preview.setPreferredSize(new Dimension(20, 20));

        GroupLayout pnlSecondaryColour3PreviewLayout = new GroupLayout(pnlSecondaryColour3Preview);
        pnlSecondaryColour3Preview.setLayout(pnlSecondaryColour3PreviewLayout);
        pnlSecondaryColour3PreviewLayout.setHorizontalGroup(
                pnlSecondaryColour3PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlSecondaryColour3PreviewLayout.setVerticalGroup(
                pnlSecondaryColour3PreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        pnlSecondaryColour3PreviewContainer.add(pnlSecondaryColour3Preview);
        pnlSecondaryColour3PreviewContainer.add(glueSecondaryColour3b);

        btnSecondaryColour3Change.setText("Change");
        btnSecondaryColour3Change.addActionListener(this::btnSecondaryColour3ChangeActionPerformed);

        lblMenuOpacity.setText("Menu Opacity");

        sldMenuOpacity.setMajorTickSpacing(10);
        sldMenuOpacity.setMinorTickSpacing(5);
        sldMenuOpacity.setPaintLabels(true);
        sldMenuOpacity.setPaintTicks(true);
        sldMenuOpacity.setSnapToTicks(true);
        sldMenuOpacity.setValue(10);
        sldMenuOpacity.setPreferredSize(new Dimension(300, 45));
        sldMenuOpacity.addChangeListener(this::sldMenuOpacityStateChanged);

        lblBlackColour.setText("Text");

        txtBlackColour.setEditable(false);
        txtBlackColour.setText("#00FF00");
        txtBlackColour.setPreferredSize(new Dimension(70, 24));

        pnlBlackColourPreviewContainer.setLayout(new BoxLayout(pnlBlackColourPreviewContainer, BoxLayout.Y_AXIS));
        pnlBlackColourPreviewContainer.add(glueBlackColoura);

        pnlBlackColourPreview.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        pnlBlackColourPreview.setPreferredSize(new Dimension(20, 20));

        GroupLayout pnlBlackColourPreviewLayout = new GroupLayout(pnlBlackColourPreview);
        pnlBlackColourPreview.setLayout(pnlBlackColourPreviewLayout);
        pnlBlackColourPreviewLayout.setHorizontalGroup(
                pnlBlackColourPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlBlackColourPreviewLayout.setVerticalGroup(
                pnlBlackColourPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        pnlBlackColourPreviewContainer.add(pnlBlackColourPreview);
        pnlBlackColourPreviewContainer.add(glueBlackColourb);

        btnBlackColourChange.setText("Change");
        btnBlackColourChange.addActionListener(this::btnBlackColourChangeActionPerformed);

        lblWhiteColour.setText("Background");
        lblWhiteColour.setToolTipText("");

        txtWhiteColour.setEditable(false);
        txtWhiteColour.setText("#00FF00");
        txtWhiteColour.setPreferredSize(new Dimension(70, 24));

        pnlWhiteColourPreviewContainer.setLayout(new BoxLayout(pnlWhiteColourPreviewContainer, BoxLayout.Y_AXIS));
        pnlWhiteColourPreviewContainer.add(glueWhiteColoura);

        pnlWhiteColourPreview.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        pnlWhiteColourPreview.setPreferredSize(new Dimension(20, 20));

        GroupLayout pnlWhiteColourPreviewLayout = new GroupLayout(pnlWhiteColourPreview);
        pnlWhiteColourPreview.setLayout(pnlWhiteColourPreviewLayout);
        pnlWhiteColourPreviewLayout.setHorizontalGroup(
                pnlWhiteColourPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlWhiteColourPreviewLayout.setVerticalGroup(
                pnlWhiteColourPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        pnlWhiteColourPreviewContainer.add(pnlWhiteColourPreview);
        pnlWhiteColourPreviewContainer.add(glueWhiteColourb);

        btnWhiteColourChange.setText("Change");
        btnWhiteColourChange.addActionListener(this::btnWhiteColourChangeActionPerformed);

        GroupLayout pnlThemeLayout = new GroupLayout(pnlTheme);
        pnlTheme.setLayout(pnlThemeLayout);
        pnlThemeLayout.setHorizontalGroup(
                pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(pnlThemeButtons, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                        .addComponent(lblPrimaryColour1)
                                                                        .addComponent(lblPrimaryColour2)
                                                                        .addComponent(lblPrimaryColour3)
                                                                        .addComponent(lblSecondaryColour1)
                                                                        .addComponent(lblSecondaryColour2)
                                                                        .addComponent(lblSecondaryColour3)
                                                                        .addComponent(lblBlackColour)
                                                                        .addComponent(lblWhiteColour))
                                                                .addGap(18, 18, 18)
                                                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                .addComponent(txtPrimaryColour1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(pnlPrimaryColour1PreviewContainer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                .addComponent(btnPrimaryColour1Change))
                                                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                .addComponent(txtPrimaryColour2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(pnlPrimaryColour2PreviewContainer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                .addComponent(btnPrimaryColour2Change))
                                                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                .addComponent(txtPrimaryColour3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(pnlPrimaryColour3PreviewContainer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                .addComponent(btnPrimaryColour3Change))
                                                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                .addComponent(txtSecondaryColour1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(pnlSecondaryColour1PreviewContainer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                .addComponent(btnSecondaryColour1Change))
                                                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                .addComponent(txtSecondaryColour2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(pnlSecondaryColour2PreviewContainer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                .addComponent(btnSecondaryColour2Change))
                                                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                .addComponent(txtSecondaryColour3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(pnlSecondaryColour3PreviewContainer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                .addComponent(btnSecondaryColour3Change))
                                                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                                                                        .addGroup(GroupLayout.Alignment.LEADING, pnlThemeLayout.createSequentialGroup()
                                                                                                .addComponent(txtWhiteColour, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(pnlWhiteColourPreviewContainer, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                                .addComponent(txtBlackColour, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(pnlBlackColourPreviewContainer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                                        .addComponent(btnBlackColourChange)
                                                                                        .addComponent(btnWhiteColourChange, GroupLayout.Alignment.TRAILING))))
                                                                .addGap(0, 0, Short.MAX_VALUE)))
                                                .addContainerGap())
                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(lblMenuOpacity)
                                                        .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                .addGap(10, 10, 10)
                                                                .addComponent(sldMenuOpacity, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                                .addGap(0, 0, Short.MAX_VALUE))))
        );
        pnlThemeLayout.setVerticalGroup(
                pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, pnlThemeLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblPrimaryColour1)
                                                .addComponent(txtPrimaryColour1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnPrimaryColour1Change))
                                        .addComponent(pnlPrimaryColour1PreviewContainer, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblPrimaryColour2)
                                                .addComponent(txtPrimaryColour2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnPrimaryColour2Change))
                                        .addComponent(pnlPrimaryColour2PreviewContainer, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblPrimaryColour3)
                                                .addComponent(txtPrimaryColour3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnPrimaryColour3Change))
                                        .addComponent(pnlPrimaryColour3PreviewContainer, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblSecondaryColour1)
                                                .addComponent(txtSecondaryColour1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnSecondaryColour1Change))
                                        .addComponent(pnlSecondaryColour1PreviewContainer, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblSecondaryColour2)
                                                .addComponent(txtSecondaryColour2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnSecondaryColour2Change))
                                        .addComponent(pnlSecondaryColour2PreviewContainer, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblSecondaryColour3)
                                                .addComponent(txtSecondaryColour3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnSecondaryColour3Change))
                                        .addComponent(pnlSecondaryColour3PreviewContainer, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblBlackColour)
                                                .addComponent(txtBlackColour, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnBlackColourChange))
                                        .addComponent(pnlBlackColourPreviewContainer, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addGroup(pnlThemeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(lblWhiteColour)
                                                .addComponent(txtWhiteColour, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnWhiteColourChange))
                                        .addComponent(pnlWhiteColourPreviewContainer, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(18, 18, 18)
                                .addComponent(lblMenuOpacity)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sldMenuOpacity, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(pnlThemeButtons, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        pnlTabs.addTab("Theme", pnlTheme);

        chkWindowModeEnabled.setText("Enable Windowed Mode");
        chkWindowModeEnabled.addItemListener(this::chkWindowModeEnabledItemStateChanged);

        lblDimensions.setText("Dimensions");

        lblDimensionsX.setText("x");

        lblBackground.setText("Background");

        txtBackgroundColour.setEditable(false);
        txtBackgroundColour.setText("#00FF00");
        txtBackgroundColour.setPreferredSize(new Dimension(70, 24));

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
                                .addContainerGap(139, Short.MAX_VALUE))
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
                                .addContainerGap(150, Short.MAX_VALUE))
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
        lblVersion.setText("1.0.21.1");
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
        interactiveWindowReloadRequired = !properties.getProperty("InteractiveWindows", "").equals(interactiveWindows);

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

        try (OutputStream output = Files.newOutputStream(Main.THEME_FILE)) {
            properties = new Properties();
            properties.setProperty("nimrodlf.p1", String.format("#%02X%02X%02X", primaryColour1.getRed(), primaryColour1.getGreen(), primaryColour1.getBlue()));
            properties.setProperty("nimrodlf.p2", String.format("#%02X%02X%02X", primaryColour2.getRed(), primaryColour2.getGreen(), primaryColour2.getBlue()));
            properties.setProperty("nimrodlf.p3", String.format("#%02X%02X%02X", primaryColour3.getRed(), primaryColour3.getGreen(), primaryColour3.getBlue()));
            properties.setProperty("nimrodlf.s1", String.format("#%02X%02X%02X", secondaryColour1.getRed(), secondaryColour1.getGreen(), secondaryColour1.getBlue()));
            properties.setProperty("nimrodlf.s2", String.format("#%02X%02X%02X", secondaryColour2.getRed(), secondaryColour2.getGreen(), secondaryColour2.getBlue()));
            properties.setProperty("nimrodlf.s3", String.format("#%02X%02X%02X", secondaryColour3.getRed(), secondaryColour3.getGreen(), secondaryColour3.getBlue()));
            properties.setProperty("nimrodlf.b", String.format("#%02X%02X%02X", blackColour.getRed(), blackColour.getGreen(), blackColour.getBlue()));
            properties.setProperty("nimrodlf.w", String.format("#%02X%02X%02X", whiteColour.getRed(), whiteColour.getGreen(), whiteColour.getBlue()));
            properties.setProperty("nimrodlf.menuOpacity", String.valueOf((int) (menuOpacity * 255)));
            properties.setProperty("nimrodlf.frameOpacity", "255");
            properties.setProperty("nimrodlf.font", String.format("%s-%s-%d",
                    font.getName(),
                    font.getStyle() == Font.PLAIN ? "PLAIN" :
                            font.getStyle() == Font.BOLD ? "BOLD" :
                                    font.getStyle() == Font.ITALIC ? "ITALIC" :
                                            "BOLDITALIC",
                    font.getSize()));
            properties.store(output, null);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to save settings", e);
        }
        dispose();
    }// GEN-LAST:event_btnDoneActionPerformed

    private void btnCancelActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnCancelActionPerformed
    {// GEN-HEADEREND:event_btnCancelActionPerformed
        theme = oldTheme;
        refreshTheme();
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

    private void btnPrimaryColour1ChangeActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnPrimaryColour1ChangeActionPerformed
    {// GEN-HEADEREND:event_btnPrimaryColour1ChangeActionPerformed
        primaryColour1 = chooseColour(primaryColour1, txtPrimaryColour1, pnlPrimaryColour1Preview, "ChooseColour");
        theme.setPrimary1(primaryColour1);
        refreshTheme();
    }// GEN-LAST:event_btnPrimaryColour1ChangeActionPerformed

    private void btnPrimaryColour2ChangeActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnPrimaryColour2ChangeActionPerformed
    {// GEN-HEADEREND:event_btnPrimaryColour2ChangeActionPerformed
        primaryColour2 = chooseColour(primaryColour2, txtPrimaryColour2, pnlPrimaryColour2Preview, "ChooseColour");
        theme.setPrimary2(primaryColour2);
        refreshTheme();
    }// GEN-LAST:event_btnPrimaryColour2ChangeActionPerformed

    private void btnPrimaryColour3ChangeActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnPrimaryColour3ChangeActionPerformed
    {// GEN-HEADEREND:event_btnPrimaryColour3ChangeActionPerformed
        primaryColour3 = chooseColour(primaryColour3, txtPrimaryColour3, pnlPrimaryColour3Preview, "ChooseColour");
        theme.setPrimary3(primaryColour3);
        refreshTheme();
    }// GEN-LAST:event_btnPrimaryColour3ChangeActionPerformed

    private void btnSecondaryColour1ChangeActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnSecondaryColour1ChangeActionPerformed
    {// GEN-HEADEREND:event_btnSecondaryColour1ChangeActionPerformed
        secondaryColour1 = chooseColour(secondaryColour1, txtSecondaryColour1, pnlSecondaryColour1Preview, "ChooseColour");
        theme.setSecondary1(secondaryColour1);
        refreshTheme();
    }// GEN-LAST:event_btnSecondaryColour1ChangeActionPerformed

    private void btnSecondaryColour2ChangeActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnSecondaryColour2ChangeActionPerformed
    {// GEN-HEADEREND:event_btnSecondaryColour2ChangeActionPerformed
        secondaryColour2 = chooseColour(secondaryColour2, txtSecondaryColour2, pnlSecondaryColour2Preview, "ChooseColour");
        theme.setSecondary2(secondaryColour2);
        refreshTheme();
    }// GEN-LAST:event_btnSecondaryColour2ChangeActionPerformed

    private void btnSecondaryColour3ChangeActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnSecondaryColour3ChangeActionPerformed
    {// GEN-HEADEREND:event_btnSecondaryColour3ChangeActionPerformed
        secondaryColour3 = chooseColour(secondaryColour3, txtSecondaryColour3, pnlSecondaryColour3Preview, "ChooseColour");
        theme.setSecondary3(secondaryColour3);
        refreshTheme();
    }// GEN-LAST:event_btnSecondaryColour3ChangeActionPerformed

    private void sldMenuOpacityStateChanged(ChangeEvent evt)// GEN-FIRST:event_sldMenuOpacityStateChanged
    {// GEN-HEADEREND:event_sldMenuOpacityStateChanged
        if (!sldMenuOpacity.getValueIsAdjusting()) {
            if (sldMenuOpacity.getValue() == 0) {
                sldMenuOpacity.setValue(1);
            } else {
                menuOpacity = sldMenuOpacity.getValue() / 100.0;
                theme.setMenuOpacity((int) (menuOpacity * 255));
            }
        }
    }// GEN-LAST:event_sldMenuOpacityStateChanged

    private void btnChangeFontActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnChangeFontActionPerformed
    {// GEN-HEADEREND:event_btnChangeFontActionPerformed
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        NimRODFontDialog dialog = new NimRODFontDialog(frame, font);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        font = dialog.getSelectedFont();
        if (!dialog.isCanceled()) {
            theme.setFont(font);
            refreshTheme();
        }
    }// GEN-LAST:event_btnChangeFontActionPerformed

    private void btnResetActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnResetActionPerformed
    {// GEN-HEADEREND:event_btnResetActionPerformed
        primaryColour1 = Color.decode("#1EA6EB");
        primaryColour2 = Color.decode("#28B0F5");
        primaryColour3 = Color.decode("#32BAFF");
        secondaryColour1 = Color.decode("#BCBCBE");
        secondaryColour2 = Color.decode("#C6C6C8");
        secondaryColour3 = Color.decode("#D0D0D2");
        blackColour = Color.decode("#000000");
        whiteColour = Color.decode("#FFFFFF");
        font = Font.decode("SansSerif-PLAIN-12");
        pnlPrimaryColour1Preview.setBackground(primaryColour1);
        txtPrimaryColour1.setText(String.format("#%02X%02X%02X", primaryColour1.getRed(), primaryColour1.getGreen(), primaryColour1.getBlue()));
        pnlPrimaryColour2Preview.setBackground(primaryColour2);
        txtPrimaryColour2.setText(String.format("#%02X%02X%02X", primaryColour2.getRed(), primaryColour2.getGreen(), primaryColour2.getBlue()));
        pnlPrimaryColour3Preview.setBackground(primaryColour3);
        txtPrimaryColour3.setText(String.format("#%02X%02X%02X", primaryColour3.getRed(), primaryColour2.getGreen(), primaryColour3.getBlue()));
        pnlSecondaryColour1Preview.setBackground(secondaryColour1);
        txtSecondaryColour1.setText(String.format("#%02X%02X%02X", secondaryColour1.getRed(), secondaryColour1.getGreen(), secondaryColour1.getBlue()));
        pnlSecondaryColour2Preview.setBackground(secondaryColour2);
        txtSecondaryColour2.setText(String.format("#%02X%02X%02X", secondaryColour2.getRed(), secondaryColour2.getGreen(), secondaryColour2.getBlue()));
        pnlSecondaryColour3Preview.setBackground(secondaryColour3);
        txtSecondaryColour3.setText(String.format("#%02X%02X%02X", secondaryColour3.getRed(), secondaryColour3.getGreen(), secondaryColour3.getBlue()));
        pnlBlackColourPreview.setBackground(blackColour);
        txtBlackColour.setText(String.format("#%02X%02X%02X", blackColour.getRed(), blackColour.getGreen(), blackColour.getBlue()));
        pnlWhiteColourPreview.setBackground(whiteColour);
        txtWhiteColour.setText(String.format("#%02X%02X%02X", whiteColour.getRed(), whiteColour.getGreen(), whiteColour.getBlue()));
        menuOpacity = 1.0;
        theme.setPrimary1(primaryColour1);
        theme.setPrimary2(primaryColour2);
        theme.setPrimary3(primaryColour3);
        theme.setSecondary1(secondaryColour1);
        theme.setSecondary2(secondaryColour2);
        theme.setSecondary3(secondaryColour3);
        theme.setBlack(blackColour);
        theme.setWhite(whiteColour);
        theme.setFont(font);
        sldMenuOpacity.setValue((int) (menuOpacity * 100));
        refreshTheme();
    }// GEN-LAST:event_btnResetActionPerformed

    private void chkAlwaysShowInformationScreenItemStateChanged(ItemEvent evt)// GEN-FIRST:event_chkAlwaysShowInformationScreenItemStateChanged
    {// GEN-HEADEREND:event_chkAlwaysShowInformationScreenItemStateChanged
        alwaysShowInformationScreen = evt.getStateChange() == ItemEvent.SELECTED;
    }// GEN-LAST:event_chkAlwaysShowInformationScreenItemStateChanged

    private void btnBlackColourChangeActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnBlackColourChangeActionPerformed
    {// GEN-HEADEREND:event_btnBlackColourChangeActionPerformed
        blackColour = chooseColour(blackColour, txtBlackColour, pnlBlackColourPreview, "ChooseColour");
        theme.setBlack(blackColour);
        refreshTheme();
    }// GEN-LAST:event_btnBlackColourChangeActionPerformed

    private void btnWhiteColourChangeActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnWhiteColourChangeActionPerformed
    {// GEN-HEADEREND:event_btnWhiteColourChangeActionPerformed
        whiteColour = chooseColour(whiteColour, txtWhiteColour, pnlWhiteColourPreview, "ChooseColour");
        theme.setWhite(whiteColour);
        refreshTheme();
    }// GEN-LAST:event_btnWhiteColourChangeActionPerformed

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

    private void refreshTheme() {
        try {
            NimRODLookAndFeel.setCurrentTheme(theme);
            UIManager.setLookAndFeel(lookAndFeel);
            SwingUtilities.updateComponentTreeUI(this);
            pack();
        } catch (UnsupportedLookAndFeelException ignored) {
        }
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
    private JButton btnPrimaryColour3Change;
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
    private Box.Filler gluePrimaryColour3a;
    private Box.Filler gluePrimaryColour3b;
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
    private JLabel lblPrimaryColour3;
    private JLabel lblScaling;
    private JLabel lblSecondaryColour1;
    private JLabel lblSecondaryColour2;
    private JLabel lblSecondaryColour3;
    private JLabel lblShimejiEE;
    private JLabel lblVersion;
    private JLabel lblWhiteColour;
    private JList<String> lstInteractiveWindows;
    private JPanel pnlAbout;
    private JPanel pnlAboutButtons;
    private JPanel pnlBackgroundImage;
    private JPanel pnlBackgroundPreview;
    private JPanel pnlBackgroundPreviewContainer;
    private JPanel pnlBlackColourPreview;
    private JPanel pnlBlackColourPreviewContainer;
    private JPanel pnlFooter;
    private JPanel pnlGeneral;
    private JPanel pnlInteractiveButtons;
    private JPanel pnlInteractiveWindows;
    private JPanel pnlPrimaryColour1Preview;
    private JPanel pnlPrimaryColour1PreviewContainer;
    private JPanel pnlPrimaryColour2Preview;
    private JPanel pnlPrimaryColour2PreviewContainer;
    private JPanel pnlPrimaryColour3Preview;
    private JPanel pnlPrimaryColour3PreviewContainer;
    private JPanel pnlSecondaryColour1Preview;
    private JPanel pnlSecondaryColour1PreviewContainer;
    private JPanel pnlSecondaryColour2Preview;
    private JPanel pnlSecondaryColour2PreviewContainer;
    private JPanel pnlSecondaryColour3Preview;
    private JPanel pnlSecondaryColour3PreviewContainer;
    private JTabbedPane pnlTabs;
    private JPanel pnlTheme;
    private JPanel pnlThemeButtons;
    private JPanel pnlWhiteColourPreview;
    private JPanel pnlWhiteColourPreviewContainer;
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
    private JTextField txtPrimaryColour3;
    private JTextField txtSecondaryColour1;
    private JTextField txtSecondaryColour2;
    private JTextField txtSecondaryColour3;
    private JTextField txtWhiteColour;
    // End of variables declaration//GEN-END:variables
}
