package com.group_finity.mascot.script;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.exception.VariableException;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.CompiledScript;
import javax.script.ScriptException;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
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
        return isClearAtInitFrame() ? "#{" + getSource() + "}" : "${" + getSource() + "}";
    }

    @Override
    public void init() {
        setValue(null);
    }

    @Override
    public void initFrame() {
        if (isClearAtInitFrame()) {
            setValue(null);
        }
    }

    @Override
    public synchronized Object get(final VariableMap variables) throws VariableException {

        if (getValue() != null) {
            return getValue();
        }

        try {
            setValue(getCompiled().eval(variables));
        } catch (final ScriptException e) {
            throw new VariableException(Main.getInstance().getLanguageBundle().getString("ScriptEvaluationErrorMessage") + ": " + source, e);
        }

        return getValue();
    }

    private void setValue(final Object value) {
        this.value = value;
    }

    private Object getValue() {
        return value;
    }

    private boolean isClearAtInitFrame() {
        return clearAtInitFrame;
    }

    private CompiledScript getCompiled() {
        return compiled;
    }

    private String getSource() {
        return source;
    }
}
