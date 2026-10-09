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

package com.dbn.migration.shared.operation;

import org.jetbrains.annotations.NotNull;

/**
 * Typed capability contract for an engine's migration features.
 *
 * <p>The feature type remains engine-specific while shared code can work with the
 * common support contract when it does not need to know the concrete feature enum.</p>
 */
public interface DatabaseMigrationFeatureSupport<F extends Enum<F>> {
    boolean supports(@NotNull F feature);

    boolean requires(@NotNull F feature);
}
