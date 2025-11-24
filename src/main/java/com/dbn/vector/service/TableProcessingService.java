package com.dbn.vector.service;

import com.dbn.common.util.Json;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.common.FileContent;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.dbn.vector.model.sourceconfig.SourceType.DATABASE_TABLE;

public class TableProcessingService {

  public String buildRowMetadata(@NotNull VectorEmbeddingRequest request, DBTableSourceConfig config) throws IOException, SQLException {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("source_type",DATABASE_TABLE);
    metadata.put("source_schema",config.getSchemaName());
    metadata.put("source_table",config.getTableName());
    metadata.put("source_column",config.getDataColumnName());

    metadata.put("embed_config", request.getEmbedConfig().getConfigMap());
    metadata.put("chunk_config", request.getChunkConfig().getConfigMap());
    return Json.writeAsString(metadata);
  }

}
