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
import com.dbn.ml.model.MLResult;
import com.dbn.ml.model.MLTaskType;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.List;

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
    private final List<String> featureColumns;

    /** Open predict dialog from a training result. */
    public MLPredictDialog(MLResult mlResult, List<String> featureColumns) {
        this(mlResult.getConnection(),
             mlResult.getModelHandle() != null ? mlResult.getModelHandle().getModelName() : mlResult.getAlgorithmName(),
             mlResult.getTaskType(),
             featureColumns);
    }

    /** Open predict dialog for an existing database model. */
    public MLPredictDialog(ConnectionHandler connection, String modelName, MLTaskType taskType, List<String> featureColumns) {
        super(connection.getProject(), "Ad-hoc Prediction", true);
        this.connection = connection;
        this.modelName = modelName;
        this.taskType = taskType;
        this.featureColumns = featureColumns;
        setModal(false);
        init();
    }

    @NotNull
    @Override
    protected MLPredictForm createForm() {
        return new MLPredictForm(this, modelName, connection, taskType, featureColumns);
    }

    @Override
    @NotNull
    protected final Action[] initializeActions() {
        return actions(
                new PredictAction(),
                getCancelAction());
    }

    public List<String> getFeatureValues() {
        return getForm().getFeatureValues();
    }

    /**
     * Custom action that runs prediction without closing the dialog.
     */
    private class PredictAction extends AbstractAction {
        PredictAction() {
            super("Predict");
            putValue(DEFAULT_ACTION, Boolean.TRUE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getForm().runPrediction();
        }
    }
}
