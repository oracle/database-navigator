package com.dbn.mcp.vfs;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.SchemaId;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.sql.SQLLanguage;
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
public class McpToolSqlVirtualFile extends DBVirtualFileBase implements DBParseableVirtualFile {
    private final ConnectionRef connection;
    private final SchemaId schemaId;
    private CharSequence content;

    public McpToolSqlVirtualFile(ConnectionHandler connection, String content) {
        super(connection.getProject(), "mcp-tool-query.sql");
        this.connection = connection.ref();
        this.content = content;
        this.schemaId = connection.getDefaultSchema();
        setCharset(connection.getSettings().getDetailSettings().getCharset());
        putUserData(LANGUAGE_DIALECT, DBLanguageDialect.get(SQLLanguage.INSTANCE, connection));
    }

    @Override
    public PsiFile initializePsiFile(DatabaseFileViewProvider viewProvider, DBLanguage<?> language) {
        ConnectionHandler conn = Failsafe.nn(getConnection());
        DBLanguageDialect dialect = conn.resolveLanguageDialect(language);
        return dialect == null ? null : viewProvider.initializePsiFile(dialect);
    }

    @Override public boolean isValid() { return connection.get() != null; }
    @Override public boolean isWritable() { return true; }
    @Override public Icon getIcon() { return Icons.FILE_SQL; }
    @Override public String getExtension() { return "sql"; }

    @NotNull @Override public ConnectionId getConnectionId() { return connection.getConnectionId(); }
    @NotNull @Override public ConnectionHandler getConnection() { return connection.ensure(); }
    @Nullable @Override public SchemaId getSchemaId() { return schemaId; }
    @Nullable @Override public DatabaseSession getSession() { return getConnection().getSessionBundle().getMainSession(); }

    @NotNull
    @Override
    public OutputStream getOutputStream(Object requestor, long modStamp, long timeStamp) {
        return new ByteArrayOutputStream() {
            @Override
            public void close() {
                setContent(this.toString());
                setTimeStamp(timeStamp);
                setModificationStamp(modStamp);
            }
        };
    }

    @NotNull @Override public byte[] contentsToByteArray() { return content.toString().getBytes(getCharset()); }
    @Override public long getLength() { return content.length(); }
    @NotNull @Override public InputStream getInputStream() { return new ByteArrayInputStream(content.toString().getBytes(getCharset())); }
}
