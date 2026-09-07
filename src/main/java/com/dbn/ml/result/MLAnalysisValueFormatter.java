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

package com.dbn.ml.result;

import lombok.experimental.UtilityClass;

@UtilityClass
class MLAnalysisValueFormatter {
    private static final double MINIMUM_DISPLAY_VALUE = 0.00005;

    static String formatNonNegative(double value) {
        return value > 0 && value < MINIMUM_DISPLAY_VALUE ? "< 0.0001" : String.format("%.4f", value);
    }
}
