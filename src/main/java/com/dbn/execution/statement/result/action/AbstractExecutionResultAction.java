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

package com.dbn.execution.statement.result.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ContextAction;
import com.dbn.common.action.DataKeys;
import com.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
public abstract class AbstractExecutionResultAction extends ContextAction<StatementExecutionCursorResult> {

    protected AbstractExecutionResultAction(String text) {
        super(text);
    }

    protected StatementExecutionCursorResult getContext(@NotNull AnActionEvent e) {
        return e.getData(DataKeys.STATEMENT_EXECUTION_CURSOR_RESULT);
    }
}
