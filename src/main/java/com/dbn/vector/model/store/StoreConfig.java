package com.dbn.vector.model.store;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class StoreConfig {
  private String id =  UUID.randomUUID().toString();
  private String schemaName;
  private String tableName;
  private String keyColumnName = "id";
  private String textColumnName = "text";
  private String embeddingColumnName = "embedding";
  private String metadataColumnName = "metadata";
  private String metadata;
  private boolean newTable;


}
