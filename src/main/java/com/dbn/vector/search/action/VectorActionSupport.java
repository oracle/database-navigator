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

package com.dbn.vector.search.action;

import com.dbn.vector.search.VectorSearchConsole;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.action.DataKeys.VECTOR_SEARCH_CONSOLE;

public interface VectorActionSupport {
    default @Nullable VectorSearchConsole getConsole(AnActionEvent e) {
        return e.getData(VECTOR_SEARCH_CONSOLE);
    }

    default @Nullable VectorSearchConsole getConsole(DataContext dataContext) {
        return dataContext.getData(VECTOR_SEARCH_CONSOLE);
    }
}
