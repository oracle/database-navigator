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

    public List<String> getLabelColumns() {
        List<String> labels = new ArrayList<>();
        if (labelColumn != null && !labelColumn.isEmpty()) {
            labels.add(labelColumn);
        }
        return labels;
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;
        super.readState(element);

        labelColumn = stringAttribute(element, "label-column", labelColumn);

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

        Element featuresElement = newElement(element, "features");
        for (String featureColumn : featureColumns) {
            Element featureElement = newElement(featuresElement, "feature");
            setStringAttribute(featureElement, "column", featureColumn);
        }
    }
}
