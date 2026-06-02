package com.dbn.mcp;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.ui.McpServerDefinitionDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.mcp.build.McpMavenPluginSupport.verifyMavenAvailability;

@State(
        name = "DBNavigator.Project.MCPServerManager",
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class McpServerBuilderManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.McpServerBuilderManager";

    private final Map<ConnectionId, McpServerDefinition> serverDefinitions = new ConcurrentHashMap<>();

    public McpServerBuilderManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC, connectionConfigListener());
    }

    public static McpServerBuilderManager getInstance(Project project) {
        return Components.projectService(project, McpServerBuilderManager.class);
    }

    @NotNull
    private ConnectionConfigListener connectionConfigListener() {
        return new ConnectionConfigListener() {
            @Override
            public void connectionRemoved(ConnectionId connectionId) {
                // remove server definitions when connection configs are deleted
                serverDefinitions.remove(connectionId);
            }
        };
    }

    public McpServerDefinition getServerDefinition(ConnectionId connectionId) {
        return serverDefinitions.computeIfAbsent(connectionId, c -> new McpServerDefinition());
    }

    public void setServerDefinition(ConnectionId connectionId, McpServerDefinition definition) {
        serverDefinitions.put(connectionId, definition);
    }

    public void openMCPBuilder(@NotNull ConnectionHandler connection) {
        Project project = connection.getProject();
        verifyMavenAvailability(project);

        McpServerBuilderManager builderManager = McpServerBuilderManager.getInstance(project);
        McpServerDefinition serverDefinition = builderManager.getServerDefinition(connection.getConnectionId());
        Dialogs.show(() -> new McpServerDefinitionDialog(connection, serverDefinition));
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        Element definitionsElement = newElement(element, "server-definitions");
        for (ConnectionId connectionId : serverDefinitions.keySet()) {
            Element definitionElement = newElement(definitionsElement, "server-definition");

            setConstantAttribute(definitionElement, "connection-id", connectionId);
            McpServerDefinition serverDefinition = serverDefinitions.get(connectionId);
            serverDefinition.writeState(definitionElement);
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element definitionsElement = element.getChild("server-definitions");
        List<Element> definitionElements = childrenOf(definitionsElement, "server-definition");
        for (Element definitionElement : definitionElements) {
            ConnectionId connectionId = constantAttribute(definitionElement, "connection-id", ConnectionId.class);
            McpServerDefinition embeddingRequest = new McpServerDefinition();
            serverDefinitions.put(connectionId, embeddingRequest);
            embeddingRequest.readState(definitionElement);
        }
    }
}
