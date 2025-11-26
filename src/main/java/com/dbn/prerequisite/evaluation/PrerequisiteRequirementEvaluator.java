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

package com.dbn.prerequisite.evaluation;

import com.dbn.common.operation.DatabaseOperation;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.prerequisite.model.PrerequisiteMandate;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.DumbAware;

import java.util.List;

public interface PrerequisiteRequirementEvaluator extends DumbAware {
    ExtensionPointName<PrerequisiteRequirementEvaluator> EP = ExtensionPointName.create("com.dbn.prerequisiteRequirementEvaluator");

    boolean supports(DatabaseOperation operation);

    /**
     * Resolves the prerequisite types, necessary for a given database operation in a specified database context.
     * (the prerequisite types are wrapped as {@link PrerequisiteMandate} to allow the reason of the prerequisite in the given context)
     *
     * @param context   the database context that provides details such as connection, session, and schema to evaluate
     *                  and determine applicable prerequisites.
     * @param operation the database operation for which the prerequisites are being resolved. It encapsulates the
     *                  operation type and associated attributes.
     * @return a list of {@code PrerequisiteDefinition} objects representing the prerequisites that should be met
     * before executing the specified database operation. Returns an empty array if no prerequisites are found.
     */
    List<PrerequisiteMandate> resolvePrerequisites(DatabaseContext context, DatabaseOperation operation);
}
