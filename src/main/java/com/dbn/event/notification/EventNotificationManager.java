/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.event.notification;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.util.Dialogs;
import com.dbn.event.registration.ui.EventRegistrationInputDialog;
import com.dbn.event.ui.EventMonitorForm;
import com.dbn.object.DBTable;
import com.dbn.prerequisite.DatabasePrerequisiteManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.Producer;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.action.UserDataKeys.EVENT_MONITOR_FORM;
import static com.dbn.common.operation.DatabaseOperation.ENABLE_DATABASE_CHANGE_NOTIFICATION;
import static com.dbn.editor.DatabaseFileEditorManager.COMPONENT_NAME;

@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class EventNotificationManager extends ProjectComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Project.EventNotificationManager";
    public static final String TOOL_WINDOW_ID = "DB Events";

    public EventNotificationManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static EventNotificationManager getInstance(Project project) {
        return Components.projectService(project, EventNotificationManager.class);
    }

    public void openEditorAndConfig(DBTable table) {
        Project project = getProject();
        DatabasePrerequisiteManager prerequisiteManager = DatabasePrerequisiteManager.getInstance(project);
        prerequisiteManager.startOperation(table,
                ENABLE_DATABASE_CHANGE_NOTIFICATION,
                () -> openRegistrationDialog(table));
    }

    private void openRegistrationDialog(DBTable object) {
        Project project = getProject();
        Dialogs.show(() -> new EventRegistrationInputDialog(project, object));
    }

    public ToolWindow getEventToolWindow() {
        Project project = getProject();
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        return toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    }

    public void showEventNotificationConsole() {
        showEventNotificationConsole(() -> new EventMonitorForm(getProject()));
    }

    public <T extends DBNForm> T showEventNotificationConsole(Producer<T> componentProducer) {
        T form = getEventsForm();
        ToolWindow toolWindow = getEventToolWindow();
        ContentManager contentManager = toolWindow.getContentManager();

        if (form == null) {
            form = componentProducer.produce();

            ContentFactory contentFactory = contentManager.getFactory();
            Content content = contentFactory.createContent(form.getComponent(), null, false);
//      content.putUserData(DIAGNOSTIC_CONTENT_CATEGORY, category);
            content.putUserData(EVENT_MONITOR_FORM, form);
            content.setCloseable(false);
            contentManager.addContent(content);
            Disposer.register(content, form);
        }

        Content content = getEventsContent();
        if (content != null) {
            contentManager.setSelectedContent(content);
        }

        toolWindow.setAvailable(true, null);
        toolWindow.show(null);

        return form;
    }

    private <T extends DBNForm> T getEventsForm() {
        Content content = getEventsContent();
        if (content != null) {
            return (T) content.getUserData(EVENT_MONITOR_FORM);
        }
        return null;
    }


    private Content getEventsContent() {
        ToolWindow toolWindow = getEventToolWindow();
        ContentManager contentManager = toolWindow.getContentManager();
        Content[] contents = contentManager.getContents();

        return contents.length > 0 ? contents[0] : null;
    }


}

