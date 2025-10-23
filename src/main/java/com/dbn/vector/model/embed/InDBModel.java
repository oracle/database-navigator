package com.dbn.vector.model.embed;

import lombok.Getter;

@Getter
public class InDBModel extends EmbedConfig{
  private String modelName;
  public InDBModel(String modelName) {
    this.modelName = modelName;
  }

  @Override
  public String getConfigJson() {
    String embedCfg = "{"
            + "\"provider\":\"database\""
            + ",\"model\":\""+modelName+"\""
            + "}";
    return embedCfg ;
  }
}
