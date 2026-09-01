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
import com.dbn.ml.model.analysis.MLAttributeContribution;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Columns of the "Attribute Contribution" table.
 *
 * @author ayoub allali
 */
public class MLAttributeContributionTableModel extends DBNDynamicTableModel<MLAttributeContribution> {
    private static final int COLUMN_CONTRIBUTION = 1;

    public MLAttributeContributionTableModel(List<MLAttributeContribution> contributions) {
        super(MLAttributeContribution.class, contributions);

        addColumn(txt("app.shared.column.Name"), c -> c.getName());
        addColumn(txt("app.machineLearning.column.Contribution"), c -> c.getContribution());
        addColumn(txt("app.machineLearning.column.RowsWithContribution"), c -> c.getOccurrences());
    }

    @Override
    public String getPresentableValue(MLAttributeContribution row, int column) {
        if (column == COLUMN_CONTRIBUTION) return MLAnalysisValueFormatter.formatNonNegative(row.getContribution());
        return super.getPresentableValue(row, column);
    }
}
