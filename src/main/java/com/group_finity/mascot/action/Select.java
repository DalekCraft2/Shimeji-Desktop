package com.group_finity.mascot.action;

import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * An action that executes only one of multiple actions that matches the conditions.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Select extends ComplexAction {
    private static final Logger log = LoggerFactory.getLogger(Select.class);

    public Select(ResourceBundle schema, final VariableMap context, final Action... actions) {
        super(schema, context, actions);
    }
}
