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
import com.dbn.connection.context.DatabaseContext;
import com.dbn.prerequisite.evaluation.PrerequisiteRequirementEvaluator;
import com.dbn.prerequisite.model.PrerequisiteMandate;
import com.dbn.prerequisite.model.PrerequisiteType;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.prerequisite.shared.PrerequisiteTypes.CREATE_TABLE;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.TABLESPACE_QUOTA;

public class CreateJavaPrerequisiteEvaluator implements PrerequisiteRequirementEvaluator {
    @Override
    public boolean supports(DatabaseOperation operation) {
        return operation == DatabaseOperation.CHANGE_JAVA_CODE;
    }

    @Override
    public List<PrerequisiteMandate> resolvePrerequisites(DatabaseContext context, DatabaseOperation operation) {
        List<PrerequisiteMandate> mandates = new ArrayList<>();
        createMandate(mandates, CREATE_TABLE, "Allows user to create tables in own schema. This is required for creating the java lob table for storing java binary");
        createMandate(mandates, TABLESPACE_QUOTA, "Allows user to create and insert rows in table in own schema. This is required for storing java source code binary");
        return mandates;
    }

    private static void createMandate(List<PrerequisiteMandate> mandates, PrerequisiteType type, String reason) {
        mandates.add(new PrerequisiteMandate(type, reason));
    }
}
