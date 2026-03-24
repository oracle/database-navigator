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
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SVMDetailBuilder implements AlgorithmDetailBuilder {

    @Override
    public boolean canHandle(MLModelDetails details, @Nullable DBMSAlgorithmType algorithmType) {
        return details.hasSVMCoefficients()
                || algorithmType == DBMSAlgorithmType.SVM_CLASSIFICATION
                || algorithmType == DBMSAlgorithmType.SVM_REGRESSION;
    }

    @Override
    public void build(JPanel panel, MLModelDetails details) {
        if (details.hasSVMCoefficients()) {
            buildCoefficientsPanel(panel, details);
        } else {
            buildNoCoefficientsNote(panel);
        }
    }

    private void buildCoefficientsPanel(JPanel panel, MLModelDetails details) {
        MLResultPanelHelper.initSection(panel, "SVM Linear Coefficients");

        List<MLModelDetails.SVMCoefficient> coefs = details.getSvmCoefficients();
        boolean hasCatValues = coefs.stream().anyMatch(c -> c.getAttributeValue() != null && !c.getAttributeValue().isEmpty());
        boolean hasClasses = coefs.stream().anyMatch(c -> c.getClassName() != null && !c.getClassName().isEmpty());

        List<String> colList = new ArrayList<>();
        colList.add("Attribute");
        if (hasCatValues) colList.add("Value");
        if (hasClasses) colList.add("Class");
        colList.add("Coefficient");
        String[] columns = colList.toArray(new String[0]);

        Object[][] data = new Object[coefs.size()][columns.length];
        for (int i = 0; i < coefs.size(); i++) {
            MLModelDetails.SVMCoefficient c = coefs.get(i);
            int col = 0;
            data[i][col++] = c.getAttributeName();
            if (hasCatValues) data[i][col++] = c.getAttributeValue() != null ? c.getAttributeValue() : "";
            if (hasClasses) data[i][col++] = c.getClassName() != null ? c.getClassName() : "";
            data[i][col] = String.format("%.4f", c.getCoefficient());
        }

        panel.add(MLResultPanelHelper.wrapTable(MLResultPanelHelper.buildReadOnlyTable(data, columns)), BorderLayout.CENTER);
    }

    private void buildNoCoefficientsNote(JPanel panel) {
        MLResultPanelHelper.initSection(panel, "SVM Model Internals");

        JLabel note = new JLabel("<html>Linear coefficients (DM\u0024VL) are only available for the <b>linear kernel</b>. " +
                "Oracle SVM is using a <b>non-linear kernel</b> (e.g. Gaussian/RBF) for this model \u2014 " +
                "coefficients are not interpretable in feature space.</html>");
        note.setForeground(JBColor.gray);
        note.setFont(note.getFont().deriveFont(12f));
        note.setBorder(JBUI.Borders.emptyTop(4));
        panel.add(note, BorderLayout.CENTER);
    }
}
