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

package com.dbn.assistant.chat.message.ui;

import com.dbn.assistant.tool.execution.AssistantToolInvocation;
import com.dbn.assistant.tool.info.AssistantToolInfoProvider;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Point;

public class AssistantToolDataDialog extends DBNDialog<AssistantToolDataForm> {
    private final Point location;
    private final AssistantToolInfoProvider info;
    private final AssistantToolInvocation invocation;

    protected AssistantToolDataDialog(@Nullable Project project, AssistantToolInfoProvider info, AssistantToolInvocation invocation, Point location) {
        super(project, "Tool Request Information", false);
        this.info = info;
        this.invocation = invocation;
        this.location = location;
        renameAction(getCancelAction(), "Close");
        setDefaultSize(600, 400);

        init();
    }

    @Override
    public @Nullable Point getInitialLocation() {
        return location;
    }

    @Override
    protected @NotNull AssistantToolDataForm createForm() {
        return new AssistantToolDataForm(this, info, invocation);
    }

    protected final Action[] createActions() {
        return createActions(getCancelAction());
    }

}
