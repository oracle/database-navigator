package com.dbn.vector.model.store;

import com.dbn.vector.model.VectorEmbeddingConfig;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Setter
@Getter
public class StoreConfig extends VectorEmbeddingConfig {
  private DestinationType destinationType = DestinationType.EXISTING_TABLE;

  private String schemaName;
  private String tableName;
  private String keyColumnName = "ID";
  private String textColumnName = "TEXT";
  private String embeddingColumnName = "EMBEDDING";
  private String metadataColumnName = "METADATA";
  private transient String metadata;

  @Override
  public void readState(Element element) {
    if (element == null) return;

    super.readState(element);
    destinationType = enumAttribute(element, "destination-type", destinationType);

    schemaName = stringAttribute(element, "schema");
    tableName = stringAttribute(element, "table");
    keyColumnName = stringAttribute(element, "key-column", keyColumnName);
    textColumnName = stringAttribute(element, "text-column", textColumnName);
    embeddingColumnName = stringAttribute(element, "embedding-column", embeddingColumnName);
    metadataColumnName = stringAttribute(element, "metadata-column", metadataColumnName);
  }

  @Override
  public void writeState(Element element) {
    super.writeState(element);
    setEnumAttribute(element, "destination-type", destinationType);
    setStringAttribute(element, "schema", schemaName);
    setStringAttribute(element, "table", tableName);
    setStringAttribute(element, "key-column", keyColumnName);
    setStringAttribute(element, "text-column", textColumnName);
    setStringAttribute(element, "embedding-column", embeddingColumnName);
    setStringAttribute(element, "metadata-column", metadataColumnName);
  }
}
