package com.group_finity.mascot.action;

import com.group_finity.mascot.script.VariableMap;

import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * An action that executes only one of multiple actions that matches the conditions.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Select extends ComplexAction {
    private static final Logger log = Logger.getLogger(Select.class.getName());

    public Select(ResourceBundle schema, final VariableMap context, final Action... actions) {
        super(schema, context, actions);
    }
}
