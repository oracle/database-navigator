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

package com.dbn.ml.model.feature;

import com.dbn.ml.model.MLConfig;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.*;

@Getter
@Setter
public class MLFeatureConfig extends MLConfig {
    // Column names selected as features (input)
    private List<String> featureColumns = new ArrayList<>();

    // Column name selected as label (output to predict)
    private String labelColumn;

    // Optional second label for multi-output regression (e.g., home_goals, away_goals)
    private String labelColumn2;

    /**
     * Returns true if this is a multi-output regression (two labels selected)
     */
    public boolean isMultiOutput() {
        return labelColumn != null && !labelColumn.isEmpty()
            && labelColumn2 != null && !labelColumn2.isEmpty();
    }

    /**
     * Returns all label columns as a list (1 or 2 items)
     */
    public List<String> getLabelColumns() {
        List<String> labels = new ArrayList<>();
        if (labelColumn != null && !labelColumn.isEmpty()) {
            labels.add(labelColumn);
        }
        if (labelColumn2 != null && !labelColumn2.isEmpty()) {
            labels.add(labelColumn2);
        }
        return labels;
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;
        super.readState(element);

        labelColumn = stringAttribute(element, "label-column", labelColumn);
        labelColumn2 = stringAttribute(element, "label-column-2", labelColumn2);

        featureColumns.clear();
        Element featuresElement = element.getChild("features");
        if (featuresElement != null) {
            for (Element featureElement : featuresElement.getChildren("feature")) {
                String columnName = stringAttribute(featureElement, "column", null);
                if (columnName != null) {
                    featureColumns.add(columnName);
                }
            }
        }
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);

        setStringAttribute(element, "label-column", labelColumn);
        setStringAttribute(element, "label-column-2", labelColumn2);

        Element featuresElement = newElement(element, "features");
        for (String featureColumn : featureColumns) {
            Element featureElement = newElement(featuresElement, "feature");
            setStringAttribute(featureElement, "column", featureColumn);
        }
    }
}
