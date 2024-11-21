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

package com.dbn.vfs.file;

import com.dbn.common.latent.Latent;
import com.dbn.common.util.Documents;
import com.dbn.connection.SessionId;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.connection.session.DatabaseSessionBundle;
import com.dbn.ddl.DDLFileAttachmentManager;
import com.dbn.ddl.DDLFileManager;
import com.dbn.ddl.DDLFileType;
import com.dbn.editor.DBContentType;
import com.dbn.editor.EditorProviderId;
import com.dbn.language.sql.SQLFileType;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DatabaseFileSystem;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.vfs.file.status.DBFileStatus.SAVING;

@Getter
@Setter
public class DBEditableObjectVirtualFile extends DBObjectVirtualFile<DBSchemaObject>/* implements VirtualFileWindow*/ {
    private static final List<DBContentVirtualFile> EMPTY_CONTENT_FILES = Collections.emptyList();
    private final Latent<List<DBContentVirtualFile>> contentFiles = Latent.basic(() -> computeContentFiles());
    private transient EditorProviderId selectedEditorProviderId;
    private SessionId databaseSessionId;

    public DBEditableObjectVirtualFile(Project project, DBObjectRef object) {
        super(project, object);
        if (object.getObjectType() == DBObjectType.TABLE) {
            databaseSessionId = SessionId.MAIN;
        }
    }

    public static DBEditableObjectVirtualFile of(@Nullable VirtualFile file) {
        if (file == null) return null;
        if (file instanceof DBEditableObjectVirtualFile) return (DBEditableObjectVirtualFile) file;
        if (file instanceof DBContentVirtualFile) return ((DBContentVirtualFile) file).getMainDatabaseFile();
        return null;
    }

    public boolean isEditorReady() {
        if (!getObjectRef().isLoaded()) return false;
        return getObject().isEditorReady();
    }

    public void makeEditorReady() {
        DBSchemaObject object = getObject();
        object.makeEditorReady();
    }


    @Override
    public DatabaseSession getSession() {
        if (databaseSessionId != null) {
            DatabaseSessionBundle sessionBundle = getConnection().getSessionBundle();
            return sessionBundle.getSession(databaseSessionId);
        }
        return super.getSession();
    }

    public List<DBContentVirtualFile> getContentFiles() {
        return contentFiles.get();
    }

    private List<DBContentVirtualFile> computeContentFiles() {
        List<DBContentVirtualFile> contentFiles = new ArrayList<>();
        DBContentType objectContentType = getContentType();
        if (objectContentType.isBundle()) {
            DBContentType[] contentTypes = objectContentType.getSubContentTypes();
            for (DBContentType contentType : contentTypes) {
                DBContentVirtualFile virtualFile =
                        contentType.isCode() ? new DBSourceCodeVirtualFile(this, contentType) :
                        contentType.isData() ? new DBDatasetVirtualFile(this, contentType) : null;
                if (virtualFile != null) {
                    contentFiles.add(virtualFile);
                }
            }
        } else {
            DBContentVirtualFile virtualFile =
                    objectContentType.isCode() ? new DBSourceCodeVirtualFile(this, objectContentType) :
                    objectContentType.isData() ? new DBDatasetVirtualFile(this, objectContentType) : null;
            if (virtualFile != null) {
                contentFiles.add(virtualFile);
            }
        }
        return contentFiles;
    }

    public boolean isContentLoaded() {
        return contentFiles != null;
    }

    public List<DBSourceCodeVirtualFile> getSourceCodeFiles() {
        List<DBSourceCodeVirtualFile> sourceCodeFiles = new ArrayList<>();
        List<DBContentVirtualFile> contentFiles = getContentFiles();
        for (DBContentVirtualFile contentFile : contentFiles) {
            if (contentFile instanceof DBSourceCodeVirtualFile) {
                sourceCodeFiles.add((DBSourceCodeVirtualFile) contentFile);
            }
        }
        return sourceCodeFiles;
    }

    @Nullable
    public List<VirtualFile> getAttachedDDLFiles() {
        DDLFileAttachmentManager fileAttachmentManager = DDLFileAttachmentManager.getInstance(getProject());
        return fileAttachmentManager.getAttachedDDLFiles(getObjectRef());
    }

    @NotNull
    public <T extends DBContentVirtualFile> T ensureContentFile(DBContentType contentType) {
        return nn(getContentFile(contentType));
    }

    @Nullable
    public <T extends DBContentVirtualFile> T getContentFile(DBContentType contentType) {
        for (DBContentVirtualFile contentFile : getContentFiles()) {
            if (contentFile.getContentType() == contentType) {
                return (T) contentFile;
            }
        }
        return null;
    }

    /*********************************************************
     *                     VirtualFile                       *
     *********************************************************/
    @Override
    @NotNull
    public FileType getFileType() {
        return guarded(SQLFileType.INSTANCE, this, f -> {
            DDLFileManager ddlFileManager = DDLFileManager.getInstance(getProject());
            DDLFileType type = ddlFileManager.getDDLFileType(getObjectType(), f.getMainContentType());
            return type == null ? SQLFileType.INSTANCE : type.getLanguageFileType();
        });
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    @NotNull
    public byte[] contentsToByteArray() throws IOException {
        DBContentType mainContentType = getMainContentType();
        if (mainContentType != null) {
            DBContentVirtualFile contentFile = getContentFile(mainContentType);
            return contentFile == null ? new byte[0] : contentFile.contentsToByteArray();
        }
        return new byte[0];
    }

    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        T userData = super.getUserData(key);
        if (key == FileDocumentManagerImpl.HARD_REF_TO_DOCUMENT_KEY) {
            return guarded(userData, () -> {
                DBContentType mainContentType = getMainContentType();
                boolean isCode = mainContentType == DBContentType.CODE || mainContentType == DBContentType.CODE_BODY;
                if (isCode) {
                    DBContentVirtualFile mainContentFile = getMainContentFile();
                    if (mainContentFile != null) {
                        Document document = Documents.getDocument(mainContentFile);
                        return (T) document;
                    }
                }
                return userData;
            });
        }
        return userData;
    }

    public DBContentType getMainContentType() {
        DBObjectRef<DBSchemaObject> objectRef = getObjectRef();
        DBObjectType objectType = objectRef.getObjectType();
        DBContentType contentType = objectType.getContentType();
        return
            contentType == DBContentType.CODE ? DBContentType.CODE :
            contentType == DBContentType.CODE_SPEC_AND_BODY ? DBContentType.CODE_BODY : null;
    }

    public DBContentVirtualFile getMainContentFile() {
        DBContentType mainContentType = getMainContentType();
        return getContentFile(mainContentType);
    }

    @Override
    public String getExtension() {
        return "psql";
    }

    @Override
    public void invalidate() {
        DatabaseFileSystem.getInstance().invalidateDatabaseFile(object);

        List<DBContentVirtualFile> contentVirtualFiles = contentFiles.value();
        if (contentVirtualFiles != null) {
            for (DBContentVirtualFile virtualFile : contentVirtualFiles) {
                virtualFile.invalidate();
            }
        }

        contentFiles.set(EMPTY_CONTENT_FILES);
        super.invalidate();
    }


    public boolean isModified() {
        if (!isContentLoaded()) return false;

        for (DBContentVirtualFile contentVirtualFile : getContentFiles()) {
            if (contentVirtualFile.isModified()) {
                return true;
            }
        }
        return false;
    }

    public boolean isSaving() {
        if (!isContentLoaded()) return false;

        for (DBSourceCodeVirtualFile sourceCodeFile : getSourceCodeFiles()) {
            if (sourceCodeFile.is(SAVING)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public DBContentType getContentType() {
        return object.getObjectType().getContentType();
    }
}

