package com.group_finity.mascot.script;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.exception.VariableException;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.CompiledScript;
import javax.script.ScriptException;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Script extends Variable {

    private static final NashornScriptEngine ENGINE = (NashornScriptEngine) new NashornScriptEngineFactory().getScriptEngine(new ScriptFilter());

    private final String source;

    private final boolean clearAtInitFrame;

    private final CompiledScript compiled;

    private Object value;

    public Script(final String source, final boolean clearAtInitFrame) throws VariableException {
        this.source = source;
        this.clearAtInitFrame = clearAtInitFrame;
        try {
            compiled = ENGINE.compile(this.source);
        } catch (final ScriptException e) {
            throw new VariableException(Main.getInstance().getLanguageBundle().getString("ScriptCompilationErrorMessage") + ": " + this.source, e);
        }
    }

    @Override
    public String toString() {
        return clearAtInitFrame ? "#{" + source + "}" : "${" + source + "}";
    }

    @Override
    public void init() {
        value = null;
    }

    @Override
    public void initFrame() {
        if (clearAtInitFrame) {
            value = null;
        }
    }

    @Override
    public synchronized Object get(final VariableMap variables) throws VariableException {
        if (value != null) {
            return value;
        }

        try {
            value = compiled.eval(variables);
        } catch (final ScriptException e) {
            throw new VariableException(Main.getInstance().getLanguageBundle().getString("ScriptEvaluationErrorMessage") + ": " + source, e);
        }

        return value;
    }
}
