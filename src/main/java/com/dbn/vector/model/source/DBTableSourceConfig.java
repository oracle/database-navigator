package com.dbn.vector.model.source;

import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
public class DBTableSourceConfig implements PersistentStateElement {
  private String schemaName;
  private String tableName;
  private String keyColumnName;
  private String dataColumnName;
  private boolean autoSync;

  @Override
  public void readState(Element element) {
    if (element == null) return;

    schemaName = stringAttribute(element, "schema");
    tableName = stringAttribute(element, "table");
    keyColumnName = stringAttribute(element, "key-column");
    dataColumnName = stringAttribute(element, "data-column");
    autoSync = booleanAttribute(element, "auto-sync", autoSync);
  }

  @Override
  public void writeState(Element element) {
    setStringAttribute(element, "schema", schemaName);
    setStringAttribute(element, "table", tableName);
    setStringAttribute(element, "key-column", keyColumnName);
    setStringAttribute(element, "data-column", dataColumnName);
    setBooleanAttribute(element, "auto-sync", autoSync);
  }
}
