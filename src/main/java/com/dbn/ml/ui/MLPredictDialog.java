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
import com.dbn.ml.model.MLResult;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Dialog for entering feature values for ad-hoc prediction.
 * Allows multiple predictions without closing the dialog.
 *
 * @author ayoub allali
 */
public class MLPredictDialog extends DBNDialog<MLPredictForm> {
    private final MLResult mlResult;
    private final List<String> featureColumns;

    public MLPredictDialog(MLResult mlResult, List<String> featureColumns) {
        super(mlResult.getConnection().getProject(), txt("msg.machineLearning.title.AdHocPrediction"), true);
        this.mlResult = mlResult;
        this.featureColumns = featureColumns;
        setModal(false);
        init();
    }

    @NotNull
    @Override
    protected MLPredictForm createForm() {
        return new MLPredictForm(this, mlResult, featureColumns);
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
            super(txt("msg.machineLearning.button.Predict"));
            putValue(DEFAULT_ACTION, Boolean.TRUE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getForm().runPrediction();
        }
    }
}
