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

package com.dbn.common.content.dependency;

import com.intellij.openapi.Disposable;

public interface ContentDependencyAdapter extends Disposable {

    default boolean isDependencyDirty() {
        return false;
    }

    /**
     * This method is typically called when the dynamic content is dirty and
     * the system tries to reload it.
     * e.g. one basic condition for reloading dirty content is valid connectivity
     */
    default boolean canLoad() {
        return true;
    }

    default boolean canLoadInBackground() {
        return true;
    }


    void refreshSources();

    /**
     * This operation is triggered before loading the dynamic content is started.
     * It can be implemented by the adapters to load non-weak dependencies for example.
     * @param force load flavor
     */
    default void beforeLoad(boolean force) {};

    /**
     * This operation is triggered after the loading of the dynamic content.
     */
    default void afterLoad() {};


    boolean canLoadFast();

}
