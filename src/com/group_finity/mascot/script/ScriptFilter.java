package com.group_finity.mascot.script;

import jdk.nashorn.api.scripting.ClassFilter;

public class ScriptFilter implements ClassFilter {
    @Override
    public boolean exposeToScripts(String className) {
        return className.startsWith("com.group_finity.mascot");
    }
}
