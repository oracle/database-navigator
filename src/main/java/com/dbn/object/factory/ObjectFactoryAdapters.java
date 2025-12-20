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

package com.dbn.object.factory;

import com.dbn.common.extension.ExtensionPointCache;
import com.dbn.object.type.DBObjectType;

import static com.dbn.common.util.Unsafe.cast;

public class ObjectFactoryAdapters extends ExtensionPointCache<DBObjectType, ObjectFactoryAdapter> {
    private static final ObjectFactoryAdapters INSTANCE = new ObjectFactoryAdapters();

    private ObjectFactoryAdapters() {
        super(ObjectFactoryAdapter.EP, a -> a.getObjectType());
    }

    public static <A extends ObjectFactoryAdapter> A get(DBObjectType assistantType) {
        return cast(INSTANCE.find(assistantType));
    }
}
