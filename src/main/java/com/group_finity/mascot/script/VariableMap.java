package com.group_finity.mascot.script;

import com.group_finity.mascot.Main;

import javax.script.Bindings;
import java.util.*;

/**
 * A map of values that can be used for evaluating {@link Variable Variables}.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class VariableMap extends AbstractMap<String, Object> implements Bindings {
    /**
     * An internal map used by this map to store {@link Variable Variables}.
     *
     * @see #getRawMap()
     */
    private final Map<String, Variable> rawMap = new LinkedHashMap<>();

    /**
     * Gets the internal map used by this map to store {@link Variable Variables}.
     *
     * @return this map's internal map
     */
    public Map<String, Variable> getRawMap() {
        return rawMap;
    }

    /**
     * Initializes the values of all variables stored in this map.
     *
     * @see Variable#init()
     */
    public void init() {
        for (final Variable o : rawMap.values()) {
            o.init();
        }
    }

    /**
     * Clears the cached values of all variables stored in this map, if necessary.
     * Called at the start of each frame.
     *
     * @see Variable#resetValue()
     */
    public void resetValues() {
        for (final Variable o : rawMap.values()) {
            o.resetValue();
        }
    }

    /**
     * The entry set instance of this {@code VariableMap}.
     *
     * @see #entrySet()
     */
    private final Set<Map.Entry<String, Object>> entrySet = new AbstractSet<>() {
        @Override
        public Iterator<Map.Entry<String, Object>> iterator() {
            return new Iterator<>() {
                private final Iterator<Map.Entry<String, Variable>> rawIterator = getRawMap().entrySet()
                        .iterator();

                @Override
                public boolean hasNext() {
                    return rawIterator.hasNext();
                }

                @Override
                public Map.Entry<String, Object> next() {
                    final Map.Entry<String, Variable> rawKeyValue = rawIterator.next();
                    final Variable value = rawKeyValue.getValue();

                    return new Map.Entry<>() {
                        @Override
                        public String getKey() {
                            return rawKeyValue.getKey();
                        }

                        @Override
                        public Object getValue() {
                            try {
                                return value.get(VariableMap.this);
                            } catch (final VariableException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public Object setValue(final Object value) {
                            throw new UnsupportedOperationException(Main.getInstance().getLanguageBundle().getString("SetValueNotSupportedErrorMessage"));
                        }
                    };
                }

                @Override
                public void remove() {
                    rawIterator.remove();
                }
            };
        }

        @Override
        public int size() {
            return getRawMap().size();
        }
    };

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return entrySet;
    }

    @Override
    public Object put(final String key, final Object value) {
        Object result;

        if (value instanceof Variable) {
            result = rawMap.put(key, (Variable) value);
        } else {
            result = rawMap.put(key, new Constant(value));
        }

        return result;
    }
}
