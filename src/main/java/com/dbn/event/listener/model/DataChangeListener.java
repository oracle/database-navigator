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

package com.dbn.event.listener.model;

import lombok.Data;
import lombok.Getter;

@Getter
@Data
public class DataChangeListener {
  private final String userName;
  private final Long    regId;
  private final int    regFlags;
  private final String callback;
  private final int    operationsFilter;
  private final int    changeLag;
  private final long   timeout;
  private final String tableName;
  private boolean active;
  //todo maybe add cloumn for the option this registration is on.

  public DataChangeListener(String userName, Long regId, int regFlags, String callback,
                            int operationsFilter, int changeLag,
                            long timeout, String tableName) {
    this.userName = userName;
    this.regId = regId;
    this.regFlags = regFlags;
    this.callback = callback;
    this.operationsFilter = operationsFilter;
    this.changeLag = changeLag;
    this.timeout = timeout;
    this.tableName = tableName;
  }



}