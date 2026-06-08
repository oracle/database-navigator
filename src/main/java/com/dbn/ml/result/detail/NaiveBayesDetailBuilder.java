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

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class NaiveBayesDetailBuilder implements AlgorithmDetailBuilder {

    @Override
    public boolean canHandle(MLModelDetails details, @Nullable DBMSAlgorithmType algorithmType) {
        return details.hasNaiveBayes();
    }

    @Override
    public void build(JPanel panel, MLModelDetails details) {
        MLResultPanelHelper.initSection(panel, txt("app.machineLearning.title.NaiveBayesProbabilities"));

        JPanel inner = new JPanel(new GridLayout(1, details.getNbConditionals().isEmpty() ? 1 : 2, 12, 0));

        // Priors table
        String[] priorCols = {
                txt("app.machineLearning.column.Class"),
                txt("app.machineLearning.column.PriorProbability"),
                txt("app.machineLearning.column.Count")};
        List<MLModelDetails.NaiveBayesPrior> priors = details.getNbPriors();
        Object[][] priorData = new Object[priors.size()][3];
        for (int i = 0; i < priors.size(); i++) {
            MLModelDetails.NaiveBayesPrior p = priors.get(i);
            priorData[i][0] = p.getTargetValue();
            priorData[i][1] = String.format("%.4f", p.getProbability());
            priorData[i][2] = p.getCount();
        }
        JPanel priorsCard = new JPanel(new BorderLayout(4, 4));
        JLabel priorsTitle = new JLabel(txt("app.machineLearning.title.ClassPriors"));
        priorsTitle.setFont(priorsTitle.getFont().deriveFont(Font.BOLD, 12f));
        priorsCard.add(priorsTitle, BorderLayout.NORTH);
        priorsCard.add(MLResultPanelHelper.wrapTable(MLResultPanelHelper.buildReadOnlyTable(priorData, priorCols)), BorderLayout.CENTER);
        inner.add(priorsCard);

        // Conditionals table
        if (!details.getNbConditionals().isEmpty()) {
            String[] condCols = {
                    txt("app.machineLearning.column.Class"),
                    txt("app.machineLearning.column.Attribute"),
                    txt("app.machineLearning.column.Value"),
                    txt("app.machineLearning.column.ConditionalProbability")};
            List<MLModelDetails.NaiveBayesConditional> conds = details.getNbConditionals();
            Object[][] condData = new Object[conds.size()][4];
            for (int i = 0; i < conds.size(); i++) {
                MLModelDetails.NaiveBayesConditional c = conds.get(i);
                condData[i][0] = c.getTargetValue();
                condData[i][1] = c.getAttributeName();
                condData[i][2] = c.getAttributeValue() != null ? c.getAttributeValue() : "";
                condData[i][3] = String.format("%.4f", c.getConditionalProbability());
            }
            JPanel condsCard = new JPanel(new BorderLayout(4, 4));
            JLabel condsTitle = new JLabel(txt("app.machineLearning.title.TopConditionalProbabilities"));
            condsTitle.setFont(condsTitle.getFont().deriveFont(Font.BOLD, 12f));
            condsCard.add(condsTitle, BorderLayout.NORTH);
            condsCard.add(MLResultPanelHelper.wrapTable(MLResultPanelHelper.buildReadOnlyTable(condData, condCols)), BorderLayout.CENTER);
            inner.add(condsCard);
        }

        panel.add(inner, BorderLayout.CENTER);
    }
}
