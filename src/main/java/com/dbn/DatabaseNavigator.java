/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn;

import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.EagerService;
import com.dbn.common.component.PersistentState;
import com.dbn.common.file.FileTypeService;
import com.dbn.common.options.setting.Settings;
import com.dbn.common.util.UUIDs;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.plugin.DBNPluginStateListener;
import com.dbn.plugin.PluginConflictManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.plugins.PluginStateManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.extensions.PluginId;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.dbn.common.component.Components.applicationService;

@Slf4j
@Getter
@State(
    name = DatabaseNavigator.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseNavigator extends ApplicationComponentBase implements PersistentState, EagerService {
    public static final String COMPONENT_NAME = "DBNavigator.Application.Settings";
    public static final String STORAGE_FILE = "dbnavigator.xml";

    public static final PluginId SQL_PLUGIN_ID = PluginId.getId("com.intellij.database");
    public static final PluginId DBN_PLUGIN_ID = PluginId.getId("DBN");

    private String clientId = UUIDs.compact();

    public DatabaseNavigator() {
        super(COMPONENT_NAME);
        PluginStateManager.addStateListener(new DBNPluginStateListener());
        //new NotificationGroup("Database Navigator", NotificationDisplayType.TOOL_WINDOW, true, ExecutionManager.TOOL_WINDOW_ID);

        PluginConflictManager.getInstance();
        FileTypeService.getInstance();

        registerExecutorExtension();

    }

    private static void registerExecutorExtension() {
/*
        // TODO review and cleanup (internal api usage) - initial motivation for this change unknown
        try {
            ExtensionsArea extensionArea = ApplicationManager.getApplication().getExtensionArea();
            boolean available = extensionArea.hasExtensionPoint(Executor.EXECUTOR_EXTENSION_NAME);
            if (!available) extensionArea.getExtensionPoint(Executor.EXECUTOR_EXTENSION_NAME).registerExtension(new DefaultDebugExecutor());
        } catch (Throwable e) {
            log.error("Failed to register debug executor extension", e);
        }
*/
    }

    public static DatabaseNavigator getInstance() {
        return applicationService(DatabaseNavigator.class);
    }

    @NotNull
    public static IdeaPluginDescriptor getPluginDescriptor() {
        return Objects.requireNonNull(PluginManagerCore.getPlugin(DBN_PLUGIN_ID));
    }

    public String getName() {
        return null;
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element diagnosticsElement = Settings.newElement(element, "diagnostics");
        Diagnostics.writeState(diagnosticsElement);
        Settings.setString(element, "client-id", clientId);
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element diagnosticsElement = element.getChild("diagnostics");
        Diagnostics.readState(diagnosticsElement);
        clientId = Settings.getString(element, "client-id", clientId);

    }
}

