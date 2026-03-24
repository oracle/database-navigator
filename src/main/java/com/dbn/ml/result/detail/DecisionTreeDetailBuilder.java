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

package com.dbn.ml.result.detail;

import com.dbn.ml.backend.dbms.DBMSAlgorithmType;
import com.dbn.ml.model.MLModelDetails;
import com.dbn.ml.result.MLResultPanelHelper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DecisionTreeDetailBuilder implements AlgorithmDetailBuilder {

    @Override
    public boolean canHandle(MLModelDetails details, @Nullable DBMSAlgorithmType algorithmType) {
        return details.hasTreeSplits();
    }

    @Override
    public void build(JPanel panel, MLModelDetails details) {
        MLResultPanelHelper.initSection(panel, "Decision Tree Splits");

        String[] columns = {"Node", "Parent", "Split Attribute", "Operator", "Value"};
        List<MLModelDetails.TreeSplit> splits = details.getTreeSplits();
        Object[][] data = new Object[splits.size()][5];
        for (int i = 0; i < splits.size(); i++) {
            MLModelDetails.TreeSplit s = splits.get(i);
            data[i][0] = s.getNode();
            data[i][1] = s.getParent();
            data[i][2] = s.getAttributeName() != null ? s.getAttributeName() : "";
            data[i][3] = s.getOperator() != null ? s.getOperator() : "";
            data[i][4] = s.getValue() != null ? s.getValue() : "";
        }

        panel.add(MLResultPanelHelper.wrapTable(MLResultPanelHelper.buildReadOnlyTable(data, columns)), BorderLayout.CENTER);
    }
}
