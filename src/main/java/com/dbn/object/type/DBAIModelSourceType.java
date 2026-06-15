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

package com.dbn.object.type;

import com.dbn.common.ui.Presentable;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum DBAIModelSourceType implements Presentable {
  MODEL_FILE(txt("app.objects.const.DBAIModelSourceType_MODEL_FILE")),
  OBJECT_STORAGE(txt("app.objects.const.DBAIModelSourceType_OBJECT_STORAGE"));

  private final String name;

  DBAIModelSourceType(String name) {
    this.name = name;
  }
}
