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
import com.dbn.connection.context.DatabaseContext;
import com.dbn.prerequisite.evaluation.PrerequisiteRequirementEvaluator;
import com.dbn.prerequisite.model.PrerequisiteMandate;
import com.dbn.prerequisite.model.PrerequisiteType;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.operation.DatabaseOperation.DEBUG_JAVA_CODE;
import static com.dbn.common.operation.DatabaseOperation.DEBUG_PLSQL_CODE_JDBC;
import static com.dbn.common.operation.DatabaseOperation.DEBUG_PLSQL_CODE_JDWP;
import static com.dbn.debugger.prerequisite.DebugPrerequisiteTypes.DEBUG_ANY_PROCEDURE;
import static com.dbn.debugger.prerequisite.DebugPrerequisiteTypes.DEBUG_CONNECT_SESSION;
import static com.dbn.debugger.prerequisite.DebugPrerequisiteTypes.EXECUTE_DBMS_DEBUG;
import static com.dbn.debugger.prerequisite.DebugPrerequisiteTypes.EXECUTE_DBMS_DEBUG_JDWP;
import static com.dbn.debugger.prerequisite.DebugPrerequisiteTypes.HOST_ACE_JDWP;
import static com.dbn.prerequisite.shared.SharedPrerequisiteTypes.CREATE_PROCEDURE;
import static com.dbn.prerequisite.shared.SharedPrerequisiteTypes.CREATE_TYPE;

public class DebugPrerequisitesEvaluator implements PrerequisiteRequirementEvaluator {


    @Override
    public boolean supports(DatabaseOperation operation) {
        return operation.isOneOf(
                DEBUG_PLSQL_CODE_JDBC,
                DEBUG_PLSQL_CODE_JDWP,
                DEBUG_JAVA_CODE);
    }

    @Override
    public List<PrerequisiteMandate> resolvePrerequisites(DatabaseContext context, DatabaseOperation operation) {
        List<PrerequisiteMandate> mandates = new ArrayList<>();

        if (operation == DEBUG_JAVA_CODE) {
            createMandate(mandates, CREATE_PROCEDURE, "Allows user to create functions, procedures, and packages in own schema. This is required for creating the java execution wrappers");
            createMandate(mandates, CREATE_TYPE, "Allows user to create database types in own schema. These are sometimes required as data converters in the java execution wrappers");
        }

        createMandate(mandates, DEBUG_CONNECT_SESSION, "Enables debugging of PL/SQL code by granting access to the databases's debugging facilities");
        createMandate(mandates, DEBUG_ANY_PROCEDURE, "Allows user to debug PL/SQL code in any schema");
        createMandate(mandates, EXECUTE_DBMS_DEBUG, "Allows the user to execute procedures and functions of the SYS.DBMS_DEBUG package, which provides an API for debugging PL/SQL code within the database");

        if (operation.isOneOf(
                DEBUG_JAVA_CODE,
                DEBUG_PLSQL_CODE_JDWP)) {

            createMandate(mandates, EXECUTE_DBMS_DEBUG_JDWP, "Allows the user to execute procedures and functions of the SYS.DBMS_DEBUG_JDWP package, which provides an API for debugging PL/SQL and Java code within the database");
            createMandate(mandates, HOST_ACE_JDWP, "Grants database access to a network location. Enables a database user to establish a JDWP connection to a specific host");
        }
        return mandates;
    }

    private static void createMandate(List<PrerequisiteMandate> mandates, PrerequisiteType type, String reason) {
        mandates.add(new PrerequisiteMandate(type, reason));
    }
}
