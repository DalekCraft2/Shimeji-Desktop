/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */

package com.group_finity.mascot;

import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.imagesetchooser.ImageSetChooser;
import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.sound.Sounds;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 * @author DalekCraft
 */
public class TrayMenuPanel extends javax.swing.JPanel implements Localizable {

    private static final Locale SAUDI_ARABIA = Locale.forLanguageTag("ar-SA");

    private static final Locale SPAIN_CATALAN = Locale.forLanguageTag("ca-ES");

    private static final Locale SPAIN = Locale.forLanguageTag("es-ES");

    private static final Locale CROATIA = Locale.forLanguageTag("hr-HR");

    private static final Locale NETHERLANDS = Locale.forLanguageTag("nl-NL");

    private static final Locale POLAND = Locale.forLanguageTag("pl-PL");

    private static final Locale BRAZILIAN_PORTUGUESE = Locale.forLanguageTag("pt-BR");

    private static final Locale PORTUGAL = Locale.forLanguageTag("pt-PT");

    private static final Locale RUSSIA = Locale.forLanguageTag("ru-RU");

    private static final Locale ROMANIA = Locale.forLanguageTag("ro-RO");

    private static final Locale SERBIA = Locale.forLanguageTag("sr-RS");

    private static final Locale FINLAND = Locale.forLanguageTag("fi-FI");

    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    private final boolean useSystemTray;

    /**
     * Creates new form TrayMenuPanel
     *
     * @param useSystemTray whether the system tray is being used.
     * If true, the menu will be disposed after one of its options has been pressed.
     * If false, the menu will not be disposed.
     */
    public TrayMenuPanel(boolean useSystemTray) {
        this.useSystemTray = useSystemTray;
        initComponents();
        localize(Main.getInstance().getLanguageBundle());
        initComponentStates();
    }

    @Override
    public void localize(ResourceBundle languageBundle) {
        Manager manager = Main.getInstance().getManager();

        Container topLevelAncestor = getTopLevelAncestor();
        if (topLevelAncestor != null) {
            Settings settings = Main.getInstance().getSettings();
            String title = settings.shimejiEeNameOverride;
            if (title.isEmpty()) {
                title = languageBundle.getString("ShimejiEE");
            }
            if (topLevelAncestor instanceof JDialog jDialog) {
                jDialog.setTitle(title);
            } else if (topLevelAncestor instanceof JFrame jFrame) {
                jFrame.setTitle(title);
            }
        }
        btnCallShimeji.setText(languageBundle.getString("CallShimeji"));
        btnFollowCursor.setText(languageBundle.getString("FollowCursor"));
        btnReduceToOne.setText(languageBundle.getString("ReduceToOne"));
        btnRestoreWindows.setText(languageBundle.getString("RestoreWindows"));
        btnAllowedBehaviors.setText(languageBundle.getString("AllowedBehaviours"));
        chkBreeding.setText(languageBundle.getString("BreedingCloning"));
        chkTransient.setText(languageBundle.getString("BreedingTransient"));
        chkTransformation.setText(languageBundle.getString("Transformation"));
        chkThrowing.setText(languageBundle.getString("ThrowingWindows"));
        chkSounds.setText(languageBundle.getString("SoundEffects"));
        chkMultiscreen.setText(languageBundle.getString("Multiscreen"));
        btnChooseShimeji.setText(languageBundle.getString("ChooseShimeji"));
        btnSettings.setText(languageBundle.getString("Settings"));
        btnLanguage.setText(languageBundle.getString("Language"));
        btnPauseAll.setText(manager.isPaused() ? languageBundle.getString("ResumeAnimations") : languageBundle.getString("PauseAnimations"));
        btnDismissAll.setText(languageBundle.getString("DismissAll"));
    }

    void refreshPauseText() {
        ResourceBundle language = Main.getInstance().getLanguageBundle();
        Manager manager = Main.getInstance().getManager();
        btnPauseAll.setText(manager.isPaused() ? language.getString("ResumeAnimations") : language.getString("PauseAnimations"));
    }

    private void initComponentStates() {
        Settings settings = Main.getInstance().getSettings();
        chkBreeding.setSelected(settings.breeding);
        chkTransient.setSelected(settings.transients);
        chkTransformation.setSelected(settings.transformation);
        chkThrowing.setSelected(settings.throwing);
        chkSounds.setSelected(settings.sounds);
        chkMultiscreen.setSelected(settings.multiscreen);

        Locale locale = settings.language;
        if (locale.equals(Locale.UK)) {
            itmEnglish.setSelected(true);
        } else if (locale.equals(SAUDI_ARABIA)) {
            itmArabic.setSelected(true);
        } else if (locale.equals(SPAIN_CATALAN)) {
            itmCatalan.setSelected(true);
        } else if (locale.equals(Locale.GERMANY)) {
            itmGerman.setSelected(true);
        } else if (locale.equals(SPAIN)) {
            itmSpanish.setSelected(true);
        } else if (locale.equals(Locale.FRANCE)) {
            itmFrench.setSelected(true);
        } else if (locale.equals(CROATIA)) {
            itmCroatian.setSelected(true);
        } else if (locale.equals(Locale.ITALY)) {
            itmItalian.setSelected(true);
        } else if (locale.equals(NETHERLANDS)) {
            itmDutch.setSelected(true);
        } else if (locale.equals(POLAND)) {
            itmPolish.setSelected(true);
        } else if (locale.equals(BRAZILIAN_PORTUGUESE)) {
            itmBrazilianPortuguese.setSelected(true);
        } else if (locale.equals(PORTUGAL)) {
            itmPortuguese.setSelected(true);
        } else if (locale.equals(RUSSIA)) {
            itmRussian.setSelected(true);
        } else if (locale.equals(ROMANIA)) {
            itmRomanian.setSelected(true);
        } else if (locale.equals(SERBIA)) {
            itmSerbian.setSelected(true);
        } else if (locale.equals(FINLAND)) {
            itmFinnish.setSelected(true);
        } else if (locale.equals(VIETNAM)) {
            itmVietnamese.setSelected(true);
        } else if (locale.equals(Locale.SIMPLIFIED_CHINESE)) {
            itmChinese.setSelected(true);
        } else if (locale.equals(Locale.TRADITIONAL_CHINESE)) {
            itmChineseTraditional.setSelected(true);
        } else if (locale.equals(Locale.KOREA)) {
            itmKorean.setSelected(true);
        } else if (locale.equals(Locale.JAPAN)) {
            itmJapanese.setSelected(true);
        }
    }

    /**
     * Recalculates the parent window's minimum size by packing it, and then reverts it to its previous size
     * if it was not already at the minimum size.
     */
    private void repackWindow() {
        Container topLevelAncestor = getTopLevelAncestor();
        Dimension prevSize = topLevelAncestor.getSize();
        boolean sizeIsMinimum = prevSize.equals(topLevelAncestor.getMinimumSize());
        topLevelAncestor.setMinimumSize(null);
        ((Window) topLevelAncestor).pack();
        topLevelAncestor.setMinimumSize(topLevelAncestor.getSize());
        // If the previous size was not the minimum size, keep the window at that size
        if (!sizeIsMinimum) {
            topLevelAncestor.setSize(prevSize);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        behaviorPopup = new javax.swing.JPopupMenu();
        chkBreeding = new javax.swing.JCheckBoxMenuItem();
        chkTransient = new javax.swing.JCheckBoxMenuItem();
        chkTransformation = new javax.swing.JCheckBoxMenuItem();
        chkThrowing = new javax.swing.JCheckBoxMenuItem();
        chkSounds = new javax.swing.JCheckBoxMenuItem();
        chkMultiscreen = new javax.swing.JCheckBoxMenuItem();
        languagePopup = new javax.swing.JPopupMenu();
        itmEnglish = new javax.swing.JCheckBoxMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        itmArabic = new javax.swing.JCheckBoxMenuItem();
        itmCatalan = new javax.swing.JCheckBoxMenuItem();
        itmGerman = new javax.swing.JCheckBoxMenuItem();
        itmSpanish = new javax.swing.JCheckBoxMenuItem();
        itmFrench = new javax.swing.JCheckBoxMenuItem();
        itmCroatian = new javax.swing.JCheckBoxMenuItem();
        itmItalian = new javax.swing.JCheckBoxMenuItem();
        itmDutch = new javax.swing.JCheckBoxMenuItem();
        itmPolish = new javax.swing.JCheckBoxMenuItem();
        itmBrazilianPortuguese = new javax.swing.JCheckBoxMenuItem();
        itmPortuguese = new javax.swing.JCheckBoxMenuItem();
        itmRussian = new javax.swing.JCheckBoxMenuItem();
        itmRomanian = new javax.swing.JCheckBoxMenuItem();
        itmSerbian = new javax.swing.JCheckBoxMenuItem();
        itmFinnish = new javax.swing.JCheckBoxMenuItem();
        itmVietnamese = new javax.swing.JCheckBoxMenuItem();
        itmChinese = new javax.swing.JCheckBoxMenuItem();
        itmChineseTraditional = new javax.swing.JCheckBoxMenuItem();
        itmKorean = new javax.swing.JCheckBoxMenuItem();
        itmJapanese = new javax.swing.JCheckBoxMenuItem();
        grpLanguage = new javax.swing.ButtonGroup();
        btnCallShimeji = new javax.swing.JButton();
        btnFollowCursor = new javax.swing.JButton();
        btnReduceToOne = new javax.swing.JButton();
        btnRestoreWindows = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        btnAllowedBehaviors = new javax.swing.JButton();
        btnChooseShimeji = new javax.swing.JButton();
        btnSettings = new javax.swing.JButton();
        btnLanguage = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        btnPauseAll = new javax.swing.JButton();
        btnDismissAll = new javax.swing.JButton();

        behaviorPopup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
                behaviorPopupPopupMenuWillBecomeInvisible(evt);
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
            }
        });

        chkBreeding.setSelected(true);
        chkBreeding.setText("Breeding/Cloning");
        chkBreeding.addItemListener(this::chkBreedingItemStateChanged);
        behaviorPopup.add(chkBreeding);

        chkTransient.setSelected(true);
        chkTransient.setText("Transients");
        chkTransient.addItemListener(this::chkTransientItemStateChanged);
        behaviorPopup.add(chkTransient);

        chkTransformation.setSelected(true);
        chkTransformation.setText("Transformation");
        chkTransformation.addItemListener(this::chkTransformationItemStateChanged);
        behaviorPopup.add(chkTransformation);

        chkThrowing.setSelected(true);
        chkThrowing.setText("Throwing Windows");
        chkThrowing.addItemListener(this::chkThrowingItemStateChanged);
        behaviorPopup.add(chkThrowing);

        chkSounds.setSelected(true);
        chkSounds.setText("Sound Effects");
        chkSounds.addItemListener(this::chkSoundsItemStateChanged);
        behaviorPopup.add(chkSounds);

        chkMultiscreen.setSelected(true);
        chkMultiscreen.setText("Move Between Screens");
        chkMultiscreen.addItemListener(this::chkMultiscreenItemStateChanged);
        behaviorPopup.add(chkMultiscreen);

        languagePopup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
                languagePopupPopupMenuWillBecomeInvisible(evt);
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
            }
        });

        grpLanguage.add(itmEnglish);
        itmEnglish.setText("English");
        itmEnglish.addActionListener(this::itmEnglishActionPerformed);
        languagePopup.add(itmEnglish);
        languagePopup.add(jSeparator3);

        grpLanguage.add(itmArabic);
        itmArabic.setText("عربي");
        itmArabic.addActionListener(this::itmArabicActionPerformed);
        languagePopup.add(itmArabic);

        grpLanguage.add(itmCatalan);
        itmCatalan.setText("Català");
        itmCatalan.addActionListener(this::itmCatalanActionPerformed);
        languagePopup.add(itmCatalan);

        grpLanguage.add(itmGerman);
        itmGerman.setText("Deutsch");
        itmGerman.addActionListener(this::itmGermanActionPerformed);
        languagePopup.add(itmGerman);

        grpLanguage.add(itmSpanish);
        itmSpanish.setText("Español");
        itmSpanish.addActionListener(this::itmSpanishActionPerformed);
        languagePopup.add(itmSpanish);

        grpLanguage.add(itmFrench);
        itmFrench.setText("Français");
        itmFrench.addActionListener(this::itmFrenchActionPerformed);
        languagePopup.add(itmFrench);

        grpLanguage.add(itmCroatian);
        itmCroatian.setText("Hrvatski");
        itmCroatian.addActionListener(this::itmCroatianActionPerformed);
        languagePopup.add(itmCroatian);

        grpLanguage.add(itmItalian);
        itmItalian.setText("Italiano");
        itmItalian.addActionListener(this::itmItalianActionPerformed);
        languagePopup.add(itmItalian);

        grpLanguage.add(itmDutch);
        itmDutch.setText("Nederlands");
        itmDutch.addActionListener(this::itmDutchActionPerformed);
        languagePopup.add(itmDutch);

        grpLanguage.add(itmPolish);
        itmPolish.setText("Polski");
        itmPolish.addActionListener(this::itmPolishActionPerformed);
        languagePopup.add(itmPolish);

        grpLanguage.add(itmBrazilianPortuguese);
        itmBrazilianPortuguese.setText("Português Brasileiro");
        itmBrazilianPortuguese.addActionListener(this::itmBrazilianPortugueseActionPerformed);
        languagePopup.add(itmBrazilianPortuguese);

        grpLanguage.add(itmPortuguese);
        itmPortuguese.setText("Português");
        itmPortuguese.addActionListener(this::itmPortugueseActionPerformed);
        languagePopup.add(itmPortuguese);

        grpLanguage.add(itmRussian);
        itmRussian.setText("ру́сский язы́к");
        itmRussian.addActionListener(this::itmRussianActionPerformed);
        languagePopup.add(itmRussian);

        grpLanguage.add(itmRomanian);
        itmRomanian.setText("Română");
        itmRomanian.addActionListener(this::itmRomanianActionPerformed);
        languagePopup.add(itmRomanian);

        grpLanguage.add(itmSerbian);
        itmSerbian.setText("Srpski");
        itmSerbian.addActionListener(this::itmSerbianActionPerformed);
        languagePopup.add(itmSerbian);

        grpLanguage.add(itmFinnish);
        itmFinnish.setText("Suomi");
        itmFinnish.addActionListener(this::itmFinnishActionPerformed);
        languagePopup.add(itmFinnish);

        grpLanguage.add(itmVietnamese);
        itmVietnamese.setText("tiếng Việt");
        itmVietnamese.addActionListener(this::itmVietnameseActionPerformed);
        languagePopup.add(itmVietnamese);

        grpLanguage.add(itmChinese);
        itmChinese.setText("简体中文");
        itmChinese.addActionListener(this::itmChineseActionPerformed);
        languagePopup.add(itmChinese);

        grpLanguage.add(itmChineseTraditional);
        itmChineseTraditional.setText("繁體中文");
        itmChineseTraditional.addActionListener(this::itmChineseTraditionalActionPerformed);
        languagePopup.add(itmChineseTraditional);

        grpLanguage.add(itmKorean);
        itmKorean.setText("한국어");
        itmKorean.setToolTipText("");
        itmKorean.addActionListener(this::itmKoreanActionPerformed);
        languagePopup.add(itmKorean);

        grpLanguage.add(itmJapanese);
        itmJapanese.setText("日本語");
        itmJapanese.addActionListener(this::itmJapaneseActionPerformed);
        languagePopup.add(itmJapanese);

        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 5, 12, 5));
        setLayout(new java.awt.GridBagLayout());

        btnCallShimeji.setText("Call Shimeji");
        btnCallShimeji.addActionListener(this::btnCallShimejiActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(btnCallShimeji, gridBagConstraints);

        btnFollowCursor.setText("Follow Cursor");
        btnFollowCursor.addActionListener(this::btnFollowCursorActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(btnFollowCursor, gridBagConstraints);

        btnReduceToOne.setText("Reduce to One");
        btnReduceToOne.addActionListener(this::btnReduceToOneActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(btnReduceToOne, gridBagConstraints);

        btnRestoreWindows.setText("Restore Windows");
        btnRestoreWindows.addActionListener(this::btnRestoreWindowsActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(btnRestoreWindows, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(jSeparator1, gridBagConstraints);

        btnAllowedBehaviors.setText("Allowed Behaviours");
        btnAllowedBehaviors.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                btnAllowedBehaviorsMouseReleased(evt);
            }
        });
        btnAllowedBehaviors.addActionListener(this::btnAllowedBehaviorsActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(btnAllowedBehaviors, gridBagConstraints);

        btnChooseShimeji.setText("Choose Shimeji...");
        btnChooseShimeji.addActionListener(this::btnChooseShimejiActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(btnChooseShimeji, gridBagConstraints);

        btnSettings.setText("Settings");
        btnSettings.addActionListener(this::btnSettingsActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(btnSettings, gridBagConstraints);

        btnLanguage.setText("Language");
        btnLanguage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                btnLanguageMouseReleased(evt);
            }
        });
        btnLanguage.addActionListener(this::btnLanguageActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(btnLanguage, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(jSeparator2, gridBagConstraints);

        btnPauseAll.setText("Pause Animations");
        btnPauseAll.addActionListener(this::btnPauseAllActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(btnPauseAll, gridBagConstraints);

        btnDismissAll.setText("Dismiss All");
        btnDismissAll.addActionListener(this::btnDismissAllActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        add(btnDismissAll, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void btnCallShimejiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCallShimejiActionPerformed
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                Main.getInstance().createMascot();
                return null;
            }

            @Override
            protected void done() {
                if (useSystemTray) {
                    return;
                }
                refreshPauseText();
            }
        };
        worker.execute();
        if (useSystemTray) {
            ((Window) getTopLevelAncestor()).dispose();
        }
    }//GEN-LAST:event_btnCallShimejiActionPerformed

    private void btnFollowCursorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFollowCursorActionPerformed
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                Main.getInstance().getManager().setBehaviorAll(UserBehavior.BEHAVIORNAME_CHASEMOUSE);
                return null;
            }

            @Override
            protected void done() {
                if (useSystemTray) {
                    return;
                }
                // Refresh the pause text in case any mascots had to be disposed
                // because the behavior failed to initialize for them
                refreshPauseText();
            }
        };
        worker.execute();
        if (useSystemTray) {
            ((Window) getTopLevelAncestor()).dispose();
        }
    }//GEN-LAST:event_btnFollowCursorActionPerformed

    private void btnReduceToOneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReduceToOneActionPerformed
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                Main.getInstance().getManager().remainOne();
                return null;
            }

            @Override
            protected void done() {
                if (useSystemTray) {
                    return;
                }
                refreshPauseText();
            }
        };
        worker.execute();
        if (useSystemTray) {
            ((Window) getTopLevelAncestor()).dispose();
        }
    }//GEN-LAST:event_btnReduceToOneActionPerformed

    private void btnRestoreWindowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRestoreWindowsActionPerformed
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                NativeFactory.getInstance().getEnvironment().restoreWindows();
                return null;
            }
        };
        worker.execute();
        if (useSystemTray) {
            ((Window) getTopLevelAncestor()).dispose();
        }
    }//GEN-LAST:event_btnRestoreWindowsActionPerformed

    private void behaviorPopupPopupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_behaviorPopupPopupMenuWillBecomeInvisible
        Point mousePosition = btnAllowedBehaviors.getMousePosition();
        if (mousePosition != null) {
            btnAllowedBehaviors.setEnabled(!btnAllowedBehaviors.contains(mousePosition));
        } else {
            btnAllowedBehaviors.setEnabled(true);
        }
    }//GEN-LAST:event_behaviorPopupPopupMenuWillBecomeInvisible

    private void btnAllowedBehaviorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAllowedBehaviorsActionPerformed
        behaviorPopup.show(btnAllowedBehaviors, 0, btnAllowedBehaviors.getHeight());
        btnAllowedBehaviors.requestFocusInWindow();
    }//GEN-LAST:event_btnAllowedBehaviorsActionPerformed

    private void btnAllowedBehaviorsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnAllowedBehaviorsMouseReleased
        btnAllowedBehaviors.setEnabled(true);
    }//GEN-LAST:event_btnAllowedBehaviorsMouseReleased

    private void chkBreedingItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkBreedingItemStateChanged
        Main.getInstance().getSettings().breeding = evt.getStateChange() == ItemEvent.SELECTED;
        btnAllowedBehaviors.setEnabled(true);
    }//GEN-LAST:event_chkBreedingItemStateChanged

    private void chkTransientItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkTransientItemStateChanged
        Main.getInstance().getSettings().transients = evt.getStateChange() == ItemEvent.SELECTED;
        btnAllowedBehaviors.setEnabled(true);
    }//GEN-LAST:event_chkTransientItemStateChanged

    private void chkTransformationItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkTransformationItemStateChanged
        Main.getInstance().getSettings().transformation = evt.getStateChange() == ItemEvent.SELECTED;
        btnAllowedBehaviors.setEnabled(true);
    }//GEN-LAST:event_chkTransformationItemStateChanged

    private void chkThrowingItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkThrowingItemStateChanged
        Main.getInstance().getSettings().throwing = evt.getStateChange() == ItemEvent.SELECTED;
        btnAllowedBehaviors.setEnabled(true);
    }//GEN-LAST:event_chkThrowingItemStateChanged

    private void chkSoundsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkSoundsItemStateChanged
        boolean selected = evt.getStateChange() == ItemEvent.SELECTED;
        Main.getInstance().getSettings().sounds = selected;
        if (!selected)
            Sounds.stopAll();
        btnAllowedBehaviors.setEnabled(true);
    }//GEN-LAST:event_chkSoundsItemStateChanged

    private void chkMultiscreenItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chkMultiscreenItemStateChanged
        Main.getInstance().getSettings().multiscreen = evt.getStateChange() == ItemEvent.SELECTED;
        btnAllowedBehaviors.setEnabled(true);
    }//GEN-LAST:event_chkMultiscreenItemStateChanged

    private void btnChooseShimejiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChooseShimejiActionPerformed
        if (useSystemTray)
            ((Window) getTopLevelAncestor()).dispose();
        // Needed to stop the guys from potentially throwing away the image set chooser window
        Main.getInstance().getManager().setEnabled(false);

        ImageSetChooser chooser = new ImageSetChooser(Main.getFrame(), true);
        chooser.setIconImage(Main.getIcon());
        Collection<String> result = chooser.display();

        if (result != null) {
            /*
             * We're on the Event Dispatch Thread here,
             * so do this on a separate thread to avoid making the UI unresponsive.
             */
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    Main.getInstance().setActiveImageSets(result);
                    return null;
                }

                @Override
                protected void done() {
                    if (useSystemTray) {
                        return;
                    }
                    refreshPauseText();
                }
            };
            worker.execute();
        }

        Main.getInstance().getManager().setEnabled(true);
    }//GEN-LAST:event_btnChooseShimejiActionPerformed

    private void btnSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSettingsActionPerformed
        if (useSystemTray)
            ((Window) getTopLevelAncestor()).dispose();
        Manager manager = Main.getInstance().getManager();

        // Needed to stop the guys from potentially throwing away the settings window
        manager.setEnabled(false);

        SettingsWindow dialog = new SettingsWindow(Main.getFrame(), true);
        dialog.setIconImage(Main.getIcon());
        dialog.init();
        // Only do the following code if the settings window wasn't cancelled
        if (dialog.display()) {
            if (dialog.isTrayMenuReloadRequired()) {
                Main.getInstance().getTrayMenu().createTrayIcon();
            }

            boolean environmentReloadRequired = dialog.isEnvironmentReloadRequired();
            boolean imageReloadRequired = dialog.isImageReloadRequired();
            boolean interactiveWindowReloadRequired = dialog.isInteractiveWindowReloadRequired();

            boolean windowedMode = Main.getInstance().getSettings().windowedMode;
            if (environmentReloadRequired) {
                // Dispose the environment here, regardless of whether it was windowed.
                // It's okay if dispose() doesn't need to be called on the EDT for the current environment,
                // because that method shouldn't take long to return.
                NativeFactory.getInstance().getEnvironment().dispose();
                if (windowedMode) {
                    /*
                     * If in windowed mode, initialize the environment on the EDT before loading any mascots
                     * so the mascots spawn at the correct positions
                     */
                    NativeFactory.resetInstance();
                    NativeFactory.getInstance().getEnvironment().init();
                }
            }

            /*
             * We're on the Event Dispatch Thread here,
             * so do this on a separate thread to avoid making the UI unresponsive.
             */
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    if (environmentReloadRequired && !windowedMode) {
                        /*
                         * If not in windowed mode, initialize the environment on the worker thread
                         * because it's not necessary to do it on the EDT
                         */
                        NativeFactory.resetInstance();
                        NativeFactory.getInstance().getEnvironment().init();
                    }
                    // TODO: Allow images to be reloaded without needing to reload all mascots as well (unless the scaling has changed)
                    if (environmentReloadRequired || imageReloadRequired) {
                        // Reload the image sets if the scaling/opacity settings were changed
                        // or if the environment was reloaded
                        Main.getInstance().reloadAllImageSets();
                    }
                    // Only refresh interactive window cache if the environment wasn't reloaded,
                    // because reloading the environment already refreshes the cache
                    if (interactiveWindowReloadRequired && !environmentReloadRequired) {
                        NativeFactory.getInstance().getEnvironment().refreshCache();
                    }

                    return null;
                }

                @Override
                protected void done() {
                    if (useSystemTray) {
                        return;
                    }
                    refreshPauseText();
                }
            };
            worker.execute();
        }

        manager.setEnabled(true);
    }//GEN-LAST:event_btnSettingsActionPerformed

    private void languagePopupPopupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_languagePopupPopupMenuWillBecomeInvisible
        Point mousePosition = btnLanguage.getMousePosition();
        if (mousePosition != null) {
            btnLanguage.setEnabled(!btnLanguage.contains(mousePosition));
        } else {
            btnLanguage.setEnabled(true);
        }
    }//GEN-LAST:event_languagePopupPopupMenuWillBecomeInvisible

    private void btnLanguageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLanguageActionPerformed
        languagePopup.show(btnLanguage, 0, btnLanguage.getHeight());
        btnLanguage.requestFocusInWindow();
    }//GEN-LAST:event_btnLanguageActionPerformed

    private void btnLanguageMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnLanguageMouseReleased
        btnLanguage.setEnabled(true);
    }//GEN-LAST:event_btnLanguageMouseReleased

    private void itmEnglishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmEnglishActionPerformed
        updateLocale(Locale.UK);
    }//GEN-LAST:event_itmEnglishActionPerformed

    private void itmArabicActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmArabicActionPerformed
        updateLocale(SAUDI_ARABIA);
    }//GEN-LAST:event_itmArabicActionPerformed

    private void itmCatalanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmCatalanActionPerformed
        updateLocale(SPAIN_CATALAN);
    }//GEN-LAST:event_itmCatalanActionPerformed

    private void itmGermanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmGermanActionPerformed
        updateLocale(Locale.GERMANY);
    }//GEN-LAST:event_itmGermanActionPerformed

    private void itmSpanishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmSpanishActionPerformed
        updateLocale(SPAIN);
    }//GEN-LAST:event_itmSpanishActionPerformed

    private void itmFrenchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmFrenchActionPerformed
        updateLocale(Locale.FRANCE);
    }//GEN-LAST:event_itmFrenchActionPerformed

    private void itmCroatianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmCroatianActionPerformed
        updateLocale(CROATIA);
    }//GEN-LAST:event_itmCroatianActionPerformed

    private void itmItalianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmItalianActionPerformed
        updateLocale(Locale.ITALY);
    }//GEN-LAST:event_itmItalianActionPerformed

    private void itmDutchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmDutchActionPerformed
        updateLocale(NETHERLANDS);
    }//GEN-LAST:event_itmDutchActionPerformed

    private void itmPolishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmPolishActionPerformed
        updateLocale(POLAND);
    }//GEN-LAST:event_itmPolishActionPerformed

    private void itmBrazilianPortugueseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmBrazilianPortugueseActionPerformed
        updateLocale(BRAZILIAN_PORTUGUESE);
    }//GEN-LAST:event_itmBrazilianPortugueseActionPerformed

    private void itmPortugueseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmPortugueseActionPerformed
        updateLocale(PORTUGAL);
    }//GEN-LAST:event_itmPortugueseActionPerformed

    private void itmRussianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmRussianActionPerformed
        updateLocale(RUSSIA);
    }//GEN-LAST:event_itmRussianActionPerformed

    private void itmRomanianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmRomanianActionPerformed
        updateLocale(ROMANIA);
    }//GEN-LAST:event_itmRomanianActionPerformed

    private void itmSerbianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmSerbianActionPerformed
        updateLocale(SERBIA);
    }//GEN-LAST:event_itmSerbianActionPerformed

    private void itmFinnishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmFinnishActionPerformed
        updateLocale(FINLAND);
    }//GEN-LAST:event_itmFinnishActionPerformed

    private void itmVietnameseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmVietnameseActionPerformed
        updateLocale(VIETNAM);
    }//GEN-LAST:event_itmVietnameseActionPerformed

    private void itmChineseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmChineseActionPerformed
        updateLocale(Locale.SIMPLIFIED_CHINESE);
    }//GEN-LAST:event_itmChineseActionPerformed

    private void itmChineseTraditionalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmChineseTraditionalActionPerformed
        updateLocale(Locale.TRADITIONAL_CHINESE);
    }//GEN-LAST:event_itmChineseTraditionalActionPerformed

    private void itmKoreanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmKoreanActionPerformed
        updateLocale(Locale.KOREA);
    }//GEN-LAST:event_itmKoreanActionPerformed

    private void itmJapaneseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itmJapaneseActionPerformed
        updateLocale(Locale.JAPAN);
    }//GEN-LAST:event_itmJapaneseActionPerformed

    private void btnPauseAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPauseAllActionPerformed
        if (useSystemTray) {
            ((Window) getTopLevelAncestor()).dispose();
        }
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                Main.getInstance().getManager().togglePauseAll();
                return null;
            }

            @Override
            protected void done() {
                if (useSystemTray) {
                    return;
                }
                refreshPauseText();
            }
        };
        worker.execute();
    }//GEN-LAST:event_btnPauseAllActionPerformed

    private void btnDismissAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDismissAllActionPerformed
        Main.getInstance().exit();
    }//GEN-LAST:event_btnDismissAllActionPerformed


    private void updateLocale(Locale locale) {
        // If we are using the system tray, dispose the tray menu window
        // because it should only persist when the system tray setting is disabled
        if (useSystemTray)
            ((Window) getTopLevelAncestor()).dispose();

        Settings settings = Main.getInstance().getSettings();
        if (!settings.language.equals(locale)) {
            settings.language = locale;
            // Load the resource bundle for the specified language
            Main.getInstance().loadLanguage(locale);

            // Update all LanguageUpdatable windows using the new language bundle
            ResourceBundle languageBundle = Main.getInstance().getLanguageBundle();
            // TODO: Update the component orientations to fit the new locale
            for (Window window : Window.getWindows()) {
                window.invalidate();
                if (window instanceof Localizable lu) {
                    lu.localize(languageBundle);
                }
            }
            // Only update the tray menu's text if it's persistent (i.e., the Use System Tray setting is disabled)
            if (!useSystemTray) {
                // Needs to be called separately for this class because this class isn't a subclass of Window
                localize(languageBundle);
                // Recalculate the size of the window, because the buttons may be different sizes than before
                repackWindow();
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu behaviorPopup;
    private javax.swing.JButton btnAllowedBehaviors;
    private javax.swing.JButton btnCallShimeji;
    private javax.swing.JButton btnChooseShimeji;
    private javax.swing.JButton btnDismissAll;
    private javax.swing.JButton btnFollowCursor;
    private javax.swing.JButton btnLanguage;
    private javax.swing.JButton btnPauseAll;
    private javax.swing.JButton btnReduceToOne;
    private javax.swing.JButton btnRestoreWindows;
    private javax.swing.JButton btnSettings;
    private javax.swing.JCheckBoxMenuItem chkBreeding;
    private javax.swing.JCheckBoxMenuItem chkMultiscreen;
    private javax.swing.JCheckBoxMenuItem chkSounds;
    private javax.swing.JCheckBoxMenuItem chkThrowing;
    private javax.swing.JCheckBoxMenuItem chkTransformation;
    private javax.swing.JCheckBoxMenuItem chkTransient;
    private javax.swing.ButtonGroup grpLanguage;
    private javax.swing.JCheckBoxMenuItem itmArabic;
    private javax.swing.JCheckBoxMenuItem itmBrazilianPortuguese;
    private javax.swing.JCheckBoxMenuItem itmCatalan;
    private javax.swing.JCheckBoxMenuItem itmChinese;
    private javax.swing.JCheckBoxMenuItem itmChineseTraditional;
    private javax.swing.JCheckBoxMenuItem itmCroatian;
    private javax.swing.JCheckBoxMenuItem itmDutch;
    private javax.swing.JCheckBoxMenuItem itmEnglish;
    private javax.swing.JCheckBoxMenuItem itmFinnish;
    private javax.swing.JCheckBoxMenuItem itmFrench;
    private javax.swing.JCheckBoxMenuItem itmGerman;
    private javax.swing.JCheckBoxMenuItem itmItalian;
    private javax.swing.JCheckBoxMenuItem itmJapanese;
    private javax.swing.JCheckBoxMenuItem itmKorean;
    private javax.swing.JCheckBoxMenuItem itmPolish;
    private javax.swing.JCheckBoxMenuItem itmPortuguese;
    private javax.swing.JCheckBoxMenuItem itmRomanian;
    private javax.swing.JCheckBoxMenuItem itmRussian;
    private javax.swing.JCheckBoxMenuItem itmSerbian;
    private javax.swing.JCheckBoxMenuItem itmSpanish;
    private javax.swing.JCheckBoxMenuItem itmVietnamese;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu languagePopup;
    // End of variables declaration//GEN-END:variables

}
