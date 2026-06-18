/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.group_finity.mascot;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Contributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 * @author Kilkakon
 * @since 1.0.21
 */
public class InformationWindow extends JFrame implements Localizable {
    private static final Logger log = LoggerFactory.getLogger(InformationWindow.class);

    // Store these in instance variables so we can reuse them if we need to relocalize the window
    private String displayName;
    private String artistHtml;
    private String scripterHtml;
    private String commissionerHtml;
    private String supportHtml;

    /**
     * Creates new form InformationWindow
     */
    public InformationWindow() {
        initComponents();
    }

    public void init(final String imageSet, final Configuration config) {
        // load image
        if (config.getSplashImagePath() != null) {
            Path splashImagePath = Main.IMAGE_DIRECTORY.resolve(imageSet).resolve(config.getSplashImagePath());
            if (Files.isRegularFile(splashImagePath)) {
                Icon icon = new ImageIcon(splashImagePath.toString());
                lblSplashImage.setIcon(icon);
            }
        }

        displayName = config.getDisplayName();

        Map<Contributor.Type, Contributor> contributors = config.getContributors();

        Contributor artist = contributors.get(Contributor.Type.ARTIST);
        if (artist != null) {
            artistHtml = "";
            if (artist.url() != null) {
                artistHtml += "<a href=\"";
                artistHtml += artist.url();
                artistHtml += "\">";
            }
            artistHtml += artist.name();
            if (artist.url() != null) {
                artistHtml += "</a>";
            }
        }

        Contributor scripter = contributors.get(Contributor.Type.SCRIPTER);
        if (scripter != null) {
            scripterHtml = "";
            if (scripter.url() != null) {
                scripterHtml += "<a href=\"";
                scripterHtml += scripter.url();
                scripterHtml += "\">";
            }
            scripterHtml += scripter.name();
            if (scripter.url() != null) {
                scripterHtml += "</a>";
            }
        }

        Contributor commissioner = contributors.get(Contributor.Type.COMMISSIONER);
        if (commissioner != null) {
            commissionerHtml = "";
            if (commissioner.url() != null) {
                commissionerHtml += "<a href=\"";
                commissionerHtml += commissioner.url();
                commissionerHtml += "\">";
            }
            commissionerHtml += commissioner.name();
            if (commissioner.url() != null) {
                commissionerHtml += "</a>";
            }
        }

        Contributor support = contributors.get(Contributor.Type.SUPPORT);
        if (support != null) {
            supportHtml = "";
            if (support.url() != null) {
                supportHtml += "<a href=\"";
                supportHtml += support.url();
                supportHtml += "\">";
            }
            supportHtml += support.name();
            if (support.url() != null) {
                supportHtml += "</a>";
            }
        }

        // Localize
        localize(Main.getInstance().getLanguageBundle());

        pnlEditorPane.addHyperlinkListener(e -> {
            final ResourceBundle languageBundle = Main.getInstance().getLanguageBundle();
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                StringTokenizer st = new StringTokenizer(e.getDescription(), " ");
                if (st.hasMoreTokens()) {
                    String url = st.nextToken();
                    if (JOptionPane.showConfirmDialog(
                            this,
                            languageBundle.getString("ConfirmVisitWebsiteMessage") + System.lineSeparator() +
                                    languageBundle.getString("ExerciseCautionAndBewareSusLinksMessage") + System.lineSeparator() + url,
                            languageBundle.getString("VisitWebsite"),
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
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
                                Main.showError(this, String.format(languageBundle.getString("FailedOpenWebBrowserErrorMessage"), url));
                            }
                        } catch (UnsupportedOperationException | URISyntaxException | IOException ex) {
                            log.error("Failed to open URL \"{}\"", url, ex);
                            Main.showError(this, String.format(languageBundle.getString("FailedOpenWebBrowserErrorMessage"), url), ex);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void localize(ResourceBundle languageBundle) {
        setTitle(displayName == null ? languageBundle.getString("Information") : displayName);

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
        if (artistHtml != null) {
            html.append(String.format(languageBundle.getString("ArtBy"), artistHtml));
        }
        if (scripterHtml != null) {
            if (artistHtml != null) {
                html.append(" - ");
            }
            html.append(String.format(languageBundle.getString("ScriptedBy"), scripterHtml));
        }
        if (commissionerHtml != null) {
            if (artistHtml != null || scripterHtml != null) {
                html.append(" - ");
            }
            html.append(String.format(languageBundle.getString("CommissionedBy"), commissionerHtml));
        }
        if (supportHtml != null) {
            if (artistHtml != null || scripterHtml != null || commissionerHtml != null) {
                html.append(" - ");
            }
            html.append(String.format(languageBundle.getString("SupportAt"), supportHtml));
        }
        html.append("</center>");

        pnlEditorPane.setText(html.toString());

        btnClose.setText(languageBundle.getString("Close"));
    }

    public void display() {
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlImage = new javax.swing.JPanel();
        lblSplashImage = new javax.swing.JLabel();
        pnlScrollPane = new javax.swing.JScrollPane();
        pnlEditorPane = new javax.swing.JEditorPane();
        pnlFooter = new javax.swing.JPanel();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Information");

        pnlImage.setLayout(new javax.swing.BoxLayout(pnlImage, javax.swing.BoxLayout.PAGE_AXIS));

        lblSplashImage.setAlignmentX(0.5F);
        pnlImage.add(lblSplashImage);

        pnlScrollPane.setBorder(null);

        pnlEditorPane.setEditable(false);
        pnlEditorPane.setBackground(getBackground());
        pnlEditorPane.setBorder(null);
        pnlEditorPane.setContentType("text/html"); // NOI18N
        pnlEditorPane.setText("");
        pnlScrollPane.setViewportView(pnlEditorPane);

        pnlFooter.setPreferredSize(new java.awt.Dimension(380, 36));
        pnlFooter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

        btnClose.setText("Close");
        btnClose.setMaximumSize(new java.awt.Dimension(130, 26));
        btnClose.setMinimumSize(new java.awt.Dimension(95, 23));
        btnClose.setName(""); // NOI18N
        btnClose.setPreferredSize(new java.awt.Dimension(130, 26));
        btnClose.addActionListener(this::btnCloseActionPerformed);
        pnlFooter.add(btnClose);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pnlImage, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                    .addComponent(pnlFooter, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlScrollPane, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlImage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlFooter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        dispose();
    }//GEN-LAST:event_btnCloseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JLabel lblSplashImage;
    private javax.swing.JEditorPane pnlEditorPane;
    private javax.swing.JPanel pnlFooter;
    private javax.swing.JPanel pnlImage;
    private javax.swing.JScrollPane pnlScrollPane;
    // End of variables declaration//GEN-END:variables
}
