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

package com.dbn.ml.model.analysis;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * How much one column contributed to the predictions of a specific model, aggregated from
 * the per row weights returned by PREDICTION_DETAILS (mean absolute weight across the test set -
 * the same technique used to derive global SHAP importance from local explanations).
 * This is our own aggregation of a documented per row explanation, not a value Oracle computes
 * or names "importance" or "impact" itself. It describes the model, not the data - contrast with
 * {@link MLFeatureImportance}.
 *
 * @author ayoub allali
 */
@Getter
@AllArgsConstructor
public class MLAttributeContribution {
    private final String name;

    /** Mean absolute contribution to predictions across the scored rows. */
    private final double contribution;

    /** Number of test rows where Oracle reported a non-zero weight for this column. */
    private final long occurrences;
}
