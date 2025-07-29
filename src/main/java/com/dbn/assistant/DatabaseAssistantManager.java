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

package com.dbn.assistant;

import com.dbn.DatabaseNavigator;
import com.dbn.assistant.adapter.AssistantAdapter;
import com.dbn.assistant.adapter.AssistantAdapters;
import com.dbn.assistant.adapter.AssistantResponseConsumer;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateDelegate;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionStatusListener;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.ui.CardLayouts.addCard;
import static com.dbn.common.ui.CardLayouts.isBlankCard;
import static com.dbn.common.ui.CardLayouts.showBlankCard;
import static com.dbn.common.ui.CardLayouts.showCard;
import static com.dbn.common.ui.CardLayouts.visibleCardId;
import static com.dbn.common.util.ContextLookup.getConnectionId;

/**
 * Main database AI-Assistance management component
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
@Slf4j
@State(
        name = DatabaseAssistantManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class DatabaseAssistantManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseAssistantManager";
    public static final String TOOL_WINDOW_ID = "DB Assistant";

    private final Map<ConnectionId, Map<AssistantType, AssistantState>> assistantStates = new ConcurrentHashMap<>();
    private final Map<ConnectionId, Map<AssistantType, ChatBoxForm>> chatBoxes = new ConcurrentHashMap<>();
    private final Map<ConnectionId, AssistantType> selectedAssistantTypes = new ConcurrentHashMap<>();

    private DatabaseAssistantManager(Project project) {
        super(project, COMPONENT_NAME);

        ProjectEvents.subscribe(project, this,
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                fileEditorManagerListener());

        ProjectEvents.subscribe(project, this,
                ConnectionStatusListener.TOPIC,
                connectionStatusListener());
    }

    public static DatabaseAssistantManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseAssistantManager.class);
    }

    private FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenSelectionChanged(FileEditorManagerEvent event) {
                FileEditor editor = event.getNewEditor();
                ConnectionId connectionId = getConnectionId(getProject(), editor);
                switchToConnection(connectionId);
            }
        };
    }

    private ConnectionStatusListener connectionStatusListener() {
        return (connectionId, sessionId) -> {
            if (sessionId != SessionId.ASSISTANT) return;

            AssistantState assistantState = getAssistantState(connectionId, AssistantType.SELECT_AI); // TODO move to selectai code
            ConnectionHandler connection = ConnectionHandler.get(connectionId);
            if (connection == null) return;

            String resourceId = connection.getConnectionResourceId(SessionId.ASSISTANT);
            assistantState.setCurrentSessionSignature(resourceId);
        };
    }

    @NotNull
    public AssistantState getAssistantState(ConnectionId connectionId,  AssistantType assistantType) {
        var assistantStates = ensureAssistantStates(connectionId);
        return assistantStates.computeIfAbsent(assistantType, c -> new AssistantStateDelegate(getProject(), connectionId, assistantType));
    }

    @NotNull
    private Map<AssistantType, AssistantState> ensureAssistantStates(ConnectionId connectionId) {
        return this.assistantStates.computeIfAbsent(connectionId, c -> new ConcurrentHashMap<>());
    }

    public void showToolWindow(@Nullable ConnectionId connectionId) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(getProject());
        ToolWindow toolWindow = Failsafe.nn(toolWindowManager.getToolWindow(TOOL_WINDOW_ID));
        toolWindow.show(null);
        switchToConnection(connectionId);
    }

    public void initializeAssistant(ConnectionId connectionId, AssistantType assistantType) {
        AssistantAdapter assistantAdapter = AssistantAdapters.get(assistantType);
        assistantAdapter.initializeAssistant(connectionId);
    }

    /**
     * switch from current connection to the new selected one from DBN navigator
     *
     * @param connectionId the new selected connection
     */
    public void switchToConnection(@Nullable ConnectionId connectionId) {
        if (connectionId == null) return; // do not switch away if switched to a non-db context

        JPanel toolWindowPanel = getToolWindowPanel();
        if (toolWindowPanel == null) return;

        AssistantType assistantType = getSelectedAssistantType(connectionId);
        AssistantContext currentContext = getCurrentContext();
        AssistantContext targetContext = new AssistantContext(connectionId, assistantType);
        if (Objects.equals(currentContext, targetContext)) return;

        initToolWindow(connectionId, assistantType);
    }

    private @NotNull AssistantType getSelectedAssistantType(@NotNull ConnectionId connectionId) {
        return selectedAssistantTypes.computeIfAbsent(connectionId, c -> getPrefferedAssistantType(c));
    }

    private @NotNull AssistantType getPrefferedAssistantType(ConnectionId connectionId) {
        Map<AssistantType, AssistantState> assistantStates = this.assistantStates.get(connectionId);
        if (assistantStates == null) return AssistantType.PUBLIC;
        if (assistantStates.isEmpty()) return AssistantType.PUBLIC;

        Set<AssistantType> assistantTypes = assistantStates.keySet();
        for (AssistantType assistantType : assistantTypes) {
            AssistantState assistantState = assistantStates.get(assistantType);
            if (assistantState.isSupported()) return assistantType;
        }

        return assistantTypes.iterator().next();
    }

    public void switchToAssistant(ConnectionId connectionId, AssistantType assistantType) {
        JPanel toolWindowPanel = getToolWindowPanel();
        if (toolWindowPanel == null) return;

        AssistantContext currentContext = getCurrentContext();
        AssistantContext targetContext = new AssistantContext(connectionId, assistantType);

        if (Objects.equals(currentContext, targetContext)) return;
        initToolWindow(connectionId, assistantType);
    }

    @Nullable
    private AssistantContext getCurrentContext() {
        JPanel toolWindowPanel = getToolWindowPanel();
        if (toolWindowPanel == null) return null;

        String identifier = visibleCardId(toolWindowPanel);
        if (identifier == null) return null;
        if (isBlankCard(identifier)) return null;

        return new AssistantContext(identifier);
    }

    @Nullable
    private ChatBoxForm initChatBox(@Nullable ConnectionId connectionId, AssistantType assistantType) {
        if (connectionId == null) return null;

        JPanel toolWindowPanel = getToolWindowPanel();
        if (toolWindowPanel == null) return null;

        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection == null) return null;

        Map<AssistantType, ChatBoxForm> chatBoxes = this.chatBoxes.computeIfAbsent(connectionId, c -> new ConcurrentHashMap<>());
        return chatBoxes.computeIfAbsent(assistantType, id ->  createChatBox(connectionId, assistantType));
    }

    private ChatBoxForm createChatBox(ConnectionId connectionId, AssistantType assistantType) {
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection == null) return null;

        ChatBoxForm chatBox = new ChatBoxForm(connection, assistantType);
        JPanel toolWindowPanel = getToolWindowPanel();
        if (toolWindowPanel == null) return null;

        AssistantContext context = new AssistantContext(connectionId, assistantType);
        addCard(toolWindowPanel, chatBox, context);

        return chatBox;
    }

    public void interruptAssistantSession(ConnectionHandler connection) {
        // TODO invoke chat interruption utility as soon as available in "select ai"
        DBNConnection assistantConnection = connection.getConnectionPool().getSessionConnection(SessionId.ASSISTANT);
        if (assistantConnection == null) return;

        assistantConnection.invalidate();
    }

    @Nullable
    public ToolWindow getToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(getProject());
        return toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    }

    @Nullable
    private JPanel getToolWindowPanel() {
        ToolWindow toolWindow = getToolWindow();
        if (toolWindow == null) return null;

        Content content = toolWindow.getContentManager().getContent(0);
        return content == null ? null : (JPanel) content.getComponent();
    }

    private void initToolWindow(ConnectionId connectionId,  AssistantType assistantType) {
        selectedAssistantTypes.put(connectionId, assistantType);

        ToolWindow toolWindow = getToolWindow();
        if (toolWindow == null) return;

        JPanel toolWindowPanel = getToolWindowPanel();
        if (toolWindowPanel == null) return;

        ChatBoxForm chatBox = initChatBox(connectionId, assistantType);
        if (chatBox == null) {
            showBlankCard(toolWindowPanel);
        } else {
            AssistantContext context = new AssistantContext(connectionId, assistantType);
            showCard(toolWindowPanel, context);
            toolWindow.setAvailable(true);
        }
    }

    public void query(
            String prompt,
            ConnectionId connectionId,
            AssistantType assistantType,
            ChatContext chatContext,
            AssistantResponseConsumer responseConsumer) {

        AssistantState assistantState = getAssistantState(connectionId, assistantType);
        AssistantAdapter assistantAdapter = assistantState.getAssistantAdapter();
        assistantAdapter.generate(prompt, connectionId, chatContext, responseConsumer);
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/

    @Override
    public Element getComponentState() {
        Element element = newElement("state");
        Element statesElement = newElement(element, "assistants");
        for (ConnectionId connectionId : assistantStates.keySet()) {
            Map<AssistantType, AssistantState> assistantStates = this.assistantStates.get(connectionId);
            for (AssistantType assistantType : assistantStates.keySet()) {
                AssistantState assistantState = assistantStates.get(assistantType);
                Element stateElement = newElement(statesElement, "assistant-state");
                assistantState.writeState(stateElement);
            }
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element statesElement = element.getChild("assistants");
        if (statesElement != null) {
            List<Element> stateElements = statesElement.getChildren();
            for (Element stateElement : stateElements) {
                AssistantState assistantState = new AssistantStateDelegate(getProject());
                assistantState.readState(stateElement);

                ConnectionId connectionId = assistantState.getConnectionId();
                AssistantType assistantType = assistantState.getAssistantType();

                var assistantStates = ensureAssistantStates(connectionId);
                assistantStates.put(assistantType, assistantState);
            }
        }
    }
}
