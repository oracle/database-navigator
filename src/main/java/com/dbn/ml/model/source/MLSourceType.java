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

package com.dbn.ml.model.source;

import com.dbn.common.ui.Presentable;
import lombok.Getter;
import org.jetbrains.annotations.Nls;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum MLSourceType implements Presentable {
    DATABASE_TABLE(txt("cfg.machineLearning.const.MLSourceType_DATABASE_TABLE")),
    FILE_SYSTEM(txt("cfg.machineLearning.const.MLSourceType_FILE_SYSTEM")),
    OBJECT_STORAGE(txt("cfg.machineLearning.const.MLSourceType_OBJECT_STORAGE"));

    private final @Nls String name;

    MLSourceType(@Nls String name) {
        this.name = name;
    }
}
