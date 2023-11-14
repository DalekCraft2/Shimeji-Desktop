package com.group_finity.mascot.script;

import jdk.nashorn.api.scripting.ClassFilter;

public class ScriptFilter implements ClassFilter {
    @Override
    public boolean exposeToScripts(String s) {
        return s.startsWith("com.group_finity.mascot");
    }
}
