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
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Conditional;
import com.dbn.connection.ConnectionId;
import com.dbn.event.registration.EventRegistrationListener;
import com.dbn.event.ui.EventMonitorForm;
import com.dbn.object.event.ObjectChangeAction;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.action.UserDataKeys.EVENT_MONITOR_FORM;
import static com.dbn.common.util.Modality.nonModal;
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

        ProjectEvents.subscribe(project, this, EventRegistrationListener.TOPIC, createEventRegistrationListener());
    }

    private EventRegistrationListener createEventRegistrationListener() {
        return event ->
                Conditional.when(event.getAction() == ObjectChangeAction.CREATE,
                    () -> Dispatch.run(nonModal(),
                            () -> showEventNotificationConsole(event.getConnectionId(), 0)));
    }

    public static EventNotificationManager getInstance(Project project) {
        return Components.projectService(project, EventNotificationManager.class);
    }

    public ToolWindow getEventMonitorToolWindow() {
        Project project = getProject();
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        return toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    }

    public void showEventNotificationConsole() {
        showEventNotificationConsole(null, -1);
    }
    public void showEventNotificationConsole(ConnectionId connectionId, int tabIndex) {
        EventMonitorForm form = ensureEventMonitorForm();
        ToolWindow toolWindow = getEventMonitorToolWindow();

        Content content = getEventMonitorContent();
        if (content != null) {
            ContentManager contentManager = toolWindow.getContentManager();
            contentManager.setSelectedContent(content);
        }



        toolWindow.setAvailable(true, null);
        toolWindow.show(null);
        form.selectContent(connectionId, tabIndex);
    }

    @NotNull
    private EventMonitorForm ensureEventMonitorForm() {
        EventMonitorForm form = getEventMonitorForm();
        if (form != null) return form;

        form = new EventMonitorForm(getProject());

        ToolWindow toolWindow = getEventMonitorToolWindow();
        ContentManager contentManager = toolWindow.getContentManager();

        ContentFactory contentFactory = contentManager.getFactory();
        Content content = contentFactory.createContent(form.getComponent(), null, false);
        content.putUserData(EVENT_MONITOR_FORM, form);
        content.setCloseable(false);
        contentManager.addContent(content);
        Disposer.register(content, form);

        return form;
    }

    @Nullable
    private EventMonitorForm getEventMonitorForm() {
        Content content = getEventMonitorContent();
        return EVENT_MONITOR_FORM.get(content);
    }


    private Content getEventMonitorContent() {
        ToolWindow toolWindow = getEventMonitorToolWindow();
        ContentManager contentManager = toolWindow.getContentManager();
        Content[] contents = contentManager.getContents();

        return contents.length > 0 ? contents[0] : null;
    }


}

