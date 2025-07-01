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

package com.dbn.prerequisite.definition;

import com.dbn.prerequisite.evaluation.PrerequisiteEvaluator;
import com.dbn.prerequisite.model.PrerequisiteType;
import com.dbn.prerequisite.resolution.PrerequisiteResolver;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public abstract class PrerequisiteDefinitionProviderBase implements PrerequisiteDefinitionProvider{
    @Getter
    private final PrerequisiteType prerequisiteType;

    @Getter(lazy = true)
    private final PrerequisiteDefinition definition = createDefinition();

    protected PrerequisiteDefinitionProviderBase(PrerequisiteType prerequisiteType) {
        this.prerequisiteType = prerequisiteType;
    }

    private PrerequisiteDefinition createDefinition() {
        PrerequisiteEvaluator evaluator = createEvaluator();
        PrerequisiteResolver resolver = createResolver();
        return createDefinition(evaluator, resolver);
    }

    @Nullable
    protected abstract PrerequisiteEvaluator createEvaluator();

    @Nullable
    protected abstract PrerequisiteResolver createResolver();

    protected abstract PrerequisiteDefinition createDefinition(PrerequisiteEvaluator evaluator, PrerequisiteResolver resolver);

}
