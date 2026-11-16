package com.group_finity.mascot;

import com.group_finity.mascot.behavior.BehaviorExecutionException;
import com.group_finity.mascot.config.BehaviorInstantiationException;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.platform.NativeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
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
     * This specifies the duration between the <i>start</i> of one tick and the start of
     * the next, rather than the <i>end</i> of one tick and the start of the next.
     * <p>
     * It is possible for the intervals between some ticks to be longer than this
     * value if a {@code Manager} contains many {@link Mascot} objects through
     * which it needs to iterate.
     */
    public static final int TICK_INTERVAL = 40;

    /**
     * A list of {@link Mascot Mascots} that are managed by this {@code Manager}.
     */
    private final List<Mascot> mascots = new ArrayList<>();

    /**
     * A lock used to allow concurrent access to {@link #mascots}.
     */
    private final ReadWriteLock mascotLock = new ReentrantReadWriteLock();

    /**
     * A queue of tasks that will be run by this {@code Manager} when {@link #tick()} is next called.
     *
     * @see #runPendingTasks()
     * @see #enqueueTask(Runnable)
     * @see #enqueueTask(Runnable, boolean)
     */
    private final ConcurrentLinkedDeque<Runnable> tasks = new ConcurrentLinkedDeque<>();

    /**
     * Whether the program should exit when the last {@link Mascot} is removed from this {@code Manager}.
     * If, for example, it is not possible to access the tray icon menu, then the process will continue running
     * indefinitely unless we terminate the program after the last mascot is removed.
     *
     * @see #isExitOnLastRemoved()
     * @see #setExitOnLastRemoved(boolean)
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
     * Lock used to ensure that calls to {@link #setEnabled(boolean)} only return
     * once the current {@linkplain tick} has finished.
     */
    private final ReadWriteLock enabledLock = new ReentrantReadWriteLock();

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
                        // Use enabledLock to only allow setEnabled() calls to return once the current tick has finished
                        enabledLock.readLock().lock();
                        try {
                            if (enabled) {
                                // Move the mascots.
                                tick();
                            }
                        } catch (RuntimeException e) {
                            log.error("An error occurred while running the tick method.", e);
                        } finally {
                            enabledLock.readLock().unlock();
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
    }

    /**
     * Updates the {@link com.group_finity.mascot.environment.Environment Environment} information
     * and advances all {@link Mascot} objects by one tick.
     */
    private void tick() {
        mascotLock.writeLock().lock();
        try {
            // There may be tasks that perform write operations, so run them whilst holding the write lock
            runPendingTasks();
        } finally {
            mascotLock.writeLock().unlock();
        }

        // Update the environmental information before iterating through the mascots
        NativeFactory.getInstance().getEnvironment().tick();

        boolean noMascots;

        mascotLock.writeLock().lock();
        try {
            if (!mascots.isEmpty()) {
                // Advance the mascots' time
                for (final Mascot mascot : mascots) {
                    mascot.tick();
                }

                // Advance the mascots' images and positions
                for (final Mascot mascot : mascots) {
                    mascot.apply();
                }
            }

            // There may be tasks that perform write operations, so run them whilst holding the write lock
            runPendingTasks();

            noMascots = mascots.isEmpty();
        } finally {
            mascotLock.writeLock().unlock();
        }

        if (exitOnLastRemoved && noMascots) {
            // exitOnLastRemoved is true and there are no mascots left, so exit.
            Main.getInstance().exit();
        }
    }

    /**
     * Runs all currently enqueued tasks.
     */
    private void runPendingTasks() {
        while (!tasks.isEmpty()) {
            tasks.removeFirst().run();
        }
    }

    /**
     * Adds the specified runnable to this {@code Manager} object's queue of tasks, and runs it on this
     * {@code Manager} object's thread after all previous tasks have been run.
     * <p>
     * If this {@code Manager} is disabled or has not been started, the specified task will be run immediately.
     *
     * @param task the task to execute on this {@code Manager} object's thread
     */
    public void enqueueTask(Runnable task) {
        enqueueTask(task, false);
    }

    /**
     * Adds the specified runnable to this {@code Manager} object's queue of tasks, and runs it on this
     * {@code Manager} object's thread after all previous tasks have been run.
     * <p>
     * If this {@code Manager} is disabled or has not been started, or if {@code bypassQueue} is {@code true}
     * and this method is being invoked from this {@code Manager} object's thread, the specified task
     * will be run immediately.
     *
     * @param task the task to execute on this {@code Manager} object's thread
     * @param bypassQueue whether to run {@code task} immediately if this method is being invoked from this
     * {@code Manager} object's thread, regardless of whether this {@code Manager} is enabled and currently running.
     * This is mainly used when performing read-only operations which have no risk of throwing
     * {@code ConcurrentModificationException} or causing other unexpected behavior.
     */
    private void enqueueTask(Runnable task, boolean bypassQueue) {
        enabledLock.readLock().lock();
        try {
            if (!enabled || thread == null || bypassQueue && Thread.currentThread() == thread) {
                task.run();
                return;
            }
        } finally {
            enabledLock.readLock().unlock();
        }

        tasks.add(task);
    }

    /**
     * Adds a {@link Mascot} to this {@code Manager}.
     * <p>
     * If this {@code Manager} is disabled or has not started running, then the addition
     * will be done during the next call to {@link #tick()}.
     *
     * @param mascot the {@code Mascot} to add
     */
    public void add(final Mascot mascot) {
        Manager oldManager = mascot.getManager();
        // Don't do anything if the mascot is already managed by this manager
        if (equals(oldManager)) {
            return;
        }

        // If the mascot was managed by another manager, remove it from that manager
        if (oldManager != null) {
            oldManager.remove(mascot);
        }

        enqueueTask(() -> {
            mascotLock.writeLock().lock();
            try {
                mascot.setManager(this);
                if (!mascots.contains(mascot)) {
                    mascots.add(mascot);
                }
            } finally {
                mascotLock.writeLock().unlock();
            }
        });
    }

    /**
     * Removes a {@link Mascot} from this {@code Manager}.
     * <p>
     * If this {@code Manager} is disabled or has not started running, then the removal
     * will be done during the next call to {@link #tick()}.
     *
     * @param mascot the {@code Mascot} to remove
     */
    public void remove(final Mascot mascot) {
        enqueueTask(() -> {
            mascotLock.writeLock().lock();
            try {
                // Don't do anything if the mascot is not managed by this manager
                if (!equals(mascot.getManager())) {
                    return;
                }

                mascot.setManager(null);
                mascots.remove(mascot);
            } finally {
                mascotLock.writeLock().unlock();
            }
        });
    }

    /**
     * Applies the specified behavior to all mascots in this {@code Manager}.
     * If the specified behavior fails to be applied to a mascot, that mascot is disposed.
     *
     * @param name the name of the behavior to apply to all mascots
     */
    public void setBehaviorAll(final String name) {
        enqueueTask(() -> {
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
        });
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
        enqueueTask(() -> {
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
        });
    }

    /**
     * Dismisses mascots until one remains.
     * The remaining mascot will be the first mascot in this {@code Manager} object's internal list of mascots.
     */
    public void remainOne() {
        enqueueTask(() -> {
            mascotLock.writeLock().lock();
            try {
                int totalMascots = mascots.size();
                for (int i = totalMascots - 1; i > 0; i--) {
                    mascots.get(i).dispose();
                }
            } finally {
                mascotLock.writeLock().unlock();
            }
        });
    }

    /**
     * Dismisses all mascots except for the one specified.
     *
     * @param mascot the mascot to retain
     */
    public void remainOne(Mascot mascot) {
        enqueueTask(() -> {
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
        }, true);
    }

    /**
     * Dismisses mascots that use the specified image set until one mascot with that image set remains.
     * The remaining mascot will be the first mascot with the specified image set in this {@code Manager}
     * object's internal list of mascots.
     *
     * @param imageSet the name of the image set whose mascots should be disposed
     */
    public void remainOne(String imageSet) {
        enqueueTask(() -> {
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
        }, true);
    }

    /**
     * Dismisses mascots that use the specified image set until only the specified mascot remains.
     *
     * @param imageSet the image set for which to check
     * @param mascot the mascot to retain
     */
    public void remainOne(String imageSet, Mascot mascot) {
        enqueueTask(() -> {
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
        }, true);
    }

    /**
     * Dismisses all mascots that use the specified image set.
     *
     * @param imageSet the image set for which to check
     */
    public void remainNone(String imageSet) {
        enqueueTask(() -> {
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
        }, true);
    }

    /**
     * Marks all mascots as dirty, meaning they will be repainted as soon as possible.
     *
     * @see Mascot#markDirty()
     */
    public void markAllDirty() {
        enqueueTask(() -> {
            mascotLock.writeLock().lock();
            try {
                for (Mascot mascot : mascots) {
                    mascot.markDirty();
                }
            } finally {
                mascotLock.writeLock().unlock();
            }
        }, true);
    }

    /**
     * Disposes all mascots in this {@code Manager}.
     */
    public void disposeAll() {
        enqueueTask(() -> {
            mascotLock.writeLock().lock();
            try {
                for (int i = mascots.size() - 1; i >= 0; i--) {
                    mascots.get(i).dispose();
                }
            } finally {
                mascotLock.writeLock().unlock();
            }
        });
    }

    /**
     * Checks whether all mascots in this {@code Manager} are paused.
     * Returns {@code false} if this {@code Manager} has no mascots.
     *
     * @return {@code true} if all mascots in this manager are paused; {@code false} if any mascot is unpaused
     * or if this {@code Manager} has no mascots
     */
    public boolean isPaused() {
        RunnableFuture<Boolean> future = new FutureTask<>(() -> {
            mascotLock.readLock().lock();
            try {
                if (mascots.isEmpty()) {
                    return false;
                }
                return mascots.stream().allMatch(Mascot::isPaused);
            } finally {
                mascotLock.readLock().unlock();
            }
        });
        enqueueTask(future, true);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Toggles the paused state of all mascots in this {@code Manager}.
     */
    public void togglePauseAll() {
        enqueueTask(() -> {
            mascotLock.writeLock().lock();
            try {
                if (mascots.isEmpty()) {
                    return;
                }

                boolean isPaused = mascots.stream().allMatch(Mascot::isPaused);

                for (final Mascot mascot : mascots) {
                    mascot.setPaused(!isPaused);
                }
            } finally {
                mascotLock.writeLock().unlock();
            }
        });
    }

    /**
     * Gets whether this {@code Manager} is enabled. If this {@code Manager} is enabled, it can update the
     * environment's information and advance the timings of its mascots.
     *
     * @return {@code true} if this {@code Manager} is enabled; {@code false} otherwise
     */
    public boolean isEnabled() {
        enabledLock.readLock().lock();
        try {
            return enabled;
        } finally {
            enabledLock.readLock().unlock();
        }
    }

    /**
     * Sets whether this {@code Manager} is enabled. If this {@code Manager} is enabled, it can update the
     * environment's information and advance the timings of its mascots.
     *
     * @param enabled {@code true} to enable this {@code Manager}; {@code false} to disable it
     */
    public void setEnabled(boolean enabled) {
        // Only allow setEnabled() calls to return once the current tick has finished
        enabledLock.writeLock().lock();
        try {
            this.enabled = enabled;
        } finally {
            enabledLock.writeLock().unlock();
        }
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
        RunnableFuture<Integer> future = new FutureTask<>(() -> {
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
        });
        enqueueTask(future, true);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
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
        RunnableFuture<WeakReference<Mascot>> future = new FutureTask<>(() -> {
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
        });
        enqueueTask(future, true);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks whether there are at least two mascots whose anchors are at the specified point.
     *
     * @param anchor the point to check
     * @return whether there are at least two mascots whose anchors are at the specified point
     */
    public boolean hasOverlappingMascotsAtPoint(Point anchor) {
        RunnableFuture<Boolean> future = new FutureTask<>(() -> {
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
        });
        enqueueTask(future, true);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
