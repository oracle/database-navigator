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

package com.dbn.assistant;

import com.dbn.DatabaseNavigator;
import com.dbn.assistant.adapter.AssistantAdapter;
import com.dbn.assistant.adapter.AssistantAdapters;
import com.dbn.assistant.adapter.AssistantResponseConsumer;
import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.window.ui.ChatBoxFormContainer;
import com.dbn.assistant.state.AssistantSelectionState;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateDelegate;
import com.dbn.assistant.state.AssistantStateListener;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.window.ToolWindows;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.ConnectionStatusListener;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.SessionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.ui.content.Content;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.util.ContextLookup.getConnectionId;

/**
 * Main database AI-Assistance management component
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
@Slf4j
@Getter
@State(
        name = DatabaseAssistantManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class DatabaseAssistantManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseAssistantManager";
    public static final @NonNls String TOOL_WINDOW_ID = "DB Assistant";

    private final Map<ConnectionId, Map<AssistantType, AssistantState>> assistantStates = new ConcurrentHashMap<>();
    private final Map<ConnectionId, AssistantType> selectedAssistantTypes = new ConcurrentHashMap<>();
    private final AssistantSelectionState selectionState = new AssistantSelectionState();

    private DatabaseAssistantManager(Project project) {
        super(project, COMPONENT_NAME);

        ProjectEvents.subscribe(project, this,
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                fileEditorManagerListener());

        ProjectEvents.subscribe(project, this,
                ConnectionStatusListener.TOPIC,
                connectionStatusListener());

        ProjectEvents.subscribe(project, this,
                ConnectionConfigListener.TOPIC,
                configChangeListener());
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
                switchContext(connectionId);
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

    private ConnectionConfigListener configChangeListener() {
        return new ConnectionConfigListener() {
            @Override
            public void connectionRemoved(ConnectionId connectionId) {
                ChatBoxFormContainer container = getFormContainer();
                if (container == null) return;

                container.removeCards(connectionId);
            }
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
        showToolWindow(connectionId, null);
    }

    public void showToolWindow(@Nullable ConnectionId connectionId, @Nullable AssistantType assistantType) {
        ToolWindow toolWindow = nn(ToolWindows.getToolWindow(getProject(), TOOL_WINDOW_ID));
        toolWindow.show(null);
        switchContext(connectionId, assistantType);
    }

    public void initializeAssistant() {
        Project project = getProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        List<ConnectionHandler> connections = connectionManager.getConnections();
        if (connections.isEmpty()) return;

        ConnectionHandler connection = connections.get(0);
        ConnectionId connectionId = connection.getConnectionId();

        switchContext(connectionId);
    }

    public void initializeAssistant(ConnectionId connectionId, AssistantType assistantType) {
        AssistantAdapter assistantAdapter = AssistantAdapters.get(assistantType);
        assistantAdapter.initializeAssistant(connectionId);
    }

    public boolean divertNotificationBalloon() {
        // notification balloons overlapping with assistant input field

        ToolWindow toolWindow = geToolWindow();
        if (toolWindow == null) return false;
        if (!toolWindow.isVisible()) return false;
        if (toolWindow.getType() != ToolWindowType.DOCKED) return false;
        if (toolWindow.getAnchor() != ToolWindowAnchor.RIGHT) return false;

        return true;
    }

    private @Nullable ToolWindow geToolWindow() {
        return ToolWindows.getToolWindow(getProject(), TOOL_WINDOW_ID);
    }

    /**
     * switch from current connection to the new selected one from DBN navigator
     *
     * @param connectionId the new selected connection
     */
    public void switchContext(@Nullable ConnectionId connectionId) {
        if (connectionId == null) return; // do not switch away if switched to a non-db context

        AssistantType assistantType = getSelectedAssistantType(connectionId);
        switchContext(connectionId, assistantType);
    }

    public void switchContext(@Nullable ConnectionId connectionId, AssistantType assistantType) {
        if (connectionId == null) return; // do not switch away if switched to a non-db context

        ToolWindow toolWindow = getToolWindow();
        if (toolWindow == null) return;

        ChatBoxFormContainer container = getFormContainer();
        if (container == null) return;

        if (assistantType == null) assistantType = getSelectedAssistantType(connectionId);
        if (container.matchesCurrentContext(connectionId, assistantType)) return;

        selectedAssistantTypes.put(connectionId, assistantType);
        boolean initialized = container.initCard(connectionId, assistantType);
        if (initialized) {
            toolWindow.setAvailable(true);
        }
    }

    public void startAssistantChat(String sourceId, ConnectionId connectionId, AssistantType assistantType, AssistantMode assistantMode, DBObjectRef<DBTable> embeddingTable) {
        switchContext(connectionId, assistantType);
        AssistantState assistantState = getAssistantState(connectionId, assistantType);

        Chat chat = assistantState.getChatForSource(sourceId);
        if (chat == null) {
            ChatContext context = assistantState.getCurrentContext();
            chat = assistantState.createChat(context);
            chat.setSourceId(sourceId);
        } else {
            assistantState.setCurrentChatId(chat.getId());
        }

        ChatContext chatContext = chat.getContext();
        chatContext.setAssistantMode(assistantMode);
        chatContext.setEmbeddingTable(embeddingTable);

        ToolWindow toolWindow = getToolWindow();
        if (toolWindow == null) return;

        ChatBoxFormContainer container = getFormContainer();
        if (container == null) return;

        toolWindow.show(null);
        container.focusInputField();
    }

    @NotNull
    private AssistantType getSelectedAssistantType(@NotNull ConnectionId connectionId) {
        AssistantType assistantType = selectedAssistantTypes.computeIfAbsent(connectionId, c -> getPrefferedAssistantType(c));
        if (assistantType != AssistantType.SELECT_AI) return assistantType;

        // reset old SELECT_AI mappings against non-oracle connections
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection == null) return AssistantType.PUBLIC;
        if (connection.getDatabaseType() != DatabaseType.ORACLE) {
            assistantType = AssistantType.PUBLIC;
            selectedAssistantTypes.put(connectionId, assistantType);
        }
        return assistantType;
    }

    @NotNull
    private AssistantType getPrefferedAssistantType(ConnectionId connectionId) {
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

    public void interruptAssistantSession(ConnectionHandler connection) {
        // TODO invoke chat interruption utility as soon as available in "select ai"
        DBNConnection assistantConnection = connection.getConnectionPool().getSessionConnection(SessionId.ASSISTANT);
        if (assistantConnection == null) return;

        assistantConnection.invalidate();
    }

    @Nullable
    public ToolWindow getToolWindow() {
        return geToolWindow();
    }

    @Nullable
    private ChatBoxFormContainer getFormContainer() {
        ToolWindow toolWindow = getToolWindow();
        if (toolWindow == null) return null;

        Content content = toolWindow.getContentManager().getContent(0);
        return content == null ? null : (ChatBoxFormContainer) content.getComponent();
    }

    public void query(
            String prompt,
            String chatId,
            ConnectionId connectionId,
            AssistantType assistantType,
            ChatContext chatContext,
            AssistantResponseConsumer responseConsumer) {

        AssistantState assistantState = getAssistantState(connectionId, assistantType);
        AssistantAdapter assistantAdapter = assistantState.getAssistantAdapter();
        String preparedPrompt = assistantAdapter.preparePrompt(connectionId, chatContext, prompt);

        assistantAdapter.checkContext(connectionId, chatContext, () -> query(
                preparedPrompt,
                chatId, connectionId,
                chatContext,
                assistantAdapter,
                responseConsumer));
    }

    private static void query(
            String prompt,
            String chatId,
            ConnectionId connectionId,
            ChatContext chatContext,
            AssistantAdapter assistantAdapter,
            AssistantResponseConsumer responseConsumer) {
        Background.run(() -> assistantAdapter.generate(
                prompt,
                chatId,
                connectionId,
                chatContext,
                responseConsumer));
    }

    public String generateTitle(
            String chatId,
            ConnectionId connectionId,
            ChatContext context,
            AssistantType assistantType) throws Exception {
        AssistantState assistantState = getAssistantState(connectionId, assistantType);
        AssistantAdapter assistantAdapter = assistantState.getAssistantAdapter();
        return assistantAdapter.generateTitle(chatId, connectionId, context);
    }

    public void notifyConfigChanges() {
        Project project = getProject();
        Set<ConnectionId> connectionIds = getAssistantStates().keySet();
        connectionIds.forEach(connectionId -> ProjectEvents.notify(project, AssistantStateListener.TOPIC, l -> l.stateChanged(project, connectionId)));
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

                boolean selected = selectedAssistantTypes.get(connectionId) == assistantType;
                if (selected) setBooleanAttribute(stateElement, "selected", true);
            }
        }
        Element selectionElement = newElement(element, "selection-state");
        selectionState.writeState(selectionElement);

        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element statesElement = element.getChild("assistants");
        List<Element> stateElements = childrenOf(statesElement);
        for (Element stateElement : stateElements) {
            AssistantState assistantState = new AssistantStateDelegate(getProject());
            assistantState.readState(stateElement);

            ConnectionId connectionId = assistantState.getConnectionId();
            AssistantType assistantType = assistantState.getAssistantType();

            var assistantStates = ensureAssistantStates(connectionId);
            assistantStates.put(assistantType, assistantState);

            boolean selected = booleanAttribute(stateElement, "selected", false);
            if (selected) selectedAssistantTypes.put(connectionId, assistantType);
        }

        Element selectionElement = element.getChild("selection-state");
        selectionState.readState(selectionElement);
    }
}
