/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.util;

import com.dbn.common.Reflection;
import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.compatibility.Workaround;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Compatibility wrapper for IntelliJ plugin descriptor and enablement APIs.
 * <p>
 * DBN needs plugin descriptors and self-disable support, while IntelliJ plugin-management
 * entry points can change status between IDE builds. This utility keeps those calls centralized
 * so version-compatible builds can adapt this single class without spreading conditional API
 * choices through feature code.
 */
@Workaround
public class Plugins {
    @Nullable
    @Compatibility
    public static IdeaPluginDescriptor getPlugin(@NotNull PluginId pluginId) {
        //return PluginManagerCore.getPlugin(pluginId);
        return Reflection.invokeMethod(PluginManagerCore.class, "getPlugin", pluginId);
    }

    @Compatibility
    public static boolean disablePlugin(@NotNull PluginId pluginId) {
        //return PluginManager.disablePlugin(pluginId.getIdString());
        Object disablePlugin = Reflection.invokeMethod(PluginManager.class, "disablePlugin", pluginId.getIdString());
        return disablePlugin instanceof Boolean && (Boolean) disablePlugin;
    }
}
