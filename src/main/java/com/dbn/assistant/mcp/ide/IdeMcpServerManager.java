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

import com.dbn.assistant.mcp.model.AssistantMcpServer;
import com.dbn.assistant.mcp.model.AssistantMcpServerType;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.latent.Latent;
import com.dbn.common.util.Environment;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.dbn.assistant.mcp.ide.IdeMcpServerAvailability.DISABLED;
import static com.dbn.assistant.mcp.ide.IdeMcpServerAvailability.ENABLED;
import static com.dbn.assistant.mcp.ide.IdeMcpServerAvailability.UNAVAILABLE;
import static com.dbn.assistant.mcp.model.AssistantMcpServer.IDE_MCP_SERVER_ID;
import static com.dbn.common.Reflection.invokeMethod;
import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.util.Unsafe.silent;

@Getter
@Setter
public class IdeMcpServerManager extends ApplicationComponentBase {
    public static final PluginId MCP_SERVER_PLUGIN_ID = PluginId.getId("com.intellij.mcpServer");

    public Latent<IdeMcpServerAvailability> mcpServerAvailability = Latent.reloadable(10, TimeUnit.SECONDS, this, m -> m.evaluateMcpServerAvailability());
    public Latent<AssistantMcpServer> ideMcpServer = Latent.mutable(
            () -> getMcpServerPort(),
            () -> createIdeMcpServer());

    private AssistantMcpServer createIdeMcpServer() {
        AssistantMcpServer mcpServer = new AssistantMcpServer(IDE_MCP_SERVER_ID);
        mcpServer.setType(AssistantMcpServerType.HTTP);
        mcpServer.setName("IDE MCP Server (" + Environment.getIdeName() + ")");
        mcpServer.setUrl(getMcpServerUrl());
        return mcpServer;
    }

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
        if (!isPluginAvailable()) return UNAVAILABLE;
        if (!isMcpServerEnabled()) return DISABLED;
        return ENABLED;
    }

    @SneakyThrows
    public boolean isMcpServerEnabled() {
        Object state = getMcpServerState();
        if (state == null) return false;

        Boolean enabled = invokeMethod(state, "getEnableMcpServer");
        return enabled != null && enabled;
    }

    @Nullable
    @SneakyThrows
    public Integer getMcpServerPort() {
        Object state = getMcpServerState();
        if (state == null) return null;

        return invokeMethod(state, "getMcpServerPort");
    }

    @SneakyThrows
    public String getMcpServerUrl() {
        Integer port = getMcpServerPort();
        if (port == null) return null;

        return "http://127.0.0.1:" + port + "/stream";
        //return "http://127.0.0.1:" + port + "/sse"; TODO support non-streamable transport
    }

    @Nullable
    private Object getMcpServerState() {
        Class<?> settingsClass = getMcpServerSettingsClass();
        if (settingsClass == null) return null;

        Object settings = invokeMethod(settingsClass, "getInstance");
        return invokeMethod(settings, "getState");
    }

    @Nullable
    private static Class<?> getMcpServerSettingsClass() {
        String className = "com.intellij.mcpserver.settings.McpServerSettings";
        ClassLoader classLoader = getPluginClassLoader();
        return silent(null, () -> Class.forName(className, false, classLoader));
    }

    @Nullable
    private static ClassLoader getPluginClassLoader() {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(MCP_SERVER_PLUGIN_ID);
        return plugin == null ? null : plugin.getPluginClassLoader();
    }

    public static boolean isIdeMcpPluginSupported() {
        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        return applicationInfo.getBuild().getBaselineVersion() >= 261;
    }

    private boolean isPluginAvailable() {
        return getMcpServerSettingsClass() != null;
    }

    public AssistantMcpServer getIdeMcpServer() {
        return ideMcpServer.get();
    }

    @NonNls
    private static final Set<String> IDE_DB_TOOL_NAMES = Set.of(
            "cancel_sql_query",
            "execute_sql_query",
            "get_database_object_description",
            "list_database_connections",
            "list_database_schemas",
            "list_recent_sql_queries",
            "list_schema_object_kinds",
            "list_schema_objects",
            "preview_table_data",
            "test_database_connection");

    public static boolean isConflictingIdeTool(String toolName) {
        // suppress tools that integrate with JB native database support
        return IDE_DB_TOOL_NAMES.contains(toolName);
    }
}
