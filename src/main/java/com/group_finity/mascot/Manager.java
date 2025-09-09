package com.group_finity.mascot;

import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An object that manages the list of {@link Mascot Mascots} and takes timing.
 * If each {@link Mascot} moves asynchronously, there will be various problems (such as when throwing a window),
 * so this class adjusts the timing of the entire mascot.
 * <p>
 * The {@link #tick()} method first retrieves the latest environment information and then moves all {@link Mascot Mascots}.
 *
 * @author Yuki Yamada
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

    /**
     * Thread that loops {@link #tick()}.
     */
    private Thread thread;

    public Manager() {
        // This is to fix a bug in Java running on Windows
        // Frequent calls to Thread.sleep with short lengths will mess up the Windows clock
        // You can avoid this problem by calling long Thread.sleep.
        new Thread() {
            {
                setDaemon(true);
                start();
            }

            @Override
            public void run() {
                while (true) {
                    try {
                        sleep(Integer.MAX_VALUE);
                    } catch (final InterruptedException ignored) {
                    }
                }
            }
        };
    }

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
        if (thread != null && thread.isAlive()) {
            // Thread is already running
            return;
        }

        thread = new Thread(() -> {
            // I think nanoTime() is used instead of currentTimeMillis() because it may be more accurate on some systems that way.

            // Previous time
            long prev = System.nanoTime() / 1000000;
            try {
                while (true) {
                    // Current time
                    // Loop until TICK_INTERVAL has passed.
                    final long cur = System.nanoTime() / 1000000;
                    if (cur - prev >= TICK_INTERVAL) {
                        if (cur > prev + TICK_INTERVAL * 2) {
                            prev = cur;
                        } else {
                            prev += TICK_INTERVAL;
                        }
                        // Move the mascots.
                        tick();
                        continue;
                    }
                    Thread.sleep(1, 0);
                }
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, "Ticker");
        thread.setDaemon(false);
        thread.start();
    }

    /**
     * Stops the thread.
     */
    public void stop() {
        if (thread == null || !thread.isAlive()) {
            // Thread is no longer running
            return;
        }
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Advances the {@link Mascot Mascots} by one frame.
     */
    private void tick() {
        // Update the environmental information first
        NativeFactory.getInstance().getEnvironment().tick();

        synchronized (mascots) {
            // Add the mascots which should be added
            mascots.addAll(added);
            added.clear();

            // Remove the mascots which should be removed
            for (final Mascot mascot : removed) {
                mascots.remove(mascot);
            }
            removed.clear();

            // Advance the mascots' time
            for (final Mascot mascot : mascots) {
                mascot.tick();
            }

            // Advance the mascots' images and positions
            for (final Mascot mascot : mascots) {
                mascot.apply();
            }
        }

        if (exitOnLastRemoved && mascots.isEmpty()) {
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
        synchronized (added) {
            added.add(mascot);
            removed.remove(mascot);
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
        synchronized (added) {
            added.remove(mascot);
            removed.add(mascot);
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
        synchronized (mascots) {
            for (final Mascot mascot : mascots) {
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
        synchronized (mascots) {
            for (final Mascot mascot : mascots) {
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
        synchronized (mascots) {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i > 0; i--) {
                mascots.get(i).dispose();
            }
        }
    }

    /**
     * Dismisses all mascots except for the one specified.
     *
     * @param mascot the mascot to not dismiss
     */
    public void remainOne(Mascot mascot) {
        synchronized (mascots) {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                if (!mascots.get(i).equals(mascot)) {
                    mascots.get(i).dispose();
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
        synchronized (mascots) {
            int totalMascots = mascots.size();
            boolean isFirst = true;
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = mascots.get(i);
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
     * @param mascot the mascot to not dismiss
     */
    public void remainOne(String imageSet, Mascot mascot) {
        synchronized (mascots) {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = mascots.get(i);
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
        synchronized (mascots) {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = mascots.get(i);
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
        synchronized (mascots) {
            for (int i = mascots.size() - 1; i >= 0; i--) {
                mascots.get(i).dispose();
            }
        }
    }

    public void togglePauseAll() {
        synchronized (mascots) {
            boolean isPaused = mascots.stream().allMatch(Mascot::isPaused);

            for (final Mascot mascot : mascots) {
                mascot.setPaused(!isPaused);
            }
        }
    }

    public boolean isPaused() {
        synchronized (mascots) {
            return mascots.stream().allMatch(Mascot::isPaused);
        }
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
        synchronized (mascots) {
            if (imageSet == null) {
                return mascots.size();
            } else {
                return (int) mascots.stream().filter(m -> m.getImageSet().equals(imageSet)).count();
            }
        }
    }

    /**
     * Returns a Mascot with the given affordance.
     *
     * @param affordance the affordance for which to check
     * @return a {@link WeakReference} to a mascot with the required affordance, or {@code null} if none was found
     */
    public WeakReference<Mascot> getMascotWithAffordance(String affordance) {
        synchronized (mascots) {
            for (final Mascot mascot : mascots) {
                if (mascot.getAffordances().contains(affordance)) {
                    return new WeakReference<>(mascot);
                }
            }
        }

        return null;
    }

    public boolean hasOverlappingMascotsAtPoint(Point anchor) {
        int count = 0;

        synchronized (mascots) {
            for (final Mascot mascot : mascots) {
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
