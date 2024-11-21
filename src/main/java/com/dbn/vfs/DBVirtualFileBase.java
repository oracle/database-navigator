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

package com.dbn.vfs;

import com.dbn.common.DevNullStreams;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.Presentable;
import com.dbn.connection.ConnectionHandler;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.sql.SQLFileType;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFilePathWrapper;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.util.LocalTimeCounter;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public abstract class DBVirtualFileBase extends VirtualFile implements DBVirtualFile, Presentable, VirtualFilePathWrapper {
    private static final byte[] EMPTY_CONTENT = new byte[0];
    private static final AtomicInteger ID_STORE = new AtomicInteger(1000);
    private final int id;
    private final ProjectRef project;
    private final WeakRef<DatabaseFileSystem> fileSystem;

    protected String path;
    protected String url;
    private volatile int documentSignature;

    private long modificationStamp = LocalTimeCounter.currentTime();
    private long timeStamp = System.currentTimeMillis();

    private String name;

    public DBVirtualFileBase(@NotNull Project project, @NotNull String name) {
        this.id = ID_STORE.incrementAndGet();
        this.name = name;
        //id = DummyFileIdGenerator.next();
        this.project = ProjectRef.of(project);
        this.fileSystem = WeakRef.of(DatabaseFileSystem.getInstance());
    }

    @NotNull
    @Override
    public EnvironmentType getEnvironmentType() {
        ConnectionHandler connection = getConnection();
        return connection == null ? EnvironmentType.DEFAULT : connection.getEnvironmentType();
    }

    @NotNull
    @Override
    public DatabaseFileSystem getFileSystem() {
        return fileSystem.ensure();
    }

    @Override
    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @Override
    public abstract Icon getIcon();

    @Override
    public VirtualFile[] getChildren() {
        return VirtualFile.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public InputStream getInputStream() throws IOException {
        return DevNullStreams.INPUT_STREAM;
    }

    @Override
    @NotNull
    public OutputStream getOutputStream(Object requestor, long modificationStamp, long timeStamp) throws IOException {
        return DevNullStreams.OUTPUT_STREAM;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public VirtualFile getParent() {
        return null;
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return SQLFileType.INSTANCE;
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return getName();
    }

    @NotNull
    @Override
    public String getPresentablePath() {
        return getFileSystem().extractPresentablePath(getPath());
    }

    @Override
    public boolean enforcePresentableName() {
        return false;
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
    public void rename(Object requestor, @NotNull String newName) throws IOException {
        throw DatabaseFileSystem.READONLY_FILE_SYSTEM;
    }

    @Override
    public void move(Object requestor, @NotNull VirtualFile newParent) throws IOException {
        throw DatabaseFileSystem.READONLY_FILE_SYSTEM;
    }

    @NotNull
    @Override
    public VirtualFile copy(Object requestor, @NotNull VirtualFile newParent, @NotNull String copyName) throws IOException {
        throw DatabaseFileSystem.READONLY_FILE_SYSTEM;
    }

    @Override
    public void delete(Object requestor) throws IOException {
        throw DatabaseFileSystem.READONLY_FILE_SYSTEM;
    }

    @Override
    public void setCachedViewProvider(@Nullable DatabaseFileViewProvider viewProvider) {
        putUserData(DatabaseFileViewProvider.CACHED_VIEW_PROVIDER, viewProvider);
    }

    @Override
    @Nullable
    public DatabaseFileViewProvider getCachedViewProvider() {
        return getUserData(DatabaseFileViewProvider.CACHED_VIEW_PROVIDER);
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        return EMPTY_CONTENT;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {}

    public void invalidate() {
        DatabaseFileViewProvider cachedViewProvider = getCachedViewProvider();

        if (cachedViewProvider != null) {
            DebugUtil.performPsiModification("disposing database view provider", () -> cachedViewProvider.markInvalidated());
            List<PsiFile> cachedPsiFiles = cachedViewProvider.getCachedPsiFiles();
            for (PsiFile cachedPsiFile: cachedPsiFiles) {
                if (cachedPsiFile instanceof DBLanguagePsiFile) {
                    DBLanguagePsiFile languagePsiFile = (DBLanguagePsiFile) cachedPsiFile;
                    Disposer.dispose(languagePsiFile);
                }
            }

            setCachedViewProvider(null);
        }
        putUserData(FileDocumentManagerImpl.HARD_REF_TO_DOCUMENT_KEY, null);
    }

}
