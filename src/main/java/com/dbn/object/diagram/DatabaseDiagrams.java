/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.object.diagram;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class DatabaseDiagrams {
    public static final PluginId PLUGIN_ID = PluginId.getId("com.intellij.diagram");

    public static boolean isAvailable() {
        return PluginManagerCore.isPluginInstalled(PLUGIN_ID) &&
                !PluginManagerCore.isDisabled(PLUGIN_ID);
    }
}
