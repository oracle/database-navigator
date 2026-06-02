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

package com.dbn.common.presentation;

import com.dbn.common.extension.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;

import javax.swing.Icon;

public interface PresentationProvider<T> extends ExtensionPoint {
    ExtensionPointName<PresentationProvider> EP = ExtensionPointName.create("com.dbn.presentationProvider");

    Class<T> getObjectType();

    default boolean supports(Class<?> objectType) {
        return getObjectType().isAssignableFrom(objectType);
    }

    String getName(T object);

    String getTypeName(T object);

    default String getDetailedName(T object) {
        return getName(object);
    }

    default Icon getIcon(T object) {
        return null;
    }

    default String getDescription(T object) {
        return null;
    }
}
