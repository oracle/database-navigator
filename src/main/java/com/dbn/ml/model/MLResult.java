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

package com.dbn.ml.model;

import com.dbn.connection.ConnectionHandler;
import lombok.Getter;
import lombok.Setter;
import org.tribuo.Model;
import org.tribuo.classification.Label;
import org.tribuo.classification.evaluation.LabelEvaluation;

@Getter
@Setter
public class MLResult {
    private Model<Label> model;
    private LabelEvaluation evaluation;
    private ConnectionHandler connection;
    private String algorithmName;
    
    private int trainingDataSize;
    private int testingDataSize;
    private int featureCount;
    private int classCount;
    
    private long trainingTimeMs;
    
    public double getAccuracy() {
        return evaluation != null ? evaluation.accuracy() : 0.0;
    }
    
    public String getConfusionMatrix() {
        return evaluation != null ? evaluation.getConfusionMatrix().toString() : "";
    }
    
    public String getEvaluationSummary() {
        return evaluation != null ? evaluation.toString() : "No evaluation available";
    }
}
