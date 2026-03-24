package com.dbn.vector;

import com.dbn.DatabaseNavigator;
import com.dbn.common.collections.LeastRecentlyUsedSet;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.execution.ExecutionManager;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.cache.DBObjectFilterType;
import com.dbn.object.cache.DBObjectNameCache;
import com.dbn.object.common.ui.DBObjectSelectionDialog;
import com.dbn.object.common.ui.DBObjectSelectionInput;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.object.type.DBVectorDistanceMetric;
import com.dbn.vector.model.VectorEmbeddingContext;
import com.dbn.vector.model.VectorEmbeddingExecutionResult;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingChunkingConfig;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.model.request.EmbeddingSourceType;
import com.dbn.vector.pipeline.EmbeddingPipeline;
import com.dbn.vector.pipeline.FileEmbeddingPipeline;
import com.dbn.vector.pipeline.QueryEmbeddingPipeline;
import com.dbn.vector.pipeline.TableEmbeddingPipeline;
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
import static com.dbn.object.type.DBObjectType.TABLE;
import static java.util.Collections.emptyList;


@State(
        name = DatabaseVectorManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseVectorManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseVectorManager";
    public static final String ENGINE_VERSION = "1.1.0";

    private final Map<ConnectionId, VectorEmbeddingRequest> requestTemplates = new ConcurrentHashMap<>();
    private final Map<ConnectionId, Map<DBObjectFilterType, DBObjectNameCache<DBTable>>> objectNameCaches = new ConcurrentHashMap<>();
    private final Map<ConnectionId, Set<DBObjectRef<DBTable>>> recentEmbeddingTables = new ConcurrentHashMap<>();

    public DatabaseVectorManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC, connectionConfigListener());
    }

    @NotNull
    private ConnectionConfigListener connectionConfigListener() {
        return new ConnectionConfigListener() {
            @Override
            public void connectionRemoved(ConnectionId connectionId) {
                // remove embedding requests and table caches when connection configs are deleted
                requestTemplates.remove(connectionId);
                objectNameCaches.remove(connectionId);
            }
        };
    }

    public DBObjectNameCache<DBTable> getObjectNamesCache(ConnectionId connectionId, DBObjectFilterType filterType) {
        var objectCaches = ensureObjectCaches(connectionId);
        return objectCaches.computeIfAbsent(filterType, t -> new DBObjectNameCache<>(connectionId, TABLE, t));
    }

    private Map<DBObjectFilterType, DBObjectNameCache<DBTable>> ensureObjectCaches(ConnectionId connectionId) {
        return objectNameCaches.computeIfAbsent(connectionId, c -> new ConcurrentHashMap<>());
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
        embeddingRequest.initialize(connection.getUserSchemaId());
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

    public ResultSet chunkTextContent(ConnectionHandler connection, EmbeddingChunkingConfig config, String text) throws SQLException {
        return DatabaseInterfaceInvoker.load(MEDIUM,
                "Chunking Data",
                "Chunking text content",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseVectorInterface vectorInterface = connection.getVectorInterface();
                    return vectorInterface.chunkTextContent(conn, text,
                            config.getChunkBy(),
                            config.getSplitBy(),
                            config.getMaxSize(),
                            config.getOverlap());
                });
    }

    public ResultSet performSimilaritySearch(ConnectionHandler connection, DBTable vectorTable, String queryText, DBVectorDistanceMetric distanceMetric, int rows) throws SQLException {
        String schemaName = vectorTable.getSchemaName(true);
        String tableName = vectorTable.getName(true);
        return performSimilaritySearch(connection, schemaName, tableName, queryText, distanceMetric, rows);
    }


    public ResultSet performSimilaritySearch(ConnectionHandler connection, String schemaName, String tableName, String queryText, DBVectorDistanceMetric metric, int rows) throws SQLException {
        return DatabaseInterfaceInvoker.load(MEDIUM,
                "Perform Similarity Search",
                "Perform similarity search on vector table " + schemaName + "." + tableName,
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseVectorInterface vectorInterface = connection.getVectorInterface();
                    return vectorInterface.performSimilaritySearch(conn,
                            schemaName,
                            tableName,
                            queryText,
                            metric.name(),
                            rows);
                });
    }

    @SneakyThrows
    public void createEmbeddings(VectorEmbeddingRequest request) {
        request.setTemplate(false); // no longer a template after used for embedding
        ConnectionHandler connection = request.getConnection();

        EmbeddingDestinationConfig destinationConfig = request.getDestinationConfig();
        Progress.prompt(
                getProject(),
                connection.getSchema(), true,
                "Embedding Data",
                "Embedding data into \"" + destinationConfig.getSchemaName() + "\".\"" + destinationConfig.getTableName() + "\"",
                p -> DatabaseInterfaceInvoker.execute(MEDIUM,
                        p.getText(),
                        p.getText2(),
                        connection.getProject(),
                        connection.getConnectionId(),
                        connection.getSchemaId(),
                        conn -> {
                            VectorEmbeddingResult result = executePipeline(request, conn, p);
                            result.finish();
                            showResultDialog(result);
//                                  callbackInfo.run();

                        }));
    }

    /**
     * Execute the embedding pipeline for the given request.
     */
    private VectorEmbeddingResult executePipeline(
            @NotNull VectorEmbeddingRequest request,
            @NotNull DBNConnection connection,
            @NotNull ProgressIndicator progressIndicator) {

        VectorEmbeddingResult result = new VectorEmbeddingResult(request);
        VectorEmbeddingContext context = new VectorEmbeddingContext(progressIndicator, connection);

        EmbeddingPipeline pipeline = createPipeline(request.getSourceConfig().getSourceType());
        pipeline.execute(context, request, result);

        return result;
    }

    /**
     * Factory method to create the appropriate pipeline based on source type.
     */
    private EmbeddingPipeline createPipeline(@NotNull EmbeddingSourceType sourceType) {
        return switch (sourceType) {
            case DATABASE_TABLE -> new TableEmbeddingPipeline();
            case DATABASE_QUERY -> new QueryEmbeddingPipeline();
            case FILE_SYSTEM -> new FileEmbeddingPipeline();
        };
    }


    private void showResultDialog(VectorEmbeddingResult result) {
        ExecutionManager executionManager = ExecutionManager.getInstance(getProject());
        Set<String> names = executionManager.getExecutionResultNames(VectorEmbeddingExecutionResult.class);
        String name = Naming.nextNumberedIdentifier("Embedding Result", true, () -> names);
        VectorEmbeddingExecutionResult executionResult = new VectorEmbeddingExecutionResult(result, name);
        executionManager.addExecutionResult(executionResult);
    }

    public Set<DBObjectRef<DBTable>> getRecentEmbeddingTables(ConnectionId connectionId) {
        return recentEmbeddingTables.computeIfAbsent(connectionId, i -> new LeastRecentlyUsedSet<>(5));
    }

    public void selectEmbeddingsTable(ConnectionId connectionId, Consumer<DBTable> consumer) {
        DBObjectSelectionInput<DBTable> input = initEmbeddingsTableSelector(connectionId);
        Dialogs.show(() -> new DBObjectSelectionDialog<>(input, consumer));
    }

    public DBObjectSelectionInput<DBTable> initEmbeddingsTableSelector(ConnectionId connectionId) {
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
        DBObjectNameCache<DBTable> names = getObjectNamesCache(connectionId, DBObjectFilterType.EMBEDDING_DESTINATION_TABLES);

        return new DBObjectSelectionInput<DBTable>(connection, DBObjectType.TABLE)
                .withSchemaFilter(s -> !s.isSystemSchema())
                .withSchemaPreselector(s -> s.getSchemaId() == connection.getUserSchemaId())
                .withObjectFilter(names);
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

        Element cachesElement = newElement(element, "object-name-caches");
        for (ConnectionId connectionId : objectNameCaches.keySet()) {
            for (DBObjectNameCache<DBTable> cache : objectNameCaches.get(connectionId).values()) {
                Element cacheElement = newElement(cachesElement, "object-name-cache");
                cache.writeState(cacheElement);
            }
        }

        Element embeddingTablesElement = newElement(element, "recent-embedding-tables");
        for (ConnectionId connectionId : recentEmbeddingTables.keySet()) {
            for (DBObjectRef<DBTable> table : recentEmbeddingTables.get(connectionId)) {
                Element cacheElement = newElement(embeddingTablesElement, "embedding-table");
                table.writeState(cacheElement);
            }
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

        Element cachesElement = element.getChild("object-name-caches");
        List<Element> cacheElements = childrenOf(cachesElement, "object-name-cache");
        for (Element cacheElement : cacheElements) {
            DBObjectNameCache<DBTable> cache = new DBObjectNameCache<>();
            cache.readState(cacheElement);

            ConnectionId connectionId = cache.getConnectionId();
            Map<DBObjectFilterType, DBObjectNameCache<DBTable>> caches = ensureObjectCaches(connectionId);
            caches.put(cache.getFilterType(), cache);
        }

        Element embeddingTablesElement = element.getChild("recent-embedding-tables");
        List<Element> embeddingTableElements = childrenOf(embeddingTablesElement, "embedding-table");
        for (Element embeddingTableElement : embeddingTableElements) {
            DBObjectRef<DBTable> tableRef = new DBObjectRef<>();
            tableRef.readState(embeddingTableElement);

            ConnectionId connectionId = tableRef.getConnectionId();
            getRecentEmbeddingTables(connectionId).add(tableRef);
        }

    }

    public List<DBTable> getVectorTables(ConnectionId connectionId, SchemaId schemaId) {
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection == null) return emptyList();

        DBSchema schema = connection.getSchema(schemaId);
        if (schema == null) return emptyList();

        DBObjectNameCache<DBTable> names = getObjectNamesCache(connectionId, DBObjectFilterType.EMBEDDING_DESTINATION_TABLES);
        return names.filter(schema.getTables());
    }
}
