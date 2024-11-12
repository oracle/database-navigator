package com.dbn.editor.code;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.component.ProjectManagerListener;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.editor.BasicTextEditor;
import com.dbn.common.editor.document.OverrideReadonlyFragmentModificationHandler;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.navigation.NavigationInstructions;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.Read;
import com.dbn.common.util.ChangeTimestamp;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.common.util.Unsafe;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.statement.ByteArray;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.editor.DBContentType;
import com.dbn.editor.EditorProviderId;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.editor.code.diff.MergeAction;
import com.dbn.editor.code.diff.SourceCodeDiffManager;
import com.dbn.editor.code.options.CodeEditorConfirmationSettings;
import com.dbn.editor.code.options.CodeEditorSettings;
import com.dbn.execution.statement.DataDefinitionChangeListener;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.psql.PSQLFile;
import com.dbn.object.DBDatasetTrigger;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileTypes.BinaryFileDecompiler;
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.text.DateFormatUtil;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.component.ApplicationMonitor.checkAppExitRequested;
import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.navigation.NavigationInstruction.FOCUS;
import static com.dbn.common.navigation.NavigationInstruction.OPEN;
import static com.dbn.common.navigation.NavigationInstruction.SCROLL;
import static com.dbn.common.notification.NotificationGroup.SOURCE_CODE;
import static com.dbn.common.util.Commons.list;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Messages.options;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.common.util.Messages.showQuestionDialog;
import static com.dbn.common.util.Messages.showWarningDialog;
import static com.dbn.common.util.Naming.unquote;
import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.database.DatabaseFeature.OBJECT_CHANGE_MONITORING;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.vfs.file.status.DBFileStatus.LOADING;
import static com.dbn.vfs.file.status.DBFileStatus.SAVING;
import static com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER;

@State(
    name = SourceCodeManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class SourceCodeManager extends ProjectComponentBase implements PersistentState, ProjectManagerListener {
    public static final String COMPONENT_NAME = "DBNavigator.Project.SourceCodeManager";

    public static SourceCodeManager getInstance(@NotNull Project project) {
        return projectService(project, SourceCodeManager.class);
    }

    private SourceCodeManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(OverrideReadonlyFragmentModificationHandler.INSTANCE);

        ProjectEvents.subscribe(project, this, DataDefinitionChangeListener.TOPIC, dataDefinitionChangeListener());
        ProjectEvents.subscribe(project, this, EnvironmentManagerListener.TOPIC, environmentManagerListener());
        ProjectEvents.subscribe(project, this, FILE_EDITOR_MANAGER, fileEditorManagerListener());
        //ProjectEvents.subscribe(project, this, FILE_EDITOR_MANAGER, new DBLanguageFileEditorListener());
        //ProjectEvents.subscribe(project, this, FILE_EDITOR_MANAGER, new SQLConsoleEditorListener());
        //ProjectEvents.subscribe(project, this, FILE_EDITOR_MANAGER, new SourceCodeEditorListener());
    }


    @NotNull
    private DataDefinitionChangeListener dataDefinitionChangeListener() {
        return new DataDefinitionChangeListener() {
            @Override
            public void dataDefinitionChanged(DBSchema schema, DBObjectType objectType) {
            }

            @Override
            public void dataDefinitionChanged(@NotNull DBSchemaObject schemaObject) {
                DBEditableObjectVirtualFile databaseFile = schemaObject.getCachedVirtualFile();
                if (databaseFile == null) return;

                if (databaseFile.isModified()) {
                    showQuestionDialog(
                            getProject(), "Unsaved changes",
                            "The " + schemaObject.getQualifiedNameWithType() + " has been updated in database. You have unsaved changes in the object editor.\n" +
                                    "Do you want to discard the changes and reload the updated database version?",
                            new String[]{"Reload", "Keep changes"}, 0,
                            option -> when(option == 0, () ->
                                    reloadAndUpdateEditors(databaseFile, false)));
                } else {
                    reloadAndUpdateEditors(databaseFile, true);
                }

            }
        };
    }

    @NotNull
    private EnvironmentManagerListener environmentManagerListener() {
        return new EnvironmentManagerListener() {
            @Override
            public void editModeChanged(Project project, DBContentVirtualFile databaseContentFile) {
                if (databaseContentFile instanceof DBSourceCodeVirtualFile) {
                    DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) databaseContentFile;
                    if (sourceCodeFile.isModified()) {
                        loadSourceCode(sourceCodeFile, true);
                    }
                }
            }
        };
    }

    @NotNull
    private FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenSelectionChanged(@NotNull FileEditorManagerEvent event) {
                FileEditor newEditor = event.getNewEditor();
                if (newEditor instanceof SourceCodeEditor) {
                    SourceCodeEditor sourceCodeEditor = (SourceCodeEditor) newEditor;
                    DBEditableObjectVirtualFile databaseFile = sourceCodeEditor.getVirtualFile().getMainDatabaseFile();
                    for (DBSourceCodeVirtualFile sourceCodeFile : databaseFile.getSourceCodeFiles()) {
                        if (!sourceCodeFile.isLoaded()) {
                            loadSourceCode(sourceCodeFile, false);
                        }
                    }

                }

            }
        };
    }

    private void reloadAndUpdateEditors(DBEditableObjectVirtualFile databaseFile, boolean startInBackground) {
        if (!databaseFile.isContentLoaded()) return;

        if (startInBackground) {
            Background.run(getProject(), () -> reloadAndUpdateEditors(databaseFile));
        } else {
            DBSchemaObject object = databaseFile.getObject();
            Progress.prompt(getProject(), object, false,
                    "Loading source code",
                    "Reloading object source code for " + object.getQualifiedNameWithType(),
                    progress -> reloadAndUpdateEditors(databaseFile));
        }
    }

    private void reloadAndUpdateEditors(DBEditableObjectVirtualFile databaseFile) {
        List<DBSourceCodeVirtualFile> sourceCodeFiles = databaseFile.getSourceCodeFiles();
        for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
            loadSourceCode(sourceCodeFile, true);
        }
    }

    public void ensureSourcesLoaded(@NotNull DBSchemaObject schemaObject, boolean notifyError) {
        DBEditableObjectVirtualFile editableObjectFile = schemaObject.getEditableVirtualFile();
        List<DBSourceCodeVirtualFile> sourceCodeFiles = editableObjectFile.getSourceCodeFiles();
        for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
            if (!sourceCodeFile.isLoaded()) {
                loadSourceFromDatabase(sourceCodeFile, false, notifyError);
            }
        }
    }

    private void loadSourceFromDatabase(@NotNull DBSourceCodeVirtualFile sourceCodeFile, boolean force, boolean notifyError) {
        boolean initialLoad = !sourceCodeFile.isLoaded();
        if (sourceCodeFile.isNot(LOADING) && (initialLoad || force)) {
            sourceCodeFile.set(LOADING, true);
            Editors.setEditorsReadonly(sourceCodeFile, true);
            Project project = getProject();
            DBSchemaObject object = sourceCodeFile.getObject();

            ProjectEvents.notify(project,
                    SourceCodeManagerListener.TOPIC,
                    (listener) -> listener.sourceCodeLoading(sourceCodeFile));
            try {
                sourceCodeFile.loadSourceFromDatabase();
            } catch (SQLException e) {
                conditionallyLog(e);
                sourceCodeFile.setSourceLoadError(e.getMessage());
                sourceCodeFile.setModified(false);
                if (notifyError) {
                    String objectDesc = object.getQualifiedNameWithType();
                    sendErrorNotification(SOURCE_CODE, txt("ntf.sourceCode.error.CannotLoadSourceCode", objectDesc, e));
                }
            } finally {
                sourceCodeFile.set(LOADING, false);
                ProjectEvents.notify(project,
                        SourceCodeManagerListener.TOPIC,
                        (listener) -> listener.sourceCodeLoaded(sourceCodeFile, initialLoad));
            }
        }
    }

    private void saveSourceToDatabase(@NotNull DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor, @Nullable Runnable successCallback) {
        if (!sourceCodeFile.isNot(SAVING)) return;

        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(getProject());
        if (!debuggerManager.checkForbiddenOperation(sourceCodeFile.getConnection())) return;

        sourceCodeFile.set(SAVING, true);
        Project project = getProject();
        try {
            Document document = Failsafe.nn(Documents.getDocument(sourceCodeFile));
            Documents.saveDocument(document);

            if (Read.call(sourceCodeFile, f -> !isValidObjectHeader(f))) return;

            DBSchemaObject object = sourceCodeFile.getObject();
            String objectQualifiedName = object.getQualifiedNameWithType();
            ProgressMonitor.setProgressDetail(txt("prc.codeEditor.message.CheckingThirdPartyChanges", objectQualifiedName));

            boolean changedInDatabase = sourceCodeFile.isChangedInDatabase(true);
            if (changedInDatabase && sourceCodeFile.isMergeRequired()) {
                String presentableChangeTime =
                        OBJECT_CHANGE_MONITORING.isSupported(object) ?
                                toLowerCase(DateFormatUtil.formatPrettyDateTime(sourceCodeFile.getDatabaseChangeTimestamp())) : "";
                String message =
                        "The " + objectQualifiedName +
                                " was changed in database by another user " + presentableChangeTime + "." +
                                "\nYou must merge the changes before saving.";

                showWarningDialog(project, "Version conflict", message,
                        options("Merge Changes", "Cancel"), 0,
                        option -> {
                            if (option == 0) {
                                Progress.prompt(project, object, false,
                                        "Loading source code",
                                        "Loading database source code for " + objectQualifiedName,
                                        progress -> openCodeMergeDialog(sourceCodeFile, fileEditor));
                            } else {
                                sourceCodeFile.set(SAVING, false);
                            }
                        });

            } else {
                storeSourceToDatabase(sourceCodeFile, fileEditor, successCallback);
            }

        } catch (Exception e) {
            conditionallyLog(e);
            showErrorDialog(project, "Could not save changes to database.", e);
            sourceCodeFile.set(SAVING, false);
        }
    }

    private void openCodeMergeDialog(@NotNull DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor) {
        Project project = getProject();
        try {
            DBSchemaObject object = sourceCodeFile.getObject();
            DBContentType contentType = sourceCodeFile.getContentType();
            SourceCodeContent sourceCodeContent = loadSourceFromDatabase(object, contentType);
            String databaseContent = sourceCodeContent.getText().toString();
            SourceCodeDiffManager diffManager = SourceCodeDiffManager.getInstance(project);
            diffManager.openCodeMergeDialog(databaseContent, sourceCodeFile, fileEditor, MergeAction.SAVE);
        } catch (SQLException e) {
            conditionallyLog(e);
            showErrorDialog(project, "Could not load database sources.", e);
        }
    }

    private boolean isValidObjectHeader(@NotNull DBSourceCodeVirtualFile sourceCodeFile) {
        DBSchemaObject object = sourceCodeFile.getObject();
        DBContentType contentType = sourceCodeFile.getContentType();
        PsiFile psiFile = sourceCodeFile.getPsiFile();

        if (psiFile != null && psiFile.getFirstChild() != null && !isValidObjectTypeAndName(psiFile, object, contentType)) {
            String message = "You are not allowed to change the name or the type of the object";
            sourceCodeFile.set(SAVING, false);
            showErrorDialog(getProject(), "Illegal action", message);
            return false;
        }
        return true;
    }

    public SourceCodeContent loadSourceFromDatabase(@NotNull DBSchemaObject object, DBContentType contentType) throws SQLException {
        SourceCodeContent sourceCodeContent = DatabaseInterfaceInvoker.load(HIGH,
                "Loading source code",
                "Loading source code of " + object.getQualifiedNameWithType(),
                object.getProject(),
                object.getConnectionId(),
                conn -> loadSourceFromDatabase(object, contentType, conn));

        String objectName = object.getName();
        DBObjectType objectType = object.getObjectType();

        DatabaseDataDefinitionInterface dataDefinition = object.getDataDefinitionInterface();
        dataDefinition.computeSourceCodeOffsets(sourceCodeContent, objectType.getTypeId(), objectName);
        return sourceCodeContent;
    }

    @NotNull
    private static SourceCodeContent loadSourceFromDatabase(@NotNull DBSchemaObject object, DBContentType contentType, DBNConnection conn) throws SQLException {
        boolean optionalContent = contentType == DBContentType.CODE_BODY;
        ResultSet resultSet = null;
        boolean writable = true;
        try {
            DatabaseMetadataInterface metadata = object.getMetadataInterface();
            resultSet = loadSourceFromDatabase(
                    object,
                    contentType,
                    metadata,
                    conn);

            StringBuilder buffer = new StringBuilder();
            while (resultSet != null && resultSet.next()) {
                String codeLine = resultSet.getString("SOURCE_CODE");
                codeLine = normalizeLine(codeLine);
                buffer.append(codeLine);
            }

            if (buffer.length() == 0 && object.getObjectType() == DBObjectType.JAVA_OBJECT) {
                CharSequence code = loadJavaDecompiledCode(object, conn, metadata);
                buffer.append(code);
                writable = false;
            }

            if (buffer.length() == 0 && !optionalContent) {
                throw new SQLException("Source lookup returned empty");
            }
            return new SourceCodeContent(buffer.toString(), writable);
        } finally {
            Resources.close(resultSet);
        }
    }

    private static CharSequence loadJavaDecompiledCode(@NotNull DBSchemaObject object, DBNConnection conn, DatabaseMetadataInterface metadata) throws SQLException {
        File tempFile = null;
        try {
            String schemaName = object.getSchemaName();
            String objectName = object.getName();
            ByteArray byteArray = metadata.loadJavaBinaryCode(schemaName, objectName, conn);
            byte[] bytes = byteArray.getValue();

            tempFile = FileUtil.createTempFile(objectName, ".class");
            Files.write(tempFile.toPath(), bytes);

            BinaryFileDecompiler decompiler = BinaryFileTypeDecompilers.getInstance().forFileType(JavaClassFileType.INSTANCE);
            VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile);
            if (virtualFile == null) return "";

            return decompiler.decompile(virtualFile);

        } catch (Exception e) {
            throw Exceptions.toSqlException(e);
        } finally {
            if (tempFile != null) {
                Path tempFilePath = tempFile.toPath();
                Unsafe.warned(() -> Files.delete(tempFilePath));
            }
        }
    }

    @NotNull
    private static String normalizeLine(String codeLine) {
        if (codeLine == null) return "";

        // normalize line breaks
        // (background: not all entries in ALL_SOURCE have line breaks)
        codeLine = StringUtils.stripEnd(codeLine, "[ \n\r\t]") + "\n";
        return codeLine;
    }

    @Nullable
    private static ResultSet loadSourceFromDatabase(
            @NotNull DBSchemaObject object,
            DBContentType contentType,
            DatabaseMetadataInterface metadata,
            @NotNull DBNConnection connection) throws SQLException {

        DBObjectType objectType = object.getObjectType();
        String schemaName = object.getSchemaName();
        String objectName = object.getName();
        short objectOverload = object.getOverload();

        switch (objectType) {
            case VIEW:
                return metadata.loadViewSourceCode(
                        schemaName,
                        objectName,
                        connection);

            case MATERIALIZED_VIEW:
                return metadata.loadMaterializedViewSourceCode(
                        schemaName,
                        objectName,
                        connection);

            case DATABASE_TRIGGER:
                return metadata.loadDatabaseTriggerSourceCode(
                        schemaName,
                        objectName,
                        connection);

            case DATASET_TRIGGER:
                DBDatasetTrigger trigger = (DBDatasetTrigger) object;
                String datasetSchemaName = trigger.getDataset().getSchemaName();
                String datasetName = trigger.getDataset().getName();
                return metadata.loadDatasetTriggerSourceCode(
                        datasetSchemaName,
                        datasetName,
                        schemaName,
                        objectName,
                        connection);

            case FUNCTION:
                return metadata.loadObjectSourceCode(
                        schemaName,
                        objectName,
                        "FUNCTION",
                        objectOverload,
                        connection);

            case PROCEDURE:
                return metadata.loadObjectSourceCode(
                        schemaName,
                        objectName,
                        "PROCEDURE",
                        objectOverload,
                        connection);

            case TYPE:
                String typeContent =
                        contentType == DBContentType.CODE_SPEC ? "TYPE" :
                        contentType == DBContentType.CODE_BODY ? "TYPE BODY" : null;

                return metadata.loadObjectSourceCode(
                        schemaName,
                        objectName,
                        typeContent,
                        connection);

            case PACKAGE:
                String packageContent =
                        contentType == DBContentType.CODE_SPEC ? "PACKAGE" :
                        contentType == DBContentType.CODE_BODY ? "PACKAGE BODY" : null;

                return metadata.loadObjectSourceCode(
                        schemaName,
                        objectName,
                        packageContent,
                        connection);

            case JAVA_OBJECT:
                return metadata.loadObjectSourceCode(
                    schemaName,
                    objectName,
                    "JAVA SOURCE",
                    connection);
            default:
                return null;
        }
    }

    @NotNull
    public ChangeTimestamp loadChangeTimestamp(@NotNull DBSchemaObject object, DBContentType contentType) throws SQLException{
        if (OBJECT_CHANGE_MONITORING.isNotSupported(object)) return ChangeTimestamp.now();

        Timestamp timestamp = DatabaseInterfaceInvoker.load(HIGHEST,
                "Loading object details",
                "Loading change timestamp for " + object.getQualifiedNameWithType(),
                object.getProject(),
                object.getConnectionId(),
                conn -> {
                    ResultSet resultSet = null;
                    try {
                        String schemaName = object.getSchemaName();
                        String objectName = object.getName();
                        String contentQualifier = getContentQualifier(object.getObjectType(), contentType);

                        DatabaseMetadataInterface metadata = object.getMetadataInterface();
                        resultSet = metadata.loadObjectChangeTimestamp(
                                schemaName,
                                objectName,
                                contentQualifier,
                                conn);

                        return resultSet.next() ? resultSet.getTimestamp(1) : null;
                    } finally {
                        Resources.close(resultSet);
                    }
                });

        if (timestamp != null) return ChangeTimestamp.of(timestamp);


        return ChangeTimestamp.now();
    }

    private static String getContentQualifier(DBObjectType objectType, DBContentType contentType) {
        switch (objectType) {
            case JAVA_OBJECT:      return "JAVA SOURCE";
            case FUNCTION:         return "FUNCTION";
            case PROCEDURE:        return "PROCEDURE";
            case VIEW:             return "VIEW";
            case DATASET_TRIGGER:  return "TRIGGER";
            case DATABASE_TRIGGER: return "TRIGGER";
            case PACKAGE:
                return
                    contentType == DBContentType.CODE_SPEC ? "PACKAGE" :
                    contentType == DBContentType.CODE_BODY ? "PACKAGE BODY" : null;
            case TYPE:
                return
                    contentType == DBContentType.CODE_SPEC ? "TYPE" :
                    contentType == DBContentType.CODE_BODY ? "TYPE BODY" : null;

        }
        return null;
    }

    private boolean isValidObjectTypeAndName(@NotNull PsiFile psiFile, @NotNull DBSchemaObject object, DBContentType contentType) {
        if(object.getObjectType()==DBObjectType.JAVA_OBJECT) return true;

        ConnectionHandler connection = object.getConnection();
        DatabaseDataDefinitionInterface dataDefinition = connection.getDataDefinitionInterface();
        if (dataDefinition.includesTypeAndNameInSourceContent(object.getObjectType().getTypeId())) {
            PsiElement psiElement = PsiUtil.getFirstLeaf(psiFile);

            String typeName = object.getTypeName();
            String subtypeName = contentType.getObjectTypeSubname();
            String objectName = object.getName();
            String schemaName = object.getSchemaName();

            if (psiElement == null || !Strings.equalsIgnoreCase(psiElement.getText(), typeName)) {
                return false;
            }

            if (subtypeName != null) {
                psiElement = PsiUtil.getNextLeaf(psiElement);
                if (psiElement == null || !Strings.equalsIgnoreCase(psiElement.getText(), subtypeName)) {
                    return false;
                }
            }

            psiElement = PsiUtil.getNextLeaf(psiElement);
            if (psiElement == null) {
                return false;
            }

            if (Strings.equalsIgnoreCase(text(psiElement), schemaName)) {
                psiElement = PsiUtil.getNextLeaf(psiElement) ;
                if (psiElement == null || !Objects.equals(psiElement.getText(), ".")) {
                    return false;
                } else {
                    psiElement = PsiUtil.getNextLeaf(psiElement);
                    if (psiElement == null || !Strings.equalsIgnoreCase(text(psiElement), objectName)) {
                        return false;
                    }
                }
            } else {
                if (!Strings.equalsIgnoreCase(text(psiElement), objectName)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String text(@NotNull PsiElement psiElement) {
        return unquote(psiElement.getText());
    }

    public void storeSourceToDatabase(DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor, @Nullable Runnable successCallback) {
        Project project = getProject();
        DBSchemaObject object = sourceCodeFile.getObject();
        Progress.prompt(project, object, false,
                "Saving source code",
                "Saving sources of " + object.getQualifiedNameWithType() + " to database",
                progress -> {
                    try {
                        sourceCodeFile.saveSourceToDatabase();
                        ProjectEvents.notify(project,
                                SourceCodeManagerListener.TOPIC,
                                (listener) -> listener.sourceCodeSaved(sourceCodeFile, fileEditor));

                    } catch (SQLException e) {
                        conditionallyLog(e);
                        showErrorDialog(project, "Could not save changes to database.", e);
                    } finally {
                        sourceCodeFile.set(SAVING, false);
                    }
                    if (successCallback != null) successCallback.run();
                });
    }

    public BasePsiElement getObjectNavigationElement(@NotNull DBSchemaObject parentObject, DBContentType contentType, DBObjectType objectType, CharSequence objectName) {
        DBEditableObjectVirtualFile editableObjectFile = parentObject.getEditableVirtualFile();
        DBContentVirtualFile contentFile = editableObjectFile.getContentFile(contentType);
        if (contentFile == null) return null;

        PSQLFile file = PsiUtil.getPsiFile(getProject(), contentFile);
        if (file == null) return null;

        return
            contentType == DBContentType.CODE_BODY ? file.lookupObjectDeclaration(objectType, objectName) :
            contentType == DBContentType.CODE_SPEC ? file.lookupObjectSpecification(objectType, objectName) : null;

    }

    public void navigateToObject(@NotNull DBSchemaObject parentObject, @NotNull BasePsiElement basePsiElement) {
        DBEditableObjectVirtualFile editableObjectFile = parentObject.getEditableVirtualFile();
        DBLanguagePsiFile psiFile = basePsiElement.getFile();
        VirtualFile elementVirtualFile = psiFile.getVirtualFile();
        if (elementVirtualFile instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) elementVirtualFile;
            BasicTextEditor textEditor = Editors.getTextEditor(sourceCodeFile);
            if (textEditor != null) {
                Project project = getProject();
                EditorProviderId editorProviderId = textEditor.getEditorProviderId();
                FileEditor fileEditor = Editors.selectEditor(project, textEditor, editableObjectFile, editorProviderId, NavigationInstructions.create(OPEN));
                basePsiElement.navigateInEditor(fileEditor, NavigationInstructions.create(FOCUS, SCROLL));
            }
        }
    }

    @Override
    public boolean canCloseProject() {
        Project project = getProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile[] openFiles = fileEditorManager.getOpenFiles();

        for (VirtualFile openFile : openFiles) {
            if (openFile instanceof DBEditableObjectVirtualFile) {
                DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) openFile;
                if (!databaseFile.isModified()) continue;
                if (databaseFile.isSaving()) continue;

                DBObjectRef object = databaseFile.getObjectRef();
                Project objectProject = object.getProject();
                if (isNotValid(objectProject)) continue;

                CodeEditorSettings codeEditorSettings = CodeEditorSettings.getInstance(objectProject);
                CodeEditorConfirmationSettings confirmationSettings = codeEditorSettings.getConfirmationSettings();

                String objectDescription = object.getQualifiedNameWithType();
                boolean exitApp = checkAppExitRequested();
                confirmationSettings.getExitOnChanges().resolve(
                        list(objectDescription),
                        option -> {
                            switch (option) {
                                case SAVE: saveSourceCodeChanges(databaseFile, () -> closeProject(exitApp)); break;
                                case DISCARD: revertSourceCodeChanges(databaseFile, () -> closeProject(exitApp)); break;
                                case SHOW: {
                                    List<DBSourceCodeVirtualFile> sourceCodeFiles = databaseFile.getSourceCodeFiles();
                                    for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
                                        if (sourceCodeFile.isModified()) {
                                            SourceCodeDiffManager diffManager = SourceCodeDiffManager.getInstance(objectProject);
                                            diffManager.opedDatabaseDiffWindow(sourceCodeFile);
                                        }
                                    }

                                } break;
                                case CANCEL: break;
                            }
                        });
                return false;
            }
        }
        return true;
    }

    public void loadSourceCode(@NotNull DBSourceCodeVirtualFile sourceCodeFile, boolean force) {
        ConnectionAction.invoke("loading the source code", false, sourceCodeFile,
                action -> Background.run(getProject(), () -> loadSourceFromDatabase(sourceCodeFile, force, false)));
    }

    public void saveSourceCode(@NotNull DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor, Runnable successCallback) {
        DBSchemaObject object = sourceCodeFile.getObject();
        ConnectionAction.invoke("saving the source code", false, sourceCodeFile,
                action -> Progress.prompt(getProject(), object, false,
                        "Saving source code",
                        "Saving source code for " + object.getQualifiedNameWithType(),
                        progress -> saveSourceToDatabase(sourceCodeFile, fileEditor, successCallback)));
    }

    public void revertSourceCodeChanges(@NotNull DBEditableObjectVirtualFile databaseFile, Runnable successCallback) {
        try {
            List<DBSourceCodeVirtualFile> sourceCodeFiles = databaseFile.getSourceCodeFiles();
            for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
                sourceCodeFile.revertLocalChanges();
            }
        } finally {
            if (successCallback != null) {
                successCallback.run();
            }
        }

/*
        String objectDescription = databaseFile.getObject().getQualifiedNameWithType();
        ConnectionAction.invoke("loading the source code", false, databaseFile,
                (action) -> Progress.background(getProject(), "Loading source code for " + objectDescription, false,
                        (progress) -> {
                            try {
                                List<DBSourceCodeVirtualFile> sourceCodeFiles = databaseFile.getSourceCodeFiles();
                                for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
                                    sourceCodeFile.revertLocalChanges();
                                }
                            } finally {
                                if (successCallback != null) {
                                    successCallback.run();
                                }
                            }
                        }));
*/
    }

    public void saveSourceCodeChanges(@NotNull DBEditableObjectVirtualFile databaseFile, Runnable successCallback) {
        DBSchemaObject object = databaseFile.getObject();
        ConnectionAction.invoke("saving the source code", false, databaseFile,
                action -> Progress.prompt(getProject(), object, false,
                        "Saving source code",
                        "Saving source code for " + object.getQualifiedNameWithType(),
                        progress -> {
                            List<DBSourceCodeVirtualFile> sourceCodeFiles = databaseFile.getSourceCodeFiles();
                            for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
                                if (sourceCodeFile.isModified()) {
                                    saveSourceToDatabase(sourceCodeFile, null, successCallback);
                                }
                            }
                        }));
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
    }
}
