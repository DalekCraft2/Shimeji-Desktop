package com.group_finity.mascot;

import com.group_finity.mascot.behavior.BehaviorExecutionException;
import com.group_finity.mascot.config.BehaviorInstantiationException;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.platform.NativeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An object that manages a list of {@link Mascot} objects and coordinates their timing.
 * Since having each mascot move asynchronously would cause issues (such as when moving a window),
 * this class synchronizes their overall timing.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Manager {
    private static final Logger log = LoggerFactory.getLogger(Manager.class);

    /**
     * The minimum interval between calls to {@link #tick()}, in milliseconds.
     * This specifies the duration between the start of one tick and the start of
     * the next, rather than the end of one tick and the start of the next.
     * <p>
     * It is possible for the intervals between some ticks to be longer than this
     * value if this {@code Manager} contains many {@link Mascot} objects through
     * which it needs to iterate.
     */
    public static final int TICK_INTERVAL = 40;

    /**
     * A list of {@link Mascot Mascots} that are managed by this {@code Manager}.
     */
    private final List<Mascot> mascots = new ArrayList<>();

    /**
     * The {@link Mascot} objects that should be added to this {@code Manager}.
     * To prevent {@link ConcurrentModificationException}, {@link Mascot} additions are reflected all at once
     * whenever {@link #tick()} is called.
     *
     * @see #add(Mascot)
     */
    private final Set<Mascot> added = new LinkedHashSet<>();

    /**
     * The {@link Mascot} objects that should be removed from this {@code Manager}.
     * To prevent {@link ConcurrentModificationException}, {@link Mascot} removals are reflected all at once
     * whenever {@link #tick()} is called.
     *
     * @see #remove(Mascot)
     */
    private final Set<Mascot> removed = new LinkedHashSet<>();

    /**
     * A lock used to allow concurrent access to {@link #mascots}.
     */
    private final ReadWriteLock mascotLock = new ReentrantReadWriteLock();

    /**
     * Whether the program should exit when the last {@link Mascot} is removed from this {@code Manager}.
     * If, for example, it is not possible to access the tray icon menu, then the process will continue running
     * indefinitely unless we terminate the program after the last mascot is removed.
     */
    private boolean exitOnLastRemoved = true;

    /**
     * Whether this {@code Manager} is enabled. If this {@code Manager} is enabled, it can update the
     * environment's information and advance the timings of its mascots. The internal thread of this {@code Manager}
     * will continue running regardless of the value of this field, but it will not call {@link #tick()}
     * if this is set to {@code false}.
     *
     * @see #isEnabled()
     * @see #setEnabled(boolean)
     */
    private boolean enabled = true;

    /**
     * Thread that calls {@link #tick()} every {@value #TICK_INTERVAL} milliseconds.
     */
    private Thread thread;

    /**
     * Creates a new {@code Manager}.
     */
    public Manager() {
        /*
        JDK bug: https://bugs.openjdk.org/browse/JDK-6435126
        This is a workaround designed to fix a Java bug on Windows.
        Frequently calling `Thread.sleep` with short durations causes the Windows system clock to drift.
        Calling `Thread.sleep` with longer durations allows you to avoid this issue.
         */
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

    /**
     * Gets whether the program should exit when the last {@link Mascot} is removed from this {@code Manager}.
     *
     * @return {@code true} if the program should exit when the last {@code Mascot} is removed; {@code false} otherwise
     */
    public boolean isExitOnLastRemoved() {
        return exitOnLastRemoved;
    }

    /**
     * Sets whether the program should exit when the last {@link Mascot} is removed from this {@code Manager}.
     *
     * @param exitOnLastRemoved {@code true} to make the program exit when the last {@code Mascot} is removed;
     * {@code false} to keep the program running
     */
    public void setExitOnLastRemoved(boolean exitOnLastRemoved) {
        this.exitOnLastRemoved = exitOnLastRemoved;
    }

    /**
     * Starts the internal thread of this {@code Manager} so it may begin updating the timings of its mascots.
     */
    public void start() {
        if (thread != null && thread.isAlive()) {
            // Thread is already running
            return;
        }

        thread = new Thread(() -> {
            // I think nanoTime() is used instead of currentTimeMillis()
            // because it may be more accurate on some systems that way.

            // Previous time
            long prev = System.nanoTime() / 1000000;
            // Current time
            long cur;
            try {
                while (true) {
                    // Loop until TICK_INTERVAL has passed.
                    cur = System.nanoTime() / 1000000;
                    if (cur - prev >= TICK_INTERVAL) {
                        if (cur <= prev + TICK_INTERVAL * 2) {
                            // If the current time is behind by multiple increments of TICK_INTERVAL,
                            // increment the previous time by TICK_INTERVAL so we do two ticks back-to-back
                            prev += TICK_INTERVAL;
                        } else {
                            prev = cur;
                        }
                        if (enabled) {
                            try {
                                // Move the mascots.
                                tick();
                            } catch (RuntimeException e) {
                                log.error("An error occurred while running the tick method.", e);
                            }
                        }
                        continue;
                    }
                    Thread.sleep(1, 0);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Ticker");
        thread.setDaemon(false);
        thread.start();
    }

    /**
     * Stops the internal thread of this {@code Manager} so it no longer updates the timings of its mascots.
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
     * Updates the {@link com.group_finity.mascot.environment.Environment Environment} information
     * and advances all {@link Mascot} objects by one tick.
     */
    private void tick() {
        // Update the environmental information first
        NativeFactory.getInstance().getEnvironment().tick();

        boolean noMascots;

        mascotLock.writeLock().lock();
        try {
            synchronized (added) {
                // Add the mascots that should be added
                if (!added.isEmpty()) {
                    mascots.addAll(added);
                    added.clear();
                }

                // Remove the mascots that should be removed
                if (!removed.isEmpty()) {
                    mascots.removeAll(removed);
                    removed.clear();
                }
            }

            noMascots = mascots.isEmpty();

            if (!noMascots) {
                // Advance the mascots' time
                for (final Mascot mascot : mascots) {
                    mascot.tick();
                }

                // Advance the mascots' images and positions
                for (final Mascot mascot : mascots) {
                    mascot.apply();
                }
            }
        } finally {
            mascotLock.writeLock().unlock();
        }

        if (exitOnLastRemoved && noMascots) {
            // exitOnLastRemoved is true and there are no mascots left, so exit.
            Main.getInstance().exit();
        }
    }

    /**
     * Adds a {@link Mascot} to this {@code Manager}.
     * Addition is done during the next call to {@link #tick()}.
     *
     * @param mascot the {@code Mascot} to add
     */
    public void add(final Mascot mascot) {
        synchronized (added) {
            added.add(mascot);
            removed.remove(mascot);
        }
        mascot.setManager(this);
    }

    /**
     * Removes a {@link Mascot} from this {@code Manager}.
     * Removal is done during the next call to {@link #tick()}.
     *
     * @param mascot the {@code Mascot} to remove
     */
    public void remove(final Mascot mascot) {
        synchronized (added) {
            added.remove(mascot);
            removed.add(mascot);
        }
        mascot.setManager(null);
    }

    /**
     * Applies the specified behavior to all mascots in this {@code Manager}.
     * If the specified behavior fails to be applied to a mascot, that mascot is disposed.
     *
     * @param name the name of the behavior to apply to all mascots
     */
    public void setBehaviorAll(final String name) {
        mascotLock.writeLock().lock();
        try {
            if (mascots.isEmpty()) {
                return;
            }
            for (final Mascot mascot : mascots) {
                Configuration configuration = Main.getInstance().getConfiguration(mascot.getImageSet());
                try {
                    mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(name), mascot));
                } catch (final BehaviorInstantiationException | BehaviorExecutionException e) {
                    log.error("Failed to set behavior to \"{}\" for mascot \"{}\"", name, mascot, e);
                    Main.showError(String.format(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), name, mascot), e);
                    mascot.dispose();
                }
            }
        } finally {
            mascotLock.writeLock().unlock();
        }
    }

    /**
     * Applies the specified behavior to all mascots in this {@code Manager} that use the specified image set.
     * If the specified behavior fails to be applied to a mascot, that mascot is disposed.
     *
     * @param configuration the configuration to use to build the behavior with the specified name
     * @param name the name of the behavior to apply to all mascots with the specified image set
     * @param imageSet the name of the image set for which to check
     */
    public void setBehaviorAll(final Configuration configuration, final String name, String imageSet) {
        mascotLock.writeLock().lock();
        try {
            if (mascots.isEmpty()) {
                return;
            }
            for (final Mascot mascot : mascots) {
                try {
                    if (mascot.getImageSet().equals(imageSet)) {
                        mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(name), mascot));
                    }
                } catch (final BehaviorInstantiationException | BehaviorExecutionException e) {
                    log.error("Failed to set behavior to \"{}\" for mascot \"{}\"", name, mascot, e);
                    Main.showError(String.format(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), name, mascot), e);
                    mascot.dispose();
                }
            }
        } finally {
            mascotLock.writeLock().unlock();
        }
    }

    /**
     * Dismisses mascots until one remains.
     * The remaining mascot will be the first mascot in this {@code Manager} object's internal list of mascots.
     */
    public void remainOne() {
        mascotLock.writeLock().lock();
        try {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i > 0; i--) {
                mascots.get(i).dispose();
            }
        } finally {
            mascotLock.writeLock().unlock();
        }
    }

    /**
     * Dismisses all mascots except for the one specified.
     *
     * @param mascot the mascot to retain
     */
    public void remainOne(Mascot mascot) {
        mascotLock.writeLock().lock();
        try {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = mascots.get(i);
                if (!m.equals(mascot)) {
                    m.dispose();
                }
            }
        } finally {
            mascotLock.writeLock().unlock();
        }
    }

    /**
     * Dismisses mascots that use the specified image set until one mascot with that image set remains.
     * The remaining mascot will be the first mascot with the specified image set in this {@code Manager}
     * object's internal list of mascots.
     *
     * @param imageSet the name of the image set whose mascots should be disposed
     */
    public void remainOne(String imageSet) {
        mascotLock.writeLock().lock();
        try {
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
        } finally {
            mascotLock.writeLock().unlock();
        }
    }

    /**
     * Dismisses mascots that use the specified image set until only the specified mascot remains.
     *
     * @param imageSet the image set for which to check
     * @param mascot the mascot to retain
     */
    public void remainOne(String imageSet, Mascot mascot) {
        mascotLock.writeLock().lock();
        try {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = mascots.get(i);
                if (m.getImageSet().equals(imageSet) && !m.equals(mascot)) {
                    m.dispose();
                }
            }
        } finally {
            mascotLock.writeLock().unlock();
        }
    }

    /**
     * Dismisses all mascots that use the specified image set.
     *
     * @param imageSet the image set for which to check
     */
    public void remainNone(String imageSet) {
        mascotLock.writeLock().lock();
        try {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = mascots.get(i);
                if (m.getImageSet().equals(imageSet)) {
                    m.dispose();
                }
            }
        } finally {
            mascotLock.writeLock().unlock();
        }
    }

    /**
     * Disposes all mascots in this {@code Manager}.
     */
    public void disposeAll() {
        mascotLock.writeLock().lock();
        try {
            for (int i = mascots.size() - 1; i >= 0; i--) {
                mascots.get(i).dispose();
            }
        } finally {
            mascotLock.writeLock().unlock();
        }
    }

    /**
     * Checks whether all mascots in this {@code Manager} are paused.
     * Returns {@code false} if this {@code Manager} has no mascots.
     *
     * @return {@code true} if all mascots in this manager are paused; {@code false} if any mascot is unpaused
     * or if this {@code Manager} has no mascots
     */
    public boolean isPaused() {
        mascotLock.readLock().lock();
        try {
            if (mascots.isEmpty()) {
                return false;
            }
            return mascots.stream().allMatch(Mascot::isPaused);
        } finally {
            mascotLock.readLock().unlock();
        }
    }

    /**
     * Toggles the paused state of all mascots in this {@code Manager}.
     */
    public void togglePauseAll() {
        mascotLock.writeLock().lock();
        try {
            if (mascots.isEmpty()) {
                return;
            }

            boolean isPaused = mascots.stream().allMatch(Mascot::isPaused);

            for (final Mascot mascot : mascots) {
                mascot.setPausedNoCallback(!isPaused);
            }
        } finally {
            mascotLock.writeLock().unlock();
        }
    }

    /**
     * Gets whether this {@code Manager} is enabled. If this {@code Manager} is enabled, it can update the
     * environment's information and advance the timings of its mascots.
     *
     * @return {@code true} if this {@code Manager} is enabled; {@code false} otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether this {@code Manager} is enabled. If this {@code Manager} is enabled, it can update the
     * environment's information and advance the timings of its mascots.
     *
     * @param enabled {@code true} to enable this {@code Manager}; {@code false} to disable it
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the total number of mascots in this {@code Manager}.
     *
     * @return the total number of mascots in this {@code Manager}
     */
    public int getCount() {
        return getCount(null);
    }

    /**
     * Gets the number of mascots in this {@code Manager} that use the specified image set.
     * If the specified image set is {@code null}, this returns the total number of mascots in this {@code Manager}.
     *
     * @param imageSet the image set for which to check, or {@code null} to get the total number of mascots
     * in this {@code Manager}
     * @return the number of mascots that use the specified image set
     */
    public int getCount(String imageSet) {
        mascotLock.readLock().lock();
        try {
            if (mascots.isEmpty()) {
                return 0;
            }

            if (imageSet == null) {
                return mascots.size();
            } else {
                return (int) mascots.stream().filter(m -> m.getImageSet().equals(imageSet)).count();
            }
        } finally {
            mascotLock.readLock().unlock();
        }
    }

    /**
     * Gets a weak reference to a mascot that is currently broadcasting the specified affordance.
     *
     * @param affordance the affordance for which to check
     * @return a {@link WeakReference} to a mascot that is broadcasting the specified affordance,
     * or {@code null} if none was found
     */
    public WeakReference<Mascot> getMascotWithAffordance(String affordance) {
        mascotLock.readLock().lock();
        try {
            if (!mascots.isEmpty()) {
                for (final Mascot mascot : mascots) {
                    if (mascot.getAffordances().contains(affordance)) {
                        return new WeakReference<>(mascot);
                    }
                }
            }
        } finally {
            mascotLock.readLock().unlock();
        }

        return null;
    }

    /**
     * Checks whether there are at least two mascots whose anchors are at the specified point.
     *
     * @param anchor the point to check
     * @return whether there are at least two mascots whose anchors are at the specified point
     */
    public boolean hasOverlappingMascotsAtPoint(Point anchor) {
        int count = 0;

        mascotLock.readLock().lock();
        try {
            if (!mascots.isEmpty()) {
                for (final Mascot mascot : mascots) {
                    if (mascot.getAnchor().equals(anchor)) {
                        count++;
                    }
                    if (count > 1) {
                        return true;
                    }
                }
            }
        } finally {
            mascotLock.readLock().unlock();
        }

        return false;
    }
}
