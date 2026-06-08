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

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.ml.DatabaseMLManager;
import com.dbn.ml.model.MLRequest;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class MLToolboxDialog extends DBNDialog<MLToolboxForm> {
    private final ConnectionRef connection;
    private final MLRequest request;

    public MLToolboxDialog(ConnectionHandler connection, MLRequest request) {
        super(connection.getProject(), txt("app.machineLearning.title.MLToolbox"), true);
        this.connection = connection.ref();
        this.request = request;

        setDefaultSize(600, 800);
        init();
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    protected @NotNull MLToolboxForm createForm() {
        return new MLToolboxForm(this, getConnection(), request);
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.machineLearning.button.TrainModel"));
        renameAction(getCancelAction(), txt("msg.shared.button.Close"));
        return actions(
                getOKAction(),
                getResetAction(),
                getCancelAction());
    }

    @NotNull
    private Action getResetAction() {
        return createAction(txt("msg.shared.button.Reset"), () -> getForm().reset());
    }

    @Override
    protected void doOKAction() {
        MLToolboxForm form = getForm();
        form.applyFormChanges();
        
        if (request.isTemplate()) {
            form.saveRequestTemplate(true);
        }

        // Close dialog first, then start training in background
        super.doOKAction();
        
        DatabaseMLManager mlManager = DatabaseMLManager.getInstance(getProject());
        mlManager.trainModel(request, getConnection());
    }

    @Override
    public void doCancelAction() {
        MLToolboxForm form = getForm();
        form.applyFormChanges();

        if (request.isTemplate()) {
            form.saveRequestTemplate(false);
        }

        super.doCancelAction();
    }
}
