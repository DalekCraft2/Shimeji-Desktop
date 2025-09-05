package com.group_finity.mascot.script;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Constant extends Variable {

    private final Object value;

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
