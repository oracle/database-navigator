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

package com.dbn.diagnostics.action;

import com.dbn.common.action.ContextAction;
import com.dbn.common.action.DataKeys;
import com.dbn.diagnostics.ParserDiagnosticsManager;
import com.dbn.diagnostics.ui.ParserDiagnosticsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractParserDiagnosticsAction extends ContextAction<ParserDiagnosticsForm> {

    @Override
    protected final ParserDiagnosticsForm getContext(@NotNull AnActionEvent e) {
        return e.getData(DataKeys.PARSER_DIAGNOSTICS_FORM);
    }


    @NotNull
    protected ParserDiagnosticsManager getManager(@NotNull Project project) {
        return ParserDiagnosticsManager.get(project);
    }
}
