package com.dbn.vector.model;

import lombok.Getter;

@Getter
public class ChunkData{
  private long id;
  private final long offset;
  private final long length;
  private final String data;

  public ChunkData(long offset, long length, String data) {
//    this.chunkId = chunkId;
    this.offset = offset;
    this.length = length;
    this.data = data;
  }
}
