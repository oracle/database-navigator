package com.dbn.vector;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.object.DBSchema;
import com.dbn.vector.model.chunk.ChunkConfiguration;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import com.dbn.vector.model.sourceconfig.FileSystemSourceConfig;
import com.dbn.vector.model.sourceconfig.SourceConfig;
import com.dbn.vector.model.store.StoreConfig;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import groovy.util.logging.Slf4j;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.Priority.MEDIUM;


@Slf4j
@State(
        name = DatabaseVectorManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public  class DatabaseVectorManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseVectorManager";

    public DatabaseVectorManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseVectorManager getInstance(Project project) {
        return Components.projectService(project, DatabaseVectorManager.class);
    }

    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element state) {

    }

    public ResultSet chunkTextContent(ConnectionHandler connection, ChunkConfiguration configuration, String text) throws SQLException {
        return DatabaseInterfaceInvoker.load(MEDIUM,
                "Chunking Data",
                "Chunking text content",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseAssistantInterface assistantInterface = connection.getAssistantInterface();
                    return assistantInterface.chunk(text, configuration, conn);
                });
    }

    @SneakyThrows
    public void query(SourceConfig sourceConfig, ChunkConfiguration chunkConfiguration, EmbedConfig embedConfig, StoreConfig storeConfig, ConnectionHandler handler, Runnable callbackInfo, Consumer<Exception> callbackError) throws SQLException {
        Progress.modal(
                getProject(),
                handler.getSchema(), true,
                "Embedding Data",
                "Creating store table" + storeConfig.getTableName(),
                 p -> {
                    try {
                        DatabaseInterfaceInvoker.execute(HIGHEST,
                                p.getText(),
                                p.getText2(),
                                handler.getProject(),
                                handler.getConnectionId(),
                                handler.getSchemaId(),
                                conn -> {
                                    DBSchema schema = handler.getSchema(handler.getUserSchema());

                                    DatabaseAssistantInterface dataDefinition = schema.getAssistantInterface();
                                    dataDefinition.createTable(conn, storeConfig.getTableName());
                                    p.setText2("Embedding data");
                                    if (sourceConfig instanceof DBTableSourceConfig) {
                                        dataDefinition.embed(conn, (DBTableSourceConfig) sourceConfig, chunkConfiguration, embedConfig, storeConfig);
                                        System.out.println("Embedding data created");
                                    } else {
                                      FileSystemSourceConfig fs = (FileSystemSourceConfig) sourceConfig;
                                      List<VirtualFile> files = fs.getVirtualFiles();

                                      for (int i = 0; i < files.size(); i++) {
                                        VirtualFile vf = files.get(i);
                                        p.setText2("Embedding (" + (i + 1) + "/" + files.size() + "): " + vf.getName());

                                        Clob clob = null;
                                        try {
                                          clob = prepareFileClob(conn, vf);
                                          dataDefinition.embed(conn, clob, chunkConfiguration, embedConfig, storeConfig);
                                        } catch (Exception e) {
                                          throw new RuntimeException(e);
                                        } finally {
                                          if (clob != null) try { clob.free(); } catch (Throwable ignored) {}
                                        }
                                      }

                                      System.out.println("Embedding data created (" + files.size() + " file(s))");
                                    }
                                    callbackInfo.run();
                                });
                    }catch (SQLException e) {
                      callbackError.accept(e);
//                      new RuntimeException(e);
                    }
                });

    }

  private Clob prepareFileClob(DBNConnection conn, VirtualFile virtualFile) throws Exception {
    Clob clob = conn.createClob();

    try (InputStream in = virtualFile.getInputStream();
         Reader reader = new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8);
         Writer writer = clob.setCharacterStream(1)) {

      char[] buf = new char[64 * 1024]; // 64 KiB buffer for large files
      int n;
      while ((n = reader.read(buf)) != -1) {
        writer.write(buf, 0, n);
      }
    }

    return clob;
  }
}
