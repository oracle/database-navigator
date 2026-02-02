/*
 * Copyright 2024-2025 Oracle and/or its affiliates
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

package com.dbn.ml.backend.tribuo;

import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.backend.MLBackendType;
import com.dbn.ml.backend.model.MLModelHandle;
import com.dbn.ml.backend.model.MLModelMetadata;
import com.dbn.ml.model.MLTaskType;
import lombok.Getter;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.Output;
import org.tribuo.classification.Label;
import org.tribuo.regression.Regressor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tribuo implementation of MLModelHandle.
 * Wraps a Tribuo Model&lt;?&gt; (in-memory model).
 *
 * @author Oracle
 */
@Getter
public class TribuoModelHandle implements MLModelHandle {

    private final Model<?> nativeModel;
    private final MLTaskType taskType;
    private final MLModelMetadata metadata;
    private final ConnectionHandler connection;

    // For evaluation - keep reference to test dataset
    private final MutableDataset<?> testDataset;

    public TribuoModelHandle(
            Model<?> model,
            MLTaskType taskType,
            List<String> featureNames,
            List<String> labelNames,
            MutableDataset<?> testDataset,
            int trainingSize,
            int testSize) {

        this.nativeModel = model;
        this.taskType = taskType;
        this.testDataset = testDataset;
        this.connection = null; // Tribuo models are not database-backed

        // Build metadata
        MLModelMetadata.MLModelMetadataBuilder metadataBuilder = MLModelMetadata.builder()
                .featureNames(featureNames)
                .labelNames(labelNames)
                .trainingDataSize(trainingSize)
                .testDataSize(testSize);

        if (taskType == MLTaskType.CLASSIFICATION) {
            Model<Label> classificationModel = (Model<Label>) model;
            List<String> classLabels = classificationModel.getOutputIDInfo()
                    .getDomain()
                    .stream()
                    .map(Label::getLabel)
                    .collect(Collectors.toList());

            metadataBuilder.classCount(classLabels.size());
            metadataBuilder.classLabels(classLabels);
        } else if (taskType == MLTaskType.REGRESSION) {
            Model<Regressor> regressionModel = (Model<Regressor>) model;
            int outputDimensions = regressionModel.getOutputIDInfo().size();
            metadataBuilder.outputDimensions(outputDimensions);
        }

        this.metadata = metadataBuilder.build();
    }

    @Override
    public MLBackendType getBackendType() {
        return MLBackendType.TRIBUO;
    }

    @Override
    public Object getNativeModel() {
        return nativeModel;
    }
}
