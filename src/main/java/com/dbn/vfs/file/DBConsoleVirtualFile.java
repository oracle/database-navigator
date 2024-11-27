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

import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import com.dbn.connection.mapping.FileConnectionContext;
import com.dbn.connection.mapping.FileConnectionContextImpl;
import com.dbn.connection.mapping.FileConnectionContextProvider;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLFileType;
import com.dbn.object.DBConsole;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.DBConsoleType;
import com.dbn.vfs.DBParseableVirtualFile;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

@Getter
public class DBConsoleVirtualFile extends DBObjectVirtualFile<DBConsole> implements DocumentListener, DBParseableVirtualFile, Comparable<DBConsoleVirtualFile>, FileConnectionContextProvider {
    private final SourceCodeContent content = new SourceCodeContent();
    private final FileConnectionContext connectionContext;

    public DBConsoleVirtualFile(@NotNull DBConsole console) {
        super(console.getProject(), DBObjectRef.of(console));

        ConnectionHandler connection = console.getConnection();
        SessionId sessionId = connection.getSessionBundle().getMainSession().getId();
        connectionContext = createConnectionContext(this, sessionId, null);

        setCharset(connection.getSettings().getDetailSettings().getCharset());
    }

    public void setText(String text) {
        if (getObject().getConsoleType() == DBConsoleType.DEBUG && Strings.isEmpty(text)) {
            ConnectionHandler connection = getConnection();
            Project project = connection.getProject();

            DatabaseDebuggerInterface debuggerInterface = connection.getDebuggerInterface();
            CodeStyleCaseSettings styleCaseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(PSQLLanguage.INSTANCE);
            text = debuggerInterface.getDebugConsoleTemplate(styleCaseSettings);
        }
        content.importContent(text);
    }

    @Override
    public PsiFile initializePsiFile(DatabaseFileViewProvider fileViewProvider, DBLanguage<?> language) {
        ConnectionHandler connection = getConnection();
        DBLanguageDialect languageDialect = connection.resolveLanguageDialect(language);
        return languageDialect == null ? null : fileViewProvider.initializePsiFile(languageDialect);
    }

    public void setName(String name) {
        super.setName(name);
        this.path = null;
        this.url = null;
   }

    @NotNull
    public DBConsole getConsole() {
        return getObject();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        switch (getType()) {
            case STANDARD: return Icons.FILE_SQL_CONSOLE;
            case DEBUG: return Icons.FILE_SQL_DEBUG_CONSOLE;
        }
        return null;
    }
    public void setDatabaseSchema(SchemaId schemaId) {
        connectionContext.setSchemaId(schemaId);
    }

    public void setDatabaseSchemaName(String schemaName) {
        if (Strings.isEmpty(schemaName)) {
            setDatabaseSchema(null);
        } else {
            setDatabaseSchema(SchemaId.get(schemaName));
        }
    }

    public String getDatabaseSchemaName() {
        return connectionContext.getSchemaName();
    }

    public void setDatabaseSessionId(SessionId sessionId) {
        connectionContext.setSessionId(sessionId);
    }

    @Override
    public DatabaseSession getSession() {
        return connectionContext.getSession();
    }

    public void setDatabaseSession(DatabaseSession databaseSession) {
        this.connectionContext.setSessionId(databaseSession == null ? SessionId.MAIN : databaseSession.getId());
    }

    @Override
    public boolean isValid() {
        return super.isValid() /*&& connection.isValid()*/;
    }

    @Override
    @Nullable
    public SchemaId getSchemaId() {
        return connectionContext.getSchemaId();
    }

    public DBConsoleType getType() {
        return getObject().getConsoleType();
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    public boolean isDefault() {
        return Objects.equals(getName(), getConnection().getName());
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return SQLFileType.INSTANCE;
    }

    @Override
    @NotNull
    public OutputStream getOutputStream(Object requestor, long modificationStamp, long timeStamp) throws IOException {
        return new ByteArrayOutputStream() {
            @Override
            public void close() {
                content.setText(toString());

                setTimeStamp(timeStamp);
                setModificationStamp(modificationStamp);

            }
        };
    }

    @Override
    @NotNull
    public byte[] contentsToByteArray() throws IOException {
        Charset charset = getCharset();
        return content.getBytes(charset);
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
        return "sql";
    }

    @Override
    public int compareTo(@NotNull DBConsoleVirtualFile o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        Document document = event.getDocument();
        content.setText(document.getCharsSequence());
        if (document instanceof DocumentEx) {
            DocumentEx documentEx = (DocumentEx) document;
            List<RangeMarker> blocks = documentEx.getGuardedBlocks();
            if (!blocks.isEmpty()) {
                content.getOffsets().setGuardedBlocks(blocks);
            }
        }
    }

    private static FileConnectionContext createConnectionContext(
            DBConsoleVirtualFile consoleFile,
            SessionId sessionId,
            SchemaId schemaId) {
        return new FileConnectionContextImpl(consoleFile.getUrl(), consoleFile.getConnectionId(), sessionId, schemaId) {
            @Override
            public void setFileUrl(String fileUrl) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean setConnectionId(ConnectionId connectionId) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
