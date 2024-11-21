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

package com.dbn.data.editor.ui;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
public abstract class ListPopupValuesProviderBase implements ListPopupValuesProvider{
    private final String description;
    private boolean loaded = true;

    public ListPopupValuesProviderBase(String description) {
        this.description = description;
    }

    public ListPopupValuesProviderBase(String description, boolean loaded) {
        this.description = description;
        this.loaded = loaded;
    }

    @Override
    public List<String> getSecondaryValues() {
        return Collections.emptyList();
    }
}
