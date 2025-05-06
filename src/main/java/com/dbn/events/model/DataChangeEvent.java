package com.dbn.events.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DataChangeEvent {
  private  String operation;
  private  String tableName;
  private  String rowId;
  private  String timestamp;
  private Long regID;
  private String connectionId;


  public DataChangeEvent(String operation, String tableName, String rowId, String timestamp, Long regId, String connectionId) {
    this.operation = operation;
    this.tableName = tableName;
    this.rowId = rowId;
    this.timestamp = timestamp;
    this.regID = regId;
    this.connectionId = connectionId;
  }

}
