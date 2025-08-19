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

package com.dbn.execution.java.wrapper.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.execution.java.wrapper.WrapperModel;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class WrapperNamesEditorDialog extends DBNDialog<WrapperNamesEditorForm> {
    private final WrapperModel model;

    public WrapperNamesEditorDialog(Project project, WrapperModel model) {
        super(project, "Wrapper Names Editor", false);
        this.setModal(true);
        this.setAutoSize(true);
        this.model = model;
        init();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return createActions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected @NotNull WrapperNamesEditorForm createForm() {
        return new WrapperNamesEditorForm(this, model);
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}
