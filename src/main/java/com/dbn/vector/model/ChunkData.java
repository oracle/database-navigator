package com.dbn.vector.model;

public class ChunkData{
  long chunkId;
  long chunk_offset;
  long chunk_length;
  String chunk_data;

  public ChunkData( long chunk_offset, long chunk_length, String chunk_data) {
//    this.chunkId = chunkId;
    this.chunk_offset = chunk_offset;
    this.chunk_length = chunk_length;
    this.chunk_data = chunk_data;
  }

  public long getChunkId() {
    return chunkId;
  }

  public long getChunk_offset() {
    return chunk_offset;
  }

  public long getChunk_length() {
    return chunk_length;
  }

  public String getChunk_data() {
    return chunk_data;
  }
}
