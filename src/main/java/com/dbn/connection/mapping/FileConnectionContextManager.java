package com.dbn.connection.mapping;

import com.dbn.DatabaseNavigator;
import com.dbn.common.action.Selectable;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.file.FileMappings;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Documents;
import com.dbn.connection.*;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.connection.mapping.ConnectionContextActions.ConnectionSetupAction;
import com.dbn.connection.mapping.ConnectionContextActions.SchemaSelectAction;
import com.dbn.connection.mapping.ConnectionContextActions.SessionCreateAction;
import com.dbn.connection.mapping.ConnectionContextActions.SessionSelectAction;
import com.dbn.connection.mapping.ui.FileConnectionMappingDialog;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.connection.session.SessionManagerListener;
import com.dbn.object.DBSchema;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.IncorrectOperationException;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.file.util.VirtualFiles.isLocalFileSystem;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Files.isDbLanguageFile;
import static com.dbn.common.util.Messages.options;
import static com.dbn.common.util.Messages.showWarningDialog;
import static com.dbn.connection.ConnectionHandler.isLiveConnection;
import static com.dbn.connection.ConnectionSelectorOptions.Option.*;
import static com.dbn.connection.ConnectionType.*;
import static com.dbn.connection.mapping.ConnectionContextActions.ConnectionSelectAction;

@State(
    name = FileConnectionContextManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
@Slf4j
public class FileConnectionContextManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.FileConnectionMappingManager";

    @Getter
    private final FileConnectionContextRegistry registry;

    private FileConnectionContextManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        this.registry = new FileConnectionContextRegistry(project);
        Disposer.register(this, this.registry);

        ProjectEvents.subscribe(project, this, SessionManagerListener.TOPIC, sessionManagerListener);
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC, connectionConfigListener);
    }

    @NotNull
    public static FileConnectionContextManager getInstance(@NotNull Project project) {
        return projectService(project, FileConnectionContextManager.class);
    }

    private final ConnectionConfigListener connectionConfigListener = new ConnectionConfigListener() {
        @Override
        public void connectionRemoved(ConnectionId connectionId) {
            registry.connectionRemoved(connectionId);
        }
    };

    public static boolean hasConnectivityContext(VirtualFile file) {
        Boolean hasConnectivityContext = file.getUserData(UserDataKeys.HAS_CONNECTIVITY_CONTEXT);
        return hasConnectivityContext == null || hasConnectivityContext;
    }

    public void openFileConnectionMappings() {
        Dialogs.show(() -> new FileConnectionMappingDialog(getProject()));
    }


    public void removeMapping(VirtualFile file) {
        notifiedChange(
                () -> registry.removeMapping(file),
                handler -> handler.mappingChanged(getProject(), file));
    }

    /*******************************************************************
     *                    Connection mappings                          *
     *******************************************************************/

    @Nullable
    public ConnectionId getConnectionId(@NotNull VirtualFile virtualFile) {
        ConnectionHandler connection = getConnection(virtualFile);
        return connection == null ? null : connection.getConnectionId();
    }

    @Nullable
    public ConnectionHandler getConnection(@NotNull VirtualFile virtualFile) {
        return registry.getDatabaseConnection(virtualFile);
    }

    public boolean setConnection(VirtualFile file, ConnectionHandler connection) {
        if (isConnectionSelectable(file)) {
            return notifiedChange(
                    () -> registry.setConnectionHandler(file, connection),
                    handler -> handler.connectionChanged(getProject(), file, connection));
        }
        return false;
    }

    public void setConnection(@NotNull Editor editor, @Nullable ConnectionHandler connection) {
        Document document = editor.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        boolean changed = setConnection(file, connection);
        if (!changed) return;
        
        // TODO add as FileConnectionMappingListener.TOPIC
        Documents.touchDocument(editor, true);
    }

    /*******************************************************************
     *                        Schema mappings                          *
     *******************************************************************/
    @Nullable
    public SchemaId getDatabaseSchema(@NotNull VirtualFile virtualFile) {
        return registry.getDatabaseSchema(virtualFile);
    }

    public boolean setDatabaseSchema(VirtualFile file, SchemaId schema) {
        if (!isSchemaSelectable(file)) return false;
        
        return notifiedChange(
                () -> registry.setDatabaseSchema(file, schema),
                handler -> handler.schemaChanged(getProject(), file, schema));
    }

    public void setDatabaseSchema(@NotNull Editor editor, SchemaId schema) {
        Document document = editor.getDocument();
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        boolean changed = setDatabaseSchema(virtualFile, schema);
        if (!changed) return;
        
        // TODO add as FileConnectionMappingListener.TOPIC
        Documents.touchDocument(editor, false);
    }

    /*******************************************************************
     *                       Session mappings                          *
     *******************************************************************/
    @Nullable
    public DatabaseSession getDatabaseSession(@NotNull VirtualFile virtualFile) {
        return registry.getDatabaseSession(virtualFile);
    }

    public boolean setDatabaseSession(VirtualFile file, DatabaseSession session) {
        if (!isSessionSelectable(file)) return false;
        
        return notifiedChange(() -> registry.setDatabaseSession(file, session),
                consumer -> consumer.sessionChanged(getProject(), file, session));
    }

    public void setDatabaseSession(@NotNull Editor editor, DatabaseSession session) {
        Document document = editor.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        setDatabaseSession(file, session);
    }



    @Nullable
    public FileConnectionContext getMapping(@NotNull VirtualFile file) {
        return registry.getFileConnectionContext(file);
    }


    public boolean isConnectionSelectable(VirtualFile file) {
        if (isNotValid(file)) return false;
        if (isLocalFileSystem(file)) return true;
        if (!isDbLanguageFile(file)) return false;

        if (file instanceof DBConsoleVirtualFile) {
            // consoles are tightly bound to connections
            return false;
        }

        if (file instanceof LightVirtualFile) {
            return hasConnectivityContext(file);
        }

        return false;
    }

    public boolean isSchemaSelectable(VirtualFile file) {
        if (isNotValid(file)) return false;
        if (isLocalFileSystem(file)) return hasConnectivityContext(file);
        if (!isDbLanguageFile(file)) return false;

        if (file instanceof DBConsoleVirtualFile) return true;
        if (file instanceof LightVirtualFile) return hasConnectivityContext(file);

        return false;
    }

    public boolean isSessionSelectable(VirtualFile file) {
        if (isNotValid(file)) return false;
        if (!isDbLanguageFile(file)) return false;
        if (isLocalFileSystem(file)) return true;

        if (file instanceof DBConsoleVirtualFile) return true;
        if (file instanceof LightVirtualFile) return hasConnectivityContext(file);

        return false;
    }


    @SneakyThrows
    private boolean notifiedChange(Callable<Boolean> action, Consumer<FileConnectionContextListener> consumer) {
        if (action.call()) {
            ProjectEvents.notify(getProject(),
                    FileConnectionContextListener.TOPIC,
                    listener -> consumer.accept(listener));
            return true;
        }
        return false;
    }

    public void selectConnectionAndSchema(@NotNull VirtualFile file, DataContext dataContext, @NotNull Runnable callback) {
        Dispatch.run(() -> {
            Project project = getProject();
            ConnectionHandler activeConnection = getConnection(file);
            if (!isLiveConnection(activeConnection)) {
                String message =
                        activeConnection == null ?
                                "The file is not linked to any connection.\nTo continue with the statement execution please select a target connection." :
                                "The connection you selected for this file is a virtual connection, used only to decide the SQL dialect.\n" +
                                        "You can not execute statements against this connection. Please select a proper connection to continue.";


                ConnectionSelectorOptions options = ConnectionSelectorOptions.options(
                        SHOW_CREATE_CONNECTION,
                        PROMPT_SCHEMA_SELECTION);

                showWarningDialog(project,
                        "No valid connection", message,
                        options("Select Connection", "Cancel"), 0,
                        option -> when(option == 0, () ->
                                promptConnectionSelector(file, dataContext, options, callback)));

            } else if (getDatabaseSchema(file) == null) {
                String message =
                        "You did not select any schema to run the statement against.\n" +
                                "To continue with the statement execution please select a schema.";
                showWarningDialog(project,
                        "No schema selected", message,
                        options("Use Current Schema", "Select Schema", "Cancel"), 0,
                        (option) -> {
                            if (option == 0) {
                                callback.run();
                            } else if (option == 1) {
                                promptSchemaSelector(file, dataContext, callback);
                            }
                        });
            } else {
                callback.run();
            }
        });
    }

    /***************************************************
     *             Select connection popup             *
     ***************************************************/
    public void promptConnectionSelector(VirtualFile file, DataContext dataContext, ConnectionSelectorOptions options, Runnable callback) {
        Project project = getProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        List<ConnectionHandler> connections = connectionBundle.getConnections();

        List<AnAction> actions = new ArrayList<>();
        if (!connections.isEmpty()) {
            for (ConnectionHandler connection : connections) {
                ConnectionSelectAction connectionAction = new ConnectionSelectAction(
                        connection,
                        file,
                        options.is(PROMPT_SCHEMA_SELECTION),
                        callback);
                actions.add(connectionAction);
            }
        }

        if (options.is(SHOW_VIRTUAL_CONNECTIONS)) {
            actions.add(Separator.create());
            for (ConnectionHandler virtualConnectionHandler : connectionBundle.listVirtualConnections()) {
                ConnectionSelectAction connectionAction = new ConnectionSelectAction(
                        virtualConnectionHandler,
                        file,
                        options.is(PROMPT_SCHEMA_SELECTION),
                        callback);
                actions.add(connectionAction);
            }
        }

        if (options.is(SHOW_CREATE_CONNECTION)) {
            actions.add(Separator.create());
            actions.add(new ConnectionSetupAction(project));
        }

        Popups.showActionsPopup("Select Connection", dataContext, actions, Selectable.selector());
    }

    /***************************************************
     *             Select schema popup                 *
     ***************************************************/
    public void promptSchemaSelector(VirtualFile file, DataContext dataContext, Runnable callback) throws IncorrectOperationException {
        Project project = getProject();
        ConnectionHandler connection = getConnection(file);
        if (connection == null) return;

        ConnectionAction.invoke("selecting the current schema", true, connection,
                action -> Progress.prompt(project, connection, true,
                        "Loading schemas",
                        "Loading schemas for connection " + connection.getName(),
                        progress -> {
                            List<AnAction> actions = new ArrayList<>();
                            if (isLiveConnection(connection)) {
                                List<DBSchema> schemas = connection.getObjectBundle().getSchemas();
                                for (DBSchema schema : schemas) {
                                    SchemaSelectAction schemaAction = new SchemaSelectAction(file, schema, callback);
                                    actions.add(schemaAction);
                                }
                            }

                            Popups.showActionsPopup("Select Schema", dataContext, actions, Selectable.selector());
                        }));
    }


    /***************************************************
     *             Select schema popup                 *
     ***************************************************/
    public void promptSessionSelector(VirtualFile file, DataContext dataContext, Runnable callback) throws IncorrectOperationException {
        Project project = getProject();
        ConnectionAction.invoke(
                "selecting the current session", true,
                getConnection(file),
                (action) -> {
                    List<AnAction> actions = new ArrayList<>();
                    ConnectionHandler connection = action.getConnection();
                    if (isLiveConnection(connection)) {
                        List<DatabaseSession> sessions = connection.getSessionBundle().getSessions(MAIN, POOL, SESSION);
                        for (DatabaseSession session : sessions) {
                            SessionSelectAction sessionAction = new SessionSelectAction(file, session, callback);
                            actions.add(sessionAction);
                        }
                        actions.add(Separator.create());
                        actions.add(new SessionCreateAction(file, connection));
                    }

                    Popups.showActionsPopup("Select Session", dataContext, actions, Selectable.selector());
                });
    }

    /***************************************
     *         SessionManagerListener      *
     ***************************************/
    private final SessionManagerListener sessionManagerListener = new SessionManagerListener() {
        @Override
        public void sessionDeleted(DatabaseSession session) {
            for (FileConnectionContext mapping : registry.getMappings().values()) {
                if (session.getId() == mapping.getSessionId()) {
                    mapping.setSessionId(SessionId.MAIN);
                }
            }
        }
    };

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        for (FileConnectionContext mapping : registry.getMappings().values()) {
            Element mappingElement = newElement(element, "mapping");
            mapping.writeState(mappingElement);
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Progress.background(getProject(), null, false, "Restoring context", "Restoring file database connection context", indicator -> loadFileMappings(element, indicator));
    }

    private void loadFileMappings(@NotNull Element element, ProgressIndicator indicator) {
        FileConnectionContextRegistry registry = this.registry;
        if (registry == null) return;

        FileMappings<FileConnectionContext> mappings = registry.getMappings();
        List<Element> mappingElements = element.getChildren();
        int size = mappingElements.size();
        for (int i = 0; i < size; i++) {
            Element child = mappingElements.get(i);
            FileConnectionContext mapping = new FileConnectionContextImpl();
            mapping.readState(child);

            VirtualFile virtualFile = mapping.getFile();
            if (virtualFile == null) continue;

            double progress = Progress.progressOf(i, size);
            indicator.setFraction(progress);
            indicator.setText2(virtualFile.getPath());

            String fileUrl = mapping.getFileUrl();
            mappings.put(fileUrl, mapping);
        }
        mappings.cleanup();
    }
}

