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
 *
 */

package com.dbn.database.common.execution;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.object.DBJavaMethod;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public interface JavaExecutionProcessor {
	void execute(JavaExecutionInput executionInput, DBDebuggerType debuggerType) throws SQLException;

	void execute(JavaExecutionInput executionInput, DBNConnection connection, DBDebuggerType debuggerType) throws SQLException;

	@NotNull
	DBJavaMethod getMethod();
}
