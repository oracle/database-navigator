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

package com.dbn.object.navigation;

import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Extension point for adding navigation features to given {@link DBObject} implementations
 * @param <T> the type of the object being extended
 */
public interface DBObjectNavigationInfoProvider<T extends DBObject> {
    ExtensionPointName<DBObjectNavigationInfoProvider> EP = ExtensionPointName.create("com.dbn.objectNavigationInfoProvider");

    DBObjectType getObjectType();

    /**
     * Returns the tooltip presented when a navigable instance of the object is accessed (e.g. in the editor)
     * @param object the object to prepare tooltip for
     * @return the tooltip representing the object and adjacent information (e.g. column name / data type)
     */
    String getNavigationTooltipText(T object);

    /**
     * Returns the default navigation target for a given object (e.g. when double-clicked in db-browser)
     * @param object the object to prepare navigation target for
     * @return the navigation target object (or null)
     */
    @Nullable
    DBObject getDefaultNavigationTarget(T object);

    /**
     * Returns a list of alternative navigation targets to be presented in the
     * context menu of the given object (e.g. granted privileges of a given role)
     * @param object the object to prepare navigation targets for
     * @return alist of {@link DBObjectNavigationList} elements representing the possible navigation targets (or null)
     */
    @Nullable
    List<DBObjectNavigationList<?>> createNavigationTargets(T object);
}
