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
        if (!rawMap.isEmpty()) {
            for (final Variable o : rawMap.values()) {
                o.init();
            }
        }
    }

    /**
     * Clears the cached values of all variables stored in this map, if necessary.
     * Called at the start of each frame.
     *
     * @see Variable#resetValue()
     */
    public void resetValues() {
        if (!rawMap.isEmpty()) {
            for (final Variable o : rawMap.values()) {
                o.resetValue();
            }
        }
    }

    @Override
    public Object put(String key, Object value) {
        Object result;

        if (value instanceof Variable variable) {
            result = rawMap.put(key, variable);
        } else {
            result = rawMap.put(key, new Constant(value));
        }

        return result;
    }

    /**
     * The entry set instance of this {@code VariableMap}.
     *
     * @see #entrySet()
     * @see EntrySet
     */
    private Set<Map.Entry<String, Object>> entrySet;

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        Set<Map.Entry<String, Object>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    final class EntrySet extends AbstractSet<Map.Entry<String, Object>> {
        @Override
        public Iterator<Map.Entry<String, Object>> iterator() {
            return new EntryIterator(getRawMap().entrySet().iterator());
        }

        @Override
        public int size() {
            return getRawMap().size();
        }
    }

    final class EntryIterator implements Iterator<Map.Entry<String, Object>> {
        private final Iterator<Map.Entry<String, Variable>> rawIterator;

        EntryIterator(Iterator<Map.Entry<String, Variable>> rawIterator) {
            this.rawIterator = rawIterator;
        }

        @Override
        public boolean hasNext() {
            return rawIterator.hasNext();
        }

        @Override
        public Map.Entry<String, Object> next() {
            return new VariableEntry(rawIterator.next());
        }
    }

    final class VariableEntry implements Map.Entry<String, Object> {
        private final Map.Entry<String, Variable> rawEntry;

        VariableEntry(Map.Entry<String, Variable> rawEntry) {
            this.rawEntry = rawEntry;
        }

        @Override
        public String getKey() {
            return rawEntry.getKey();
        }

        @Override
        public Object getValue() {
            try {
                return rawEntry.getValue().get(VariableMap.this);
            } catch (final VariableException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object setValue(Object value) {
            throw new UnsupportedOperationException(Main.getInstance().getLanguageBundle().getString(
                    "SetValueNotSupportedErrorMessage"));
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Map.Entry<?, ?> e
                    && Objects.equals(getKey(), e.getKey())
                    && Objects.equals(getValue(), e.getValue());
        }

        @Override
        public int hashCode() {
            Object value = getValue();
            return (getKey() == null ? 0 : getKey().hashCode()) ^
                    (value == null ? 0 : value.hashCode());
        }

        @Override
        public String toString() {
            return getKey() + '=' + getValue();
        }
    }
}
