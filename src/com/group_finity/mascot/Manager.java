package com.group_finity.mascot;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maintains a list of mascot, the object to time.
 * <p>
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public class Manager {

    private static final Logger log = Logger.getLogger(Manager.class.getName());

    /**
     * Interval timer is running.
     */
    public static final int TICK_INTERVAL = 40;

    /**
     * A list of mascots.
     */
    private final List<Mascot> mascots = new ArrayList<>();

    /**
     * The mascot will be added later.
     * {@link ConcurrentModificationException} to prevent the addition of the mascot {@link #tick()} are each simultaneously reflecting.
     */
    private final Set<Mascot> added = new LinkedHashSet<>();

    /**
     * The mascot will be added later.
     * {@link ConcurrentModificationException} to prevent the deletion of the mascot {@link #tick()} are each simultaneously reflecting.
     */
    private final Set<Mascot> removed = new LinkedHashSet<>();

    private boolean exitOnLastRemoved = true;

    private Thread thread;

    public void setExitOnLastRemoved(boolean exitOnLastRemoved) {
        this.exitOnLastRemoved = exitOnLastRemoved;
    }

    public boolean isExitOnLastRemoved() {
        return exitOnLastRemoved;
    }

    public void start() {
        if (thread != null && thread.isAlive()) {
            return;
        }

        thread = new Thread(() -> {
            long prev = System.nanoTime() / 1000000;
            try {
                while (true) {
                    final long cur = System.nanoTime() / 1000000;
                    if (cur - prev >= TICK_INTERVAL) {
                        if (cur > prev + TICK_INTERVAL * 2) {
                            prev = cur;
                        } else {
                            prev += TICK_INTERVAL;
                        }
                        tick();
                        continue;
                    }
                    Thread.sleep(1, 0);
                }
            } catch (final InterruptedException ignored) {
            }
        }, "Ticker");
        thread.setDaemon(false);
        thread.start();
    }

    public void stop() {
        if (thread == null || !thread.isAlive()) {
            return;
        }
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException ignored) {
        }
    }

    private void tick() {
        // Update the first environmental information
        NativeFactory.getInstance().getEnvironment().tick();

        synchronized (getMascots()) {

            // Add the mascot if it should be added
            for (final Mascot mascot : getAdded()) {
                getMascots().add(mascot);
            }
            getAdded().clear();

            // Remove the mascot if it should be removed
            for (final Mascot mascot : getRemoved()) {
                getMascots().remove(mascot);
            }
            getRemoved().clear();

            // Advance mascot's time
            for (final Mascot mascot : getMascots()) {
                mascot.tick();
            }

            // Advance mascot's time
            for (final Mascot mascot : getMascots()) {
                mascot.apply();
            }
        }

        if (isExitOnLastRemoved()) {
            if (getMascots().isEmpty()) {
                Main.getInstance().exit();
            }
        }
    }

    public void add(final Mascot mascot) {
        synchronized (getAdded()) {
            getAdded().add(mascot);
            getRemoved().remove(mascot);
        }
        mascot.setManager(this);
    }

    public void remove(final Mascot mascot) {
        synchronized (getAdded()) {
            getAdded().remove(mascot);
            getRemoved().add(mascot);
        }
        mascot.setManager(null);
    }

    public void setBehaviorAll(final String name) {
        synchronized (getMascots()) {
            for (final Mascot mascot : getMascots()) {
                try {
                    Configuration configuration = Main.getInstance().getConfiguration(mascot.getImageSet());
                    mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(name)));
                } catch (final BehaviorInstantiationException e) {
                    log.log(Level.SEVERE, "Failed to initialize the following actions", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                    mascot.dispose();
                } catch (final CantBeAliveException e) {
                    log.log(Level.SEVERE, "Fatal Error", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                    mascot.dispose();
                }
            }
        }
    }

    public void setBehaviorAll(final Configuration configuration, final String name, String imageSet) {
        synchronized (getMascots()) {
            for (final Mascot mascot : getMascots()) {
                try {
                    if (mascot.getImageSet().equals(imageSet)) {
                        mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(name)));
                    }
                } catch (final BehaviorInstantiationException e) {
                    log.log(Level.SEVERE, "Failed to initialize the following actions", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                    mascot.dispose();
                } catch (final CantBeAliveException e) {
                    log.log(Level.SEVERE, "Fatal Error", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
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
            for (int i = totalMascots - 1; i > 0; --i) {
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
            for (int i = totalMascots - 1; i >= 0; --i) {
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
            for (int i = totalMascots - 1; i >= 0; --i) {
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
            for (int i = totalMascots - 1; i >= 0; --i) {
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
            for (int i = totalMascots - 1; i >= 0; --i) {
                Mascot m = getMascots().get(i);
                if (m.getImageSet().equals(imageSet)) {
                    m.dispose();
                }
            }
        }
    }

    public void togglePauseAll() {
        synchronized (getMascots()) {
            boolean isPaused = false;
            if (!getMascots().isEmpty()) {
                isPaused = getMascots().get(0).isPaused();
            }

            for (final Mascot mascot : getMascots()) {
                mascot.setPaused(!isPaused);
            }
        }
    }

    public boolean isPaused() {
        boolean isPaused = false;

        synchronized (getMascots()) {
            if (!getMascots().isEmpty()) {
                isPaused = getMascots().get(0).isPaused();
            }
        }

        return isPaused;
    }

    public int getCount() {
        return getCount(null);
    }

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
     * @param affordance
     * @return A WeakReference to a mascot with the required affordance, or null
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

    public void disposeAll() {
        synchronized (getMascots()) {
            for (int i = getMascots().size() - 1; i >= 0; --i) {
                getMascots().get(i).dispose();
            }
        }
    }
}
