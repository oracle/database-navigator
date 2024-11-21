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

package com.dbn.data.model;

import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.dispose.UnlistedDisposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DataModelRow<
        M extends DataModel<? extends DataModelRow<M, C>, C>,
        C extends DataModelCell<? extends DataModelRow<M, C>, M>>
        extends StatefulDisposable, UnlistedDisposable {

    List<C> getCells();

    @Nullable
    C getCell(String columnName);

    @Nullable
    Object getCellValue(String columnName);

    @Nullable
    C getCellAtIndex(int index);

    int getIndex();

    void setIndex(int index);

    @NotNull
    M getModel();
}
