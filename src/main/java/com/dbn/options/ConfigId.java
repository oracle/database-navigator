package com.dbn.options;

import lombok.Getter;

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

    ConfigId(String name) {
        this.name = name;
    }
}
