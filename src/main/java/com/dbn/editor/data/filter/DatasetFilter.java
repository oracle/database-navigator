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

import com.dbn.common.options.PersistentConfiguration;
import com.dbn.connection.ConnectionId;
import com.dbn.data.sorting.SortingState;
import com.dbn.object.DBDataset;
import com.intellij.openapi.options.UnnamedConfigurable;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public interface DatasetFilter extends UnnamedConfigurable, PersistentConfiguration {
    Icon getIcon();
    @NotNull
    String getId();
    String getName();
    String getVolatileName();
    ConnectionId getConnectionId();
    String getDatasetName();
    boolean isPersisted();
    boolean isTemporary();
    boolean isIgnored();
    DatasetFilterType getFilterType();

    String getError();
    void setError(String error);

    DatasetFilterGroup getFilterGroup() ;

    String createSelectStatement(DBDataset dataset, SortingState sortingState);
}
