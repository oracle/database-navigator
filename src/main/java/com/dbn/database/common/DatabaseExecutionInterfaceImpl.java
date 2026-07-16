/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.database.common;

import com.dbn.common.approval.UserApprovalManager;
import com.dbn.common.database.AuthenticationInfo;
import com.dbn.connection.AuthenticationType;
import com.dbn.database.common.execution.MethodExecutionProcessor;
import com.dbn.database.common.execution.SimpleFunctionExecutionProcessor;
import com.dbn.database.common.execution.SimpleProcedureExecutionProcessor;
import com.dbn.database.interfaces.DatabaseExecutionInterface;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.execution.script.ScriptExecutionInput;
import com.dbn.execution.script.ScriptPasswordDelivery;
import com.dbn.object.DBFunction;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProcedure;

import static com.dbn.common.approval.UserApprovalAction.PASSWORD_ENVIRONMENT_VARIABLE;
import static com.dbn.execution.script.ScriptPasswordDelivery.CREDENTIAL_FILE;
import static com.dbn.execution.script.ScriptPasswordDelivery.ENVIRONMENT_VARIABLE;

public abstract class DatabaseExecutionInterfaceImpl implements DatabaseExecutionInterface {

    public MethodExecutionProcessor createSimpleMethodExecutionProcessor(DBMethod method) {
        if (method instanceof DBFunction function) {
            return new SimpleFunctionExecutionProcessor(function);
        }
        if (method instanceof DBProcedure procedure) {
            return new SimpleProcedureExecutionProcessor(procedure);

        }
        return null;
    }

    protected static void verifyEnvironmentPasswordApproval(ScriptExecutionInput executionInput) {
        AuthenticationInfo authenticationInfo = executionInput.getConnection().getAuthenticationInfo();

        AuthenticationType authType = authenticationInfo.getType();
        if (authType != AuthenticationType.USER_PASSWORD) return;

        UserApprovalManager approvalManager = UserApprovalManager.getInstance();
        CmdLineInterface cmdLineInterface = executionInput.getCmdLineInterface();

        ScriptPasswordDelivery passwordDelivery = executionInput.getPasswordDelivery();
        if (passwordDelivery == ENVIRONMENT_VARIABLE) {
            // Environment-based password delivery exposes credentials to the child process environment.
            approvalManager.ensureApproved(PASSWORD_ENVIRONMENT_VARIABLE, cmdLineInterface);
        } else if (passwordDelivery == CREDENTIAL_FILE) {
            // File-based execution supersedes any previously stored approval for the environment fallback.
            approvalManager.revoke(PASSWORD_ENVIRONMENT_VARIABLE, cmdLineInterface);
        }
    }

}
