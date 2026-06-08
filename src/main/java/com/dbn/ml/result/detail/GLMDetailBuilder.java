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
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class GLMDetailBuilder implements AlgorithmDetailBuilder {

    @Override
    public boolean canHandle(MLModelDetails details, @Nullable DBMSAlgorithmType algorithmType) {
        return details.hasGLMCoefficients();
    }

    @Override
    public void build(JPanel panel, MLModelDetails details) {
        MLResultPanelHelper.initSection(panel, txt("app.machineLearning.title.GLMCoefficients"));

        String[] columns = {
                txt("app.machineLearning.column.Attribute"),
                txt("app.machineLearning.column.Value"),
                txt("app.machineLearning.column.Coefficient"),
                txt("app.machineLearning.column.StdError"),
                txt("app.machineLearning.column.PValue")};
        List<MLModelDetails.GLMCoefficient> coefs = details.getGlmCoefficients();
        Object[][] data = new Object[coefs.size()][5];
        for (int i = 0; i < coefs.size(); i++) {
            MLModelDetails.GLMCoefficient c = coefs.get(i);
            data[i][0] = c.getAttributeName();
            data[i][1] = c.getAttributeValue() != null ? c.getAttributeValue() : "";
            data[i][2] = String.format("%.4f", c.getCoefficient());
            data[i][3] = String.format("%.4f", c.getStdError());
            data[i][4] = String.format("%.4f", c.getPValue());
        }

        JBTable table = MLResultPanelHelper.buildReadOnlyTable(data, columns);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected && col == 4) {
                    try {
                        double p = Double.parseDouble(value.toString());
                        setForeground(p < 0.05
                                ? new JBColor(new Color(0, 120, 0), new Color(80, 200, 80))
                                : JBColor.foreground());
                    } catch (NumberFormatException ignored) {
                        setForeground(JBColor.foreground());
                    }
                } else {
                    setForeground(JBColor.foreground());
                }
                return c;
            }
        });

        panel.add(MLResultPanelHelper.wrapTable(table), BorderLayout.CENTER);
    }
}
