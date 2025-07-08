package com.dbn.vector.ui;

import lombok.Getter;

@Getter
public class ChunkConfiguration {
  private String by ;
  private int max;
  private String splitBy;
  private int overlap;

  public ChunkConfiguration(String by, int max, String splitBy, int overlap) {
    this.by = by;
    this.max = max;
    this.splitBy = splitBy;
    this.overlap = overlap;
  }
}
