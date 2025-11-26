package com.dbn.vector.common;

import com.dbn.common.ui.Presentable;
import lombok.Getter;

@Getter
public enum ModelSourceType implements Presentable {
  MODEL_FILE("Model File"),
  OBJECT_STORAGE("Object Storage");

  private final String name;

  ModelSourceType(String name) {
    this.name = name;
  }
}
