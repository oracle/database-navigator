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

package com.dbn.assistant.service.selectai.editor;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.service.selectai.SelectAiContextUtil;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.editor.EditorNotificationPanel;
import com.dbn.common.feature.FeatureAcknowledgement;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.assistant.AssistantType.SELECT_AI;
import static com.dbn.nls.NlsResources.txt;

public class SelectAiEditorNotificationPanel extends EditorNotificationPanel {

    public SelectAiEditorNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
        super(project, file, fileEditor, MessageType.INFO);
        setIcon(Icons.COMMON_INFO);
        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
                txt("ntf.assistant.text.SelectAiEditorNotification"),
                txt("ntf.assistant.link.OracleSelectAi"),
                "https://www.oracle.com/autonomous-database/select-ai/");
        setContent(hyperLinkForm);

        createActionLabel(txt("app.assistant.action.Configure"), () -> configure());
        createActionLabel(txt("app.assistant.action.Chat"), () -> chat());
        createActionLabel(txt("app.assistant.action.Dismiss"), () -> dismiss());
    }

    private void configure() {
        ConnectionId connectionId = getConnectionId();
        SelectAiContextUtil.openProfileConfiguration(connectionId);
    }

    private void chat() {
        ConnectionId connectionId = getConnectionId();
        DatabaseAssistantManager assistantManager = getAssistantManager();
        AssistantState assistantState = assistantManager.getAssistantState(connectionId, SELECT_AI);
        assistantState.setAcknowledgement(FeatureAcknowledgement.NOTICED);
        assistantManager.showToolWindow(connectionId, SELECT_AI);
    }

    private void dismiss() {
        ConnectionId connectionId = getConnectionId();
        DatabaseAssistantManager assistantManager = getAssistantManager();
        AssistantState assistantState = assistantManager.getAssistantState(connectionId, SELECT_AI);
        assistantState.setAcknowledgement(FeatureAcknowledgement.DISMISSED);
    }

    private @NotNull DatabaseAssistantManager getAssistantManager() {
        return DatabaseAssistantManager.getInstance(getProject());
    }

}
