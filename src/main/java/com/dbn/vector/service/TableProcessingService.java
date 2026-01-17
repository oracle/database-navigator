package com.dbn.vector.service;

import com.dbn.common.util.Json;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.request.EmbeddingTableSource;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.dbn.vector.DatabaseVectorManager.ENGINE_VERSION;
import static com.dbn.vector.model.request.EmbeddingSourceType.DATABASE_TABLE;

public class TableProcessingService {

  public String buildRowMetadata(@NotNull VectorEmbeddingRequest request, EmbeddingTableSource sourceTable) {
    @NonNls
    Map<String, Object> sourceMetadata = new LinkedHashMap<>();
    sourceMetadata.put("source_type", DATABASE_TABLE);
    sourceMetadata.put("owner_name", sourceTable.getSchemaName());
    sourceMetadata.put("table_name", sourceTable.getTableName());
    sourceMetadata.put("key_column_name", sourceTable.getKeyColumnName());
    sourceMetadata.put("data_column_name", sourceTable.getDataColumnName());

    @NonNls
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("engine_version", ENGINE_VERSION);
    metadata.put("embedding_source", sourceMetadata);
    metadata.put("embedding_config", request.getModelConfig().getConfigMap());
    metadata.put("chunking_config", request.getChunkConfig().getConfigMap());
    return Json.writeAsString(metadata);
  }

}
