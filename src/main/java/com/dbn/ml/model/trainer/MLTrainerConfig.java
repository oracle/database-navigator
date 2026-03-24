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

package com.dbn.ml.model.trainer;

import com.dbn.ml.model.MLConfig;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.*;

@Getter
@Setter
public class MLTrainerConfig extends MLConfig {
    private MLTrainerType trainerType = MLTrainerType.LOGISTIC_REGRESSION;
    private String modelName;

    // Train/Test split configuration
    private double trainTestSplitRatio = 0.7; // 70% training, 30% testing
    private boolean useFixedSeed = true;
    private long randomSeed = 1L;

    // Partitioned model support
    private List<String> partitionColumns = new ArrayList<>();

    @Override
    public void readState(Element element) {
        if (element == null) return;
        super.readState(element);

        trainerType = enumAttribute(element, "trainer-type", trainerType);
        modelName = stringAttribute(element, "model-name", modelName);
        trainTestSplitRatio = doubleAttribute(element, "split-ratio", trainTestSplitRatio);
        useFixedSeed = booleanAttribute(element, "use-fixed-seed", useFixedSeed);
        randomSeed = longAttribute(element, "random-seed", randomSeed);

        partitionColumns.clear();
        Element partitionsElement = element.getChild("partition-columns");
        if (partitionsElement != null) {
            for (Element partElement : partitionsElement.getChildren("partition")) {
                String columnName = stringAttribute(partElement, "column", null);
                if (columnName != null) {
                    partitionColumns.add(columnName);
                }
            }
        }
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);

        setEnumAttribute(element, "trainer-type", trainerType);
        setStringAttribute(element, "model-name", modelName);
        setDoubleAttribute(element, "split-ratio", trainTestSplitRatio);
        setBooleanAttribute(element, "use-fixed-seed", useFixedSeed);
        setLongAttribute(element, "random-seed", randomSeed);

        if (!partitionColumns.isEmpty()) {
            Element partitionsElement = newElement(element, "partition-columns");
            for (String col : partitionColumns) {
                Element partElement = newElement(partitionsElement, "partition");
                setStringAttribute(partElement, "column", col);
            }
        }
    }
}
