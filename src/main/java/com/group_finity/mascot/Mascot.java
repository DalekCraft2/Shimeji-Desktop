package com.group_finity.mascot;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Hotspot;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.behavior.BehaviorExecutionException;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.config.BehaviorInstantiationException;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.image.MascotImage;
import com.group_finity.mascot.menu.MenuScroller;
import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.platform.TranslucentWindow;
import com.group_finity.mascot.sound.Sounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents an instance of a mascot character in the desktop environment.
 * <p>
 * Mascots move using {@link Behavior} objects, which represents long-term and complex behavior;
 * and {@link com.group_finity.mascot.action.Action Action} objects, which represents short-term
 * and monotonous movements.
 * <p>
 * Mascots have an internal timer and call an  {@code Action} at regular intervals.
 * The {@code Action} animates the mascot by calling {@link Animation#apply(Mascot, int)}.
 * <p>
 * When an {@code Action} ends, or at other specific times, the {@code Behavior} is called to transition
 * to the next {@code Action}.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Mascot {
    private static final Logger log = LoggerFactory.getLogger(Mascot.class);

    /**
     * The ID of the last generated {@code Mascot}.
     * This is incremented whenever a {@code Mascot} is instantiated.
     */
    private static final AtomicInteger lastId = new AtomicInteger();

    /**
     * The unique ID of this {@code Mascot}.
     * Exists only to make it easier to view debug logs.
     */
    private final int id;

    /**
     * The name of the image set that is currently used by this {@code Mascot}.
     *
     * @see #getImageSet()
     * @see #setImageSet(String)
     */
    private String imageSet;

    /**
     * The {@link MascotEnvironment} that allows this {@code Mascot} to access information about the
     * desktop environment.
     *
     * @see #getEnvironment()
     */
    private final MascotEnvironment environment = new MascotEnvironment(this);

    /**
     * The window that displays this {@code Mascot}.
     */
    private TranslucentWindow window;

    /**
     * The {@link Manager} that manages this {@code Mascot}.
     *
     * @see #getManager()
     * @see #setManager(Manager)
     */
    private Manager manager = null;

    /**
     * The ground coordinates of this {@code Mascot}.
     * For most actions, this will be the point that aligns with a border of the screen
     * (for example, when this {@code Mascot} is walking on the work area floor, this point will be aligned with
     * the work area floor border).
     *
     * @see #getAnchor()
     * @see #setAnchor(Point)
     */
    private Point anchor = new Point(0, 0);

    /**
     * The image that is currently being displayed on the window of this {@code Mascot}.
     *
     * @see #getImage()
     * @see #setImage(MascotImage)
     */
    private MascotImage image = null;

    /**
     * Whether this {@code Mascot} is facing right.
     * <p>
     * Mascot images are treated as facing left by default, so setting this to {@code true} will cause the
     * image to be flipped unless there is a dedicated right-facing image for this {@code Mascot} to use instead.
     *
     * @see #isLookRight()
     * @see #setLookRight(boolean)
     */
    private boolean lookRight = false;

    /**
     * The behavior that is currently being executed by this {@code Mascot}.
     *
     * @see #getBehavior()
     * @see #setBehavior(Behavior)
     */
    private Behavior behavior = null;

    /**
     * The number of ticks that have elapsed since this {@code Mascot} was created.
     * This only increments when this {@code Mascot} is {@linkplain #animating animating} and is not
     * {@linkplain #paused paused}.
     * <p>
     * While it's technically possible for this to overflow, the user would need to keep the application running
     * for the following amount of time for it to happen:
     * <pre>
     *     Max Integer Value: 2,147,483,647
     *     FPS: 60
     *
     *     2,147,483,647 / 60 = ~35,791,394.1 seconds
     *     ~35,791,394.1 / 60 = ~596,523.2 minutes
     *     ~596,523.2 / 60 = ~9,942.0 hours
     *     ~9,942.0 / 24 = ~414.2 days
     * </pre>
     *
     * @see #getTime()
     */
    private int time = 0;

    /**
     * Whether this {@code Mascot} is currently animating.
     * When a {@code Mascot} is not animating, {@link #tick()} and {@link #apply()} will do nothing.
     * This is used to pause this {@code Mascot} whilst its context menu is opened, and to prevent updating this
     * {@code Mascot} after {@link #dispose()} has been called on it.
     *
     * @see #isAnimating()
     * @see #setAnimating(boolean)
     */
    // TODO: Rename this to better distinguish it from the "paused" variable
    private boolean animating = true;

    /**
     * Whether this {@code Mascot} is paused.
     * When a {@code Mascot} is paused, {@link #tick()} and {@link #apply()} will do nothing.
     * This is used by the context menu of this {@code Mascot} and by the program's
     * {@linkplain TrayMenu tray menu} to toggle the paused state of this {@code Mascot}.
     *
     * @see #isPaused()
     * @see #setPaused(boolean)
     * @see #setPausedNoCallback(boolean)
     */
    private boolean paused = false;

    /**
     * Whether this {@code Mascot} is being dragged by the mouse cursor.
     *
     * @see #isDragging()
     * @see #setDragging(boolean)
     */
    private boolean dragging = false;

    /**
     * The key of the sound that is currently being played by this {@code Mascot}.
     * When this value is {@code null}, no sound is played.
     *
     * @see #getSound()
     * @see #setSound(String)
     */
    private String sound = null;

    /**
     * The debug window that displays information about this {@code Mascot} and its environment.
     */
    protected DebugWindow debugWindow = null;

    /**
     * The affordances that are currently being broadcast by this {@code Mascot}.
     *
     * @see #getAffordances()
     */
    private final List<String> affordances = new ArrayList<>(5);

    /**
     * A lock used to allow concurrent access to {@link #hotspots}.
     * This is necessary due to the EDT reading from the {@code hotspots} field to draw
     * the hotspot bounds when Draw Shimeji Bounds is enabled.
     *
     * @see #getHotspotLock()
     */
    private final ReadWriteLock hotspotLock = new ReentrantReadWriteLock();

    /**
     * The {@link Hotspot} objects that are currently clickable on this {@code Mascot}.
     *
     * @see #getHotspots()
     * @see #clearHotspots()
     * @see #setHotspots(Collection)
     */
    private final List<Hotspot> hotspots = new ArrayList<>(5);

    /**
     * The position of the mouse cursor, relative to the window of this {@code Mascot}.
     * <p>
     * This is set by behaviors when the user triggers a hotspot on this {@code Mascot},
     * so that this {@code Mascot} knows to check for any new hotspots that emerge while
     * the mouse is pressed.
     * When this value is {@code null}, it indicates that no hotspots are being clicked.
     *
     * @see #isHotspotClicked()
     * @see #getCursorPosition()
     * @see #setCursorPosition(Point)
     */
    private Point cursor = null;

    /**
     * The dimensions of the last non-null image this {@code Mascot} had.
     * This is set by {@link #setImage(MascotImage)} whenever the current image is not {@code null}.
     * When the current image is {@code null}, this value is used by {@link #getBounds()} to calculate the bounds.
     */
    private Dimension prevImageSize = null;

    /**
     * The anchor of the last non-null image this {@code Mascot} had.
     * This is set by {@link #setImage(MascotImage)} whenever the current image is not {@code null}.
     * When the current image is {@code null}, this value is used by {@link #getBounds()} to calculate the bounds.
     */
    private Point prevImageAnchor = null;

    /**
     * A lock used to allow concurrent access to {@link #prevImageSize} and {@link #prevImageAnchor}.
     * This is necessary due to the EDT reading from those fields to draw the mascot bounds when
     * Draw Shimeji Bounds is enabled.
     */
    private final ReadWriteLock imageFieldLock = new ReentrantReadWriteLock();

    /**
     * Whether the window of this {@code Mascot} needs to be repainted.
     * This may be set to {@code true} for various reasons (for instance, if {@link #image} is changed),
     * and set to {@code false} after {@link #apply()} has been called.
     */
    private boolean needsRepaint = true;

    /**
     * The state of the "Draw Shimeji Bounds" setting as of the last tick.
     * This is used to detect when the setting has changed, in which case
     * {@link #needsRepaint} is set to {@code true}.
     */
    private boolean prevDrawShimejiBounds = false;

    /**
     * A map that can be used by scripts to store and access custom variables.
     * This field is not accessed by the program itself.
     * <p>
     * This is only initialized upon calling {@link #getVariables()}.
     *
     * @see #getVariables()
     */
    private Map<String, Object> variables = null;

    /**
     * Creates a new {@code Mascot} with the specified image set.
     *
     * @param imageSet the name of the image set that this {@code Mascot} will initially use
     */
    public Mascot(final String imageSet) {
        id = lastId.incrementAndGet();
        this.imageSet = imageSet;

        log.info("Created mascot \"{}\" with image set \"{}\"", this, imageSet);

        Runnable runnable = () -> {
            window = NativeFactory.getInstance().newTranslucentWindow();

            // Always show on top
            window.setAlwaysOnTop(true);

            Component windowComponent = window.asComponent();

            // Register the mouse handler
            windowComponent.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    Mascot.this.mousePressed(e);
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    Mascot.this.mouseReleased(e);
                }
            });
            windowComponent.addMouseMotionListener(new MouseMotionListener() {
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

            // For drawing the outlines of hotspots and the mascot's bounds, for debugging purposes
            JComponent debugComp = new JComponent() {
                @Override
                public void paintComponent(Graphics g) {
                    boolean shouldBeEnabled = Main.getInstance().getSettings().drawShimejiBounds;
                    if (isEnabled() != shouldBeEnabled)
                        setEnabled(shouldBeEnabled);
                    if (shouldBeEnabled) {
                        super.paintComponent(g);

                        int width = getWidth();
                        int height = getHeight();

                        // Draw hotspots
                        g.setColor(Color.BLUE);
                        ReadWriteLock lock = getHotspotLock();
                        lock.readLock().lock();
                        try {
                            if (!getHotspots().isEmpty()) {
                                for (Hotspot hotspot : getHotspots()) {
                                    Shape shape = hotspot.getShape();
                                    if (shape instanceof Rectangle rectangle) {
                                        int x = lookRight ? width - rectangle.x - rectangle.width : rectangle.x;
                                        g.drawRect(x, rectangle.y, rectangle.width - 1, rectangle.height - 1);
                                    } else if (shape instanceof Ellipse2D ellipse) {
                                        double x = lookRight ? width - ellipse.getX() - ellipse.getWidth() : ellipse.getX();
                                        g.drawOval((int) x, (int) ellipse.getY(), (int) ellipse.getWidth(), (int) ellipse.getHeight());
                                    }
                                }
                            }
                        } finally {
                            lock.readLock().unlock();
                        }

                        // Draw bounds
                        g.setColor(Color.RED);
                        g.drawRect(getX(), getY(), width - 1, height - 1);

                        // Draw image anchor
                        g.setColor(Color.GREEN);
                        Point imageAnchor;
                        imageFieldLock.readLock().lock();
                        try {
                            imageAnchor = prevImageAnchor;
                        } finally {
                            imageFieldLock.readLock().unlock();
                        }
                        if (imageAnchor != null) {
                            // Because the image anchor is a single point, it is drawn as a circle and several lines for visibility
                            g.drawOval(imageAnchor.x - 5, imageAnchor.y - 5, 10, 10);
                            g.drawLine(imageAnchor.x - 10, imageAnchor.y, imageAnchor.x + 10, imageAnchor.y);
                            g.drawLine(imageAnchor.x, imageAnchor.y - 10, imageAnchor.x, imageAnchor.y + 10);
                            g.drawLine(imageAnchor.x - 10, imageAnchor.y - 10, imageAnchor.x + 10, imageAnchor.y + 10);
                            g.drawLine(imageAnchor.x - 10, imageAnchor.y + 10, imageAnchor.x + 10, imageAnchor.y - 10);
                        }
                    }
                }
            };
            debugComp.setBackground(new Color(0, 0, 0, 0));
            debugComp.setOpaque(false);
            debugComp.setPreferredSize(windowComponent.getPreferredSize());
            windowComponent.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    debugComp.setPreferredSize(e.getComponent().getPreferredSize());
                }
            });
            ((Container) windowComponent).add(debugComp);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    @Override
    public String toString() {
        return "mascot" + id;
    }

    /**
     * Called when a mouse button is pressed on the window of this {@code Mascot}.
     *
     * @param event the event created by a mouse button being pressed
     * @see MouseListener#mousePressed(MouseEvent)
     */
    private void mousePressed(final MouseEvent event) {
        // Check for popup triggers in both mousePressed and mouseReleased
        // because popup menus are triggered differently on different systems
        if (event.isPopupTrigger()) {
            showPopup(event.getX(), event.getY());
        } else {
            // Switch to drag animation when mouse is pressed
            if (!paused && behavior != null) {
                try {
                    behavior.mousePressed(event);
                } catch (final BehaviorExecutionException e) {
                    log.error("Severe error in mouse press handler for mascot \"{}\"", this, e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("SevereShimejiErrorErrorMessage"), e);
                    dispose();
                }
            }
        }
    }

    /**
     * Called when a mouse button is released on the window of this {@code Mascot}.
     *
     * @param event the event created by a mouse button being released
     * @see MouseListener#mousePressed(MouseEvent)
     */
    private void mouseReleased(final MouseEvent event) {
        // Check for popup triggers in both mousePressed and mouseReleased
        // because popup menus are triggered differently on different systems
        if (event.isPopupTrigger()) {
            showPopup(event.getX(), event.getY());
        } else {
            if (!paused && behavior != null) {
                try {
                    behavior.mouseReleased(event);
                } catch (final BehaviorExecutionException e) {
                    log.error("Severe error in mouse release handler for mascot \"{}\"", this, e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("SevereShimejiErrorErrorMessage"), e);
                    dispose();
                }
            }
        }
    }

    /**
     * Creates and displays the context menu of this {@code Mascot} at the position {@code (x, y)}, in the coordinate
     * space of the window of this {@code Mascot}.
     *
     * @param x the x-coordinate at which the popup should be positioned, relative to the window of this {@code Mascot}
     * @param y the y-coordinate at which the popup should be positioned, relative to the window of this {@code Mascot}
     */
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

        final JMenuItem callAnotherItem = new JMenuItem(languageBundle.getString("CallAnother"));
        callAnotherItem.addActionListener(event -> Main.getInstance().createMascot(imageSet));

        final JMenuItem followCursorItem = new JMenuItem(languageBundle.getString("FollowCursor"));
        followCursorItem.addActionListener(event -> manager.setBehaviorAll(Main.getInstance().getConfiguration(imageSet), UserBehavior.BEHAVIORNAME_CHASEMOUSE, imageSet));

        final JMenuItem restoreWindowsItem = new JMenuItem(languageBundle.getString("RestoreWindows"));
        restoreWindowsItem.addActionListener(event -> environment.restoreIE());

        final JMenuItem debugMenuItem = new JMenuItem(languageBundle.getString("RevealStatistics"));
        debugMenuItem.addActionListener(event -> {
            if (debugWindow == null) {
                debugWindow = new DebugWindow();
            }
            debugWindow.setVisible(true);
        });

        JMenu setBehaviorMenu = new JMenu(languageBundle.getString("SetBehaviour"));
        JMenu allowedBehaviorsMenu = new JMenu(languageBundle.getString("AllowedBehaviours"));
        setBehaviorMenu.setAutoscrolls(true);
        JMenuItem item;
        JCheckBoxMenuItem toggleItem;
        final Configuration config = Main.getInstance().getConfiguration(imageSet);
        for (final String behaviorName : config.getBehaviorNames()) {
            if (!config.isBehaviorHidden(behaviorName)) {
                // If there is a language property for the behavior name, use the translated behavior name as the
                // behavior's display name; otherwise, insert spaces between the individual words in the behavior
                // name and use that as the display name
                String displayName = languageBundle.containsKey(behaviorName) ? languageBundle.getString(behaviorName) :
                        behaviorName.replaceAll("([a-z])(IE)?([A-Z])", "$1 $2 $3").replaceAll(" {2}", " ");

                boolean behaviorEnabled = config.isBehaviorEnabled(behaviorName, this);
                if (behaviorEnabled && !behaviorName.contains("/")) {
                    item = new JMenuItem(displayName);
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            try {
                                setBehavior(config.buildBehavior(behaviorName));
                            } catch (BehaviorInstantiationException | BehaviorExecutionException ex) {
                                log.error("Failed to set behavior to \"{}\" for mascot \"{}\"", behaviorName, this, ex);
                                Main.showError(String.format(languageBundle.getString("FailedSetBehaviourErrorMessage"), behaviorName, this), ex);
                            }
                        }
                    });
                    setBehaviorMenu.add(item);
                }
                if (config.isBehaviorToggleable(behaviorName) && !behaviorName.contains("/")) {
                    toggleItem = new JCheckBoxMenuItem(displayName, behaviorEnabled);
                    toggleItem.addItemListener(e -> Main.getInstance().setMascotBehaviorEnabled(behaviorName, this, e.getStateChange() == ItemEvent.SELECTED));
                    allowedBehaviorsMenu.add(toggleItem);
                }
            }
        }
        // Create the MenuScrollers after adding all the items to the menus,
        // so the menus are positioned correctly when first shown.
        MenuScroller.setScrollerFor(setBehaviorMenu, 30, 125);
        MenuScroller.setScrollerFor(allowedBehaviorsMenu, 30, 125);

        final JMenuItem pauseItem = new JMenuItem(isPaused() ? languageBundle.getString("ResumeAnimations") : languageBundle.getString("PauseAnimations"));
        pauseItem.addActionListener(event -> setPaused(!isPaused()));

        final JMenuItem disposeMenu = new JMenuItem(languageBundle.getString("Dismiss"));
        disposeMenu.addActionListener(e -> dispose());

        final JMenuItem remainOneItem = new JMenuItem(languageBundle.getString("DismissOthers"));
        remainOneItem.addActionListener(event -> manager.remainOne(imageSet, this));

        final JMenuItem remainOnlyOneItem = new JMenuItem(languageBundle.getString("DismissAllOthers"));
        remainOnlyOneItem.addActionListener(event -> manager.remainOne(this));

        final JMenuItem closeMenu = new JMenuItem(languageBundle.getString("DismissAll"));
        closeMenu.addActionListener(e -> Main.getInstance().exit());

        popup.add(callAnotherItem);
        popup.addSeparator();
        popup.add(followCursorItem);
        popup.add(restoreWindowsItem);
        popup.add(debugMenuItem);
        popup.addSeparator();
        if (setBehaviorMenu.getMenuComponentCount() > 0) {
            popup.add(setBehaviorMenu);
        }
        if (allowedBehaviorsMenu.getMenuComponentCount() > 0) {
            popup.add(allowedBehaviorsMenu);
        }
        // Only add a second separator if either menu has a component count greater than 0. Just in case!
        if (setBehaviorMenu.getMenuComponentCount() > 0 || allowedBehaviorsMenu.getMenuComponentCount() > 0) {
            popup.addSeparator();
        }
        popup.add(pauseItem);
        popup.addSeparator();
        popup.add(disposeMenu);
        popup.add(remainOneItem);
        popup.add(remainOnlyOneItem);
        popup.add(closeMenu);

        final Component windowComponent = window.asComponent();

        // TODO: Get the popup to close when clicking outside of it
        windowComponent.requestFocus();

        // Lightweight popups expect the mascot window to draw them if they fall inside the mascot window's boundary.
        // As the mascot window can't support this, we need to set them to heavyweight.
        popup.setLightWeightPopupEnabled(false);
        popup.show(windowComponent, x, y);
    }

    /**
     * Advances this {@code Mascot} by one tick.
     * <p>
     * After this method returns, {@link #apply()} should be called to update the graphics of this {@code Mascot}.
     */
    synchronized void tick() {
        if (isAnimating()) {
            if (behavior != null) {
                try {
                    behavior.next();
                } catch (final BehaviorExecutionException e) {
                    log.error("Could not get next behavior for mascot \"{}\"", this, e);
                    Main.showError(String.format(Main.getInstance().getLanguageBundle().getString("CouldNotGetNextBehaviourErrorMessage"), this), e);
                    dispose();
                }

                time++;
            }

            SwingUtilities.invokeLater(() -> {
                if (debugWindow != null) {
                    // This sets the title of the actual debug window--not the "Window Title" field--to the mascot's ID
                    // Unfortunately, doing this makes it possible to select it as the activeIE because it no longer has an empty name, so I have commented it out
                    // debugWindow.setTitle(toString());

                    if (behavior instanceof UserBehavior userBehavior) {
                        debugWindow.setBehavior(userBehavior.getName().replaceAll("([a-z])(IE)?([A-Z])", "$1 $2 $3").replaceAll(" {2}", " "));
                    } else {
                        debugWindow.setBehavior("");
                    }
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
            });
        }
    }

    /**
     * Updates the graphics of the window of this {@code Mascot}, and plays a sound if {@link #sound} is not
     * {@code null} and refers to a sound clip that is not already playing.
     */
    public void apply() {
        // Make sure to repaint the mascot if the Draw Shimeji Bounds setting has changed since the last tick
        boolean drawShimejiBounds = Main.getInstance().getSettings().drawShimejiBounds;
        if (prevDrawShimejiBounds != drawShimejiBounds)
            needsRepaint = true;
        prevDrawShimejiBounds = drawShimejiBounds;

        if (!isAnimating() && !needsRepaint) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            final Component windowComponent = window.asComponent();
            Rectangle bounds = getBounds();
            /* Compare each value individually instead of using windowComponent.getBounds().equals(),
            to avoid heap allocations caused by Component.getBounds() */
            if (windowComponent.getX() != bounds.x || windowComponent.getY() != bounds.y ||
                    windowComponent.getWidth() != bounds.width || windowComponent.getHeight() != bounds.height) {
                // Set the bounds of the window to the mascot's bounds
                windowComponent.setBounds(bounds);
            }
            if (needsRepaint) {
                // If Draw Shimeji Bounds is enabled, always keep the window visible so we can actually see the bounds
                boolean shouldBeVisible = image != null || Main.getInstance().getSettings().drawShimejiBounds;
                if (windowComponent.isVisible() == shouldBeVisible) {
                    window.updateImage(); // Redraw
                } else {
                    /*
                    setVisible(true) repaints the window too, so there's no need to call
                    window.updateImage() afterward if we call this first
                     */
                    windowComponent.setVisible(shouldBeVisible);
                }
                needsRepaint = false;
            }
        });

        // play sound if requested
        if (Sounds.isEnabled() && sound != null && Sounds.contains(sound)) {
            Clip clip = Sounds.get(sound);
            if (!clip.isRunning()) {
                clip.stop();
                clip.setMicrosecondPosition(0);
                clip.start();
            }
        }
    }

    /**
     * Clears all resources held by this {@code Mascot} and removes it from its {@link Manager}.
     */
    public synchronized void dispose() {
        log.info("Destroying mascot \"{}\"", this);

        SwingUtilities.invokeLater(() -> {
            if (debugWindow != null) {
                debugWindow.dispose();
                debugWindow = null;
            }
            window.dispose();
        });

        animating = false;
        // Clear affordances so the mascot is not participating in any interactions, as that can cause an NPE
        affordances.clear();
        if (manager != null) {
            manager.remove(this);
        }
    }

    /**
     * Checks whether the specified point is within the bounds of any of the hotspots on this {@code Mascot},
     * and updates the cursor to use the hand graphic if so. Otherwise, sets the cursor to use the default graphic.
     *
     * @param position the point to check when determining which graphic to apply to the cursor
     */
    private void refreshCursor(Point position) {
        hotspotLock.readLock().lock();
        try {
            if (hotspots.isEmpty()) {
                refreshCursor(false);
            } else {
                boolean useHand = hotspots.stream().anyMatch(hotspot ->
                        hotspot.contains(this, position) &&
                                Main.getInstance().getConfiguration(imageSet).isBehaviorEnabled(hotspot.getBehaviour(), this));

                refreshCursor(useHand);
            }
        } finally {
            hotspotLock.readLock().unlock();
        }
    }

    /**
     * Sets whether the cursor should use the hand graphic when hovering over the window of this {@code Mascot}.
     *
     * @param useHand {@code true} to apply the hand graphic to the cursor;
     * {@code false} to apply the default graphic to the cursor
     */
    private void refreshCursor(boolean useHand) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> refreshCursor(useHand));
            return;
        }
        int newType = useHand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR;
        Component windowComponent = window.asComponent();
        if (windowComponent.getCursor().getType() != newType) {
            windowComponent.setCursor(Cursor.getPredefinedCursor(newType));
        }
    }

    /**
     * Gets the {@link Manager} that manages this {@code Mascot}.
     *
     * @return the {@code Manager} that manages this {@code Mascot}
     * @see #setManager(Manager)
     */
    public Manager getManager() {
        return manager;
    }

    /**
     * Sets the {@link Manager} that manages this {@code Mascot}.
     * <p>
     * <b>This method is for internal use only. It should not be used in scripts.</b>
     *
     * @param manager the new {@code Manager} to manage this {@code Mascot}
     * @see #getManager()
     */
    public void setManager(final Manager manager) {
        this.manager = manager;
    }

    /**
     * Gets the ground coordinates of this {@code Mascot}.
     * For most actions, this will be the point that aligns with a border of the screen
     * (for example, when this {@code Mascot} is walking on the work area floor, this point will be aligned with
     * the work area floor border).
     *
     * @return the ground coordinates of this {@code Mascot}
     * @see #setAnchor(Point)
     */
    public Point getAnchor() {
        return anchor;
    }

    /**
     * Sets the ground coordinates of this {@code Mascot}.
     *
     * @param anchor the new ground coordinates of this {@code Mascot}
     * @see #getAnchor()
     */
    public void setAnchor(Point anchor) {
        this.anchor = anchor;
    }

    /**
     * Gets the image that is currently being displayed on the window of this {@code Mascot}.
     *
     * @return the image that is currently being displayed on the window of this {@code Mascot}
     * @see #setImage(MascotImage)
     */
    public MascotImage getImage() {
        return image;
    }

    /**
     * Sets the image that is being displayed on the window of this {@code Mascot}.
     * Changes will apply when {@link #apply()} is next invoked.
     *
     * @param image the new image to display on the window of this {@code Mascot}
     * @see #getImage()
     */
    public void setImage(final MascotImage image) {
        if (Objects.equals(this.image, image)) {
            return;
        }

        this.image = image;

        if (image != null) {
            imageFieldLock.writeLock().lock();
            try {
                prevImageAnchor = image.getCenter();
                prevImageSize = image.getSize();
            } finally {
                imageFieldLock.writeLock().unlock();
            }
        }

        SwingUtilities.invokeLater(() -> {
            if (image != null) {
                window.setImage(image.getImage());
            } else {
                window.setImage(null);
            }
            needsRepaint = true;
        });
    }

    /**
     * Gets whether this {@code Mascot} is facing right.
     * <p>
     * Mascot images are treated as facing left by default, so a return value of {@code true} means the
     * image is currently flipped unless there is a dedicated right-facing image for this {@code Mascot}
     * to use instead.
     *
     * @return {@code true} if this {@code Mascot} is facing right; {@code false} if it is facing left
     * @see #setLookRight(boolean)
     */
    public boolean isLookRight() {
        return lookRight;
    }

    /**
     * Sets whether this {@code Mascot} is facing right.
     * <p>
     * Mascot images are treated as facing left by default, so setting this to {@code true} will cause the
     * image to be flipped unless there is a dedicated right-facing image for this {@code Mascot} to use instead.
     *
     * @param lookRight {@code true} to make this {@code Mascot} face right;
     * {@code false} to make it face left
     * @see #isLookRight()
     */
    public void setLookRight(final boolean lookRight) {
        this.lookRight = lookRight;
    }

    /**
     * Calculates the current bounding rectangle of this {@code Mascot} using its {@linkplain #getAnchor() anchor},
     * the {@linkplain MascotImage#getCenter() anchor of its current image}, and the
     * {@linkplain MascotImage#getSize() size of its current image}.
     * <p>
     * The bounding rectangle is defined as follows:
     * <pre>
     *     x = anchor.x - imageAnchor.x
     *     y = anchor.y - imageAnchor.y
     *     width = imageSize.width
     *     height = imageSize.height
     * </pre>
     * The x and y coordinates represent the position of the upper-left corner of the rectangle.
     * <p>
     * If the current image of this {@code Mascot} is {@code null}, then the rectangle will be calculated
     * using the image anchor and size of the most recent non-{@code null} image that was applied to this
     * {@code Mascot}.
     *
     * @return the current bounding rectangle of this {@code Mascot}
     */
    public Rectangle getBounds() {
        // Find the window area from the ground coordinates and image anchor coordinates.
        // The image anchor has already been adjusted for scaling.
        Point imageAnchor;
        Dimension imageSize;
        imageFieldLock.readLock().lock();
        try {
            imageAnchor = prevImageAnchor;
            imageSize = prevImageSize;
        } finally {
            imageFieldLock.readLock().unlock();
        }
        int x = anchor.x;
        int y = anchor.y;
        if (imageAnchor != null) {
            x -= imageAnchor.x;
            y -= imageAnchor.y;
        }
        final int width = imageSize == null ? 0 : imageSize.width;
        final int height = imageSize == null ? 0 : imageSize.height;

        return new Rectangle(x, y, width, height);
    }

    /**
     * Gets the number of ticks that have elapsed since this {@code Mascot} was created.
     * This only increments when this {@code Mascot} is {@linkplain #isAnimating() animating} and is not
     * {@linkplain #isPaused() paused}.
     *
     * @return the number of ticks that have elapsed since this {@code Mascot} was created
     */
    public int getTime() {
        return time;
    }

    /**
     * Gets the behavior that is currently being executed by this {@code Mascot}.
     *
     * @return the behavior that is currently being executed by this {@code Mascot}
     * @see #setBehavior(Behavior)
     */
    public Behavior getBehavior() {
        return behavior;
    }

    /**
     * Sets the behavior that is being executed by this {@code Mascot}.
     *
     * @param behavior the behavior to be executed by this {@code Mascot}
     * @throws BehaviorExecutionException if the specified behavior fails to initialize
     * @see #getBehavior()
     */
    public void setBehavior(final Behavior behavior) throws BehaviorExecutionException {
        this.behavior = behavior;
        if (this.behavior != null) {
            this.behavior.init(this);
        }
    }

    /**
     * Gets the number of {@code Mascot} objects in the {@link Manager} of this {@code Mascot}
     * that use the same image set as this {@code Mascot}.
     *
     * @return the number of {@code Mascot} objects that use the same image set as this {@code Mascot}
     * @see Manager#getCount(String)
     */
    public int getCount() {
        return manager != null ? manager.getCount(imageSet) : 0;
    }

    /**
     * Gets the total number of {@code Mascot} objects in the {@link Manager} of this {@code Mascot}.
     *
     * @return the total number of {@code Mascot} objects in the {@link Manager} of this {@code Mascot}
     * @see Manager#getCount()
     */
    public int getTotalCount() {
        return manager != null ? manager.getCount() : 0;
    }

    /**
     * Gets whether this {@code Mascot} is animating.
     * When a {@code Mascot} is not animating, {@link #tick()} and {@link #apply()} will do nothing.
     *
     * @return {@code true} if this {@code Mascot} is animating; {@code false} if it is not animating
     * @see #setAnimating(boolean)
     */
    private boolean isAnimating() {
        return animating && !paused;
    }

    /**
     * Sets whether this {@code Mascot} is currently animating.
     * When a {@code Mascot} is not animating, {@link #tick()} and {@link #apply()} will do nothing.
     *
     * @param animating {@code true} to mark this {@code Mascot} as animating; {@code false} to mark it
     * as not animating
     * @see #isAnimating()
     */
    private void setAnimating(final boolean animating) {
        this.animating = animating;
    }

    /**
     * Gets the {@link MascotEnvironment} that allows this {@code Mascot} to access information about the
     * desktop environment.
     *
     * @return the {@code MascotEnvironment} of this {@code Mascot}
     */
    public MascotEnvironment getEnvironment() {
        return environment;
    }

    /**
     * Gets the affordances that are currently being broadcast by this {@code Mascot}.
     *
     * @return the affordances that are currently being broadcast by this {@code Mascot}
     */
    public List<String> getAffordances() {
        return affordances;
    }

    /**
     * Gets the lock that is used to allow concurrent access to list of {@link Hotspot} objects on this {@code Mascot}.
     * <p>
     * Before doing any operations that reads from / writes to the list of hotspots, callers must invoke this method to
     * retrieve the hotspot lock and invoke {@code readLock().lock()} or {@code writeLock().lock()} on it, to allow for
     * proper concurrent access to the list of hotspots on this {@code Mascot}.
     * Immediately after that call, the code that reads from / writes to the hotspot list must be in a try-finally block
     * that invokes {@code readLock().unlock()} or {@code writeLock().unlock()}, depending on which lock was used.
     * This ensures that the lock is always unlocked at the end of the operation, even if an exception was thrown during
     * the operation.
     * <p>
     * A sample usage can be seen below:
     * <pre>
     *     Hotspot hotspot = null;
     *     ReadWriteLock lock = mascot.getHotspotLock();
     *     lock.readLock().lock
     *     try {
     *          hotspot = mascot.getHotspots().getFirst();
     *     } finally {
     *          lock.readLock().unlock();
     *     }
     * </pre>
     * <p>
     * {@code writeLock()} should be used when invoking {@link #clearHotspots()} or {@link #setHotspots(Collection)},
     * and {@code readLock()} should be used when reading from the returned list of {@link #getHotspots()}.
     * It is not recommended to use {@code writeLock()} with {@code getHotspots()} to modify the contents of the
     * hotspot list directly; rather, callers should use either {@code clearHotspots()} or
     * {@code setHotspots(Collection)} for that.
     *
     * @return the lock that is used to allow concurrent access to list of {@link Hotspot} objects on this {@code Mascot}
     * @see #getHotspots()
     * @see #clearHotspots()
     * @see #setHotspots(Collection)
     */
    public ReadWriteLock getHotspotLock() {
        return hotspotLock;
    }

    /**
     * Gets the list of {@link Hotspot} objects that are currently clickable on this {@code Mascot}.
     * <p>
     * Before reading from the returned list, callers must retrieve the hotspot lock from {@link #getHotspotLock()}
     * and invoke {@code readLock().lock()} on it, to allow for proper concurrent access to the returned list.
     * Immediately after that call, the code that reads from the returned list must be in a try-finally block that
     * invokes {@code readLock().unlock()}. This ensures that the lock is always unlocked at the end of the operation,
     * even if an exception was thrown during the operation.
     * <p>
     * A sample usage can be seen below:
     * <pre>
     *     Hotspot hotspot = null;
     *     ReadWriteLock lock = mascot.getHotspotLock();
     *     lock.readLock().lock
     *     try {
     *          hotspot = mascot.getHotspots().getFirst();
     *     } finally {
     *          lock.readLock().unlock();
     *     }
     * </pre>
     * <p>
     * It is recommended to use {@link #clearHotspots()} or {@link #setHotspots(Collection)} to modify the
     * hotspot list, rather than invoking this method and modifying the contents of the returned list directly.
     *
     * @return the {@link Hotspot} objects that are currently clickable on this {@code Mascot}
     * @see #getHotspotLock()
     * @see #clearHotspots()
     * @see #setHotspots(Collection)
     */
    public List<Hotspot> getHotspots() {
        return hotspots;
    }

    /**
     * Clears the list of {@link Hotspot} objects that are currently clickable on this {@code Mascot}.
     * <p>
     * Before invoking this method, the caller must retrieve the hotspot lock from {@link #getHotspotLock()}
     * and invoke {@code writeLock().lock()} on it, to allow for proper concurrent access to the list of hotspots
     * on this {@code Mascot}. Immediately after that call, the code that invokes this method must be in a
     * try-finally block that invokes {@code writeLock().unlock()} in the finally block. This ensures that the
     * lock is always unlocked at the end of the operation, even if an exception was thrown during the operation.
     * <p>
     * A sample usage can be seen below:
     * <pre>
     *     ReadWriteLock lock = mascot.getHotspotLock();
     *     lock.writeLock().lock
     *     try {
     *          mascot.clearHotspots();
     *     } finally {
     *          lock.writeLock().unlock();
     *     }
     * </pre>
     *
     * @see #getHotspotLock()
     * @see #getHotspots()
     * @see #setHotspots(Collection)
     */
    public void clearHotspots() {
        if (hotspots.isEmpty()) {
            return;
        }
        hotspots.clear();

        // If DrawShimejiBounds is enabled, we need to redraw the hotspot boundaries
        if (Main.getInstance().getSettings().drawShimejiBounds) {
            needsRepaint = true;
        }
    }

    /**
     * Clears the list of {@link Hotspot} objects that are currently clickable on this {@code Mascot}, and replaces
     * its contents with that of the specified collection.
     * <p>
     * Before invoking this method, the caller must retrieve the hotspot lock from {@link #getHotspotLock()}
     * and invoke {@code writeLock().lock()} on it, to allow for proper concurrent access to the list of hotspots
     * on this {@code Mascot}. Immediately after that call, the code that invokes this method must be in a
     * try-finally block that invokes {@code writeLock().unlock()} in the finally block. This ensures that the
     * lock is always unlocked at the end of the operation, even if an exception was thrown during the operation.
     * <p>
     * A sample usage can be seen below:
     * <pre>
     *     Collection&lt;Hotspot&gt; newHotspots = ...
     *     ReadWriteLock lock = mascot.getHotspotLock();
     *     lock.writeLock().lock
     *     try {
     *          mascot.setHotspots(newHotspots);
     *     } finally {
     *          lock.writeLock().unlock();
     *     }
     * </pre>
     * <p>
     * It is recommended to use {@link #clearHotspots()} to clear the contents of the hotspot list, rather than
     * passing an empty collection to this method.
     *
     * @param hotspots the new contents of the hotspot list
     * @see #getHotspotLock()
     * @see #getHotspots()
     * @see #clearHotspots()
     */
    public void setHotspots(Collection<Hotspot> hotspots) {
        if (this.hotspots.isEmpty() && hotspots.isEmpty()) {
            return;
        }
        this.hotspots.clear();
        this.hotspots.addAll(hotspots);

        // If DrawShimejiBounds is enabled, we need to redraw the hotspot boundaries
        if (Main.getInstance().getSettings().drawShimejiBounds) {
            needsRepaint = true;
        }
    }

    /**
     * Gets the name of the image set that is currently used by this {@code Mascot}.
     *
     * @return the name of the image set that is currently used by this {@code Mascot}
     * @see #setImageSet(String)
     */
    public String getImageSet() {
        return imageSet;
    }

    /**
     * Sets the image set that is used by this {@code Mascot}.
     *
     * @param imageSet the name of the new image set to be used by this {@code Mascot}
     * @see #getImageSet()
     */
    public void setImageSet(final String imageSet) {
        this.imageSet = imageSet;
    }

    /**
     * Gets the key of the sound that is currently being played by this {@code Mascot}.
     *
     * @return the key of the sound that is currently being played by this {@code Mascot}, or {@code null}
     * if no sound is being played
     * @see #setSound(String)
     */
    public String getSound() {
        return sound;
    }

    /**
     * Sets the key of the sound that is being played by this {@code Mascot}.
     * Changes will apply when {@link #apply()} is next invoked.
     *
     * @param sound the key of the sound to be played by this {@code Mascot}, or {@code null} if no sound should play
     * @see #getSound()
     */
    public void setSound(final String sound) {
        this.sound = sound;
    }

    /**
     * Gets whether this {@code Mascot} is paused.
     * When a {@code Mascot} is paused, {@link #tick()} and {@link #apply()} will do nothing.
     *
     * @return {@code true} if this {@code Mascot} is paused; {@code false} if it is unpaused
     * @see #setPaused(boolean)
     * @see #setPausedNoCallback(boolean)
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Sets whether this {@code Mascot} is paused, and notifies the program's tray menu to update the text
     * of its "Pause/Resume Animations" button.
     * When a {@code Mascot} is paused, {@link #tick()} and {@link #apply()} will do nothing.
     *
     * @param paused {@code true} to pause this {@code Mascot}; {@code false} to unpause it
     * @see #isPaused()
     * @see #setPausedNoCallback(boolean)
     * @see TrayMenu#refreshPauseText()
     */
    public void setPaused(final boolean paused) {
        if (this.paused == paused) {
            return;
        }

        this.paused = paused;
        Main.getInstance().getTrayMenu().refreshPauseText();
    }

    /**
     * Sets whether this {@code Mascot} is paused.
     * When a {@code Mascot} is paused, {@link #tick()} and {@link #apply()} will do nothing.
     * <p>
     * Unlike {@link #setPaused(boolean)}, this method does not notify the program's {@link TrayMenu tray menu}
     * to update the text of its "Pause/Resume Animations" button.
     *
     * @param paused {@code true} to pause this {@code Mascot}; {@code false} to unpause it
     * @see #isPaused()
     * @see #setPaused(boolean)
     */
    void setPausedNoCallback(final boolean paused) {
        this.paused = paused;
    }

    /**
     * Gets whether this {@code Mascot} is being dragged by the mouse cursor.
     *
     * @return {@code true} if this {@code Mascot} is being dragged by the mouse cursor; {@code false} otherwise
     * @see #setDragging(boolean)
     */
    public boolean isDragging() {
        return dragging;
    }

    /**
     * Sets whether this {@code Mascot} is being dragged by the mouse cursor.
     *
     * @param dragging {@code true} to mark this {@code Mascot} as being dragged by the mouse cursor;
     * {@code false} to mark it as not being dragged
     * @see #isDragging()
     */
    public void setDragging(final boolean dragging) {
        this.dragging = dragging;
    }

    /**
     * Gets whether any hotspots are being clicked on this {@code Mascot}.
     *
     * @return {@code true} if any hotspots are being clicked on this {@code Mascot}; {@code false} otherwise
     */
    public boolean isHotspotClicked() {
        return cursor != null;
    }

    /**
     * Gets the position of the mouse cursor, relative to the window of this {@code Mascot}.
     *
     * @return the position of the mouse cursor relative to the window of this {@code Mascot}, or {@code null}
     * if no hotspots are being clicked
     * @see #isHotspotClicked()
     * @see #setCursorPosition(Point)
     */
    public Point getCursorPosition() {
        return cursor;
    }

    /**
     * Sets the position of the mouse cursor, relative to the window of this {@code Mascot}.
     *
     * @param cursor the new position of the mouse cursor, relative to the window of this {@code Mascot}.
     * If {@code null}, it will indicate that no hotspots are being clicked.
     * @see #isHotspotClicked()
     * @see #getCursorPosition()
     */
    public void setCursorPosition(final Point cursor) {
        if (this.cursor == null && cursor == null) {
            return;
        }

        this.cursor = cursor;

        if (this.cursor == null) {
            refreshCursor(false);
        } else {
            refreshCursor(cursor);
        }
    }

    /**
     * Gets a map that can be used by scripts to store and access custom variables.
     *
     * @return a map of custom variables
     */
    public Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new LinkedHashMap<>();
        }
        return variables;
    }
}
