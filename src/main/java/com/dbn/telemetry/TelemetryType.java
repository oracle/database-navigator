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

package com.dbn.telemetry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NonNls;

/** The telemetry collected by DBN and its stable report metric names. */
@Getter
@RequiredArgsConstructor
public enum TelemetryType {
    ACTION_INVOCATION("dbn.action.invocation"),
    DIALOG_USAGE("dbn.dialog.usage"),
    FEATURE_USAGE("dbn.feature.usage"),
    DATABASE_CALL("dbn.database.call"),
    CONNECTION("dbn.connection"),
    EXECUTION("dbn.execution");

    private final @NonNls String metricName;
}
