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

import com.dbn.database.common.execution.MethodExecutionProcessor;
import com.dbn.database.common.execution.SimpleFunctionExecutionProcessor;
import com.dbn.database.common.execution.SimpleProcedureExecutionProcessor;
import com.dbn.database.interfaces.DatabaseExecutionInterface;
import com.dbn.object.DBFunction;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProcedure;

public abstract class DatabaseExecutionInterfaceImpl implements DatabaseExecutionInterface {

    public MethodExecutionProcessor createSimpleMethodExecutionProcessor(DBMethod method) {
        if (method instanceof DBFunction) {
            DBFunction function = (DBFunction) method;
            return new SimpleFunctionExecutionProcessor(function);
        }
        if (method instanceof DBProcedure) {
            DBProcedure procedure = (DBProcedure) method;
            return new SimpleProcedureExecutionProcessor(procedure);

        }
        return null;
    }

}
