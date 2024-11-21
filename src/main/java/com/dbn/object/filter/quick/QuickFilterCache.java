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

package com.dbn.object.filter.quick;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class QuickFilterCache {
    private final Map<String, ?> cache = new HashMap<>();

    public <T extends DBObject> void add(DBObjectList<T> list, @Nullable ObjectQuickFilter<T> filter) {

    }

    private Map<String, ?> find(BrowserTreeNode node) {
        return null;
    }

}
