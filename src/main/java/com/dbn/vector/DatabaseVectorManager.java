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
import com.dbn.execution.ExecutionManager;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.vector.model.FileResult;
import com.dbn.vector.model.PipelineStep;
import com.dbn.vector.model.SourceResult;
import com.dbn.vector.model.SourceStatus;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.TableResult;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.chunk.ChunkConfig;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.sourceconfig.FileSystemSourceConfig;
import com.dbn.vector.model.sourceconfig.SourceConfig;
import com.dbn.vector.model.sourceconfig.SourceType;
import com.dbn.vector.model.store.DestinationType;
import com.dbn.vector.model.store.StoreConfig;
import com.dbn.vector.result.VectorEmbeddingExecutionResult;
import com.dbn.vector.ui.VectorAiDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import groovy.util.logging.Slf4j;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;

import static com.dbn.common.Priority.MEDIUM;
import static com.dbn.common.operation.DatabaseOperation.CREATE_VECTOR_EMBEDDINGS;
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
    static final String FILES_TABLE   = "document_files";

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
    // so we have one request per connectionId
  //
    @NotNull
    private static VectorEmbeddingRequest createEmbeddingRequest(ConnectionId connectionId) {
        VectorEmbeddingRequest embeddingRequest = new VectorEmbeddingRequest();
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
        embeddingRequest.initialize(connection.getUserSchema());
        return embeddingRequest;
    }

    public void openVectorToolbox(ConnectionHandler connection) {
        CREATE_VECTOR_EMBEDDINGS.start(connection, () -> Dialogs.show(() -> new VectorAiDialog(connection)));
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

        Progress.prompt(
                getProject(),
                handler.getSchema(), true,
                "Embedding Data",
                "Embedding data into \"" + storeConfig.getSchemaName() + "\".\"" + storeConfig.getTableName() + "\"",
                 p -> {
                     ConnectionId connectionId = handler.getConnectionId();
                     DatabaseInterfaceInvoker.execute(MEDIUM,
                                p.getText(),
                                p.getText2(),
                                handler.getProject(),
                             connectionId,
                                handler.getSchemaId(),
                                conn -> {
                                  createEmbeddings(request,handler, callbackInfo, callbackError, p, conn, storeConfig, connectionId, sourceConfig, chunkConfig, embedConfig);
                                });
                });

    }

  private void createEmbeddings(VectorEmbeddingRequest request,ConnectionHandler handler, Runnable callbackInfo, Consumer<Exception> callbackError, ProgressIndicator p, DBNConnection conn, StoreConfig storeConfig, ConnectionId connectionId, SourceConfig sourceConfig, ChunkConfig chunkConfig, EmbedConfig embedConfig) throws SQLException {

    DatabaseAssistantInterface dataDefinition = handler.getAssistantInterface();
    VectorEmbeddingResult result = new VectorEmbeddingResult(conn.getConnectionHandler());
//    request.setResult(result);
    result.setSourceType(sourceConfig.getSourceType());

    SourceResult src = null;
    if (SourceType.DATABASE_TABLE.equals( sourceConfig.getSourceType())){
      src = new TableResult(
              handler.getConnectionId(),
              storeConfig.getSchemaName(),
              storeConfig.getTableName());
    }else if(SourceType.FILE_SYSTEM.equals(sourceConfig.getSourceType())){
      src = new FileResult();
    }

//    result.getSourceResults().add(src);
    List<SourceResult> sourceResults = new ArrayList<>();

    StepResult ensureDestStep = src.startStep(PipelineStep.ENSURE_DESTINATION);
    try{
      ensureDestinationTable(conn, storeConfig, dataDefinition, connectionId);
      ensureDestStep.markSuccess();
    }catch (SQLException e){
      ensureDestStep.markFailed("ENSURE_DEST_ERROR", e.getMessage());
      src.finishFailed("ENSURE_DEST_ERROR", e.getMessage());
      if (ensureDestStep.isCritical()) {
        result.finish();
        showResultDialog(result);
        return;
      }
    }



    p.setText2("Embedding data");
    SourceType sourceType = sourceConfig.getSourceType();
    String chunkConfigJson = chunkConfig.getConfigJson();
    String embedConfigJson = embedConfig.getConfigJson();

    if (sourceType == SourceType.DATABASE_TABLE) {
      StepResult embedStep = src.startStep(PipelineStep.EMBED);
      try{
        int embeddings = createEmbeddingsFromTable(conn, storeConfig, sourceConfig, dataDefinition, chunkConfigJson, embedConfigJson);
        embedStep.markSuccess();
        src.finishSuccess(embeddings);
      }catch (SQLException e){
        embedStep.markFailed("EMBED_ERROR", e.getMessage());
        src.finishFailed("EMBED_ERROR", e.getMessage());
      }
    } else if (sourceType == SourceType.FILE_SYSTEM){
      StepResult ensureDocumentStep = src.startStep(PipelineStep.ENSURE_DOCUMENT_TABLE);
      try {
        dataDefinition.ensureDocumentsTable(conn,FILES_TABLE);
        ensureDocumentStep.markSuccess();
      }catch (SQLException e){
        ensureDocumentStep.markFailed("DOCUMENTS_TABLE_ERROR",e.getMessage());
        src.finishFailed("DOCUMENTS_TABLE_ERROR",e.getMessage());
        if (ensureDocumentStep.isCritical()) {
          result.finish();
          showResultDialog(result);
          return;
        }
      }
    // each SourceResult has a list of StepResult .
      FileSystemSourceConfig fs = sourceConfig.getFileSourceConfig();
      List<VirtualFile> files = fs.getFiles();
      for (int i = 0; i < files.size(); i++) {
        // here per each file we need a new SourceResult object
        SourceResult sourceResult = null;
        sourceResult = createSourceResultFilledWithsuccededSteps(ensureDestStep,ensureDocumentStep);
        prepareAndEmbedFile(result, (FileResult) sourceResult,callbackError, p, conn, files, i, dataDefinition, embedConfigJson, chunkConfigJson, storeConfig);
        result.getSourceResults().add(sourceResult);
      }

    }
    result.finish();
    showResultDialog(result);
//    callbackInfo.run();
  }

  private SourceResult createSourceResultFilledWithsuccededSteps(StepResult ensureDestStep, StepResult ensureDocumentStep) {
      SourceResult sr = new FileResult();
      sr.getSteps().add(ensureDestStep);
      sr.getSteps().add(ensureDocumentStep);
      return sr;

  }

  private static int createEmbeddingsFromTable(DBNConnection conn, StoreConfig storeConfig, SourceConfig sourceConfig, DatabaseAssistantInterface dataDefinition, String chunkConfigJson, String embedConfigJson) throws SQLException {
    return dataDefinition.embedDataContent(conn, sourceConfig.getTableSourceConfig(), chunkConfigJson, embedConfigJson, storeConfig);
  }

  @SneakyThrows
  private void prepareAndEmbedFile(VectorEmbeddingResult result, FileResult src, Consumer<Exception> callbackError, ProgressIndicator p, DBNConnection conn, List<VirtualFile> files, int i, DatabaseAssistantInterface dataDefinition, String embedConfigJson, String chunkConfigJson, StoreConfig storeConfig) {
    String id = generateDocumentId();

    VirtualFile vf = files.get(i);
      src.setFile(vf);
      src.setDocId(id);


      p.setText2("Uploading file \"" + vf.getName() + "\" (" + (i + 1) + "/" + files.size() + ")");

      long crcFile = checkIfFileExistsUsingCRC(result,src,conn, dataDefinition, vf);

      Map<String,Object> fileMetadataMap = getFileMetadata(conn,vf);
      //convert Metadata to json
      String fileMetadata = Json.writeAsString(fileMetadataMap);
      Blob blobData = null;
      if (!src.getStatus().equals(SourceStatus.RUNNING)){
        return;
      }
      StepResult uploadingFileStep = src.startStep(PipelineStep.UPLOADING_FILE);
      try {
          dataDefinition.insertEmptyDocumentRow(conn,FILES_TABLE,id,fileMetadata,crcFile);
          blobData = selectBlobFromInsertedLine(conn, dataDefinition, id, vf);
          uploadingFileStep.markSuccess();
      }catch (SQLException e){
        uploadingFileStep.markFailed("UPLOAD_ERROR", e.getMessage());
        src.finishFailed("UPLOAD_ERROR", e.getMessage());
      }

      String rowMetadataJson = buildRowMetadata(embedConfigJson, chunkConfigJson, fileMetadataMap, id);
//      storeConfig.setMetadata(rowMetadataJson);
      p.setText2("Embedding file \"" + vf.getName() + "\" (" + (i + 1) + "/" + files.size() + ")");
      if (!src.getStatus().equals(SourceStatus.RUNNING)){
        return;
      }
      StepResult embedStep = src.startStep(PipelineStep.EMBED);
      try{
        int embeddings = dataDefinition.embedFileContent(conn, chunkConfigJson, embedConfigJson, storeConfig, blobData, rowMetadataJson);
        embedStep.markSuccess();
        src.finishSuccess(embeddings);
      }catch (SQLException e){
        embedStep.markFailed("EMBED_ERROR", e.getMessage());
        src.finishFailed("EMBED_ERROR", e.getMessage());
      }
  }

  private static String buildRowMetadata(String embedConfigJson, String chunkConfigJson, Map<String,Object> fileMetadataMap, String id) {
    Map<String ,Object> m = new HashMap<>(fileMetadataMap);
    m.put("doc_id", id);
    m.put("embed_config", embedConfigJson);
    m.put("chunk_config", chunkConfigJson);
    return Json.writeAsString(fileMetadataMap);
  }

  private static @NotNull Blob selectBlobFromInsertedLine(DBNConnection conn, DatabaseAssistantInterface dataDefinition, String id, VirtualFile vf) throws SQLException, IOException {
    ResultSet rs = dataDefinition.selectEmptyBlob(conn,FILES_TABLE, id);

    while (!rs.next()) {
      throw new SQLException("No row found in " + FILES_TABLE + " for id=" + id);
    }
    Blob blobData = rs.getBlob(1);

    try (InputStream in = vf.getInputStream();
         OutputStream out = blobData.setBinaryStream(1)) {
      byte[] buf = new byte[64 * 1024];
      int r;
      while ((r = in.read(buf)) != -1) {
        out.write(buf, 0, r);
      }
      out.flush();
    }
    return blobData;
  }

  private long checkIfFileExistsUsingCRC(VectorEmbeddingResult result, FileResult src, DBNConnection conn, DatabaseAssistantInterface dataDefinition, VirtualFile vf)  {
    long crcFile = 0;
    StepResult checkCRCStep = src.startStep(PipelineStep.CHECK_CRC);
      try{
        crcFile = computeCRC(vf);
        boolean alreadyExists = dataDefinition.fileAlreadyUploadedByCRC(conn,FILES_TABLE,crcFile);
        checkCRCStep.markSuccess();
        if (alreadyExists) {
          src.setExisted(true);
        }
      }catch (Exception e){
        checkCRCStep.markFailed("CRC_ERROR",e.getMessage());
        src.finishFailed("CRC_ERROR",e.getMessage());
      }

    return crcFile;
  }

  private static @NotNull String generateDocumentId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private static void ensureDestinationTable(DBNConnection conn, StoreConfig storeConfig, DatabaseAssistantInterface dataDefinition, ConnectionId connectionId) throws SQLException {
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
  }

  private long computeCRC(VirtualFile vf) throws IOException {
      //todo 64? limit the risk of c
    // check intellij's 64
    // use varchar instead of number and  appened the files size
    CRC32 crc = new CRC32();
    try (InputStream in = vf.getInputStream()) {
      byte[] buffer = new byte[64 * 1024];
      int read;
      while ((read = in.read(buffer)) != -1) {
        crc.update(buffer, 0, read);
      }
      return crc.getValue();
    }
  }

  private void showResultDialog(VectorEmbeddingResult result) {
    ExecutionManager executionManager = ExecutionManager.getInstance(getProject());
    VectorEmbeddingExecutionResult executionResult = new VectorEmbeddingExecutionResult(result);
    executionManager.addExecutionResult(executionResult);
    // Ensure dialog is opened on the EDT
//    ApplicationManager.getApplication().invokeLater(() ->
//        Dialogs.show(() -> new VectorEmbeddingResultDialog(getProject(), result))
//    );
  }


  @SneakyThrows
  private Map<String, Object> getFileMetadata(DBNConnection conn, VirtualFile vf) {
    Map<String, Object> params = new HashMap<>();

    params.put("filename", vf.getName());
    params.put("path", vf.getPath());
    params.put("size_bytes", vf.getLength());
    params.put("uploaded_by", conn.getSchema() != null ? conn.getSchema() : "unknown");
    params.put("uploaded_at", Instant.now().toString());

    return params;
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
