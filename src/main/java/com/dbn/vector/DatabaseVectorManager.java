package com.dbn.vector;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Json;
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
import com.dbn.vector.ui.VectorAiDialog;
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
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.Priority.MEDIUM;


@Slf4j
@State(
        name = DatabaseVectorManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public  class DatabaseVectorManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseVectorManager";
    static final String FILES_TABLE   = "document_files";   // NEWLINE|SENTENCE|PARAGRAPH

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

    public void openVectorToolbox(ConnectionHandler connection) {
        Dialogs.show(() -> new VectorAiDialog(connection));
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
    //todo think of an Object as Request that has all the input of the user
    // also a Result Object .
    public void query(SourceConfig sourceConfig, ChunkConfiguration chunkConfiguration, EmbedConfig embedConfig, StoreConfig storeConfig, ConnectionHandler handler, Runnable callbackInfo, Consumer<Exception> callbackError)  {
        Progress.modal(
                getProject(),
                handler.getSchema(), true,
                "Embedding Data",
                "Creating store table" + storeConfig.getTableName(),
                 p -> {
//                    try {
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
                                        //todo keep if else open to sother source config
                                    } else {
                                      FileSystemSourceConfig fs = (FileSystemSourceConfig) sourceConfig;
                                      List<VirtualFile> files = fs.getVirtualFiles();
                                      dataDefinition.ensureDocumentsTable(conn,FILES_TABLE);
                                      for (int i = 0; i < files.size(); i++) {
                                        VirtualFile vf = files.get(i);
                                        p.setText2("Embedding (" + (i + 1) + "/" + files.size() + "): " + vf.getName());


                                          InputStream in = null ;
                                          try {
                                            in = prepareFileBlob(conn, vf);

                                            String id = UUID.randomUUID().toString().replace("-", "");
                                            Map fileMetadataMap = getFileMeatadata(conn,vf);
                                            String fileMetadata = Json.OBJECT_MAPPER.writeValueAsString(fileMetadataMap);
                                            dataDefinition.insertEmptyDocumentRow(conn,FILES_TABLE,id,fileMetadata);
                                            dataDefinition.streamContentToBlob(conn,FILES_TABLE,id,in);


                                            fileMetadataMap.put("doc_Id",id);
                                            fileMetadataMap.put("embedd_config",embedConfig.getConfigJson());
                                            fileMetadataMap.put("chunk_config",embedConfig.getConfigJson());
                                            String rowMetadata = Json.OBJECT_MAPPER.writeValueAsString(fileMetadataMap);
                                            storeConfig.setMetadata(rowMetadata);
                                            dataDefinition.embed(conn, id, FILES_TABLE,chunkConfiguration, embedConfig, storeConfig); // add this overload
                                          } catch (Exception e) {
                                            callbackError.accept(e);
                                          } finally { if (in != null) try { in.close(); } catch (Throwable ignored) {} }
                                      }

                                      System.out.println("Embedding data created (" + files.size() + " file(s))");
                                    }
                                    callbackInfo.run();
                                });
//                    }catch (SQLException e) {
//                      callbackError.accept(e);
////                      new RuntimeException(e);
//                    }
                });

    }
  @SneakyThrows
  private Map<String, Object> getFileMeatadata(DBNConnection conn, VirtualFile vf) {
    Map<String, Object> params = new java.util.HashMap<>();

    params.put("filename", vf.getName());
    params.put("path", vf.getPath());
    params.put("size_bytes", vf.getLength());
    params.put("uploaded_by", conn.getSchema() != null ? conn.getSchema() : "unknown");
    params.put("uploaded_at", java.time.Instant.now().toString());

    return params;
  }


  private InputStream prepareFileBlob(DBNConnection conn, VirtualFile vf) throws IOException, SQLException {
     return vf.getInputStream();
  }

  private static boolean isTextLike(String name) {
    String n = name.toLowerCase();
    return n.endsWith(".txt") || n.endsWith(".md") || n.endsWith(".csv") || n.endsWith(".json") || n.endsWith(".xml");
  }

  private Clob prepareFileClob(DBNConnection conn, VirtualFile virtualFile) throws SQLException, IOException {
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
