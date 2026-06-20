package com.group_finity.mascot.script;

import com.group_finity.mascot.Main;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.CompiledScript;
import javax.script.ScriptException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An implementation of {@link Variable} that represents a scripted value. The value can be evaluated by being supplied
 * with a {@link VariableMap}, and can optionally be reevaluated at the start of each frame.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Script extends Variable {

    /**
     * The script engine used to compile all scripts.
     */
    private static final NashornScriptEngine ENGINE = (NashornScriptEngine) new NashornScriptEngineFactory().getScriptEngine(className -> false);

    /**
     * The source that was used to compile this script.
     */
    private final String source;

    /**
     * Whether this script's cached value should be cleared at the start of each frame, forcing it to be
     * reevaluated.
     *
     * @see #resetValue()
     */
    private final boolean allowValueReset;

    /**
     * The compiled script object that is used to evaluate this script's value.
     */
    private final CompiledScript compiled;

    /**
     * The value of this script. Is evaluated at most once per frame, and is set to {@code null} at the start of each
     * frame if {@link #allowValueReset} is {@code true}.
     *
     * @see #init()
     * @see #resetValue()
     * @see #get(VariableMap)
     */
    private Object value;

    /**
     * A lock used to allow concurrent access to {@link #value}.
     */
    private final ReadWriteLock valueLock = new ReentrantReadWriteLock();

    /**
     * Whether {@link #value} needs to be reevaluated.
     * <p>
     * It is better to use a boolean field to check this rather than checking whether {@code value == null},
     * because it is possible that {@code value} could evaluate to {@code null}. If {@link #get(VariableMap)}
     * were called after {@code value} had evaluated to {@code null}, and the method used a {@code null} check
     * to determine whether to reevaluate it, then it would try to reevaluate it a second time, which is undesired.
     */
    private boolean needsReevaluation = true;

    /**
     * Creates a new Script.
     *
     * @param source the source that will be compiled into a script
     * @param allowValueReset whether this script's cached value should be cleared at the start of each frame,
     * forcing it to be reevaluated
     * @throws VariableException if {@code source} is in script syntax but is not compilable
     */
    public Script(final String source, final boolean allowValueReset) throws VariableException {
        this.source = source;
        this.allowValueReset = allowValueReset;
        try {
            compiled = ENGINE.compile(this.source);
        } catch (final ScriptException e) {
            throw new VariableException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "ScriptCompilationErrorMessage"), this.source), e);
        }
    }

    @Override
    public String toString() {
        return (allowValueReset ? "#{" : "${") + source + '}';
    }

    @Override
    public void init() {
        valueLock.writeLock().lock();
        try {
            value = null;
            needsReevaluation = true;
        } finally {
            valueLock.writeLock().unlock();
        }
    }

    /**
     * Clears the cached value of this script if {@link #allowValueReset} is {@code true},
     * so the value may be reevaluated when {@link #get(VariableMap)} is next called.
     * Called at the start of each frame.
     *
     * @see #get(VariableMap)
     */
    @Override
    public void resetValue() {
        if (allowValueReset) {
            valueLock.writeLock().lock();
            try {
                value = null;
                needsReevaluation = true;
            } finally {
                valueLock.writeLock().unlock();
            }
        }
    }

    @Override
    public Object get(final VariableMap variables) throws VariableException {
        valueLock.readLock().lock();
        try {
            if (!needsReevaluation) {
                return value;
            }
        } finally {
            valueLock.readLock().unlock();
        }

        valueLock.writeLock().lock();
        try {
            // Check needsReevaluation again, in case another thread
            // acquired the lock and modified the value before we could
            if (!needsReevaluation) {
                return value;
            }

            value = compiled.eval(variables);
            needsReevaluation = false;
        } catch (final ScriptException e) {
            throw new VariableException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "ScriptEvaluationErrorMessage"), source), e);
        } finally {
            valueLock.writeLock().unlock();
        }

        return value;
    }
}
