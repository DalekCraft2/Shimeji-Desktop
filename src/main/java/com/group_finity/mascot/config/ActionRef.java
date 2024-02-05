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
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
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
        getParams().putAll(refNode.getAttributes());

        log.log(Level.INFO, "Finished loading action reference: {0}", this);
    }

    @Override
    public String toString() {
        return "Action(" + getName() + ")";
    }

    private String getName() {
        return name;
    }

    private Map<String, String> getParams() {
        return params;
    }

    private Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void validate() throws ConfigurationException {
        if (!getConfiguration().getActionBuilders().containsKey(getName())) {
            log.log(Level.SEVERE, "There is no corresponding behavior for action reference: {0}", this);
            throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("NoBehaviourFoundErrorMessage") + "(" + this + ")");
        }
    }

    @Override
    public Action buildAction(final Map<String, String> params) throws ActionInstantiationException {
        final Map<String, String> newParams = new LinkedHashMap<>(params);
        newParams.putAll(getParams());
        return getConfiguration().buildAction(getName(), newParams);
    }
}
