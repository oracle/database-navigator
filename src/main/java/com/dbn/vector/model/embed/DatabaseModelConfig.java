package com.dbn.vector.model.embed;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Json;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
public class DatabaseModelConfig implements PersistentStateElement {
  private String schemaName;
  private String modelName;

  public String getConfigJson() {
    Map params = getConfigMap();
    return Json.writeAsString(params);
  }

  @NonNull
  public Map<String, ?> getConfigMap() {
      return Map.of(
              "provider","database",
              "model",schemaName + "." + modelName);
  }

  @Override
  public void readState(Element element) {
    if (element == null) return;

    schemaName = stringAttribute(element, "schema");
    modelName = stringAttribute(element, "model");
  }

  @Override
  public void writeState(Element element) {
    setStringAttribute(element, "schema", schemaName);
    setStringAttribute(element, "model", modelName);
  }
}
