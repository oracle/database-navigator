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

package com.dbn.editor.session;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.option.InteractiveOptionBroker;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.common.util.TimeUtil;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.session.model.SessionBrowserModel;
import com.dbn.editor.session.options.SessionBrowserSettings;
import com.dbn.editor.session.options.SessionInterruptionOption;
import com.dbn.options.ProjectSettingsManager;
import com.dbn.vfs.file.DBSessionBrowserVirtualFile;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.notification.NotificationGroup.SESSION_BROWSER;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Commons.list;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@State(
    name = SessionBrowserManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class SessionBrowserManager extends ProjectComponentBase implements PersistentState {

    public static final String COMPONENT_NAME = "DBNavigator.Project.SessionEditorManager";
    public static final String EMPTY_CONTENT = "";

    private final List<DBSessionBrowserVirtualFile> openFiles = ContainerUtil.createConcurrentList();
    private Timer timestampUpdater;

    private SessionBrowserManager(Project project) {
        super(project, COMPONENT_NAME);

        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
    }

    public static SessionBrowserManager getInstance(@NotNull Project project) {
        return projectService(project, SessionBrowserManager.class);
    }

    private FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (file instanceof DBSessionBrowserVirtualFile) {
                    boolean schedule = openFiles.isEmpty();
                    DBSessionBrowserVirtualFile sessionBrowserFile = (DBSessionBrowserVirtualFile) file;
                    openFiles.add(sessionBrowserFile);

                    if (schedule) {
                        timestampUpdater = new Timer("DBN - Session Browser (timestamp update timer)");
                        timestampUpdater.schedule(new UpdateTimestampTask(), TimeUtil.Millis.ONE_SECOND, TimeUtil.Millis.ONE_SECOND);
                    }
                }
            }

            @Override
            public void whenFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (file instanceof DBSessionBrowserVirtualFile) {
                    DBSessionBrowserVirtualFile sessionBrowserFile = (DBSessionBrowserVirtualFile) file;
                    openFiles.remove(sessionBrowserFile);

                    if (openFiles.isEmpty() && timestampUpdater != null) {
                        Disposer.dispose(timestampUpdater);
                    }
                }
            }
        };
    }


    public SessionBrowserSettings getSessionBrowserSettings() {
        return ProjectSettingsManager.getInstance(getProject()).getOperationSettings().getSessionBrowserSettings();
    }

    public void openSessionBrowser(ConnectionHandler connection) {
        ConnectionAction.invoke("opening the session browser", false, connection,
                (action) -> {
                    Project project = getProject();
                    DBSessionBrowserVirtualFile sessionBrowserFile = connection.getSessionBrowserFile();
                    Editors.openFileEditor(project, sessionBrowserFile, true);
                });
    }

    public SessionBrowserModel loadSessions(DBSessionBrowserVirtualFile sessionBrowserFile) {
        ConnectionHandler connection = sessionBrowserFile.getConnection();
        try {
            return DatabaseInterfaceInvoker.load(HIGH,
                    "Loading sessions",
                    "Loading database sessions",
                    connection.getProject(),
                    connection.getConnectionId(),
                    conn -> {
                        DBNResultSet resultSet = null;
                        try {
                            DatabaseMetadataInterface metadata = connection.getMetadataInterface();
                            resultSet = (DBNResultSet) metadata.loadSessions(conn);
                            return new SessionBrowserModel(connection, resultSet);
                        } finally {
                            Resources.close(resultSet);
                        }
                    });

        } catch (SQLException e) {
            conditionallyLog(e);
            SessionBrowserModel model = new SessionBrowserModel(connection);
            model.setLoadError(e.getMessage());
            return model;
        }
    }

    public String loadSessionCurrentSql(ConnectionHandler connection, Object sessionId) {
        if (!DatabaseFeature.SESSION_CURRENT_SQL.isSupported(connection)) return EMPTY_CONTENT;

        try {
            return DatabaseInterfaceInvoker.load(HIGH,
                    "Loading session details",
                    "Loading current session details",
                    connection.getProject(),
                    connection.getConnectionId(),
                    conn -> {
                        ResultSet resultSet = null;
                        try {
                            DatabaseMetadataInterface metadata = connection.getMetadataInterface();
                            resultSet = metadata.loadSessionCurrentSql(sessionId, conn);
                            if (resultSet.next()) {
                                return resultSet.getString(1);
                            }
                        } finally {
                            Resources.close(resultSet);
                        }
                        return EMPTY_CONTENT;
            });
        } catch (SQLException e) {
            conditionallyLog(e);
            sendWarningNotification(SESSION_BROWSER, txt("ntf.sessions.error.FailedToLoadCurrentSql", e));
        }

        return EMPTY_CONTENT;
    }

    public void interruptSessions(@NotNull SessionBrowser sessionBrowser, List<SessionIdentifier> sessionIds, SessionInterruptionType type) {
        ConnectionHandler connection = Failsafe.nn(sessionBrowser.getConnection());
        if (DatabaseFeature.SESSION_INTERRUPTION_TIMING.isSupported(connection)) {

            SessionBrowserSettings sessionBrowserSettings = getSessionBrowserSettings();
            InteractiveOptionBroker<SessionInterruptionOption> disconnect =
                    type == SessionInterruptionType.TERMINATE ? sessionBrowserSettings.getKillSession() :
                    type == SessionInterruptionType.DISCONNECT  ? sessionBrowserSettings.getDisconnectSession() : null;

            if (disconnect != null) {
                String subject = sessionIds.size() > 1 ? "selected sessions" : "session with id \"" + sessionIds.iterator().next().toString() + "\"";
                disconnect.resolve(
                        list(subject, connection.getName()),
                        option -> {
                            if (option != SessionInterruptionOption.CANCEL && option != SessionInterruptionOption.ASK) {
                                doInterruptSessions(sessionBrowser, sessionIds, type, option);
                            }
                        });
            }
        } else {
            doInterruptSessions(sessionBrowser, sessionIds, SessionInterruptionType.TERMINATE, SessionInterruptionOption.NORMAL);
        }
    }

    private void doInterruptSessions(SessionBrowser sessionBrowser, List<SessionIdentifier> sessionIds, SessionInterruptionType type, SessionInterruptionOption option) {
        Progress.prompt(getProject(), sessionBrowser, true,
                txt("prc.sessions.title.InterruptingSessions"),
                type.taskAction(sessionIds.size()),
                progress -> {
                    try {
                        ConnectionHandler connection = sessionBrowser.getConnection();

                        progress.setIndeterminate(false);
                        Map<SessionIdentifier, SQLException> errors = new HashMap<>();
                        DatabaseMetadataInterface metadata = connection.getMetadataInterface();
                        int index = 0;
                        for (val entry : sessionIds) {
                            Object sessionId = entry.getSessionId();
                            Object serialNumber = entry.getSerialNumber();

                            // TODO NLS
                            checkDisposed();
                            progress.checkCanceled();
                            progress.setText(Strings.capitalize(type.disconnectingAction()) + " session id " + sessionId + " (serial " + serialNumber + ")");
                            progress.setFraction(Progress.progressOf(index, sessionIds.size()));

                            try {
                                boolean immediate = option == SessionInterruptionOption.IMMEDIATE;
                                boolean postTransaction = option == SessionInterruptionOption.POST_TRANSACTION;
                                DatabaseInterfaceInvoker.execute(HIGH, connection.getProject(), connection.getConnectionId(), conn -> {
                                    if (type == SessionInterruptionType.DISCONNECT) metadata.disconnectSession(sessionId, serialNumber, postTransaction, immediate, conn); else
                                    if (type == SessionInterruptionType.TERMINATE) metadata.terminateSession(sessionId, serialNumber, immediate, conn);
                                });
                            } catch (SQLException e) {
                                conditionallyLog(e);
                                errors.put(entry, e);
                            }
                            index++;
                        }

                        promptInterruptionResult(connection, sessionIds, errors, type);
                    } finally {
                        sessionBrowser.loadSessions(false);
                    }
                });
    }

    private void promptInterruptionResult(ConnectionHandler connection, List<SessionIdentifier> idenrifiers, Map<SessionIdentifier, SQLException> errors, SessionInterruptionType type) {
        // TODO NLS
        DatabaseMessageParserInterface messageParserInterface = connection.getMessageParserInterface();

        Project project = getProject();
        int sessionCount = idenrifiers.size();
        String disconnectedAction = type.disconnectedAction();
        String disconnectingAction = type.disconnectingAction();

        if (sessionCount == 1) {
            SessionIdentifier identifier = idenrifiers.get(0);
            Object sessionId = identifier.getSessionId();
            if (errors.size() == 0) {
                Messages.showInfoDialog(project, "Info", "Session " + sessionId + " " + disconnectedAction + ".");
            } else {
                SQLException exception = errors.get(identifier);
                if (messageParserInterface.isSuccessException(exception)) {
                    Messages.showInfoDialog(project, "Info", "Session " + sessionId + " " + disconnectingAction + " requested.\n" + exception.getMessage());
                } else {
                    Messages.showErrorDialog(project, "Error " + disconnectingAction + " session " + sessionId + ".", exception);
                }

            }
        } else {
            if (errors.isEmpty()) {
                Messages.showInfoDialog(project, "Info", sessionCount + " sessions " + disconnectedAction + ".");
            } else {
                StringBuilder message = new StringBuilder();
                boolean success = Lists.allMatch(errors.values(), error -> messageParserInterface.isSuccessException(error));
                if (success) {
                    message.append(sessionCount);
                    message.append(" sessions ");
                    message.append(disconnectingAction);
                    message.append(" requested:");
                } else {
                    message.append("Error ");
                    message.append(disconnectingAction);
                    message.append(" one or more of the selected sessions:");
                }
                for (SessionIdentifier identifier : idenrifiers) {
                    SQLException exception = errors.get(identifier);
                    message.append("\n - session id ").append(identifier).append(": ");
                    if (exception == null) {
                        message.append(disconnectedAction);
                    } else {
                        message.append(exception.getMessage().trim());
                    }
                }
                if (success) {
                    Messages.showInfoDialog(project, "Info", message.toString());
                } else {
                    Messages.showErrorDialog(project, message.toString());
                }
            }
        }
    }

    private class UpdateTimestampTask extends TimerTask {
        @Override
        public void run() {
            if (openFiles.isEmpty()) return;

            Project project = getProject();
            if (isNotValid(project)) return;

            List<SessionBrowser> sessionBrowsers = Editors.getFileEditors(project, SessionBrowser.class);
            if (sessionBrowsers.isEmpty()) return;

            Dispatch.run(() -> {
                for (SessionBrowser sessionBrowser : sessionBrowsers) {
                    sessionBrowser.refreshLoadTimestamp();
                }
            });
        }
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(timestampUpdater);
        super.disposeInner();
    }


    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        return newElement("state");
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
    }
}
