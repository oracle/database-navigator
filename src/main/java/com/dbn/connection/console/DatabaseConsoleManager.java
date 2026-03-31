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

package com.dbn.connection.console;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.Write;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.common.util.Titles;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.connection.console.ui.CreateRenameConsoleDialog;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.connection.session.DatabaseSessionBundle;
import com.dbn.connection.session.SessionManagerListener;
import com.dbn.editor.code.options.CodeEditorChangesOption;
import com.dbn.editor.code.options.CodeEditorConfirmationSettings;
import com.dbn.editor.code.options.CodeEditorSettings;
import com.dbn.object.DBConsole;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DBConsoleType;
import com.dbn.vfs.DatabaseFileManager;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.file.util.VirtualFiles.createFileDeleteEvent;
import static com.dbn.common.file.util.VirtualFiles.createFileRenameEvent;
import static com.dbn.common.file.util.VirtualFiles.notifiedFileChange;
import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;
import static com.dbn.common.util.Commons.array;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Naming.nextNumberedIdentifier;
import static com.dbn.common.util.Strings.isOneOf;
import static com.dbn.connection.config.ConnectionConfigListener.whenNameChanged;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.editor.code.options.CodeEditorChangesOption.CANCEL;
import static com.dbn.editor.code.options.CodeEditorChangesOption.DISCARD;
import static com.dbn.editor.code.options.CodeEditorChangesOption.SAVE;
import static com.dbn.nls.NlsResources.txt;

@State(
    name = DatabaseConsoleManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseConsoleManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseConsoleManager";

    private DatabaseConsoleManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, SessionManagerListener.TOPIC, sessionManagerListener);
        ProjectEvents.subscribe(project, this, FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, fileEditorManagerListenerBefore());
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC, whenNameChanged((id, oldName) -> renameConsoles(id, oldName)));
    }

    private void renameConsoles(ConnectionId id, String oldName) {
        ConnectionHandler connection = ConnectionHandler.get(id);
        if (connection == null) return;

        String newName = connection.getName();
        List<DBConsole> consoles = connection.getConsoleBundle().getConsoles();
        for (DBConsole console : consoles) {
            String consoleName = console.getName();
            if (consoleName.startsWith(oldName)) {
                consoleName = newName + consoleName.substring(oldName.length());
                renameConsole(console, consoleName);
            }
        }
    }

    private FileEditorManagerListener.Before fileEditorManagerListenerBefore() {
        return new FileEditorManagerListener.Before() {
            @Override
            public void beforeFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (!(file instanceof DBConsoleVirtualFile consoleFile)) return;

                DBConsole console = consoleFile.getConsole();
                if (!console.isTemporary()) return;

                Project project = source.getProject();

                CodeEditorSettings editorSettings = CodeEditorSettings.getInstance(project);
                CodeEditorConfirmationSettings confirmationSettings = editorSettings.getConfirmationSettings();
                confirmationSettings.getTemporaryConsole().resolve(project,
                        array(console.getName(), console.getSource()),
                        option -> processCodeChangeOption(consoleFile, option));

            }
        };
    }

    private void processCodeChangeOption(DBConsoleVirtualFile consoleFile, CodeEditorChangesOption option) {
        Project project = getProject();
        DBConsole console = consoleFile.getConsole();

        if (option == CANCEL) {
            FileEditorManager editorManager = FileEditorManager.getInstance(project);
            editorManager.openFile(consoleFile, true);
            return;
        }
        if (option == SAVE) {
            console.setTemporary(false);
            return;
        }

        if (option == DISCARD) {
            removeConsole(console);
        }
    }

    public static DatabaseConsoleManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseConsoleManager.class);
    }

    public void showCreateConsoleDialog(ConnectionHandler connection, DBConsoleType consoleType) {
        showCreateRenameConsoleDialog(connection, null, consoleType);
    }

    public void showRenameConsoleDialog(@NotNull DBConsole console) {
        ConnectionHandler connection = console.getConnection();
        showCreateRenameConsoleDialog(
                connection,
                console,
                console.getConsoleType());
    }


    private void showCreateRenameConsoleDialog(ConnectionHandler connection, DBConsole console, DBConsoleType consoleType) {
        Dialogs.show(() -> console == null ?
                new CreateRenameConsoleDialog(connection, consoleType) :
                new CreateRenameConsoleDialog(connection, console));
    }

    public String getNextConsoleName(ConnectionHandler connection) {
        Set<String> consoleNames = connection.getConsoleBundle().getConsoleNames();
        String baseName = connection.getName() + " 1";
        return nextNumberedIdentifier(baseName, true, () -> consoleNames);
    }

    public void createConsole(ConnectionHandler connection, String name, String content, DBConsoleType type, @Nullable Consumer<DBConsole> consumer) {
        Project project = connection.getProject();
        Progress.background(project, connection, true,
                txt("prc.consoles.title.CreatingConsole"),
                txt("prc.consoles.text.CreatingConsole", type.getName(), name),
                indicator -> {
                    DBConsole console = connection.getConsoleBundle().createConsole(name, type);
                    DBConsoleVirtualFile consoleFile = console.getVirtualFile();
                    consoleFile.setContent(content);

                    reloadConsoles(connection);
                    if (consumer != null) {
                        consumer.accept(console);
                    }
                    Editors.openFileEditor(project, consoleFile, true);
                });
    }

    public void renameConsole(@NotNull DBConsole console, String newName) {
        String oldName = console.getName();
        if (Objects.equals(oldName, newName)) return;

        ConnectionHandler connection = console.getConnection();
        DatabaseConsoleBundle consoleBundle = connection.getConsoleBundle();

        DBConsoleVirtualFile virtualFile = console.getVirtualFile();
        VFileEvent renameEvent = createFileRenameEvent(virtualFile, oldName, newName);
        notifiedFileChange(renameEvent, () -> consoleBundle.renameConsole(oldName, newName));

        reloadConsoles(connection);
    }

    public void deleteConsole(DBConsole console) {
        Messages.showQuestionDialog(
                getProject(),
                txt("msg.consoles.title.DeleteConsole"),
                txt("msg.consoles.question.DeleteConsole"),
                Messages.OPTIONS_YES_NO, 0,
                option -> when(option == 0, () -> removeConsole(console)));

    }

    private void removeConsole(DBConsole console) {
        Project project = console.getProject();
        ConnectionHandler connection = console.getConnection();
        DatabaseConsoleBundle consoleBundle = connection.getConsoleBundle();

        DBConsoleVirtualFile consoleFile = console.getVirtualFile();

        DatabaseFileManager fileManager = DatabaseFileManager.getInstance(project);
        fileManager.closeFile(consoleFile);

        VFileEvent deleteEvent = createFileDeleteEvent(consoleFile);
        notifiedFileChange(deleteEvent, () -> consoleBundle.removeConsole(console));

        reloadConsoles(connection);
    }

    private void reloadConsoles(@NotNull ConnectionHandler connection) {
        DBObjectBundle objectBundle = connection.getObjectBundle();
        DBObjectList<?> objectList = objectBundle.getObjectList(DBObjectType.CONSOLE);
        if (objectList == null) return;

        objectList.markDirty();
    }

    public void saveConsoleToFile(DBConsoleVirtualFile consoleFile) {
        Project project = getProject();
        String consoleName = consoleFile.getName();
        FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor(
                Titles.signed(txt("msg.consoles.title.SaveToFile")),
                txt("msg.consoles.info.SaveToFile", consoleName), "sql");

        FileChooserFactory fileChooserFactory = FileChooserFactory.getInstance();
        FileSaverDialog fileSaverDialog = fileChooserFactory.createSaveFileDialog(fileSaverDescriptor, project);
        Document document = Documents.getDocument(consoleFile);
        if (document == null) return;

        VirtualFileWrapper fileWrapper = fileSaverDialog.save((VirtualFile) null, consoleName);
        if (fileWrapper == null) return;

        VirtualFile file = fileWrapper.getVirtualFile(true);
        if (file == null) return;

        byte[] content = document.getCharsSequence().toString().getBytes();
        Write.run(project, () -> {
            try {
                file.setBinaryContent(content);
            } catch (IOException e) {
                conditionallyLog(e);
                String fileName = fileWrapper.getFile().getName();
                Messages.showErrorDialog(project,
                        txt("msg.consoles.title.CouldNotSaveToFile"),
                        txt("msg.consoles.error.CouldNotSaveToFile", fileName), e);
            }
        });

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        contextManager.setConnection(file, consoleFile.getConnection());
        contextManager.setDatabaseSchema(file, consoleFile.getSchemaId());
        contextManager.setDatabaseSession(file, consoleFile.getSession());
        Editors.openFileEditor(project, file, true);
    }

    /***************************************
     *         SessionManagerListener      *
     ***************************************/
    private final SessionManagerListener sessionManagerListener = new SessionManagerListener() {
        @Override
        public void sessionDeleted(DatabaseSession session) {
            ConnectionManager connectionManager = ConnectionManager.getInstance(getProject());
            List<ConnectionHandler> connections = connectionManager.getConnectionBundle().getAllConnections();
            for (ConnectionHandler connection : connections) {
                List<DBConsole> consoles = connection.getConsoleBundle().getConsoles();
                for (DBConsole console : consoles) {
                    DBConsoleVirtualFile virtualFile = console.getVirtualFile();
                    if (virtualFile.getSession() == session) {
                        DatabaseSession mainSession = connection.getSessionBundle().getMainSession();
                        virtualFile.setDatabaseSession(mainSession);
                    }
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
        Element element = newElement("state");
        ConnectionManager connectionManager = ConnectionManager.getInstance(getProject());
        List<ConnectionHandler> connections = connectionManager.getConnectionBundle().getAllConnections();
        for (ConnectionHandler connection : connections) {
            Element connectionElement = newElement(element, "connection");
            connectionElement.setAttribute("id", connection.getConnectionId().id());

            List<DBConsole> consoles = connection.getConsoleBundle().getConsoles();
            for (DBConsole console : consoles) {
                DBConsoleVirtualFile file = console.getVirtualFile();
                Element consoleElement = newElement(connectionElement, "console");

                DatabaseSession databaseSession = Commons.nvl(
                        file.getSession(),
                        connection.getSessionBundle().getMainSession());

                setStringAttribute(consoleElement, "name", console.getName());
                setStringAttribute(consoleElement, "type", console.getConsoleType().name());
                setStringAttribute(consoleElement, "schema", file.getDatabaseSchemaName());
                setStringAttribute(consoleElement, "session", databaseSession.getName());

                Set<String> attributeNames = file.getAttributeNames();
                for (String attributeName : attributeNames) {
                    String attributeValue = file.getAttribute(attributeName);
                    setStringAttribute(consoleElement, attributeName, attributeValue);
                }


                writeCdata(consoleElement, file.getContent().exportContent());
            }
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        for (Element connectionElement : element.getChildren()) {
            ConnectionId connectionId = connectionIdAttribute(connectionElement, "id");
            ConnectionHandler connection = ConnectionHandler.get(connectionId);
            if (isNotValid(connection)) continue;

            DatabaseConsoleBundle consoleBundle = connection.getConsoleBundle();
            for (Element consoleElement : connectionElement.getChildren()) {
                String consoleName = stringAttribute(consoleElement, "name");

                // schema
                String schema = stringAttribute(consoleElement, "schema");

                // session
                String session = stringAttribute(consoleElement, "session");
                DatabaseSessionBundle sessionBundle = connection.getSessionBundle();
                DatabaseSession databaseSession = Strings.isEmpty(session) ?
                        sessionBundle.getMainSession() :
                        sessionBundle.getSession(session);
                // type
                DBConsoleType consoleType = enumAttribute(consoleElement, "type", DBConsoleType.STANDARD);
                DBConsole console = consoleBundle.getConsole(consoleName, consoleType, true);
                DBConsoleVirtualFile file = console.getVirtualFile();

                // attributes
                for (Attribute attribute : consoleElement.getAttributes()) {
                    String attributeName = attribute.getName();
                    if (isOneOf(attributeName, "name", "schema", "session", "type")) continue;

                    String attributeValue = attribute.getValue();
                    file.setAttribute(attributeName, attributeValue);
                }


                String consoleText = readCdata(consoleElement);

                file.setContent(consoleText);
                file.setDatabaseSchemaName(schema);
                file.setDatabaseSession(databaseSession);
            }
        }
    }
}
