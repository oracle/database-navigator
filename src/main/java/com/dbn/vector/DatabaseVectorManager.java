package com.dbn.vector;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Json;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.chunk.ChunkConfig;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.sourceconfig.FileSystemSourceConfig;
import com.dbn.vector.model.sourceconfig.SourceConfig;
import com.dbn.vector.model.sourceconfig.SourceType;
import com.dbn.vector.model.store.DestinationType;
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
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.Priority.MEDIUM;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.type.DBObjectType.TABLE;


@Slf4j
@State(
        name = DatabaseVectorManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public  class DatabaseVectorManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseVectorManager";
    private final Map<ConnectionId, VectorEmbeddingRequest> embeddingRequests = new ConcurrentHashMap<>();
    static final String FILES_TABLE   = "document_files";   // NEWLINE|SENTENCE|PARAGRAPH

    public DatabaseVectorManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC, connectionConfigListener());
    }

    @NotNull
    private ConnectionConfigListener connectionConfigListener() {
        return new ConnectionConfigListener() {
            @Override
            public void connectionRemoved(ConnectionId connectionId) {
                // remove embedding requests when connection configs are deleted
                embeddingRequests.remove(connectionId);
            }
        };
    }
    public static DatabaseVectorManager getInstance(Project project) {
        return Components.projectService(project, DatabaseVectorManager.class);
    }

    public VectorEmbeddingRequest getEmbeddingRequest(ConnectionId connectionId) {
        return embeddingRequests.computeIfAbsent(connectionId, c -> createEmbeddingRequest(c));
    }

    @NonNull
    private static VectorEmbeddingRequest createEmbeddingRequest(ConnectionId connectionId) {
        VectorEmbeddingRequest embeddingRequest = new VectorEmbeddingRequest();
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
        embeddingRequest.initialize(connection.getUserSchema());
        return embeddingRequest;
    }

    public void openVectorToolbox(ConnectionHandler connection) {
        Dialogs.show(() -> new VectorAiDialog(connection));
    }

    public ResultSet chunkTextContent(ConnectionHandler connection, ChunkConfig config, String text) throws SQLException {
        return DatabaseInterfaceInvoker.load(MEDIUM,
                "Chunking Data",
                "Chunking text content",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseAssistantInterface assistantInterface = connection.getAssistantInterface();
                    return assistantInterface.chunkTextContent(text,
                            config.getChunkBy(),
                            config.getSplitBy(),
                            config.getMaxSize(),
                            config.getOverlap(), conn);
                });
    }

    @SneakyThrows
    //todo think of an Object as Request that has all the input of the user
    // also a Result Object .
    public void createEmbeddings(VectorEmbeddingRequest request, ConnectionHandler handler, Runnable callbackInfo, Consumer<Exception> callbackError)  {
        SourceConfig sourceConfig = request.getSourceConfig();
        ChunkConfig chunkConfig = request.getChunkConfig();
        EmbedConfig embedConfig = request.getEmbedConfig();
        StoreConfig storeConfig = request.getStoreConfig();

        Progress.modal(
                getProject(),
                handler.getSchema(), true,
                "Embedding Data",
                "Creating store table" + storeConfig.getTableName(),
                 p -> {
//                    try {
                     ConnectionId connectionId = handler.getConnectionId();
                     DatabaseInterfaceInvoker.execute(HIGHEST,
                                p.getText(),
                                p.getText2(),
                                handler.getProject(),
                             connectionId,
                                handler.getSchemaId(),
                                conn -> {
                                    DatabaseAssistantInterface dataDefinition = handler.getAssistantInterface();
                                    if (storeConfig.getDestinationType() == DestinationType.NEW_TABLE) {
                                        dataDefinition.createEmbeddingTable(conn,
                                                storeConfig.getSchemaName(),
                                                storeConfig.getTableName(),
                                                storeConfig.getKeyColumnName(),
                                                storeConfig.getTextColumnName(),
                                                storeConfig.getEmbeddingColumnName(),
                                                storeConfig.getMetadataColumnName());

                                        // refresh tables in the browser
                                        SchemaId ownerId = SchemaId.get(storeConfig.getSchemaName());
                                        ObjectChangeEvent.notify(CREATE, TABLE, connectionId, ownerId);

                                    }

                                    p.setText2("Embedding data");
                                    SourceType sourceType = sourceConfig.getSourceType();
                                    String chunkConfigJson = chunkConfig.getConfigJson();
                                    String embedConfigJson = embedConfig.getConfigJson();

                                    if (sourceType == SourceType.DATABASE_TABLE) {
                                        dataDefinition.embed(conn, sourceConfig.getTableSourceConfig(), chunkConfigJson, embedConfigJson, storeConfig);
                                        System.out.println("Embedding data created");
                                        //todo keep if else open to sother source config
                                    } else if (sourceType == SourceType.FILE_SYSTEM){
                                      FileSystemSourceConfig fs = sourceConfig.getFileSourceConfig();
                                      List<VirtualFile> files = fs.getFiles();
                                      dataDefinition.ensureDocumentsTable(conn,FILES_TABLE);
                                      for (int i = 0; i < files.size(); i++) {
                                        VirtualFile vf = files.get(i);
                                        p.setText2("Embedding (" + (i + 1) + "/" + files.size() + "): " + vf.getName());


                                          InputStream in = null ;
                                          try {
                                            in = prepareFileBlob(conn, vf);

                                            String id = UUID.randomUUID().toString().replace("-", "");
                                            Map<String, Object> fileMetadataMap = getFileMeatadata(conn,vf);
                                            String fileMetadata = Json.writeAsString(fileMetadataMap);
                                            dataDefinition.insertEmptyDocumentRow(conn,FILES_TABLE,id,fileMetadata);
                                            dataDefinition.streamContentToBlob(conn,FILES_TABLE,id,in);


                                            fileMetadataMap.put("doc_id",id);
                                            fileMetadataMap.put("embed_config", embedConfig.getConfigMap());
                                            fileMetadataMap.put("chunk_config", chunkConfig.getConfigMap());
                                            String rowMetadata = Json.writeAsFormattedString(fileMetadataMap);
                                            storeConfig.setMetadata(rowMetadata);
                                            dataDefinition.embed(conn, id, FILES_TABLE, chunkConfigJson, embedConfigJson, storeConfig); // add this overload
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

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        Element requestsElement = newElement(element, "embedding-requests");
        for (ConnectionId connectionId : embeddingRequests.keySet()) {
            Element requestElement = newElement(requestsElement, "embedding-request");

            setConstantAttribute(requestElement, "connection-id", connectionId);
            VectorEmbeddingRequest embeddingRequest = embeddingRequests.get(connectionId);
            embeddingRequest.writeState(requestElement);
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element requestsElement = element.getChild("embedding-requests");
        List<Element> requestElements = childrenOf(requestsElement, "embedding-request");
        for (Element requestElement : requestElements) {
            ConnectionId connectionId = constantAttribute(requestElement, "connection-id", ConnectionId.class);
            VectorEmbeddingRequest embeddingRequest = new VectorEmbeddingRequest();
            embeddingRequests.put(connectionId, embeddingRequest);
            embeddingRequest.readState(requestElement);
        }
    }
}
