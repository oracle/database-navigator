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

package com.dbn.execution.java.prerequisite;

import com.dbn.common.operation.DatabaseOperation;
import com.dbn.prerequisite.evaluation.PrerequisiteRequirementEvaluatorBase;
import com.dbn.prerequisite.model.PrerequisiteMandate;

import java.util.List;

import static com.dbn.prerequisite.shared.PrerequisiteTypes.CREATE_PROCEDURE;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.CREATE_TYPE;

public class ExecuteJavaPrerequisitesEvaluator extends PrerequisiteRequirementEvaluatorBase {
    @Override
    public boolean supports(DatabaseOperation operation) {
        return operation == DatabaseOperation.EXECUTE_JAVA_CODE;
    }

    @Override
    protected void createMandates(List<PrerequisiteMandate> mandates, DatabaseOperation operation) {
        createMandate(mandates, CREATE_PROCEDURE, "Allows user to create functions, procedures, and packages in own schema. This is required for creating the java execution wrappers");
        createMandate(mandates, CREATE_TYPE, "Allows user to create database types in own schema. These are sometimes required as data converters in the java execution wrappers");
    }
}
