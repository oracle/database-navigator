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

package com.dbn.ml;

import com.dbn.common.resource.ResourceLimitPolicy;
import com.dbn.common.resource.ResourceLimits;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Resource bounds for ML source text and confusion-matrix processing.
 */
@UtilityClass
public class MLResourceLimits implements ResourceLimitPolicy {
    public static final int MAX_ML_TEXT_LENGTH = 4000;
    public static final int MAX_CONFUSION_MATRIX_ROWS = 10_000;
    public static final int MAX_CONFUSION_MATRIX_CLASSES = 100;
    public static final int MAX_CONFUSION_MATRIX_CELLS = 10_000;
    public static final int MAX_CONFUSION_MATRIX_LABEL_LENGTH = MAX_ML_TEXT_LENGTH;

    private static final ResourceLimits LIMITS = ResourceLimits.builder()
            .add("ml-text-characters", MAX_ML_TEXT_LENGTH)
            .add("confusion-matrix-rows", MAX_CONFUSION_MATRIX_ROWS)
            .add("confusion-matrix-classes", MAX_CONFUSION_MATRIX_CLASSES)
            .add("confusion-matrix-cells", MAX_CONFUSION_MATRIX_CELLS)
            .add("confusion-matrix-label-characters", MAX_CONFUSION_MATRIX_LABEL_LENGTH)
            .build();

    public static boolean isMLTextLengthAllowed(long length) {
        return LIMITS.allows("ml-text-characters", length);
    }

    public static boolean isConfusionMatrixRowCountAllowed(long rowCount) {
        return LIMITS.allows("confusion-matrix-rows", rowCount);
    }

    public static boolean isConfusionMatrixLabelAllowed(@NotNull String label) {
        return LIMITS.allows("confusion-matrix-label-characters", label.length());
    }

    public static boolean isConfusionMatrixClassCountAllowed(long classCount) {
        return LIMITS.allows("confusion-matrix-classes", classCount) &&
                LIMITS.get("confusion-matrix-cells").allowsProduct(classCount, classCount);
    }
}
