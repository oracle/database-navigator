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

package com.dbn.connection.session;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.routine.Consumer;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.SessionId;
import com.dbn.connection.session.ui.CreateRenameSessionDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@State(
    name = DatabaseSessionManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseSessionManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseSessionManager";

    private DatabaseSessionManager(final Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseSessionManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseSessionManager.class);
    }

    public void showCreateSessionDialog(
            @NotNull ConnectionHandler connection,
            @Nullable Consumer<DatabaseSession> callback) {

        showCreateRenameSessionDialog(connection, null, callback);
    }

    public void showRenameSessionDialog(
            @NotNull DatabaseSession session,
            @Nullable Consumer<DatabaseSession> callback) {

        showCreateRenameSessionDialog(session.getConnection(), session, callback);
    }


    private void showCreateRenameSessionDialog(
            @NotNull ConnectionHandler connection,
            @Nullable DatabaseSession session,
            @Nullable Consumer<DatabaseSession> consumer) {

        Dialogs.show(() -> new CreateRenameSessionDialog(connection, session), (dialog, exitCode) -> {
            if (consumer == null) return;
            consumer.accept(dialog.getSession());
        });
    }

    public DatabaseSession createSession(ConnectionHandler connection, String name) {
        DatabaseSession session = connection.getSessionBundle().createSession(name);
        ProjectEvents.notify(getProject(),
                SessionManagerListener.TOPIC,
                (listener) -> listener.sessionCreated(session));
        return session;
    }

    public void renameSession(DatabaseSession session, String newName) {
        ConnectionHandler connection = session.getConnection();
        String oldName = session.getName();
        connection.getSessionBundle().renameSession(oldName, newName);
        ProjectEvents.notify(getProject(),
                SessionManagerListener.TOPIC,
                (listener) -> listener.sessionChanged(session));
    }

    public void deleteSession(@NotNull DatabaseSession session) {
        ConnectionHandler connection = session.getConnection();
        connection.getSessionBundle().deleteSession(session.getId());
        ProjectEvents.notify(getProject(),
                SessionManagerListener.TOPIC,
                (listener) -> listener.sessionDeleted(session));
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newElement("state");
        ConnectionManager connectionManager = ConnectionManager.getInstance(getProject());
        List<ConnectionHandler> connections = connectionManager.getConnectionBundle().getAllConnections();
        for (ConnectionHandler connection : connections) {
            Element connectionElement = newElement(element, "connection");
            connectionElement.setAttribute("id", connection.getConnectionId().id());

            List<DatabaseSession> sessions = connection.getSessionBundle().getSessions();
            for (DatabaseSession session : sessions) {
                if (session.isCustom()) {
                    Element sessionElement = newElement(connectionElement, "session");
                    sessionElement.setAttribute("name", session.getName());
                    sessionElement.setAttribute("id", session.getId().id());
                }
            }
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        for (Element connectionElement : element.getChildren()) {
            ConnectionId connectionId = connectionIdAttribute(connectionElement, "id");
            ConnectionHandler connection = ConnectionHandler.get(connectionId);

            if (connection != null) {
                DatabaseSessionBundle sessionBundle = connection.getSessionBundle();
                for (Element sessionElement : connectionElement.getChildren()) {
                    String sessionName = stringAttribute(sessionElement, "name");
                    SessionId sessionId = SessionId.get(stringAttribute(sessionElement, "id"));
                    sessionBundle.addSession(sessionId, sessionName);
                }
            }
        }
    }
}
