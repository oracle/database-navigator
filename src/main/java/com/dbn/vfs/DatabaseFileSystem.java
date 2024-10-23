package com.dbn.vfs;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.project.Projects;
import com.dbn.common.util.Safe;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseEntity;
import com.dbn.connection.config.ConnectionDetailSettings;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBConsole;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.project.ProjectComponentsInitializer;
import com.dbn.vfs.file.*;
import com.intellij.openapi.components.NamedComponent;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.thread.ThreadMonitor.isTimeSensitiveThread;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.vfs.DatabaseFileSystem.FilePathType.*;

@Slf4j
public class DatabaseFileSystem extends VirtualFileSystem implements /*NonPhysicalFileSystem,*/ NamedComponent {
    // TODO Review NonPhysical marker: attempts to make this file system non-physical backfired with the PsiFileManager, specifically with rhe psifile/document linkage

    public static final char PS = '/';
    public static final String PSS = "" + '/';
    private static final String PROTOCOL = "db";
    private static final String PROTOCOL_PREFIX = PROTOCOL + "://";

    public enum FilePathType {
        OBJECTS("objects", "objects"),
        OBJECT_CONTENTS("object_contents", "object contents"),
        CONSOLES("consoles", "consoles"),
        SESSION_BROWSERS("session_browsers", "session browsers"),
        SESSION_STATEMENTS("session_statements", "session statements"),
        FILTER_EXPRESSIONS("filter_expressions", "filter expressions"),
        DATASET_FILTERS("dataset_filters", "dataset filters"),
        LOOSE_CONTENTS("loose_contents", "loose contents");

        private final String urlToken;
        private final String presentableUrlToken;

        FilePathType(String urlToken, String presentableUrlToken) {
            this.urlToken = urlToken + PS;
            this.presentableUrlToken = presentableUrlToken + PS;
        }

        @Override
        public String toString() {
            return urlToken;
        }

        boolean is(String path) {
            return path.startsWith(urlToken);
        }

        String collate(String path) {
            return path.substring(urlToken.length());
        }
    }

    static final IOException READONLY_FILE_SYSTEM = new IOException("Operation not supported");

    private final Map<DBObjectRef<?>, DBEditableObjectVirtualFile> filesCache = new ConcurrentHashMap<>();

    public DatabaseFileSystem() {
        Projects.projectClosed(project -> clearCachedFiles(project));
    }

    public static DatabaseFileSystem getInstance() {
        return (DatabaseFileSystem) VirtualFileManager.getInstance().getFileSystem(PROTOCOL);
        //return ApplicationManager.getApplication().getComponent(DatabaseFileSystem.class);
    }
                                                                            
    @Override
    @NotNull
    public String getProtocol() {
        return PROTOCOL;
    }

    /**
     * [connection_id]/consoles/[console_name]
     * [connection_id]/session_browsers/[console_name]
     * [connection_id]/dataset_filters/[dataset_ref_serialized]
     * [connection_id]/objects/[object_ref_serialized]
     * [connection_id]/object_contents/[content_Type]/[object_ref_serialized]
     */

    @Override
    @Nullable
    public VirtualFile findFileByPath(@NotNull @NonNls String path) {
        try {
            return lookupFileByPath(path);
        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
        } catch (Throwable e) {
            log.error("Error loading file for given path \"{}\"", path, e);
        }
        return null;
    }

    @Nullable
    private DBVirtualFileBase lookupFileByPath(@NonNls @NotNull String path) {
        if (path.startsWith(PROTOCOL_PREFIX)) {
            path = path.substring(PROTOCOL_PREFIX.length());
        }

        int index = path.indexOf(PS);
        if (index < 0) return null;

        ConnectionId connectionId = ConnectionId.get(path.substring(0, index));
        ConnectionHandler connection = ConnectionHandler.get(connectionId);

        if (isNotValid(connection) || !connection.isEnabled()) return null;
        if (!allowFileLookup(connection)) return null;

        Project project = connection.getProject();
        String relativePath = path.substring(index + 1);
        if (CONSOLES.is(relativePath)) {
            String consoleName = CONSOLES.collate(relativePath);
            DBConsole console = connection.getConsoleBundle().getConsole(consoleName);
            return Safe.call(console, target -> target.getVirtualFile());

        } else if (SESSION_BROWSERS.is(relativePath)) {
            return connection.getSessionBrowserFile();

        } else if (OBJECTS.is(relativePath)) {
            String objectIdentifier = OBJECTS.collate(relativePath);
            DBObjectRef<DBSchemaObject> objectRef = new DBObjectRef<>(connectionId, objectIdentifier);
            DBEditableObjectVirtualFile databaseFile = findOrCreateDatabaseFile(project, objectRef);
            return databaseFile;

        } else if (OBJECT_CONTENTS.is(relativePath)) {
            String contentIdentifier = OBJECT_CONTENTS.collate(relativePath);
            int contentTypeEndIndex = contentIdentifier.indexOf(PS);
            String contentTypeStr = contentIdentifier.substring(0, contentTypeEndIndex);
            DBContentType contentType = DBContentType.valueOf(contentTypeStr);

            String objectIdentifier = contentIdentifier.substring(contentTypeEndIndex + 1);
            DBObjectRef<DBSchemaObject> objectRef = new DBObjectRef<>(connectionId, objectIdentifier);
            DBEditableObjectVirtualFile virtualFile = findOrCreateDatabaseFile(project, objectRef);
            if (virtualFile == null) return null;
            return virtualFile.getContentFile(contentType);
        }

        return null;
    }

    public boolean isValidPath(String path, Project project) {
        if (path.startsWith(PROTOCOL_PREFIX)) {
            path = path.substring(PROTOCOL_PREFIX.length());
        }

        if (path.startsWith("null")) return false;

        int index = path.indexOf(PS);
        if (index < 0) return false;

        ConnectionId connectionId = ConnectionId.get(path.substring(0, index));
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (project.isInitialized() && connection == null) return false;

        String relativePath = path.substring(index + 1);
        for (FilePathType pathType : FilePathType.values()) {
            if (pathType.is(relativePath)) {
                return true;
            }
        }

        return false;
    }


    @NotNull
    @Override
    public String extractPresentableUrl(@NotNull String url) {
        if (url.startsWith(PROTOCOL_PREFIX)) {
            url = url.substring(PROTOCOL_PREFIX.length());
        }
        return extractPresentablePath(url);
    }

    public String extractPresentablePath(@NotNull String path) {
        int index = path.indexOf(PS);
        ConnectionId connectionId = ConnectionId.get(index == -1 ? path : path.substring(0, index));
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection == null) {
            path = path.replace(connectionId.id(), "UNKNOWN");
        } else {
            path = path.replace(connectionId.id(), connection.getName());
        }

        if (index > -1) {
            for (FilePathType value : FilePathType.values()) {
                path = path.replace(value.urlToken, value.presentableUrlToken);
            }

            path = path.replace(PSS, File.separator);
        }

        return path;
    }

    private boolean allowFileLookup(ConnectionHandler connection) {
        ConnectionDetailSettings connectionDetailSettings = connection.getSettings().getDetailSettings();
        if (connectionDetailSettings.isRestoreWorkspace()) {
            return true;
        } else {
            Project project = connection.getProject();
            ProjectComponentsInitializer initializer = ProjectComponentsInitializer.getInstance(project);
            return initializer.isInitialized();
        }
    }

    @Nullable
    public DBEditableObjectVirtualFile findDatabaseFile(DBSchemaObject object) {
        DBObjectRef<?> objectRef = object.ref();
        return filesCache.get(objectRef);
    }

    @Nullable
    public DBEditableObjectVirtualFile findOrCreateDatabaseFile(@NotNull DBObject object) {
        return findOrCreateDatabaseFile(object.getProject(), object.ref());
    }

    @Nullable
    public DBEditableObjectVirtualFile findOrCreateDatabaseFile(@NotNull Project project, @NotNull DBObjectRef<?> ref) {
        ConnectionHandler connection = ref.getConnection();
        if (isNotValid(connection)) return null;

        DBObject object = ref.get();
        if (isNotValid(object) && isTimeSensitiveThread()) {
            DBObjectBundle objectBundle = connection.getObjectBundle();
            objectBundle.getObjectInitializer().initObject(ref);
            return null;
        }
        return filesCache.computeIfAbsent(ref, r -> new DBEditableObjectVirtualFile(project, r));
    }

    public void invalidateDatabaseFile(DBObjectRef<?> objectRef) {
        filesCache.remove(objectRef);
    }

    public static boolean isFileOpened(DBObjectRef<?> object) {
        Project project = object.getProject();
        if (project == null) return false;

        DatabaseFileManager fileManager = DatabaseFileManager.getInstance(project);
        return fileManager.isFileOpened(object);
    }

    public static boolean isFileOpened(DBObject object) {
        Project project = object.getProject();
        DatabaseFileManager fileManager = DatabaseFileManager.getInstance(project);
        return fileManager.isFileOpened(object);
    }

    /********************************************************
     *                    GENERIC                           *
     ********************************************************/
    @NotNull
    public static String createFilePath(DBVirtualFile virtualFile) {
        try {
            ConnectionId connectionId = virtualFile.getConnectionId();

            if (virtualFile instanceof DBConsoleVirtualFile) {
                DBConsoleVirtualFile file = (DBConsoleVirtualFile) virtualFile;
                return connectionId + PSS + CONSOLES + file.getName();
            }

            if (virtualFile instanceof DBConnectionVirtualFile) {
                DBConnectionVirtualFile file = (DBConnectionVirtualFile) virtualFile;
                return file.getConnectionId() + "";
            }

            if (virtualFile instanceof DBObjectVirtualFile) {
                DBObjectVirtualFile<?> file = (DBObjectVirtualFile<?>) virtualFile;
                DBObjectRef<?> objectRef = file.getObjectRef();
                return createObjectPath(objectRef);
            }

            if (virtualFile instanceof DBContentVirtualFile) {
                DBContentVirtualFile file = (DBContentVirtualFile) virtualFile;
                DBObjectRef<?> objectRef = file.getObjectRef();
                DBContentType contentType = file.getContentType();
                return objectRef.getConnectionId() + PSS + OBJECT_CONTENTS + contentType.name() + PS + objectRef.serialize();
            }

            if (virtualFile instanceof DBObjectListVirtualFile) {
                DBObjectListVirtualFile<?> file = (DBObjectListVirtualFile<?>) virtualFile;
                DBObjectList<?> objectList = file.getObjectList();
                DatabaseEntity parentElement = objectList.getParentEntity();
                String listName = objectList.getObjectType().getListName();
                if (parentElement instanceof DBObject) {
                    DBObject object = (DBObject) parentElement;
                    DBObjectRef<?> objectRef = object.ref();
                    return connectionId + PSS + objectRef.serialize() + PSS + listName;
                } else {
                    return connectionId + PSS + listName; }
            }

            if (virtualFile instanceof DBDatasetFilterVirtualFile) {
                DBDatasetFilterVirtualFile file = (DBDatasetFilterVirtualFile) virtualFile;
                return connectionId + PSS + DATASET_FILTERS + file.getDataset().ref().serialize();
            }

            if (virtualFile instanceof DBSessionBrowserVirtualFile) {
                DBSessionBrowserVirtualFile file = (DBSessionBrowserVirtualFile) virtualFile;
                return connectionId + PSS + SESSION_BROWSERS + file.getName();

            }

            if (virtualFile instanceof DBSessionStatementVirtualFile) {
                DBSessionStatementVirtualFile file = (DBSessionStatementVirtualFile) virtualFile;
                return connectionId + PSS + SESSION_STATEMENTS + file.getName();
            }

            if (virtualFile instanceof DBObjectFilterExpressionFile) {
                DBObjectFilterExpressionFile file = (DBObjectFilterExpressionFile) virtualFile;
                return connectionId + PSS + FILTER_EXPRESSIONS + PSS + file.getName();
            }

            if (virtualFile instanceof DBLooseContentVirtualFile) {
                DBLooseContentVirtualFile file = (DBLooseContentVirtualFile) virtualFile;
                return connectionId + PSS + LOOSE_CONTENTS + PSS + DBObjectRef.serialised(file.getObject());
            }

            throw new IllegalArgumentException("File of type " + virtualFile.getClass() + " is not supported");
        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
            return "DISPOSED/"+ UUID.randomUUID();
        }
    }

    @NotNull
    public static String createObjectPath(DBObjectRef<?> object) {
        return object.getConnectionId() + PSS + OBJECTS + object.serialize();
    }

    @NotNull
    public static String createFileUrl(DBVirtualFile virtualFile) {
        return PROTOCOL_PREFIX + createFilePath(virtualFile);
    }

    @NotNull
    public static String createObjectUrl(DBObjectRef<?> object) {
        return PROTOCOL_PREFIX + createObjectPath(object);
    }


    /*********************************************************
     *                  VirtualFileSystem                    *
     *********************************************************/

    @Override
    public void refresh(boolean b) {

    }

    @Override
    @Nullable
    public VirtualFile refreshAndFindFileByPath(@NotNull String s) {
        return null;
    }

    @Override
    public void addVirtualFileListener(@NotNull VirtualFileListener listener) {

    }

    @Override
    public void removeVirtualFileListener(@NotNull VirtualFileListener listener) {

    }

    @Override
    protected void deleteFile(Object o, @NotNull VirtualFile virtualFile) {}

    @Override
    protected void moveFile(Object o, @NotNull VirtualFile virtualFile, @NotNull VirtualFile virtualFile1) throws IOException {
        throw READONLY_FILE_SYSTEM;
    }

    @Override
    protected void renameFile(Object o, @NotNull VirtualFile virtualFile, @NotNull String s) throws IOException {
        throw READONLY_FILE_SYSTEM;
    }

    @Override
    @NotNull
    protected VirtualFile createChildFile(Object o, @NotNull VirtualFile virtualFile, @NotNull String s) throws IOException {
        throw READONLY_FILE_SYSTEM;
    }

    @Override
    @NotNull
    protected VirtualFile createChildDirectory(Object o, @NotNull VirtualFile virtualFile, @NotNull String s) throws IOException {
        throw READONLY_FILE_SYSTEM;
    }

    @Override
    @NotNull
    protected VirtualFile copyFile(Object o, @NotNull VirtualFile virtualFile, @NotNull VirtualFile virtualFile1, @NotNull String s) throws IOException {
        throw READONLY_FILE_SYSTEM;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.DatabaseFileSystem";
    }

    /*********************************************************
     *              FileEditorManagerListener                *
     *********************************************************/


    void clearCachedFiles(Project project) {
        Iterator<DBObjectRef<?>> objectRefs = filesCache.keySet().iterator();
        while (objectRefs.hasNext()) {
            DBObjectRef<?> objectRef = objectRefs.next();
            DBEditableObjectVirtualFile file = filesCache.get(objectRef);
            if (file.getProject() == project) {
                objectRefs.remove();
                Disposer.dispose(file);
            }
        }
    }

    public boolean isDatabaseUrl(String fileUrl) {
        return fileUrl.startsWith(PROTOCOL_PREFIX);
    }

    @Override
    public boolean isCaseSensitive() {
        return false;
    }
}
