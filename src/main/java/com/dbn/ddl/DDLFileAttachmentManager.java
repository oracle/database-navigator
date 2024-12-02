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

package com.dbn.ddl;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.file.FileEventType;
import com.dbn.common.file.FileMappingEvent;
import com.dbn.common.file.FileMappings;
import com.dbn.common.file.VirtualFileInfo;
import com.dbn.common.file.util.FileSearchRequest;
import com.dbn.common.file.util.VirtualFiles;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.Write;
import com.dbn.common.ui.dialog.SelectionListDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Dialogs.DialogCallback;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Files;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.ddl.options.DDLFileSettings;
import com.dbn.ddl.ui.AttachDDLFileDialog;
import com.dbn.ddl.ui.DDLFileNameListCellRenderer;
import com.dbn.ddl.ui.DetachDDLFileDialog;
import com.dbn.editor.DBContentType;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.editor.code.SourceCodeManagerListener;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettingsManager;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import com.jgoodies.common.base.Strings;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.file.FileEventType.DELETED;
import static com.dbn.common.file.FileEventType.MOVED;
import static com.dbn.common.file.FileEventType.RENAMED;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.common.util.Lists.first;
import static com.dbn.common.util.Messages.options;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.vfs.DatabaseFileSystem.isFileOpened;

@State(
    name = DDLFileAttachmentManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DDLFileAttachmentManager extends ProjectComponentBase implements PersistentState {

    public static final String COMPONENT_NAME = "DBNavigator.Project.DDLFileAttachmentManager";

    private final FileMappings<DBObjectRef<DBSchemaObject>> mappings;
    private final Map<DBObjectType, String> preferences = new ConcurrentHashMap<>();

    private DDLFileAttachmentManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        this.mappings = new FileMappings<>(project, this);

        mappings.addVerifier((url, o) -> {
            VirtualFile file = VirtualFiles.findFileByUrl(url);
            if (file == null) return false;

            return isValidDDLFile(file, o);
        });

        mappings.addEventHandler((FileMappingEvent<DBObjectRef<DBSchemaObject>> e) -> {
            FileEventType eventType = e.getEventType();
            if (!eventType.isOneOf(MOVED, RENAMED, DELETED)) return;

            DBObjectRef<DBSchemaObject> target = e.getTarget();
            if (target == null) return;

            DBSchemaObject object = target.get();
            if (object == null) return;

            DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(getProject());
            if (editorManager.isFileOpen(object)) {
                Dispatch.run(() -> reopenEditor(object));
            }
        });

        ProjectEvents.subscribe(project, this, SourceCodeManagerListener.TOPIC, sourceCodeManagerListener());
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC, connectionConfigListener());
    }

    public static DDLFileAttachmentManager getInstance(@NotNull Project project) {
        return projectService(project, DDLFileAttachmentManager.class);
    }

    @NotNull
    private SourceCodeManagerListener sourceCodeManagerListener() {
        return new SourceCodeManagerListener() {
            @Override
            public void sourceCodeLoaded(@NotNull DBSourceCodeVirtualFile sourceCodeFile, boolean initialLoad) {
                if (!initialLoad && DatabaseFileSystem.isFileOpened(sourceCodeFile.getObject())) {
                    updateDDLFiles(sourceCodeFile.getMainDatabaseFile());
                }
            }

            @Override
            public void sourceCodeSaved(@NotNull DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor) {
                updateDDLFiles(sourceCodeFile.getMainDatabaseFile());
            }
        };
    }

    @NotNull
    private ConnectionConfigListener connectionConfigListener() {
        return new ConnectionConfigListener() {
            @Override
            public void connectionRemoved(ConnectionId connectionId) {
                mappings.removeIf(m -> Objects.equals(m.getConnectionId(), connectionId));
            }
        };
    }

    @Nullable
    public List<VirtualFile> getAttachedDDLFiles(DBObjectRef<DBSchemaObject> objectRef) {
        return mappings.files(objectRef);
    }

    @Nullable
    public DBSchemaObject getMappedObject(@NotNull VirtualFile ddlFile) {
        return DBObjectRef.get(getMappedObjectRef(ddlFile));
    }

    @Nullable
    public DBObjectRef<DBSchemaObject> getMappedObjectRef(@NotNull VirtualFile ddlFile) {
        return mappings.get(ddlFile.getUrl());
    }

    public ConnectionHandler getMappedConnection(VirtualFile ddlFile) {
        DBObjectRef<DBSchemaObject> objectRef = mappings.get(ddlFile.getUrl());
        if (objectRef == null) return null;

        ConnectionId connectionId = objectRef.getConnectionId();
        return ConnectionHandler.get(connectionId);
    }

    public boolean hasAttachedDDLFiles(DBObjectRef<DBSchemaObject> objectRef) {
        return mappings.contains(objectRef);
    }

    private boolean isValidDDLFile(VirtualFile virtualFile, DBObjectRef<DBSchemaObject> objectRef) {
        List<DDLFileType> ddlFileTypes = getDdlFileTypes(objectRef);
        for (DDLFileType ddlFileType : ddlFileTypes) {
            if (ddlFileType.getExtensions().contains(virtualFile.getExtension())) {
                return true;
            }
        }
        return false;
    }

    public void showFileAttachDialog(DBSchemaObject object, List<VirtualFileInfo> fileInfos, boolean showLookupOption, DialogCallback<AttachDDLFileDialog> callback) {
        Dialogs.show(() -> new AttachDDLFileDialog(fileInfos, object, showLookupOption), callback);
    }

    public void showFileDetachDialog(DBSchemaObject object, List<VirtualFileInfo> fileInfos, DialogCallback<DetachDDLFileDialog> callback) {
        Dialogs.show(() -> new DetachDDLFileDialog(fileInfos, object), callback);
    }

    public void attachDDLFile(DBObjectRef<DBSchemaObject> objectRef, VirtualFile virtualFile) {
        if (objectRef == null) return;

        // avoid initialising inside editor creation (slow operation assertions since 23.3)
        Documents.cacheDocument(virtualFile);

        mappings.put(virtualFile.getUrl(), objectRef);
        Project project = getProject();
        ProjectEvents.notify(project,
                DDLFileAttachmentManagerListener.TOPIC,
                (listener) -> listener.ddlFileAttached(project, virtualFile));
    }

    public void detachDDLFile(VirtualFile virtualFile) {
        DBObjectRef<DBSchemaObject> objectRef = mappings.remove(virtualFile.getUrl());
        resetDDLFileContext(virtualFile, objectRef);

        Project project = getProject();
        ProjectEvents.notify(project,
                DDLFileAttachmentManagerListener.TOPIC,
                (listener) -> listener.ddlFileDetached(project, virtualFile));
    }

    private void resetDDLFileContext(VirtualFile file, @Nullable DBObjectRef<DBSchemaObject> object) {
        if (object == null) return;

        // map last used connection/schema
        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(getProject());
        ConnectionHandler activeConnection = contextManager.getConnection(file);
        if (activeConnection != null) return;

        DBSchemaObject schemaObject = object.get();
        if (schemaObject == null) return;

        ConnectionHandler connection = schemaObject.getConnection();
        contextManager.setConnection(file, connection);
        contextManager.setDatabaseSchema(file, schemaObject.getSchemaId());
    }

    private List<VirtualFile> lookupApplicableDDLFiles(@NotNull DBObjectRef<DBSchemaObject> objectRef) {
        List<VirtualFile> fileList = new ArrayList<>();

        Project project = getProject();
        List<DDLFileType> ddlFileTypes = getDdlFileTypes(objectRef);
        for (DDLFileType ddlFileType : ddlFileTypes) {
            for (String extension : ddlFileType.getExtensions()) {
                String namePattern = Files.toRegexFileNamePattern("*" + objectRef.getFileName() + "*." + extension);
                FileSearchRequest searchRequest = FileSearchRequest.forPatterns(namePattern);
                VirtualFile[] files = VirtualFiles.findFiles(project, searchRequest);
                fileList.addAll(Arrays.asList(files));
            }
        }
        return fileList;
    }

    @NotNull
    private List<DDLFileType> getDdlFileTypes(@NotNull DBObjectRef<DBSchemaObject> objectRef) {
        DBObjectType objectType = objectRef.getObjectType();
        DDLFileManager ddlFileManager = DDLFileManager.getInstance(getProject());
        return ddlFileManager.getDDLFileTypes(objectType);
    }

    public List<VirtualFile> lookupDetachedDDLFiles(DBObjectRef<DBSchemaObject> object) {
        List<String> fileUrls = getAttachedFileUrls(object);
        List<VirtualFile> virtualFiles = lookupApplicableDDLFiles(object);
        List<VirtualFile> detachedVirtualFiles = new ArrayList<>();
        for (VirtualFile virtualFile : virtualFiles) {
            if (!fileUrls.contains(virtualFile.getUrl())) {
                detachedVirtualFiles.add(virtualFile);
            }
        }

        return detachedVirtualFiles;
    }

    public void createDDLFile(@NotNull DBObjectRef<DBSchemaObject> objectRef) {
        DDLFileNameProvider fileNameProvider = getDDLFileNameProvider(objectRef);
        Project project = getProject();

        if (fileNameProvider != null) {
            FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
            descriptor.setTitle(txt("msg.ddlFiles.title.SelectNewFileLocation"));

            VirtualFile[] selectedDirectories = FileChooser.chooseFiles(descriptor, project, null);
            if (selectedDirectories.length > 0) {
                String fileName = fileNameProvider.getFileName();
                VirtualFile parentDirectory = selectedDirectories[0];
                DBSchemaObject object = objectRef.ensure();

                try {
                    VirtualFile virtualFile = Write.compute(() -> parentDirectory.createChildData(this, fileName));
                    attachDDLFile(objectRef, virtualFile);
                    DBEditableObjectVirtualFile editableObjectFile = object.getEditableVirtualFile();
                    updateDDLFiles(editableObjectFile);

                } catch (IOException e) {
                    conditionallyLog(e);
                    Messages.showErrorDialog(project, txt("msg.ddlFiles.error.FileCreationFailed", parentDirectory + File.separator + fileName), e);
                }

                reopenEditor(object);
            }
        } else {
            showMissingFileAssociations(objectRef);
        }
    }

    public void updateDDLFiles(DBEditableObjectVirtualFile databaseFile) {
        Project project = getProject();
        DDLFileSettings ddlFileSettings = DDLFileSettings.getInstance(project);
        if (!ddlFileSettings.getGeneralSettings().isDdlFilesSynchronizationEnabled()) return;

        DDLFileManager ddlFileManager = DDLFileManager.getInstance(project);
        List<VirtualFile> ddlFiles = databaseFile.getAttachedDDLFiles();
        if (ddlFiles == null || ddlFiles.isEmpty()) return;

        for (VirtualFile ddlFile : ddlFiles) {
            DDLFileType ddlFileType = ddlFileManager.getDDLFileTypeForExtension(ddlFile.getExtension());
            DBContentType fileContentType = ddlFileType.getContentType();

            StringBuilder buffer = new StringBuilder();
            if (fileContentType.isBundle()) {
                DBContentType[] contentTypes = fileContentType.getSubContentTypes();
                for (DBContentType contentType : contentTypes) {
                    DBSourceCodeVirtualFile sourceCodeFile = databaseFile.getContentFile(contentType);
                    if (sourceCodeFile == null) continue;

                    String statement = ddlFileManager.createDDLStatement(sourceCodeFile, contentType);
                    if (Strings.isNotBlank(statement)) {
                        buffer.append(statement);
                        buffer.append('\n');
                    }
                    if (contentType != contentTypes[contentTypes.length - 1]) buffer.append('\n');
                }
            } else {
                DBSourceCodeVirtualFile sourceCodeFile = databaseFile.getContentFile(fileContentType);
                if (sourceCodeFile != null) {
                    buffer.append(ddlFileManager.createDDLStatement(sourceCodeFile, fileContentType));
                    buffer.append('\n');
                }
            }
            Document document = Documents.getDocument(ddlFile);
            if (document != null) {
                Documents.setText(document, buffer);
            }
        }
    }

    public void attachDDLFiles(DBObjectRef<DBSchemaObject> objectRef) {
        Progress.prompt(
                getProject(),
                objectRef, true,
                txt("msg.ddlFiles.title.AttachingDdlFiles"),
                txt("msg.ddlFiles.info.AttachingDdlFiles", objectRef.getQualifiedNameWithType()), t -> {
                    DDLFileNameProvider ddlFileNameProvider = getDDLFileNameProvider(objectRef);
                    if (ddlFileNameProvider == null) return;

                    List<VirtualFile> files = lookupDetachedDDLFiles(objectRef);
                    if (files.isEmpty()) {
                        List<String> fileUrls = getAttachedFileUrls(objectRef);

                        StringBuilder message = new StringBuilder();
                        message.append(fileUrls.isEmpty() ?
                                txt("msg.ddlFiles.info.NoDdlFilesFound") :
                                "msg.ddlFiles.info.NoAdditionalDdlFilesFound");

                        if (!fileUrls.isEmpty()) {
                            fileUrls = convert(fileUrls, f -> VirtualFiles.ensureFilePath(f));
                            message.append(txt("msg.ddlFiles.info.AlreadyAttachedFiles",
                                    objectRef.getQualifiedNameWithType(),
                                    String.join("\n", fileUrls)));
                        }

                        String[] options = {txt("app.shared.button.CreateNew"), txt("app.shared.button.Cancel")};
                        Messages.showInfoDialog(getProject(),
                                txt("msg.ddlFiles.title.NoDdlFilesFound"),
                                message.toString(), options, 0,
                                option -> when(option == 0, () -> createDDLFile(objectRef)));
                    } else {
                        List<VirtualFileInfo> fileInfos = VirtualFileInfo.fromFiles(files, getProject());
                        DBSchemaObject object = objectRef.ensure();
                        showFileAttachDialog(object, fileInfos, false, (dialog, exitCode) ->
                                when(exitCode != DialogWrapper.CANCEL_EXIT_CODE,
                                        () -> reopenEditor(object)));
                    }
                });

    }

    public void detachDDLFiles(DBObjectRef<DBSchemaObject> objectRef) {
        Progress.prompt(
                getProject(),
                objectRef, true,
                txt("msg.ddlFiles.title.DetachingDdlFiles"),
                txt("msg.ddlFiles.info.DetachingDdlFiles", objectRef.getQualifiedNameWithType()),
                t -> {
                    List<VirtualFile> files = getAttachedDDLFiles(objectRef);
                    if (files == null) return;

                    List<VirtualFileInfo> fileInfos = VirtualFileInfo.fromFiles(files, getProject());
                    DBSchemaObject object = objectRef.ensure();
                    showFileDetachDialog(object, fileInfos, (dialog, exitCode) ->
                            when(exitCode != DialogWrapper.CANCEL_EXIT_CODE,
                                    () -> reopenEditor(object)));
                });
    }

    private void reopenEditor(DBSchemaObject object) {
        Project project = object.getProject();
        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
        editorManager.reopenEditor(object);
    }

    @Nullable
    private DDLFileNameProvider getDDLFileNameProvider(DBObjectRef<DBSchemaObject> object) {
        List<DDLFileType> ddlFileTypes = getDdlFileTypes(object);
        if (ddlFileTypes.isEmpty()) {
            showMissingFileAssociations(object);
            return null;
        }

        Map<String, DDLFileType> extensionMappings = new LinkedHashMap<>();
        for (DDLFileType ddlFileType : ddlFileTypes) {
            for (String extension : ddlFileType.getExtensions()) {
                extensionMappings.put(extension, ddlFileType);
            }
        }

        if (extensionMappings.size() == 1) {
            String extension = extensionMappings.keySet().iterator().next();
            DDLFileType ddlFileType = extensionMappings.get(extension);
            return new DDLFileNameProvider(object, ddlFileType, extension);
        }

        List<DDLFileNameProvider> fileNameProviders = new ArrayList<>();
        for (String extension : extensionMappings.keySet()) {
            DDLFileType ddlFileType = extensionMappings.get(extension);
            DDLFileNameProvider fileNameProvider = new DDLFileNameProvider(object, ddlFileType, extension);
            fileNameProviders.add(fileNameProvider);
        }

        return Dispatch.call(() -> openFileNameProvidersDialog(object, fileNameProviders));
    }

    private DDLFileNameProvider openFileNameProvidersDialog(DBObjectRef<?> object, List<DDLFileNameProvider> providers) {
        DDLFileNameProvider preferredProvider = first(providers, p -> Objects.equals(preferences.get(p.getObjectType()), p.getExtension()));

        SelectionListDialog<DDLFileNameProvider> fileTypeDialog = new SelectionListDialog<>(
                getProject(),
                txt("msg.ddlFiles.title.SelectDdlFileType"),
                providers,
                preferredProvider,
                object);

        JBList<DDLFileNameProvider> selectionList = fileTypeDialog.getForm().getSelectionList();
        selectionList.setCellRenderer(new DDLFileNameListCellRenderer());

        fileTypeDialog.show();
        List<DDLFileNameProvider> selection = fileTypeDialog.getSelection();
        if (selection == null || selection.isEmpty()) throw new ProcessCanceledException();

        DDLFileNameProvider selectedProvider = selection.get(0);

        DBObjectType objectType = selectedProvider.getObjectType();
        String extension = selectedProvider.getExtension();
        preferences.put(objectType, extension);

        return selectedProvider;
    }

    public void showMissingFileAssociations(DBObjectRef<DBSchemaObject> objectRef) {
        Messages.showWarningDialog(
                getProject(),
                txt("msg.ddlFiles.title.NoDdlFileAssociation"),
                txt("msg.ddlFiles.question.NoDdlFileAssociation", objectRef.getObjectType().getListName()),
                options(txt("app.shared.button.OpenSettings"), txt("app.shared.button.Cancel")), 0,
                option -> when(option == 0, () -> {
                    ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(getProject());
                    settingsManager.openProjectSettings(ConfigId.DDL_FILES);
                }));
    }

    private List<String> getAttachedFileUrls(DBObjectRef<DBSchemaObject> objectRef) {
        return mappings.fileUrls(objectRef);
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newElement("state");

        Element mappingsElement = newElement(element, "mappings");
        for (String fileUrl : mappings.fileUrls()) {
            val objectRef = mappings.get(fileUrl);

            Element mappingElement = newElement(mappingsElement, "mapping");
            setStringAttribute(mappingElement, "file-url", fileUrl);
            objectRef.writeState(mappingElement);
        }

        Element preferencesElement = newElement(element, "preferences");
        for (DBObjectType objectType : preferences.keySet()) {
            String fileExtension = preferences.get(objectType);

            Element mappingElement = newElement(preferencesElement, "mapping");
            setEnumAttribute(mappingElement, "object-type", objectType);
            setStringAttribute(mappingElement, "file-extension", fileExtension);
        }

        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element mappingsElement = element.getChild("mappings");
        List<Element> mappingElements = mappingsElement == null ? // TODO backward compatibility
                element.getChildren("mapping") :
                mappingsElement.getChildren();

        for (Element mappingElement : mappingElements) {
            String fileUrl = stringAttribute(mappingElement, "file-url");
            if (Strings.isEmpty(fileUrl)) continue;

            fileUrl = VirtualFiles.ensureFileUrl(fileUrl);
            DBObjectRef<DBSchemaObject> objectRef = DBObjectRef.from(mappingElement);
            if (objectRef == null) continue;

            mappings.put(fileUrl, objectRef);
        }

        Element preferencesElement = element.getChild("preferences");
        if (preferencesElement != null) {
            List<Element> preferenceElements = preferencesElement.getChildren();
            for (Element mappingElement : preferenceElements) {
                DBObjectType objectType = enumAttribute(mappingElement, "object-type", DBObjectType.UNKNOWN);
                String fileExtension = stringAttribute(mappingElement, "file-extension");
                preferences.put(objectType, fileExtension);
            }
        }

        Background.run(() -> mappings.cleanup());
    }

    public void warmUpAttachedDDLFiles(VirtualFile file) {
        if (file instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile objectFile = (DBEditableObjectVirtualFile) file;
            if (isFileOpened(objectFile.getObject())) return;

            List<VirtualFile> files = getAttachedDDLFiles(objectFile.getObjectRef());
            Documents.cacheDocuments(files);
        }

    }

}
