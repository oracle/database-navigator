package com.dbn.vector;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.execution.ExecutionManager;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.chunk.ChunkConfig;
import com.dbn.vector.model.source.SourceType;
import com.dbn.vector.model.store.StoreConfig;
import com.dbn.vector.pipeline.EmbeddingPipeline;
import com.dbn.vector.pipeline.FileEmbeddingPipeline;
import com.dbn.vector.pipeline.TableEmbeddingPipeline;
import com.dbn.vector.result.VectorEmbeddingExecutionResult;
import com.dbn.vector.ui.VectorToolboxDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.Priority.MEDIUM;
import static com.dbn.common.operation.DatabaseOperation.CREATE_VECTOR_EMBEDDINGS;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;


@State(
        name = DatabaseVectorManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseVectorManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseVectorManager";
    public static final String ENGINE_VERSION = "1.0.0";

    private final Map<ConnectionId, VectorEmbeddingRequest> requestTemplates = new ConcurrentHashMap<>();

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
                requestTemplates.remove(connectionId);
            }
        };
    }
    public static DatabaseVectorManager getInstance(Project project) {
        return Components.projectService(project, DatabaseVectorManager.class);
    }

    public VectorEmbeddingRequest getRequestTemplate(ConnectionId connectionId) {
        return requestTemplates.computeIfAbsent(connectionId, c -> createEmbeddingRequest(c));
    }

    public void setRequestTemplate(ConnectionId connectionId, VectorEmbeddingRequest request) {
        requestTemplates.put(connectionId, request);
    }

    @NotNull
    private static VectorEmbeddingRequest createEmbeddingRequest(ConnectionId connectionId) {
        VectorEmbeddingRequest embeddingRequest = new VectorEmbeddingRequest(connectionId);
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
        embeddingRequest.initialize(connection.getUserSchema());
        return embeddingRequest;
    }

    public void openVectorToolbox(ConnectionHandler connection) {
        VectorEmbeddingRequest requestTemplate = getRequestTemplate(connection.getConnectionId());
        VectorEmbeddingRequest request = requestTemplate.clone();
        request.setTemplate(true);
        openVectorToolbox(connection, request);
    }

    public void openVectorToolbox(ConnectionHandler connection, VectorEmbeddingRequest request) {
        if (request.isTemplate()) {
            CREATE_VECTOR_EMBEDDINGS.start(connection, () -> doOpenVectorToolbox(connection, request));
        } else {
            // no prerequisite check when opening from execution results
            doOpenVectorToolbox(connection, request);
        }
    }

    private static void doOpenVectorToolbox(ConnectionHandler connection, VectorEmbeddingRequest request) {
        Dialogs.show(() -> new VectorToolboxDialog(connection, request));
    }

    public ResultSet chunkTextContent(ConnectionHandler connection, ChunkConfig config, String text) throws SQLException {
        return DatabaseInterfaceInvoker.load(MEDIUM,
                "Chunking Data",
                "Chunking text content",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseVectorInterface vectorInterface = connection.getVectorInterface();
                    return vectorInterface.chunkTextContent(text,
                            config.getChunkBy(),
                            config.getSplitBy(),
                            config.getMaxSize(),
                            config.getOverlap(), conn);
                });
    }

    @SneakyThrows
    public void createEmbeddings(VectorEmbeddingRequest request, ConnectionHandler handler)  {
        request.setTemplate(false); // no longer a template after used for embedding

        StoreConfig storeConfig = request.getStoreConfig();
        Progress.prompt(
                getProject(),
                handler.getSchema(), true,
                "Embedding Data",
                "Embedding data into \"" + storeConfig.getSchemaName() + "\".\"" + storeConfig.getTableName() + "\"",
                 p -> {
                     DatabaseInterfaceInvoker.execute(MEDIUM,
                                p.getText(),
                                p.getText2(),
                                handler.getProject(),
                                handler.getConnectionId(),
                                handler.getSchemaId(),
                                conn -> {
                                  VectorEmbeddingResult result = null;
                                  try {
                                    result = executePipeline(request, handler, conn, p);
                                  } catch (Exception e) {
                                    throw new RuntimeException(e);
                                  }
                                  result.finish();
                                  showResultDialog(result);
//                                  callbackInfo.run();

                                });
                });
    }
    /**
     * Execute the embedding pipeline for the given request.
     */
    @SneakyThrows
    private VectorEmbeddingResult executePipeline(
            @NotNull VectorEmbeddingRequest request,
            @NotNull ConnectionHandler handler,
            @NotNull DBNConnection connection,
            @NotNull ProgressIndicator progressIndicator) throws Exception {
        

        VectorEmbeddingResult result = new VectorEmbeddingResult(request);
        result.setSourceType(request.getSourceConfig().getSourceType());
        

        EmbeddingPipeline pipeline = createPipeline(request.getSourceConfig().getSourceType());
        pipeline.execute(request, handler, connection, progressIndicator, result);
        
        return result;
    }

    /**
     * Factory method to create the appropriate pipeline based on source type.
     */
    private EmbeddingPipeline createPipeline(@NotNull SourceType sourceType) {
        switch (sourceType) {
            case DATABASE_TABLE:
                return new TableEmbeddingPipeline();
            case FILE_SYSTEM:
                return new FileEmbeddingPipeline();
            default:
                throw new IllegalArgumentException("Unsupported source type: " + sourceType);
        }
    }


  private void showResultDialog(VectorEmbeddingResult result) {
    ExecutionManager executionManager = ExecutionManager.getInstance(getProject());
    Set<String> names = executionManager.getExecutionResultNames(VectorEmbeddingExecutionResult.class);
    String name = Naming.nextNumberedIdentifier("Embedding Result",true,()->names);
    VectorEmbeddingExecutionResult executionResult = new VectorEmbeddingExecutionResult(result,name);
    executionManager.addExecutionResult(executionResult);
  }


    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        Element requestsElement = newElement(element, "embedding-requests");
        for (ConnectionId connectionId : requestTemplates.keySet()) {
            Element requestElement = newElement(requestsElement, "embedding-request");

            setConstantAttribute(requestElement, "connection-id", connectionId);
            VectorEmbeddingRequest embeddingRequest = requestTemplates.get(connectionId);
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
            VectorEmbeddingRequest embeddingRequest = new VectorEmbeddingRequest(connectionId);
            requestTemplates.put(connectionId, embeddingRequest);
            embeddingRequest.readState(requestElement);
        }
    }
}
