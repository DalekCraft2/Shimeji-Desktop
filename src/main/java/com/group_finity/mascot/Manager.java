package com.group_finity.mascot;

import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An object that manages the list of {@link Mascot Mascots} and takes timing.
 * If each {@link Mascot} moves asynchronously, there will be various problems (such as when throwing a window),
 * so this class adjusts the timing of the entire mascot.
 * <p>
 * The {@link #tick()} method first retrieves the latest environment information and then moves all {@link Mascot Mascots}.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Manager {

    private static final Logger log = Logger.getLogger(Manager.class.getName());

    /**
     * The duration of each tick, in milliseconds.
     */
    public static final int TICK_INTERVAL = 40;

    /**
     * A list of {@link Mascot Mascots} which are managed by this {@code Manager}.
     */
    private final List<Mascot> mascots = new ArrayList<>();

    /**
     * List of {@link Mascot Mascots} to be added.
     * To prevent {@link ConcurrentModificationException}, {@link Mascot} additions are reflected all at once every {@link #tick()}.
     */
    private final Set<Mascot> added = new LinkedHashSet<>();

    /**
     * List of {@link Mascot Mascots} to be removed.
     * To prevent {@link ConcurrentModificationException}, {@link Mascot} removals are reflected all at once every {@link #tick()}.
     */
    private final Set<Mascot> removed = new LinkedHashSet<>();

    /**
     * Whether the program should exit when the last {@link Mascot} is deleted.
     * If you fail to create a tray icon, the process will remain forever unless you close the program when the {@link Mascot} disappears.
     */
    private boolean exitOnLastRemoved = true;

    /** {@link ScheduledExecutorService} which calls the {@link #tick()} method. */
    private ScheduledExecutorService executorService;

    public Manager() {}

    public void setExitOnLastRemoved(boolean exitOnLastRemoved) {
        this.exitOnLastRemoved = exitOnLastRemoved;
    }

    public boolean isExitOnLastRemoved() {
        return exitOnLastRemoved;
    }

    /**
     * Starts the thread.
     */
    public void start() {
        if (executorService != null && !executorService.isShutdown()) {
            log.warning("An attempt was made to start the scheduler, but it is already running.");
            return;
        }

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                this.tick();
            } catch (final Exception e) {
                log.log(Level.SEVERE, "An error occurred while running the tick method.", e);
                this.stop();
            }
        }, 0, TICK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the thread.
     */
    public void stop() {
        if (executorService == null || executorService.isShutdown()) {
            log.warning("An attempt was made to stop the scheduler, but it is not running.");
            return;
        }

        try {
            executorService.shutdownNow();

            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                log.log(Level.WARNING, "The executor service did not terminate in the allotted time.");
            }
        } catch (final InterruptedException | SecurityException e) {
            log.log(Level.SEVERE, "Failed to shutdown the executor service.", e);
        }

        executorService = null;
    }

    /**
     * Advances the {@link Mascot Mascots} by one frame.
     */
    private void tick() {
        // Update the environmental information first
        NativeFactory.getInstance().getEnvironment().tick();

        synchronized (getMascots()) {

            // Add the mascots which should be added
            for (final Mascot mascot : getAdded()) {
                getMascots().add(mascot);
            }
            getAdded().clear();

            // Remove the mascots which should be removed
            for (final Mascot mascot : getRemoved()) {
                getMascots().remove(mascot);
            }
            getRemoved().clear();

            // Advance the mascots' time
            for (final Mascot mascot : getMascots()) {
                mascot.tick();
            }

            // Advance the mascots' images and positions
            for (final Mascot mascot : getMascots()) {
                mascot.apply();
            }
        }

        if (isExitOnLastRemoved() && getMascots().isEmpty()) {
            // exitOnLastRemoved is true and there are no mascots left, so exit.
            Main.getInstance().exit();
        }
    }

    /**
     * Adds a {@link Mascot}.
     * Addition is done at the next {@link #tick()} timing.
     *
     * @param mascot the {@link Mascot} to add
     */
    public void add(final Mascot mascot) {
        synchronized (getAdded()) {
            getAdded().add(mascot);
            getRemoved().remove(mascot);
        }
        mascot.setManager(this);
    }

    /**
     * Removes a {@link Mascot}.
     * Removal is done at the next {@link #tick()} timing.
     *
     * @param mascot the {@link Mascot} to remove
     */
    public void remove(final Mascot mascot) {
        synchronized (getAdded()) {
            getAdded().remove(mascot);
            getRemoved().add(mascot);
        }
        mascot.setManager(null);
        // Clear affordances so the mascot is not participating in any interactions, as that can cause an NPE
        mascot.getAffordances().clear();
    }

    /**
     * Sets the {@link Behavior} for all {@link Mascot Mascots}.
     *
     * @param name the name of the {@link Behavior}
     */
    public void setBehaviorAll(final String name) {
        synchronized (getMascots()) {
            for (final Mascot mascot : getMascots()) {
                try {
                    Configuration configuration = Main.getInstance().getConfiguration(mascot.getImageSet());
                    mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(name), mascot));
                } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                    log.log(Level.SEVERE, "Failed to set behavior to \"" + name + "\" for mascot \"" + mascot + "\"", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), e);
                    mascot.dispose();
                }
            }
        }
    }

    /**
     * Sets the {@link Behavior} for all {@link Mascot Mascots} with the specified image set.
     *
     * @param configuration the {@link Configuration} to use to build the {@link Behavior}
     * @param name the name of the {@link Behavior}
     * @param imageSet the image set for which to check
     */
    public void setBehaviorAll(final Configuration configuration, final String name, String imageSet) {
        synchronized (getMascots()) {
            for (final Mascot mascot : getMascots()) {
                try {
                    if (mascot.getImageSet().equals(imageSet)) {
                        mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(name), mascot));
                    }
                } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                    log.log(Level.SEVERE, "Failed to set behavior to \"" + name + "\" for mascot \"" + mascot + "\"", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), e);
                    mascot.dispose();
                }
            }
        }
    }

    /**
     * Dismisses mascots until one remains.
     */
    public void remainOne() {
        synchronized (getMascots()) {
            int totalMascots = getMascots().size();
            for (int i = totalMascots - 1; i > 0; i--) {
                getMascots().get(i).dispose();
            }
        }
    }

    /**
     * Dismisses all mascots except for the one specified.
     *
     * @param mascot the mascot to not dismiss
     */
    public void remainOne(Mascot mascot) {
        synchronized (getMascots()) {
            int totalMascots = getMascots().size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                if (!getMascots().get(i).equals(mascot)) {
                    getMascots().get(i).dispose();
                }
            }
        }
    }

    /**
     * Dismisses mascots which use the specified image set until one mascot remains.
     *
     * @param imageSet the image set for which to check
     */
    public void remainOne(String imageSet) {
        synchronized (getMascots()) {
            int totalMascots = getMascots().size();
            boolean isFirst = true;
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = getMascots().get(i);
                if (m.getImageSet().equals(imageSet) && isFirst) {
                    isFirst = false;
                } else if (m.getImageSet().equals(imageSet) && !isFirst) {
                    m.dispose();
                }
            }
        }
    }

    /**
     * Dismisses mascots which use the specified image set until only the specified mascot remains.
     *
     * @param imageSet the image set for which to check
     * @param mascot   the mascot to not dismiss
     */
    public void remainOne(String imageSet, Mascot mascot) {
        synchronized (getMascots()) {
            int totalMascots = getMascots().size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = getMascots().get(i);
                if (m.getImageSet().equals(imageSet) && !m.equals(mascot)) {
                    m.dispose();
                }
            }
        }
    }

    /**
     * Dismisses all mascots which use the specified image set.
     *
     * @param imageSet the image set for which to check
     */
    public void remainNone(String imageSet) {
        synchronized (getMascots()) {
            int totalMascots = getMascots().size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = getMascots().get(i);
                if (m.getImageSet().equals(imageSet)) {
                    m.dispose();
                }
            }
        }
    }

    /**
     * Disposes all {@link Mascot Mascots}.
     */
    public void disposeAll() {
        synchronized (getMascots()) {
            for (int i = getMascots().size() - 1; i >= 0; i--) {
                getMascots().get(i).dispose();
            }
        }
    }

    public void togglePauseAll() {
        synchronized (getMascots()) {
            boolean isPaused = getMascots().stream().allMatch(Mascot::isPaused);

            for (final Mascot mascot : getMascots()) {
                mascot.setPaused(!isPaused);
            }
        }
    }

    public boolean isPaused() {
        boolean isPaused;

        synchronized (getMascots()) {
            isPaused = getMascots().stream().allMatch(Mascot::isPaused);
        }

        return isPaused;
    }

    /**
     * Gets the current number of {@link Mascot Mascots}.
     *
     * @return the current number of {@link Mascot Mascots}
     */
    public int getCount() {
        return getCount(null);
    }

    /**
     * Gets the current number of {@link Mascot Mascots} with the given image set.
     *
     * @param imageSet the image set for which to check
     * @return the current number of {@link Mascot Mascots}
     */
    public int getCount(String imageSet) {
        synchronized (getMascots()) {
            if (imageSet == null) {
                return getMascots().size();
            } else {
                return (int) getMascots().stream().filter(m -> m.getImageSet().equals(imageSet)).count();
            }
        }
    }

    private List<Mascot> getMascots() {
        return mascots;
    }

    private Set<Mascot> getAdded() {
        return added;
    }

    private Set<Mascot> getRemoved() {
        return removed;
    }

    /**
     * Returns a Mascot with the given affordance.
     *
     * @param affordance the affordance for which to check
     * @return a {@link WeakReference} to a mascot with the required affordance, or {@code null} if none was found
     */
    public WeakReference<Mascot> getMascotWithAffordance(String affordance) {
        synchronized (getMascots()) {
            for (final Mascot mascot : getMascots()) {
                if (mascot.getAffordances().contains(affordance)) {
                    return new WeakReference<>(mascot);
                }
            }
        }

        return null;
    }

    public boolean hasOverlappingMascotsAtPoint(Point anchor) {
        int count = 0;

        synchronized (getMascots()) {
            for (final Mascot mascot : getMascots()) {
                // TODO Have this account for the entirety of the mascots' windows instead of just a single point
                if (mascot.getAnchor().equals(anchor)) {
                    count++;
                }
                if (count > 1) {
                    return true;
                }
            }
        }

        return false;
    }
}
