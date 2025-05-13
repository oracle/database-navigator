/*
 * Copyright 2025 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.events.notification.model;

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
