package com.dbn.vector.model.embed;

import com.dbn.common.util.Json;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.Map;

@Getter
public class InDBModel extends EmbedConfig{
  private String modelName;
  public InDBModel(String modelName) {
    this.modelName = modelName;
  }
  @SneakyThrows
  @Override
  public String getConfigJson() {
    Map params = Map.of("provider","database",
            "model",modelName);

    return Json.OBJECT_MAPPER.writeValueAsString(params);
  }
}
