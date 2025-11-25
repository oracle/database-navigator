package com.dbn.vector.service;

import com.dbn.common.util.Json;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.dbn.vector.model.sourceconfig.SourceType.DATABASE_TABLE;

public class TableProcessingService {

  public String buildRowMetadata(@NotNull VectorEmbeddingRequest request, DBTableSourceConfig config) {
    @NonNls
    Map<String, Object> sourceMetadata = new LinkedHashMap<>();
    sourceMetadata.put("source_type", DATABASE_TABLE);
    sourceMetadata.put("owner_name", config.getSchemaName());
    sourceMetadata.put("table_name", config.getTableName());
    sourceMetadata.put("column_name", config.getDataColumnName());

    @NonNls
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("embedding_source", sourceMetadata);
    metadata.put("embedding_config", request.getEmbedConfig().getConfigMap());
    metadata.put("chunking_config", request.getChunkConfig().getConfigMap());
    return Json.writeAsString(metadata);
  }

}
