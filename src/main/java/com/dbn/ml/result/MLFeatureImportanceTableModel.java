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
import com.dbn.ml.model.analysis.MLFeatureImportance;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Columns of the "Feature Importance" table. Column functions return raw numbers so the
 * table sorts numerically; formatting happens in {@link #getPresentableValue(MLFeatureImportance, int)}.
 *
 * @author ayoub allali
 */
public class MLFeatureImportanceTableModel extends DBNDynamicTableModel<MLFeatureImportance> {
    private static final int COLUMN_IMPORTANCE = 1;
    private static final int COLUMN_MEAN = 6;
    private static final int COLUMN_STD_DEV = 7;

    public MLFeatureImportanceTableModel(List<MLFeatureImportance> features) {
        super(MLFeatureImportance.class, features);

        addColumn(txt("app.shared.column.Name"), f -> f.getName());
        addColumn(txt("app.machineLearning.column.Importance"), f -> f.getImportance());
        addColumn(txt("app.shared.column.Type"), f -> f.getTypeName());
        addColumn(txt("app.machineLearning.column.DistinctValues"), f -> f.getDistinctValues());
        addColumn(txt("app.machineLearning.column.Min"), f -> f.getMinValue());
        addColumn(txt("app.machineLearning.column.Max"), f -> f.getMaxValue());
        addColumn(txt("app.machineLearning.column.Mean"), f -> f.getMean());
        addColumn(txt("app.machineLearning.column.StdDev"), f -> f.getStdDev());
    }

    @Override
    public String getPresentableValue(MLFeatureImportance row, int column) {
        Object value = getValue(row, column);
        if (value == null) return "";

        switch (column) {
            case COLUMN_IMPORTANCE: return String.format("%.4f", (Double) value);
            case COLUMN_MEAN:
            case COLUMN_STD_DEV: return String.format("%.4f", (Double) value);
            default: return super.getPresentableValue(row, column);
        }
    }
}
