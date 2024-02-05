package com.group_finity.mascot.script;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
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

    private Object getValue() {
        return value;
    }

    @Override
    public void init() {
    }

    @Override
    public void initFrame() {
    }

    @Override
    public Object get(final VariableMap variables) {
        return getValue();
    }

}
