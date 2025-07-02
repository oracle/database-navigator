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

package com.dbn.debugger.prerequisite;

import com.dbn.common.operation.DatabaseOperation;
import com.dbn.common.operation.DatabaseOperationType;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.prerequisite.evaluation.PrerequisiteRequirementEvaluator;
import com.dbn.prerequisite.model.PrerequisiteType;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.operation.DatabaseOperationType.DEBUG_JAVA_CODE;
import static com.dbn.debugger.prerequisite.DebugPrerequisiteTypes.DEBUG_ANY_PROCEDURE;
import static com.dbn.debugger.prerequisite.DebugPrerequisiteTypes.DEBUG_CONNECT_SESSION;
import static com.dbn.debugger.prerequisite.DebugPrerequisiteTypes.EXECUTE_DBMS_DEBUG;
import static com.dbn.debugger.prerequisite.DebugPrerequisiteTypes.EXECUTE_DBMS_DEBUG_JDWP;

public class DebugPrerequisitesEvaluator implements PrerequisiteRequirementEvaluator {
    @Override
    public List<PrerequisiteType> resolvePrerequisites(DatabaseContext context, DatabaseOperation operation) {
        List<PrerequisiteType> prerequisites = new ArrayList<>();

        prerequisites.add(DEBUG_CONNECT_SESSION);
        prerequisites.add(DEBUG_ANY_PROCEDURE);
        prerequisites.add(EXECUTE_DBMS_DEBUG);


        DatabaseOperationType operationType = operation.getType();
        DBDebuggerType debuggerType = operation.getAttribute("DEBUGGER_TYPE");
        if (operationType == DEBUG_JAVA_CODE || debuggerType == DBDebuggerType.JDWP) {
            prerequisites.add(EXECUTE_DBMS_DEBUG_JDWP);
        }
        return prerequisites;
    }
}
