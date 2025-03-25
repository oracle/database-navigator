/*
 * Copyright 2025 Oracle and/or its affiliates
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

import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.util.SlowOps;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.DBVirtualFile;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Failsafe.nd;

@Getter
abstract class DBObjectContentVirtualFile<T extends DBObject> extends LightVirtualFile implements DBVirtualFile {
    private final DBObjectRef<T> object;
    protected String path;
    protected String url;

    DBObjectContentVirtualFile(T object, FileType fileType, Language language, CharSequence content) {
        super(buildFileName(object, fileType),
                fileType,
                content,
                System.currentTimeMillis());

        setLanguage(language);
        setCharset(object.getCharset());

        this.object = DBObjectRef.of(object);

        Project project = object.getProject();

        // cache view provider
        DatabaseFileViewProvider viewProvider = new DatabaseFileViewProvider(project, this, true);

        PsiManagerEx psiManager = (PsiManagerEx) PsiManager.getInstance(project);
        FileManager fileManager = psiManager.getFileManager();
        fileManager.setViewProvider(this, viewProvider);
    }

    private static String buildFileName(DBObject object, FileType fileType) {
        return object.getName() + "." + fileType.getDefaultExtension();
    }

    @NotNull
    @Override
    public final String getPath() {
        if (path == null)
            path = DatabaseFileSystem.createFilePath(this);
        return path;
    }

    @NotNull
    @Override
    public final String getUrl() {
        if (url == null)
            url = DatabaseFileSystem.createFileUrl(this);
        return url;
    }

    @Override
    public final boolean isValid() {
        return SlowOps.isValid(object);
    }

    @NotNull
    public final T getObject() {
        return DBObjectRef.ensure(object);
    }

    @NotNull
    @Override
    public final Project getProject() {
        return getObject().getProject();
    }

    @Override
    public Icon getIcon() {
        return getFileType().getIcon();
    }

    @NotNull
    @Override
    public final ConnectionId getConnectionId() {
        return getObject().getConnectionId();
    }

    @Override
    @NotNull
    public final ConnectionHandler getConnection() {
        return getObject().getConnection();
    }

    @Nullable
    @Override
    public final SchemaId getSchemaId() {
        return getObject().getSchemaId();
    }

    @Override
    public final DatabaseSession getSession() {
        return getConnection().getSessionBundle().getMainSession();
    }

    @Override
    public void invalidate() {
    }

    public final PsiFile getPsiFile() {
        DatabaseFileViewProvider viewProvider = nd(getCachedViewProvider());
        return viewProvider.getPsi(getLanguage());
    }

    @NotNull
    @Override
    public final EnvironmentType getEnvironmentType() {
        return getConnection().getEnvironmentType();
    }
}
