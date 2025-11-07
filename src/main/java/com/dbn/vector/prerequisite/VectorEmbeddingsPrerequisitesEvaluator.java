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

package com.dbn.vector.prerequisite;

import com.dbn.common.operation.DatabaseOperation;
import com.dbn.prerequisite.evaluation.PrerequisiteRequirementEvaluatorBase;
import com.dbn.prerequisite.model.PrerequisiteMandate;

import java.util.List;

import static com.dbn.common.operation.DatabaseOperation.CREATE_VECTOR_EMBEDDINGS;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.CREATE_TABLE;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.DATABASE_VERSION_23_1;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.EXECUTE_DBMS_VECTOR;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.EXECUTE_DBMS_VECTOR_CHAIN;

public class VectorEmbeddingsPrerequisitesEvaluator extends PrerequisiteRequirementEvaluatorBase {

    @Override
    public boolean supports(DatabaseOperation operation) {
        return operation == CREATE_VECTOR_EMBEDDINGS;
    }

    @Override
    protected void createMandates(List<PrerequisiteMandate> mandates, DatabaseOperation operation) {
        createMandate(mandates, DATABASE_VERSION_23_1, "Support for vector embeddings requires Oracle Database 23.1 or later");
        createMandate(mandates, CREATE_TABLE, "Allows user to create tables in own schema. This is required for creating the content staging and vector embeddings table");
        createMandate(mandates, EXECUTE_DBMS_VECTOR, "Allows the user to execute procedures and functions of the SYS.DBMS_VECTOR package, which provides support vector operations");
        createMandate(mandates, EXECUTE_DBMS_VECTOR_CHAIN, "Allows the user to execute procedures and functions of the CTXSYS.DBMS_VECTOR_CHAIN package, which provides support for creating and managing vector embeddings");
    }
}
