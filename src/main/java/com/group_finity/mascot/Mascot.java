package com.group_finity.mascot;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.hotspot.Hotspot;
import com.group_finity.mascot.image.MascotImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.group_finity.mascot.menu.MenuScroller;
import com.group_finity.mascot.sound.Sounds;

import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mascot object.
 * <p>
 * Mascots move using {@link Behavior}, which represents long-term and complex behavior,
 * and {@link Action}, which represents short-term and monotonous movements.
 * <p>
 * Mascots have an internal timer and call {@link Action} at regular intervals.
 * {@link Action} animates the mascot by calling {@link Animation}.
 * <p>
 * When {@link Action} ends or at other specific times, {@link Behavior} is called and moves to the next {@link Action}.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Mascot {
    /**
     * Whether to draw the mascots' bounds and hotspots, for debugging purposes.
     * <p>
     * Currently, on Windows, this will only work when rendering with AWT instead of JNA.
     */
    public static final boolean DRAW_DEBUG = false;

    private static final Logger log = Logger.getLogger(Mascot.class.getName());

    /**
     * The ID of the last generated {@code Mascot}.
     */
    private static AtomicInteger lastId = new AtomicInteger();

    /**
     * The {@code Mascot}'s ID.
     * Exists only to make it easier to view debug logs.
     */
    private final int id;

    private String imageSet;
    /**
     * The window that displays the {@code Mascot}.
     */
    private final TranslucentWindow window = NativeFactory.getInstance().newTransparentWindow();

    /**
     * The {@link Manager} that manages this {@code Mascot}.
     */
    private Manager manager = null;

    /**
     * The {@code Mascot}'s ground coordinates.
     * For example, its feet or its hands when hanging.
     */
    private Point anchor = new Point(0, 0);

    /**
     * The image to display.
     */
    private MascotImage image = null;

    /**
     * Whether the {@code Mascot} is facing right.
     * The original image is treated as facing left, so setting this to {@code true} will cause it to be reversed.
     */
    private boolean lookRight = false;

    /**
     * An object that represents the long-term behavior of this {@code Mascot}.
     */
    private Behavior behavior = null;

    /**
     * Time that increases every tick of the timer.
     */
    private int time = 0;

    /**
     * Whether the animation is running.
     */
    private boolean animating = true;

    private boolean paused = false;

    /**
     * Set by behaviours when the {@code Mascot} is being dragged by the mouse cursor,
     * as opposed to hotspots or the like.
     */
    private boolean dragging = false;

    /**
     * Mascot display environment.
     */
    private MascotEnvironment environment = new MascotEnvironment(this);

    private String sound = null;

    protected DebugWindow debugWindow = null;

    private final List<String> affordances = new ArrayList<>(5);

    private final List<Hotspot> hotspots = new ArrayList<>(5);

    /**
     * Set by behaviours when the user has triggered a hotspot on this {@code Mascot},
     * so that the {@code Mascot} knows to check for any new hotspots that emerge while
     * the mouse is held down.
     */
    private Point cursor = null;

    public Mascot(final String imageSet) {
        id = lastId.incrementAndGet();
        this.imageSet = imageSet;

        log.log(Level.INFO, "Created mascot \"{0}\" with image set \"{1}\"", new Object[]{this, imageSet});

        // Always show on top
        getWindow().setAlwaysOnTop(true);

        // Register the mouse handler
        getWindow().asComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                Mascot.this.mousePressed(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                Mascot.this.mouseReleased(e);
            }
        });
        getWindow().asComponent().addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                if (paused) {
                    refreshCursor(false);
                } else {
                    if (isHotspotClicked()) {
                        setCursorPosition(e.getPoint());
                    } else {
                        refreshCursor(e.getPoint());
                    }
                }
            }

            @Override
            public void mouseDragged(final MouseEvent e) {
                if (paused) {
                    refreshCursor(false);
                } else {
                    if (isHotspotClicked()) {
                        setCursorPosition(e.getPoint());
                    } else {
                        refreshCursor(e.getPoint());
                    }
                }
            }
        });

        if (DRAW_DEBUG) {
            // For drawing the outlines of hotspots and the mascot's bounds, for debugging purposes
            JComponent debugComp = new JComponent() {
                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    // Draw hotspots
                    g.setColor(Color.BLUE);
                    Dimension imageSize = getImage().getSize();
                    for (Hotspot hotspot : getHotspots()) {
                        Shape shape = hotspot.getShape();
                        if (shape instanceof Rectangle) {
                            Rectangle rectangle = (Rectangle) shape;
                            int x = lookRight ? imageSize.width - rectangle.x - rectangle.width : rectangle.x;
                            g.drawRect(x, rectangle.y, rectangle.width, rectangle.height);
                        } else if (shape instanceof Ellipse2D) {
                            Ellipse2D ellipse = (Ellipse2D) shape;
                            double x = lookRight ? imageSize.width - ellipse.getX() - ellipse.getWidth() : ellipse.getX();
                            g.drawOval((int) x, (int) ellipse.getY(), (int) ellipse.getWidth(), (int) ellipse.getHeight());
                        }
                    }

                    // Draw bounds
                    g.setColor(Color.RED);
                    Rectangle bounds = getBounds();
                    g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

                    // Draw image anchor
                    g.setColor(Color.GREEN);
                    Point imageAnchor = getImage().getCenter();
                    // Because the image anchor is a single point, it is drawn as a circle and several lines for visibility
                    g.drawOval(imageAnchor.x - 5, imageAnchor.y - 5, 10, 10);
                    g.drawLine(imageAnchor.x - 10, imageAnchor.y, imageAnchor.x + 10, imageAnchor.y);
                    g.drawLine(imageAnchor.x, imageAnchor.y - 10, imageAnchor.x, imageAnchor.y + 10);
                    g.drawLine(imageAnchor.x - 10, imageAnchor.y - 10, imageAnchor.x + 10, imageAnchor.y + 10);
                    g.drawLine(imageAnchor.x - 10, imageAnchor.y + 10, imageAnchor.x + 10, imageAnchor.y - 10);
                }
            };
            debugComp.setBackground(new Color(0, 0, 0, 0));
            debugComp.setOpaque(false);
            debugComp.setPreferredSize(getWindow().asComponent().getPreferredSize());
            getWindow().asComponent().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    debugComp.setPreferredSize(e.getComponent().getPreferredSize());
                }
            });
            ((Container) getWindow().asComponent()).add(debugComp);
        }
    }

    @Override
    public String toString() {
        return "mascot" + id;
    }

    private void mousePressed(final MouseEvent event) {
        // Check for popup triggers in both mousePressed and mouseReleased
        // because it works differently on different systems
        if (event.isPopupTrigger()) {
            SwingUtilities.invokeLater(() -> showPopup(event.getX(), event.getY()));
        } else {
            // Switch to drag animation when mouse is pressed
            if (!isPaused() && getBehavior() != null) {
                try {
                    getBehavior().mousePressed(event);
                } catch (final CantBeAliveException e) {
                    log.log(Level.SEVERE, "Severe error in mouse press handler for mascot \"" + this + "\"", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("SevereShimejiErrorErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                    dispose();
                }
            }
        }
    }

    private void mouseReleased(final MouseEvent event) {
        // Check for popup triggers in both mousePressed and mouseReleased
        // because it works differently on different systems
        if (event.isPopupTrigger()) {
            SwingUtilities.invokeLater(() -> showPopup(event.getX(), event.getY()));
        } else {
            if (!isPaused() && getBehavior() != null) {
                try {
                    getBehavior().mouseReleased(event);
                } catch (final CantBeAliveException e) {
                    log.log(Level.SEVERE, "Severe error in mouse release handler for mascot \"" + this + "\"", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("SevereShimejiErrorErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                    dispose();
                }
            }
        }
    }

    private void showPopup(final int x, final int y) {
        final JPopupMenu popup = new JPopupMenu();
        final ResourceBundle languageBundle = Main.getInstance().getLanguageBundle();

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                setAnimating(true);
            }

            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                setAnimating(false);
            }
        });

        // "Another One!" menu item
        final JMenuItem increaseMenu = new JMenuItem(languageBundle.getString("CallAnother"));
        increaseMenu.addActionListener(event -> Main.getInstance().createMascot(imageSet));

        // "Bye Bye!" menu item
        final JMenuItem disposeMenu = new JMenuItem(languageBundle.getString("Dismiss"));
        disposeMenu.addActionListener(e -> dispose());

        // "Follow Mouse!" menu item
        final JMenuItem gatherMenu = new JMenuItem(languageBundle.getString("FollowCursor"));
        gatherMenu.addActionListener(event -> getManager().setBehaviorAll(Main.getInstance().getConfiguration(imageSet), Main.BEHAVIOR_GATHER, imageSet));

        // "Reduce to One!" menu item
        final JMenuItem oneMenu = new JMenuItem(languageBundle.getString("DismissOthers"));
        oneMenu.addActionListener(event -> getManager().remainOne(imageSet, this));

        // "Reduce to One!" menu item
        final JMenuItem onlyOneMenu = new JMenuItem(languageBundle.getString("DismissAllOthers"));
        onlyOneMenu.addActionListener(event -> getManager().remainOne(this));

        // "Restore IE!" menu item
        final JMenuItem restoreMenu = new JMenuItem(languageBundle.getString("RestoreWindows"));
        restoreMenu.addActionListener(event -> NativeFactory.getInstance().getEnvironment().restoreIE());

        // Debug menu item
        final JMenuItem debugMenu = new JMenuItem(languageBundle.getString("RevealStatistics"));
        debugMenu.addActionListener(event -> {
            if (debugWindow == null) {
                debugWindow = new DebugWindow();
            }
            debugWindow.setVisible(true);
        });

        // "Bye Everyone!" menu item
        final JMenuItem closeMenu = new JMenuItem(languageBundle.getString("DismissAll"));
        closeMenu.addActionListener(e -> Main.getInstance().exit());

        // "Paused" Menu item
        final JMenuItem pauseMenu = new JMenuItem(isAnimating() ? languageBundle.getString("PauseAnimations") : languageBundle.getString("ResumeAnimations"));
        pauseMenu.addActionListener(event -> setPaused(!isPaused()));

        // Add the Behaviors submenu. It is currently slightly buggy; sometimes the menu ghosts.
        // JLongMenu submenu = new JLongMenu(languageBundle.getString("SetBehaviour"), 30);
        JMenu submenu = new JMenu(languageBundle.getString("SetBehaviour"));
        JMenu allowedSubmenu = new JMenu(languageBundle.getString("AllowedBehaviours"));
        submenu.setAutoscrolls(true);
        JMenuItem item;
        JCheckBoxMenuItem toggleItem;
        final Configuration config = Main.getInstance().getConfiguration(getImageSet());
        for (String behaviorName : config.getBehaviorNames()) {
            final String command = behaviorName;
            try {
                if (!config.isBehaviorHidden(command)) {
                    String caption = behaviorName.replaceAll("([a-z])(IE)?([A-Z])", "$1 $2 $3").replaceAll(" {2}", " ");
                    if (config.isBehaviorEnabled(command, this) && !command.contains("/")) {
                        item = new JMenuItem(languageBundle.containsKey(behaviorName) ?
                                languageBundle.getString(behaviorName) :
                                caption);
                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(final ActionEvent e) {
                                try {
                                    setBehavior(config.buildBehavior(command));
                                } catch (BehaviorInstantiationException | CantBeAliveException ex) {
                                    // TODO Determine whether this catch block is supposed to dispose of the mascot
                                    log.log(Level.SEVERE, "Failed to set behavior to \"" + command + "\" for mascot \"" + this + "\"", ex);
                                    Main.showError(languageBundle.getString("CouldNotSetBehaviourErrorMessage") + "\n" + ex.getMessage() + "\n" + languageBundle.getString("SeeLogForDetails"));
                                }
                            }
                        });
                        submenu.add(item);
                    }
                    if (config.isBehaviorToggleable(command) && !command.contains("/")) {
                        toggleItem = new JCheckBoxMenuItem(caption, config.isBehaviorEnabled(command, this));
                        toggleItem.addItemListener(e -> Main.getInstance().setMascotBehaviorEnabled(command, this, !config.isBehaviorEnabled(command, this)));
                        allowedSubmenu.add(toggleItem);
                    }
                }
            } catch (RuntimeException e) {
                // just skip if something goes wrong
            }
        }
        // Create the MenuScroller after adding all the items to the submenu, so it is positioned correctly when first shown.
        MenuScroller.setScrollerFor(submenu, 30, 125);
        MenuScroller.setScrollerFor(allowedSubmenu, 30, 125);

        popup.add(increaseMenu);
        popup.addSeparator();
        popup.add(gatherMenu);
        popup.add(restoreMenu);
        popup.add(debugMenu);
        popup.addSeparator();
        if (submenu.getMenuComponentCount() > 0) {
            popup.add(submenu);
        }
        if (allowedSubmenu.getMenuComponentCount() > 0) {
            popup.add(allowedSubmenu);
        }
        // Only add a second separator if either menu has a component count greater than 0. Just in case!
        if (submenu.getMenuComponentCount() > 0 || allowedSubmenu.getMenuComponentCount() > 0) {
            popup.addSeparator();
        }
        popup.add(pauseMenu);
        popup.addSeparator();
        popup.add(disposeMenu);
        popup.add(oneMenu);
        popup.add(onlyOneMenu);
        popup.add(closeMenu);

        // TODO Get the popup to close when clicking outside of it
        getWindow().asComponent().requestFocus();

        // Lightweight popups expect the shimeji window to draw them if they fall inside the shimeji window's boundary.
        // As the shimeji window can't support this, we need to set them to heavyweight.
        popup.setLightWeightPopupEnabled(false);
        popup.show(getWindow().asComponent(), x, y);
    }

    void tick() {
        if (isAnimating()) {
            if (getBehavior() != null) {
                try {
                    getBehavior().next();
                } catch (final CantBeAliveException e) {
                    log.log(Level.SEVERE, "Could not get next behavior for mascot \"" + this + "\"", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("CouldNotGetNextBehaviourErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                    dispose();
                }

                setTime(getTime() + 1);
            }

            if (debugWindow != null) {
                // This sets the title of the actual debug window--not the "Window Title" field--to the mascot's ID
                // Unfortunately, doing this makes it possible to select it as the activeIE because it no longer has an empty name, so I have commented it out
                // debugWindow.setTitle(toString());

                debugWindow.setBehaviour(behavior.toString().substring(9, behavior.toString().length() - 1).replaceAll("([a-z])(IE)?([A-Z])", "$1 $2 $3").replaceAll(" {2}", " "));
                debugWindow.setShimejiX(anchor.x);
                debugWindow.setShimejiY(anchor.y);

                Area activeWindow = environment.getActiveIE();
                debugWindow.setWindowTitle(environment.getActiveIETitle());
                debugWindow.setWindowX(activeWindow.getLeft());
                debugWindow.setWindowY(activeWindow.getTop());
                debugWindow.setWindowWidth(activeWindow.getWidth());
                debugWindow.setWindowHeight(activeWindow.getHeight());

                Area workArea = environment.getWorkArea();
                debugWindow.setEnvironmentX(workArea.getLeft());
                debugWindow.setEnvironmentY(workArea.getTop());
                debugWindow.setEnvironmentWidth(workArea.getWidth());
                debugWindow.setEnvironmentHeight(workArea.getHeight());
            }
        }
    }

    public void apply() {
        if (isAnimating()) {
            // Make sure there's an image
            if (getImage() != null) {
                // Set the window region
                getWindow().asComponent().setBounds(getBounds());

                // Set the image
                getWindow().setImage(getImage().getImage());

                // Display
                if (!getWindow().asComponent().isVisible()) {
                    getWindow().asComponent().setVisible(true);
                }

                // Redraw
                getWindow().updateImage();
            } else {
                if (getWindow().asComponent().isVisible()) {
                    getWindow().asComponent().setVisible(false);
                }
            }

            // play sound if requested
            if (!Sounds.isMuted() && sound != null && Sounds.contains(sound)) {
                synchronized (log) {
                    Clip clip = Sounds.getSound(sound);
                    if (!clip.isRunning()) {
                        clip.stop();
                        clip.setMicrosecondPosition(0);
                        clip.start();
                    }
                }
            }
        }
    }

    public void dispose() {
        log.log(Level.INFO, "Destroying mascot \"{0}\"", this);

        if (debugWindow != null) {
            debugWindow.setVisible(false);
            debugWindow = null;
        }

        animating = false;
        getWindow().dispose();
        if (getManager() != null) {
            getManager().remove(this);
        }
    }

    private void refreshCursor(Point position) {
        synchronized (getHotspots()) {
            boolean useHand = hotspots.stream().anyMatch(hotspot -> hotspot.contains(this, position) &&
                    Main.getInstance().getConfiguration(imageSet).isBehaviorEnabled(hotspot.getBehaviour(), this));

            refreshCursor(useHand);
        }
    }

    private void refreshCursor(Boolean useHand) {
        getWindow().asComponent().setCursor(Cursor.getPredefinedCursor(useHand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(final Manager manager) {
        this.manager = manager;
    }

    public Point getAnchor() {
        return anchor;
    }

    public void setAnchor(Point anchor) {
        this.anchor = anchor;
    }

    public MascotImage getImage() {
        return image;
    }

    public void setImage(final MascotImage image) {
        this.image = image;
    }

    public boolean isLookRight() {
        return lookRight;
    }

    public void setLookRight(final boolean lookRight) {
        this.lookRight = lookRight;
    }

    public Rectangle getBounds() {
        if (getImage() != null) {
            // Find the window area from the ground coordinates and image center coordinates. The center has already been adjusted for scaling.
            final int top = getAnchor().y - getImage().getCenter().y;
            final int left = getAnchor().x - getImage().getCenter().x;

            return new Rectangle(left, top, getImage().getSize().width, getImage().getSize().height);
        } else {
            // as we have no image let's return what we were last frame
            return getWindow().asComponent().getBounds();
        }
    }

    public int getTime() {
        return time;
    }

    private void setTime(final int time) {
        this.time = time;
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public void setBehavior(final Behavior behavior) throws CantBeAliveException {
        this.behavior = behavior;
        this.behavior.init(this);
    }

    public int getCount() {
        return manager != null ? getManager().getCount(imageSet) : 0;
    }

    public int getTotalCount() {
        return manager != null ? getManager().getCount() : 0;
    }

    private boolean isAnimating() {
        return animating && !paused;
    }

    private void setAnimating(final boolean animating) {
        this.animating = animating;
    }

    private TranslucentWindow getWindow() {
        return window;
    }

    public MascotEnvironment getEnvironment() {
        return environment;
    }

    public List<String> getAffordances() {
        return affordances;
    }

    public List<Hotspot> getHotspots() {
        return hotspots;
    }

    public void setImageSet(final String set) {
        imageSet = set;
    }

    public String getImageSet() {
        return imageSet;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(final String name) {
        sound = name;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(final boolean paused) {
        this.paused = paused;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(final boolean isDragging) {
        dragging = isDragging;
    }

    public boolean isHotspotClicked() {
        return cursor != null;
    }

    public Point getCursorPosition() {
        return cursor;
    }

    public void setCursorPosition(final Point point) {
        cursor = point;

        if (point == null) {
            refreshCursor(false);
        } else {
            refreshCursor(point);
        }
    }
}
