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
import com.dbn.common.icon.Icons;
import com.dbn.common.util.SlowOps;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.DBDataset;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.DBParseableVirtualFile;
import com.dbn.vfs.DBVirtualFileBase;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import static com.dbn.common.action.UserDataKeys.LANGUAGE_DIALECT;

@Getter
@Setter
public class DBDatasetFilterVirtualFile extends DBVirtualFileBase implements DBParseableVirtualFile {
    private final DBObjectRef<DBDataset> dataset;
    private CharSequence content;

    public DBDatasetFilterVirtualFile(DBDataset dataset, String content) {
        super(dataset.getProject(), dataset.getName());
        this.dataset = DBObjectRef.of(dataset);
        this.content = content;
        ConnectionHandler connection = Failsafe.nn(getConnection());
        setCharset(connection.getSettings().getDetailSettings().getCharset());
        putUserData(PARSE_ROOT_ID_KEY, "subquery");
        putUserData(LANGUAGE_DIALECT, DBLanguageDialect.get(SQLLanguage.INSTANCE, connection));
    }

    @Override
    public PsiFile initializePsiFile(DatabaseFileViewProvider fileViewProvider, DBLanguage<?> language) {
        ConnectionHandler connection = Failsafe.nn(getConnection());
        DBLanguageDialect languageDialect = connection.resolveLanguageDialect(language);
        return languageDialect == null ? null : fileViewProvider.initializePsiFile(languageDialect);
    }

    @Override
    public boolean isValid() {
        return SlowOps.isValid(dataset);
    }

    public DBDataset getDataset() {
        return DBObjectRef.get(dataset);
    }

    @Override
    public Icon getIcon() {
        return Icons.DBO_TABLE;
    }


    @NotNull
    @Override
    public ConnectionId getConnectionId() {
        return dataset.getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return dataset.ensureConnection();
    }

    @Nullable
    @Override
    public SchemaId getSchemaId() {
        DBDataset dataset = getDataset();
        return dataset == null ? null : dataset.getSchemaId();
    }

    @Nullable
    @Override
    public DatabaseSession getSession() {
        return getConnection().getSessionBundle().getMainSession();
    }

    @Override
    public boolean isWritable() {
        return true;
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
    @NotNull
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
        return "sql";
    }

}
