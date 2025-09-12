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

import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Point;

import static com.dbn.common.util.Commons.nvl;

public class AssistantToolDataDialog extends DBNDialog<AssistantToolDataForm> {
    private final String request;
    private final String response;
    private final Point location;

    protected AssistantToolDataDialog(@Nullable Project project, String title, String request, String response, Point location) {
        super(project, title, false);
        this.request = nvl(request, "");
        this.response = nvl(response, "");
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
        return new AssistantToolDataForm(this, getProject(), request, response);
    }

    protected final Action[] createActions() {
        return createActions(getCancelAction());
    }

}
