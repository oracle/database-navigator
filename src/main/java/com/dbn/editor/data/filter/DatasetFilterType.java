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

package com.dbn.editor.data.filter;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.Presentable;
import lombok.Getter;
import org.jetbrains.annotations.Nls;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum DatasetFilterType implements Presentable{
    NONE(txt("cfg.dataEditor.const.DatasetFilterType_NONE"), Icons.DATASET_FILTER_EMPTY, Icons.DATASET_FILTER_EMPTY),
    BASIC(txt("cfg.dataEditor.const.DatasetFilterType_BASIC"), Icons.DATASET_FILTER_BASIC, Icons.DATASET_FILTER_BASIC_ERR),
    CUSTOM(txt("cfg.dataEditor.const.DatasetFilterType_CUSTOM"), Icons.DATASET_FILTER_CUSTOM, Icons.DATASET_FILTER_CUSTOM_ERR),
    GLOBAL(txt("cfg.dataEditor.const.DatasetFilterType_GLOBAL"), Icons.DATASET_FILTER_GLOBAL, Icons.DATASET_FILTER_GLOBAL_ERR);

    private final String name;
    private final Icon icon;
    private final Icon errIcon;

    DatasetFilterType(@Nls String name, Icon icon, Icon errIcon) {
        this.name = name;
        this.icon = icon;
        this.errIcon = errIcon;
    }
}
