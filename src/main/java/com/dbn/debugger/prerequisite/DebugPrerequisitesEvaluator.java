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
import com.dbn.prerequisite.evaluation.PrerequisiteRequirementEvaluatorBase;
import com.dbn.prerequisite.model.PrerequisiteMandate;

import java.util.List;

import static com.dbn.common.operation.DatabaseOperation.DEBUG_JAVA_CODE;
import static com.dbn.common.operation.DatabaseOperation.DEBUG_PLSQL_CODE_JDBC;
import static com.dbn.common.operation.DatabaseOperation.DEBUG_PLSQL_CODE_JDWP;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.CREATE_PROCEDURE;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.CREATE_TYPE;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.DEBUG_ANY_PROCEDURE;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.DEBUG_CONNECT_SESSION;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.EXECUTE_DBMS_DEBUG;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.EXECUTE_DBMS_DEBUG_JDWP;
import static com.dbn.prerequisite.shared.PrerequisiteTypes.HOST_ACE_JDWP;

public class DebugPrerequisitesEvaluator extends PrerequisiteRequirementEvaluatorBase {


    @Override
    public boolean supports(DatabaseOperation operation) {
        return operation.isOneOf(
                DEBUG_PLSQL_CODE_JDBC,
                DEBUG_PLSQL_CODE_JDWP,
                DEBUG_JAVA_CODE);
    }

    @Override
    protected void createMandates(List<PrerequisiteMandate> mandates, DatabaseOperation operation) {
        if (operation == DEBUG_JAVA_CODE) {
            createMandate(mandates, CREATE_PROCEDURE, txt("msg.prerequisite.text.Reason_CREATE_PROCEDURE"));
            createMandate(mandates, CREATE_TYPE, txt("msg.prerequisite.text.Reason_CREATE_TYPE"));
        }

        createMandate(mandates, DEBUG_CONNECT_SESSION, txt("msg.prerequisite.text.Reason_DEBUG_CONNECT_SESSION"));
        createMandate(mandates, DEBUG_ANY_PROCEDURE, txt("msg.prerequisite.text.Reason_DEBUG_ANY_PROCEDURE"));
        createMandate(mandates, EXECUTE_DBMS_DEBUG, txt("msg.prerequisite.text.Reason_EXECUTE_DBMS_DEBUG"));

        if (operation.isOneOf(
                DEBUG_JAVA_CODE,
                DEBUG_PLSQL_CODE_JDWP)) {

            createMandate(mandates, EXECUTE_DBMS_DEBUG_JDWP, txt("msg.prerequisite.text.Reason_EXECUTE_DBMS_DEBUG_JDWP"));
            createMandate(mandates, HOST_ACE_JDWP, txt("msg.prerequisite.text.Reason_HOST_ACE_JDWP"));
        }
    }
}
