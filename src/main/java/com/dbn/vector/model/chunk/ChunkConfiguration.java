package com.dbn.vector.model.chunk;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChunkConfiguration {
  private String by ;
  private int max;
  private String splitBy;
  private int overlap;

  public ChunkConfiguration() {}
  public ChunkConfiguration(String by, int max, String splitBy, int overlap) {
    this.by = by;
    this.max = max;
    this.splitBy = splitBy;
    this.overlap = overlap;
  }

  public String getConfigJson(){
    String chunkCfg = "{"
            + "\"by\":\"" + by + "\""
            + ",\"max\":" + max
            + ",\"overlap\":" + overlap
            + ",\"split\":\"" + splitBy + "\""
            + "}";
    return chunkCfg;
  }
}
