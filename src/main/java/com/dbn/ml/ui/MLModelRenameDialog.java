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

package com.dbn.ml.ui;

import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

/**
 * Dialog for renaming an ML model in the database.
 *
 * @author ayoub allali
 */
public class MLModelRenameDialog extends DBNDialog<MLModelRenameForm> {
    private final String currentModelName;
    private final Consumer<String> modelNameConsumer;

    public MLModelRenameDialog(Project project, String currentModelName, Consumer<String> modelNameConsumer) {
        super(project, txt("msg.machineLearning.title.RenameModel"), true);
        this.currentModelName = currentModelName;
        this.modelNameConsumer = modelNameConsumer;
        setModal(true);
        setAutoSize(true);
        setResizable(false);
        init();
    }

    @NotNull
    @Override
    protected MLModelRenameForm createForm() {
        return new MLModelRenameForm(this, currentModelName);
    }

    @Override
    @NotNull
    protected final Action[] initializeActions() {
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        String modelName = getForm().getModelName();
        super.doOKAction();

        if (modelName.equals(currentModelName)) return;
        modelNameConsumer.accept(modelName);
    }
}
