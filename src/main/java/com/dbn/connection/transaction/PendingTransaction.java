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

package com.dbn.connection.transaction;

import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.vfs.DBVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class PendingTransaction {
    private final WeakRef<VirtualFile> file;
    private final WeakRef<DBNConnection> connection;
    private int changesCount = 0;

    public PendingTransaction(@NotNull DBNConnection connection, VirtualFile file) {
        this.file = WeakRef.of(file);
        this.connection = WeakRef.of(connection);
    }

    @NotNull
    public DBNConnection getConnection() {
        return connection.ensure();
    }

    @Nullable
    public VirtualFile getFile() {
        return file.get();
    }

    public SessionId getSessionId() {
        return getConnection().getSessionId();
    }

    public int getChangesCount() {
        return changesCount;
    }

    public void incrementChangesCount() {
        changesCount++;
    }

    public String getFilePath() {
        VirtualFile file = getFile();
        return file == null ? "Unknown file" : file.getPresentableUrl();
    }

    public Icon getFileIcon() {
        VirtualFile file = getFile();

        if (file != null) {
            if (file instanceof DBVirtualFile) {
                DBVirtualFile databaseVirtual = (DBVirtualFile) file;
                return databaseVirtual.getIcon();
            } else {
                return file.getFileType().getIcon();
            }
        }
        return Icons.FILE_SQL;
    }
}
