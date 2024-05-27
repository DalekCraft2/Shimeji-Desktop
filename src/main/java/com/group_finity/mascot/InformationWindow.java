/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.group_finity.mascot;

import com.group_finity.mascot.config.Configuration;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kilkakon
 * @since 1.0.21
 */
public class InformationWindow extends JFrame {
    private static final Logger log = Logger.getLogger(InformationWindow.class.getName());
    private String imageSet;

    /**
     * Creates new form InformationWindow
     */
    public InformationWindow() {
        initComponents();
    }

    public void init(final String imageSet, final Configuration config) {
        // initialise controls
        setLocationRelativeTo(null);
        this.imageSet = imageSet;

        // load image
        if (config.containsInformationKey("SplashImage")) {
            Path splashImagePath = Main.IMAGE_DIRECTORY.resolve(imageSet).resolve(config.getInformation("SplashImage"));
            if (Files.exists(splashImagePath)) {
                Icon icon = new ImageIcon(splashImagePath.toString());
                lblSplashImage.setIcon(icon);
            }
        }

        // text
        final ResourceBundle language = Main.getInstance().getLanguageBundle();
        setTitle(config.containsInformationKey("Name") ? config.getInformation("Name") : language.getString("Information"));

        StringBuilder html = new StringBuilder("<center style=\"font:");
        if (lblSplashImage.getFont().getStyle() == Font.BOLD) {
            html.append("bold ");
        }
        if (lblSplashImage.getFont().getStyle() == Font.ITALIC) {
            html.append("italic ");
        }
        if (lblSplashImage.getFont().getStyle() == Font.BOLD + Font.ITALIC) {
            html.append("italic bold ");
        }
        html.append(lblSplashImage.getFont().getSize());
        html.append("pt ");
        html.append(lblSplashImage.getFont().getFontName());
        html.append("\">");
        if (config.containsInformationKey("ArtistName")) {
            html.append(language.getString("ArtBy"));
            html.append(" ");
            if (config.containsInformationKey("ArtistURL")) {
                html.append("<a href=\"");
                html.append(config.getInformation("ArtistURL"));
                html.append("\">");
            }
            html.append(config.getInformation("ArtistName"));
            if (config.containsInformationKey("ArtistURL")) {
                html.append("</a>");
            }
        }
        if (config.containsInformationKey("ScripterName")) {
            if (config.containsInformationKey("ArtistName")) {
                html.append(" - ");
            }
            html.append(language.getString("ScriptedBy"));
            html.append(" ");
            if (config.containsInformationKey("ScripterURL")) {
                html.append("<a href=\"");
                html.append(config.getInformation("ScripterURL"));
                html.append("\">");
            }
            html.append(config.getInformation("ScripterName"));
            if (config.containsInformationKey("ScripterURL")) {
                html.append("</a>");
            }
        }
        if (config.containsInformationKey("CommissionerName")) {
            if (config.containsInformationKey("ArtistName") || config.containsInformationKey("ScripterName")) {
                html.append(" - ");
            }
            html.append(language.getString("CommissionedBy"));
            html.append(" ");
            if (config.containsInformationKey("CommissionerURL")) {
                html.append("<a href=\"");
                html.append(config.getInformation("CommissionerURL"));
                html.append("\">");
            }
            html.append(config.getInformation("CommissionerName"));
            if (config.containsInformationKey("CommissionerURL")) {
                html.append("</a>");
            }
        }
        if (config.containsInformationKey("SupportName")) {
            if (config.containsInformationKey("ArtistName") || config.containsInformationKey("ScripterName") || config.containsInformationKey("CommissionerName")) {
                html.append(" - ");
            }
            html.append(language.getString("SupportAt"));
            html.append(" ");
            if (config.containsInformationKey("SupportURL")) {
                html.append("<a href=\"");
                html.append(config.getInformation("SupportURL"));
                html.append("\">");
            }
            html.append(config.getInformation("SupportName"));
            if (config.containsInformationKey("SupportURL")) {
                html.append("</a>");
            }
        }
        html.append("</center>");

        pnlEditorPane.setText(html.toString());
        pnlEditorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                StringTokenizer st = new StringTokenizer(e.getDescription(), " ");
                if (st.hasMoreTokens()) {
                    String url = st.nextToken();
                    if (JOptionPane.showConfirmDialog(
                            this,
                            language.getString("ConfirmVisitWebsiteMessage") + "\n" + language.getString("ExerciseCautionAndBewareSusLinksMessage") + "\n" + url,
                            language.getString("VisitWebsite"),
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
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
                        } catch (IOException | UnsupportedOperationException | URISyntaxException ex) {
                            log.log(Level.SEVERE, "Failed to open URL \"" + url + "\"", ex);
                            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
        btnClose.setText(language.getString("Close"));
    }

    public boolean display() {
        float menuScaling = Float.parseFloat(Main.getInstance().getProperties().getProperty("MenuDPI", "96")) / 96;
        pnlEditorPane.setBackground(getBackground());
        pnlEditorPane.setBorder(null);
        pnlScrollPane.setBorder(null);
        pnlScrollPane.setViewportBorder(null);

        // scale controls to fit
        lblSplashImage.setPreferredSize(new Dimension((int) (lblSplashImage.getIcon().getIconWidth() * menuScaling), (int) (lblSplashImage.getIcon().getIconHeight() * menuScaling)));
        pnlEditorPane.setPreferredSize(new Dimension((int) (pnlEditorPane.getPreferredSize().width * menuScaling), (int) (pnlEditorPane.getPreferredSize().height * menuScaling)));
        pnlScrollPane.setPreferredSize(new Dimension((int) (pnlScrollPane.getPreferredSize().width * menuScaling), (int) (pnlScrollPane.getPreferredSize().height * menuScaling)));
        btnClose.setPreferredSize(new Dimension((int) (btnClose.getPreferredSize().width * menuScaling), (int) (btnClose.getPreferredSize().height * menuScaling)));
        pnlFooter.setPreferredSize(new Dimension(pnlFooter.getPreferredSize().width, btnClose.getPreferredSize().height + 6));
        pack();
        setVisible(true);

        return true;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblSplashImage = new JLabel();
        pnlScrollPane = new JScrollPane();
        pnlEditorPane = new JEditorPane();
        pnlFooter = new JPanel();
        btnClose = new JButton();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        pnlEditorPane.setEditable(false);
        pnlEditorPane.setBorder(null);
        pnlEditorPane.setContentType("text/html"); // NOI18N
        pnlEditorPane.setText("");
        pnlScrollPane.setViewportView(pnlEditorPane);

        pnlFooter.setPreferredSize(new Dimension(380, 36));
        pnlFooter.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));

        btnClose.setText("Close");
        btnClose.setMaximumSize(new Dimension(130, 26));
        btnClose.setMinimumSize(new Dimension(95, 23));
        btnClose.setName(""); // NOI18N
        btnClose.setPreferredSize(new Dimension(130, 26));
        btnClose.addActionListener(this::btnCloseActionPerformed);
        pnlFooter.add(btnClose);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(pnlFooter, GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE)
                                        .addComponent(pnlScrollPane)
                                        .addComponent(lblSplashImage, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblSplashImage, GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pnlScrollPane, GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pnlFooter, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(ActionEvent evt)// GEN-FIRST:event_btnCloseActionPerformed
    {// GEN-HEADEREND:event_btnCloseActionPerformed
        dispose();
    }// GEN-LAST:event_btnCloseActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException ex) {
            log.log(Level.SEVERE, "Failed to set Look & Feel", ex);
        }
        //</editor-fold>

        /* Create and display the form */
        EventQueue.invokeLater(() -> {
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnClose;
    private JLabel lblSplashImage;
    private JEditorPane pnlEditorPane;
    private JPanel pnlFooter;
    private JScrollPane pnlScrollPane;
    // End of variables declaration//GEN-END:variables
}
