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

package com.dbn.object.properties;

import com.dbn.object.common.DBObject;
import com.dbn.object.common.extension.DBObjectExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;

import java.util.List;

/**
 * Extension point for providing additional presentable properties of given {@link DBObject} implementations
 * The extension is used in components rendering detailed object information (e.g. browser object info view)
 * @param <T> the type of the object being extended
 */
public interface DBObjectPropertiesProvider<T extends DBObject> extends DBObjectExtensionPoint {
    ExtensionPointName<DBObjectPropertiesProvider> EP = ExtensionPointName.create("com.dbn.objectPropertiesProvider");

    /**
     * Returns a list of presentable properties for the given object
     * @param object the object to prepare properties for
     * @return a list of {@link DBObjectProperty} elements
     */
    List<DBObjectProperty> getProperties(T object);
}
