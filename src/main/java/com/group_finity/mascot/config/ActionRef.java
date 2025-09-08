package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class ActionRef implements IActionBuilder {

    private static final Logger log = Logger.getLogger(ActionRef.class.getName());

    private final Configuration configuration;

    private final String name;

    private final Map<String, String> params = new LinkedHashMap<>();

    public ActionRef(final Configuration configuration, final Entry refNode) {
        this.configuration = configuration;

        name = refNode.getAttribute(configuration.getSchema().getString("Name"));
        params.putAll(refNode.getAttributes());

        log.log(Level.FINE, "Finished loading action reference: {0}", this);
    }

    @Override
    public String toString() {
        return "Action(" + name + ")";
    }

    @Override
    public void validate() throws ConfigurationException {
        if (!configuration.getActionBuilders().containsKey(name)) {
            log.log(Level.SEVERE, "There is no corresponding action for action reference: {0}", this);
            throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("NoActionFoundErrorMessage") + "(" + this + ")");
        }
    }

    @Override
    public Action buildAction(final Map<String, String> params) throws ActionInstantiationException {
        final Map<String, String> newParams = new LinkedHashMap<>(params);
        newParams.putAll(this.params);
        return configuration.buildAction(name, newParams);
    }
}
