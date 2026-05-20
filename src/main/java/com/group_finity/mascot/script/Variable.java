package com.group_finity.mascot.script;

/**
 * Represents a scriptable value that can be used by {@link com.group_finity.mascot.action.Action Actions} or
 * {@link com.group_finity.mascot.behavior.Behavior Behaviors}.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public abstract class Variable {

    /**
     * Parses a variable from the given source.
     * <p>
     * By default, the return value will be a {@link Constant}. However, if the source is in script syntax (i.e.,
     * {@code ${...}} or {@code #{...}}, the return value will be a {@link Script}.
     * <p>
     * For scripts that start with {@code $}, the script will store its value when it is first evaluated, and then
     * return that value every time it is called. For scripts that start with {@code #}, the script's stored value will
     * be cleared at the start of each frame, forcing it to be reevaluated.
     *
     * @param source the source from which to parse the variable
     * @return the parsed variable, or {@code null} if {@code source} is {@code null}
     * @throws VariableException if {@code source} is in script syntax but is not compilable
     */
    public static Variable parse(final String source) throws VariableException {
        Variable result = null;

        if (source != null) {
            if (source.startsWith("${") && source.endsWith("}")) {
                result = new Script(source.substring(2, source.length() - 1), false);
            } else if (source.startsWith("#{") && source.endsWith("}")) {
                result = new Script(source.substring(2, source.length() - 1), true);
            } else {
                result = new Constant(parseConstant(source));
            }
        }

        return result;
    }

    /**
     * Parses a constant value from the given source.
     * <p>
     * If the source can be parsed as a boolean or a double, it will be returned as such.
     * Otherwise, it will be returned as a string.
     *
     * @param source the source from which to parse the constant
     * @return the parsed constant, or {@code null} if {@code source} is {@code null}
     */
    private static Object parseConstant(final String source) {
        Object result = null;

        if (source != null) {
            if (source.equals("true")) {
                result = Boolean.TRUE;
            } else if (source.equals("false")) {
                result = Boolean.FALSE;
            } else {
                try {
                    result = Double.parseDouble(source);
                } catch (final NumberFormatException e) {
                    result = source;
                }
            }
        }

        return result;
    }

    /**
     * Initializes the value of this variable.
     */
    public abstract void init();

    /**
     * Clears the cached value of this variable if necessary.
     * Called at the start of each frame.
     */
    public abstract void resetValue();

    /**
     * Evaluates this variable using the provided context.
     *
     * @param variables the context to use to evaluate the variable
     * @return the evaluated value
     * @throws VariableException if the variable fails to be evaluated
     */
    public abstract Object get(VariableMap variables) throws VariableException;
}
