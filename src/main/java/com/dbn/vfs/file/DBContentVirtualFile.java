package com.dbn.vfs.file;

import com.dbn.common.property.PropertyHolder;
import com.dbn.common.ref.WeakRef;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.ddl.DDLFileManager;
import com.dbn.editor.DBContentType;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DBVirtualFileBase;
import com.dbn.vfs.file.status.DBFileStatus;
import com.dbn.vfs.file.status.DBFileStatusHolder;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.File;

import static com.dbn.vfs.file.status.DBFileStatus.MODIFIED;

@Getter
public abstract class DBContentVirtualFile extends DBVirtualFileBase implements PropertyHolder<DBFileStatus>  {
    private final WeakRef<DBEditableObjectVirtualFile> mainDatabaseFile;
    private final FileType fileType;

    private final DBFileStatusHolder status = new DBFileStatusHolder(this);

    protected DBContentType contentType;

    public DBContentVirtualFile(@NotNull DBEditableObjectVirtualFile mainDatabaseFile, DBContentType contentType) {
        super(mainDatabaseFile.getProject(), mainDatabaseFile.getObjectRef().getObjectName());
        this.mainDatabaseFile = WeakRef.of(mainDatabaseFile);
        this.contentType = contentType;

        DBObjectRef<DBSchemaObject> objectRef = mainDatabaseFile.getObjectRef();
        DBObjectType objectType = objectRef.getObjectType();

        Project project = getProject();
        DDLFileManager ddlFileManager = DDLFileManager.getInstance(project);
        this.fileType = ddlFileManager.resolveFileType(objectType, contentType);
    }

    @Override
    public boolean set(DBFileStatus status, boolean value) {
        return this.status.set(status, value);
    }

    @Override
    public boolean is(DBFileStatus status) {
        return this.status.is(status);
    }
    @Override
    @Nullable
    public SchemaId getSchemaId() {
        DBSchema schema = getObject().getSchema();
        return SchemaId.from(schema);
    }

    @NotNull
    public DBEditableObjectVirtualFile getMainDatabaseFile() {
        return mainDatabaseFile.ensure();
    }

    @Override
    public boolean isValid() {
        DBEditableObjectVirtualFile mainDatabaseFile = this.mainDatabaseFile.get();
        return mainDatabaseFile != null && mainDatabaseFile.isValid();
    }

    @NotNull
    public DBSchemaObject getObject() {
        return getMainDatabaseFile().getObject();
    }

    @NotNull
    public DBObjectRef<DBSchemaObject> getObjectRef() {
        return getMainDatabaseFile().getObjectRef();
    }


    @NotNull
    @Override
    public ConnectionId getConnectionId() {
        return getMainDatabaseFile().getConnectionId();
    }

    @NotNull
    @Override
    public ConnectionHandler getConnection() {
        return getMainDatabaseFile().getConnection();
    }

    public DBLanguageDialect getLanguageDialect() {
        DBObjectType objectType = getObjectRef().getObjectType();

        boolean view = objectType.isOneOf(DBObjectType.VIEW, DBObjectType.MATERIALIZED_VIEW);
        DBLanguage language = view ? SQLLanguage.INSTANCE : PSQLLanguage.INSTANCE;

        ConnectionHandler connection = getConnection();
        return connection.getLanguageDialect(language);
    }

    /*********************************************************
     *                     VirtualFile                       *
     *********************************************************/
    @NotNull
    public String getPresentablePath() {
        DBObjectRef<DBSchemaObject> object = getMainDatabaseFile().getObjectRef();
        return getConnection().getName() + File.separatorChar +
                object.getObjectType().getListName() + File.separatorChar +
                object.getQualifiedName() + " - " + getContentType().getDescription();
    }

    @NotNull
    @Override
    public String getPresentableName() {
        DBObjectRef<DBSchemaObject> object = getMainDatabaseFile().getObjectRef();
        return object.getObjectName() + " - " + getContentType().getDescription();
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    @Nullable
    public VirtualFile getParent() {
        if (!isValid()) return null;

        DBObjectRef parentObject = getObjectRef().getParentRef();
        if (parentObject == null) return null;

        return DBObjectVirtualFile.of(parentObject);
    }

    @Override
    public Icon getIcon() {
        DBObjectType objectType = getObjectRef().getObjectType();
        DBContentType contentType = getContentType();
        return objectType.getIcon(contentType);
    }

    public void setModified(boolean modified) {
        set(MODIFIED, modified);
    }

    public boolean isModified() {
        return is(MODIFIED);
    }
}
