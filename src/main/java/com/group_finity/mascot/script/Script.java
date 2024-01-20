package com.group_finity.mascot.script;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.exception.VariableException;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public class Script extends Variable {

    private static final ThreadLocal<GraalJSScriptEngine> engine;

    static {
        engine = ThreadLocal.withInitial(
                () -> GraalJSScriptEngine.create(null,
                    Context.newBuilder("js")
                        .allowHostAccess(HostAccess.ALL)
                        .allowHostClassLookup(s -> s.startsWith("com.group_finity.mascot"))
                        .allowExperimentalOptions(true)
                        .option("js.nashorn-compat", "true")
                )
        );
    }

    private final String source;

    private final boolean clearAtInitFrame;

    private final CompiledScript compiled;

    private Object value;

    public Script(final String source, final boolean clearAtInitFrame) throws VariableException {

        this.source = source;
        this.clearAtInitFrame = clearAtInitFrame;
        try {
            compiled = ((Compilable) engine.get()).compile(this.source);
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

        Map<String, Variable> filteredMap = variables.getRawMap().entrySet().stream()
                .filter((entry) -> entry.getValue() != this)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        VariableMap tempVariables = new VariableMap();
        tempVariables.getRawMap().putAll(filteredMap);

        try {setValue(getCompiled().eval(tempVariables));
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
