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

package com.dbn.diagnostics;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SessionId;
import com.dbn.diagnostics.data.DiagnosticBundle;
import com.dbn.diagnostics.data.DiagnosticCategory;
import com.dbn.diagnostics.data.DiagnosticType;
import com.dbn.diagnostics.options.ui.DiagnosticSettingsDialog;
import com.dbn.diagnostics.ui.ConnectionDiagnosticsForm;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.Producer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.action.UserDataKeys.DIAGNOSTIC_CONTENT_CATEGORY;
import static com.dbn.common.action.UserDataKeys.DIAGNOSTIC_CONTENT_FORM;
import static com.dbn.common.component.Components.projectService;

@State(
    name = DiagnosticsManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DiagnosticsManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DiagnosticsManager";
    public static final String TOOL_WINDOW_ID = "DB Diagnostics";

    private final Map<ConnectionId, DiagnosticBundle<String>> metadataInterfaceDiagnostics = new ConcurrentHashMap<>();
    private final Map<ConnectionId, DiagnosticBundle<SessionId>> connectivityDiagnostics = new ConcurrentHashMap<>();

    private DiagnosticsManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DiagnosticsManager getInstance(@NotNull Project project) {
        return projectService(project, DiagnosticsManager.class);
    }

    public DiagnosticBundle<String> getMetadataInterfaceDiagnostics(ConnectionId connectionId) {
        return metadataInterfaceDiagnostics.
                computeIfAbsent(connectionId, id -> DiagnosticBundle.composite(DiagnosticType.METADATA_INTERFACE));
    }

    public DiagnosticBundle<SessionId> getConnectivityDiagnostics(ConnectionId connectionId) {
        return connectivityDiagnostics.
                computeIfAbsent(connectionId, id -> DiagnosticBundle.basic(DiagnosticType.DATABASE_CONNECTIVITY));
    }

    public void openDiagnosticsSettings() {
        Dialogs.show(() -> new DiagnosticSettingsDialog(getProject()));
    }

    public void showConnectionDiagnostics() {
        showDiagnosticsConsole(DiagnosticCategory.CONNECTION, () -> new ConnectionDiagnosticsForm(getProject()));
    }

    @NotNull
    public <T extends DBNForm> T showDiagnosticsConsole(
            @NotNull DiagnosticCategory category,
            @NotNull Producer<T> componentProducer) {

        T form = getDiagnosticsForm(category);
        ToolWindow toolWindow = getDiagnosticsToolWindow();
        ContentManager contentManager = toolWindow.getContentManager();

        if (form == null) {
            form = componentProducer.produce();

            ContentFactory contentFactory = contentManager.getFactory();
            Content content = contentFactory.createContent(form.getComponent(), category.getName(), false);
            content.putUserData(DIAGNOSTIC_CONTENT_CATEGORY, category);
            content.putUserData(DIAGNOSTIC_CONTENT_FORM, form);
            content.setCloseable(true);
            contentManager.addContent(content);
            Disposer.register(content, form);
        }

        Content content = getDiagnosticsContent(category);
        if (content != null) {
            contentManager.setSelectedContent(content);
        }

        toolWindow.setAvailable(true, null);
        toolWindow.show(null);

        return form;

    }

    @Nullable
    private  <T extends DBNForm> T getDiagnosticsForm(@NotNull DiagnosticCategory category) {
        Content content = getDiagnosticsContent(category);
        if (content != null) {
            return (T) content.getUserData(DIAGNOSTIC_CONTENT_FORM);
        }
        return null;
    }

    @Nullable
    private  Content getDiagnosticsContent(@NotNull DiagnosticCategory category) {
        ToolWindow toolWindow = getDiagnosticsToolWindow();
        ContentManager contentManager = toolWindow.getContentManager();
        Content[] contents = contentManager.getContents();
        for (Content content : contents) {
            if (content.getUserData(DIAGNOSTIC_CONTENT_CATEGORY) == category) {
                return content;
            }
        }
        return null;
    }


    public void closeDiagnosticsConsole(DiagnosticCategory category) {
        ToolWindow toolWindow = getDiagnosticsToolWindow();
        ContentManager contentManager = toolWindow.getContentManager();
        Content[] contents = contentManager.getContents();
        for (Content content : contents) {
            if (content.getUserData(DIAGNOSTIC_CONTENT_CATEGORY) == category) {
                contentManager.removeContent(content, true);
            }
        }

        if (contentManager.getContents().length == 0) {
            toolWindow.setAvailable(false, null);
        }
    }

    public ToolWindow getDiagnosticsToolWindow() {
        Project project = getProject();
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        return toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
    }

    @Override
    public void disposeInner() {
        metadataInterfaceDiagnostics.clear();
        connectivityDiagnostics.clear();
        super.disposeInner();
    }
}
