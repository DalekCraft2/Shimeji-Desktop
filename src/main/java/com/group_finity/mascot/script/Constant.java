package com.group_finity.mascot.script;

/**
 * An implementation of {@link Variable} that represents a constant value.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Constant extends Variable {

    /**
     * The value of this constant.
     */
    private final Object value;

    /**
     * Creates a new Constant.
     *
     * @param value the value of this constant
     */
    public Constant(final Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value == null ? "null" : value.toString();
    }

    @Override
    public void init() {
    }

    @Override
    public void initFrame() {
    }

    @Override
    public Object get(final VariableMap variables) {
        return value;
    }
}
