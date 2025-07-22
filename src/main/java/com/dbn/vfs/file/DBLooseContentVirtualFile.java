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

import com.dbn.common.dispose.Failsafe;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.vfs.DBParseableVirtualFile;
import com.dbn.vfs.DBVirtualFileBase;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

@Getter
@Setter
public class DBLooseContentVirtualFile extends DBVirtualFileBase implements DBParseableVirtualFile {
    private final ConnectionRef connection;
    private final FileType fileType;
    private CharSequence content;

    public DBLooseContentVirtualFile(ConnectionHandler connection, String fileName, FileType fileType, String content) {
        super(connection.getProject(), fileName);
        this.connection = connection.ref();
        this.content = content;
        this.fileType = fileType;
        setCharset(connection.getSettings().getDetailSettings().getCharset());
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public PsiFile initializePsiFile(DatabaseFileViewProvider fileViewProvider, DBLanguage<?> language) {
        ConnectionHandler connection = Failsafe.nn(getConnection());
        DBLanguageDialect languageDialect = connection.resolveLanguageDialect(language);
        return languageDialect == null ? null : fileViewProvider.initializePsiFile(languageDialect);
    }

    @Override
    public Icon getIcon() {
        return fileType.getIcon();
    }

    @NotNull
    @Override
    public ConnectionId getConnectionId() {
        return connection.getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    @NotNull
    public OutputStream getOutputStream(Object requestor, long modificationStamp, long timeStamp) throws IOException {
        return new ByteArrayOutputStream() {
            @Override
            public void close() {
                setContent(this.toString());

                setTimeStamp(timeStamp);
                setModificationStamp(modificationStamp);
            }
        };
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        Charset charset = getCharset();
        return content.toString().getBytes(charset);
    }

    @Override
    public long getLength() {
        return content.length();
    }

    @NotNull
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(contentsToByteArray());
    }

    @Override
    public String getExtension() {
        return fileType.getDefaultExtension();
    }

}
