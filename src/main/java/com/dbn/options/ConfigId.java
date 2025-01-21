/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.options;

import lombok.Getter;
import org.jetbrains.annotations.NonNls;

@Getter
public enum ConfigId {
  BROWSER("Database Browser"),
  NAVIGATION("Navigation"),
  DATA_GRID("Data Grid"),
  DATA_EDITOR("Data Editor"),
  CODE_EDITOR("Code Editor"),
  CODE_COMPLETION("Code Completion"),
  CODE_STYLE("Code Style"),
  EXECUTION_ENGINE("Execution Engine"),
  DDL_FILES("DDL Files"),
  CONNECTIONS("Connections"),
  OPERATIONS("Operations"),
  ASSISTANT("Assistant"),
  GENERAL("General");

    private final String name;

    ConfigId(@NonNls String name) {
        this.name = name;
    }
}
