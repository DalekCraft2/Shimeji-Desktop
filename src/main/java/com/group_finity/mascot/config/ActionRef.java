package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An object that builds action references.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class ActionRef implements IActionBuilder {

    private static final Logger log = LoggerFactory.getLogger(ActionRef.class);

    private final Configuration configuration;

    private final String name;

    private final Map<String, String> params = new LinkedHashMap<>();

    public ActionRef(final Configuration configuration, final Entry refNode) {
        this.configuration = configuration;

        name = refNode.getAttribute(configuration.getSchema().getString("Name"));
        params.putAll(refNode.getAttributes());

        log.debug("Finished loading action reference: {}", this);
    }

    @Override
    public String toString() {
        return "ActionRef[name=" + name + "]";
    }

    @Override
    public void validate() throws ConfigurationException {
        if (!configuration.getActionBuilders().containsKey(name)) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("NoActionFoundErrorMessage"), name));
        }
    }

    @Override
    public Action buildAction(final Map<String, String> params) throws ActionInstantiationException {
        final Map<String, String> newParams = new LinkedHashMap<>(params);
        newParams.putAll(this.params);
        return configuration.buildAction(name, newParams);
    }
}
