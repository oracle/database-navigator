/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.ml.backend.dbms;

import com.dbn.ml.model.MLTaskType;
import org.jetbrains.annotations.NonNls;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class MLObjectNames {
    public static final @NonNls String SCHEDULER_JOB_PREFIX = "ML";

    private MLObjectNames() {}

    public static String timestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    public static String stagingTable(String timestamp) {
        return "ML_STAGING_" + timestamp;
    }

    public static String externalTable(String timestamp) {
        return "ML_EXT_" + timestamp;
    }

    public static String trainTable(String timestamp) {
        return "ML_TRAIN_" + timestamp;
    }

    public static String testTable(String timestamp) {
        return "ML_TEST_" + timestamp;
    }

    public static String settingsTable(String timestamp) {
        return "ML_SETTINGS_" + timestamp;
    }

    public static String applyResultTable(String timestamp) {
        return "ML_APPLY_" + timestamp;
    }

    public static String confusionMatrixTable(String timestamp) {
        return "ML_CM_" + timestamp;
    }

    public static String accuracyTable(String confusionMatrixTable) {
        return confusionMatrixTable + "_ACC";
    }

    public static String rocTable(String timestamp) {
        return "ML_ROC_" + timestamp;
    }

    public static String aucTable(String rocTable) {
        return rocTable + "_AUC";
    }

    public static String liftTable(String timestamp) {
        return "ML_LIFT_" + timestamp;
    }

    public static String featureImportanceTable(String timestamp) {
        return "ML_AI_" + timestamp;
    }

    public static String fallbackModel(MLTaskType taskType, String timestamp) {
        String taskPrefix = taskType == MLTaskType.CLASSIFICATION ? "CLS" : "REG";
        return "ML_MODEL_" + taskPrefix + "_" + timestamp;
    }
}
