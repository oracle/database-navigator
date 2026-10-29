/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.editor.code.source;

import com.dbn.object.common.DBObject;
import com.dbn.object.common.extension.DBObjectExtensionPointCache;
import com.dbn.object.type.DBObjectType;

public class DBObjectSourceCodeAdapters extends DBObjectExtensionPointCache<DBObjectSourceCodeAdapter> {
    private static final DBObjectSourceCodeAdapters INSTANCE = new DBObjectSourceCodeAdapters();

    private DBObjectSourceCodeAdapters() {
        super(DBObjectSourceCodeAdapter.EP);
    }

    public static <T extends DBObject> DBObjectSourceCodeAdapter<T> get(DBObjectType objectType) {
        return INSTANCE.find(objectType);
    }
}
