package com.dbn.vector.model.store;

import com.dbn.vector.ui.source.ui.SourceDataForm;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class StoreConfig {
  SourceDataForm.SourceType sourceType;
  private String id =  UUID.randomUUID().toString();
  private String tableName;
  private String embeddingColumn = "embedding";
  private String textColumn = "text";
  private String metadataColumn = "metadata";
  private String metadata;


}
