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

import com.dbn.database.DatabaseFeature;
import com.dbn.prerequisite.evaluation.PrerequisiteEvaluator;
import com.dbn.prerequisite.model.Prerequisite;
import com.dbn.prerequisite.model.PrerequisiteBase;
import com.dbn.prerequisite.model.PrerequisiteCategory;
import com.dbn.prerequisite.model.PrerequisiteType;
import lombok.Getter;

import java.util.Set;

@Getter
public class PrerequisiteDefinitionBase implements PrerequisiteDefinition {
    private final String name;
    private final String description;
    private final PrerequisiteType type;
    private final PrerequisiteCategory category;
    private final PrerequisiteEvaluator evaluator;
    private final Set<DatabaseFeature> features;


    public PrerequisiteDefinitionBase(String name, String description, PrerequisiteType type, PrerequisiteCategory category, PrerequisiteEvaluator evaluator, DatabaseFeature ... features) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.category = category;
        this.evaluator = evaluator;
        this.features = Set.of(features);
    }

    @Override
    public boolean supports(DatabaseFeature feature) {
        return features.contains(feature);
    }

    @Override
    public Prerequisite createPrerequisite() {
        return new PrerequisiteBase(this);
    }
}
