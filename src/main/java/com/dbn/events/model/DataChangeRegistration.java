package com.dbn.events.model;

import lombok.Data;
import lombok.Getter;

@Getter
@Data
public class DataChangeRegistration {
  private final Long    regId;
  private final int    regFlags;
  private final String callback;
  private final int    operationsFilter;
  private final int    changeLag;
  private final long   timeout;
  private final String tableName;
  private boolean active;
  //todo maybe add cloumn for the option this registration is on.

  public DataChangeRegistration(Long regId, int regFlags, String callback,
                                int operationsFilter, int changeLag,
                                long timeout, String tableName) {
    this.regId = regId;
    this.regFlags = regFlags;
    this.callback = callback;
    this.operationsFilter = operationsFilter;
    this.changeLag = changeLag;
    this.timeout = timeout;
    this.tableName = tableName;
  }



}