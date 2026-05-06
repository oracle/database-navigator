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

package com.dbn.assistant.mcp.ide;

import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.latent.Latent;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

import static com.dbn.assistant.mcp.ide.IdeMcpServerAvailability.PLUGIN_DISABLED;
import static com.dbn.assistant.mcp.ide.IdeMcpServerAvailability.PLUGIN_UNAVAILABLE;
import static com.dbn.assistant.mcp.ide.IdeMcpServerAvailability.SERVER_DISABLED;
import static com.dbn.assistant.mcp.ide.IdeMcpServerAvailability.SERVER_ENABLED;
import static com.dbn.common.Reflection.invokeMethod;
import static com.dbn.common.component.Components.applicationService;
import static com.intellij.ide.plugins.PluginManagerCore.isDisabled;
import static com.intellij.ide.plugins.PluginManagerCore.isLoaded;

@Getter
@Setter
public class IdeMcpServerManager extends ApplicationComponentBase {

    public static final PluginId MCP_SERVER_PLUGIN_ID = PluginId.getId("com.intellij.mcpServer");
    public Latent<IdeMcpServerAvailability> mcpServerAvailability = Latent.reloadable(10, TimeUnit.SECONDS, this, m -> m.evaluateMcpServerAvailability());

    public IdeMcpServerManager() {
        super("DBNavigator.IdeMcpServerManager");

    }

    public static IdeMcpServerManager getInstance() {
        return applicationService(IdeMcpServerManager.class);
    }

    public IdeMcpServerAvailability getMcpServerAvailability(boolean reevaluate) {
        if (reevaluate) mcpServerAvailability.reset();

        return mcpServerAvailability.get();
    }

    public void resetMcpServerAvailability() {
        mcpServerAvailability.reset();
    }


    private IdeMcpServerAvailability evaluateMcpServerAvailability() {
        if (!isLoaded(MCP_SERVER_PLUGIN_ID)) return PLUGIN_UNAVAILABLE;
        if (isDisabled(MCP_SERVER_PLUGIN_ID)) return PLUGIN_DISABLED;
        if (!isMcpServerEnabled()) return SERVER_DISABLED;
        return SERVER_ENABLED;
    }

    @Nullable
    private static ClassLoader getPluginClassLoader() {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(MCP_SERVER_PLUGIN_ID);
        return plugin == null ? null : plugin.getPluginClassLoader();
    }

    @SneakyThrows
    public boolean isMcpServerEnabled() {
        Object state = getMcpServerState();
        if (state == null) return false;

        Boolean enabled = invokeMethod(state, "getEnableMcpServer");
        return enabled != null && enabled;
    }

    @SneakyThrows
    public String getMcpServerUrl() {
        Object state = getMcpServerState();
        if (state == null) return null;

        Integer port = invokeMethod(state, "getMcpServerPort");
        return "http://127.0.0.1:" + port + "/stream";
    }

    @Nullable
    private static Object getMcpServerState() throws ClassNotFoundException {
        ClassLoader pluginClassLoader = getPluginClassLoader();
        Class<?> settingsClass = Class.forName("com.intellij.mcpserver.settings.McpServerSettings", false, pluginClassLoader);

        Object settings = invokeMethod(settingsClass, "getInstance");
        return invokeMethod(settings, "getState");
    }
}
