package com.group_finity.mascot.action;

import com.group_finity.mascot.script.VariableMap;

import java.util.logging.Logger;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public class Select extends ComplexAction {
    private static final Logger log = Logger.getLogger(Select.class.getName());

    public Select(java.util.ResourceBundle schema, final VariableMap params, final Action... actions) {
        super(schema, params, actions);
    }
}
