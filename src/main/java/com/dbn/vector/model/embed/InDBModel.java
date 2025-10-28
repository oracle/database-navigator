package com.dbn.vector.model.embed;

import com.dbn.common.util.Json;
import lombok.Getter;

import java.util.Map;

@Getter
public class InDBModel extends EmbedConfig{
  private final String modelName;

  public InDBModel(String modelName) {
    this.modelName = modelName;
  }

  @Override
  public String getConfigJson() {
    Map params = Map.of(
            "provider","database",
            "model",modelName);

    return Json.writeAsString(params);
  }
}
