package com.group_finity.mascot.script;

import org.openjdk.nashorn.api.scripting.ClassFilter;

/**
 * @author Kilkakon
 */
public class ScriptFilter implements ClassFilter {
    @Override
    public boolean exposeToScripts(String className) {
        return className.startsWith("com.group_finity.mascot");
    }
}
