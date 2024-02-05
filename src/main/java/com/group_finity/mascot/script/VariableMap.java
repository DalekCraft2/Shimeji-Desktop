package com.group_finity.mascot.script;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.exception.VariableException;

import javax.script.Bindings;
import java.util.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class VariableMap extends AbstractMap<String, Object> implements Bindings {

    private final Map<String, Variable> rawMap = new LinkedHashMap<>();

    public Map<String, Variable> getRawMap() {
        return rawMap;
    }

    public void init() {
        for (final Variable o : getRawMap().values()) {
            o.init();
        }
    }

    public void initFrame() {
        for (final Variable o : getRawMap().values()) {
            o.initFrame();
        }
    }

    private final Set<Map.Entry<String, Object>> entrySet = new AbstractSet<>() {

        @Override
        public Iterator<Map.Entry<String, Object>> iterator() {

            return new Iterator<>() {

                private Iterator<Map.Entry<String, Variable>> rawIterator = getRawMap().entrySet()
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
            result = getRawMap().put(key, (Variable) value);
        } else {
            result = getRawMap().put(key, new Constant(value));
        }

        return result;

    }

}
