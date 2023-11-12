package com.group_finity.mascot.config;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;

import java.util.Map;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public interface IActionBuilder {

    void validate() throws ConfigurationException;

    Action buildAction(final Map<String, String> params) throws ActionInstantiationException;

}
