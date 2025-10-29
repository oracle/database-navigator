package com.dbn.vector.model.chunk;

import com.dbn.common.util.Json;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;

import java.util.Map;

@Getter
@Setter
@NonNls
public class ChunkConfig {
  private String chunkBy;
  private String splitBy;
  private int max;
  private int overlap;

  public ChunkConfig() {}

  public ChunkConfig(String chunkBy, int max, String splitBy, int overlap) {
    this.chunkBy = chunkBy;
    this.max = max;
    this.splitBy = splitBy;
    this.overlap = overlap;
  }

  public String getConfigJson(){
    return Json.writeAsString(Map.of(
            "chunkBy", chunkBy,
            "splitBy", splitBy,
            "max", max,
            "overlap", overlap));
  }
}
