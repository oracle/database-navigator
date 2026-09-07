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
import com.dbn.ml.backend.model.MLPredictionAttribute;
import com.dbn.ml.model.MLTaskType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NonNls;

import javax.swing.Action;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Dialog for entering feature values for ad-hoc prediction.
 * Allows multiple predictions without closing the dialog.
 *
 * @author ayoub allali
 */
public class MLPredictDialog extends DBNDialog<MLPredictForm> {
    private final String modelName;
    private final ConnectionHandler connection;
    private final MLTaskType taskType;
    private final List<MLPredictionAttribute> attributes;
    private final @NonNls String predictionStatement;

    public MLPredictDialog(
            ConnectionHandler connection,
            String modelName,
            MLTaskType taskType,
            List<MLPredictionAttribute> attributes,
            @NonNls String predictionStatement) {

        super(connection.getProject(), txt("msg.machineLearning.title.AdHocPrediction"), true);
        this.connection = connection;
        this.modelName = modelName;
        this.taskType = taskType;
        this.attributes = attributes;
        this.predictionStatement = predictionStatement;
        setModal(false);
        setDefaultSize(1200, 800);
        init();
    }

    @NotNull
    @Override
    protected MLPredictForm createForm() {
        return new MLPredictForm(this, modelName, connection, taskType, attributes, predictionStatement);
    }

    @Override
    @NotNull
    protected final Action[] initializeActions() {
        renameAction(getCancelAction(), txt("msg.shared.button.Close"));
        return actions(getCancelAction());
    }
}
