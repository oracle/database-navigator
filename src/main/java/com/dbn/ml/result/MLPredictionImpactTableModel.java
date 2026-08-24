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

package com.dbn.ml.result;

import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.ml.model.analysis.MLPredictionImpact;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Columns of the "Prediction Impact" table.
 *
 * @author ayoub allali
 */
public class MLPredictionImpactTableModel extends DBNDynamicTableModel<MLPredictionImpact> {
    private static final int COLUMN_IMPACT = 1;

    public MLPredictionImpactTableModel(List<MLPredictionImpact> impacts) {
        super(MLPredictionImpact.class, impacts);

        addColumn(txt("app.shared.column.Name"), i -> i.getName());
        addColumn(txt("app.machineLearning.column.Impact"), i -> i.getImpact());
        addColumn(txt("app.machineLearning.column.RowsUsed"), i -> i.getOccurrences());
    }

    @Override
    public String getPresentableValue(MLPredictionImpact row, int column) {
        if (column == COLUMN_IMPACT) return String.format("%.4f", row.getImpact());
        return super.getPresentableValue(row, column);
    }
}
