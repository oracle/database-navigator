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

package com.dbn.assistant.tool.impl;

import com.dbn.assistant.tool.AssistantToolBase;
import com.dbn.assistant.tool.spec.ProgramSourceCodeTool;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBFunction;
import com.dbn.object.DBMethod;
import com.dbn.object.DBPackage;
import com.dbn.object.DBProcedure;
import com.dbn.object.DBProgram;
import com.dbn.object.DBSchema;
import com.dbn.object.DBType;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class ProgramSourceCodeToolImpl extends AssistantToolBase implements ProgramSourceCodeTool {

    @Override
    public ProgramSourceCode loadProgramSourceCode(String schemaName, String programName, String programType) throws SQLException {
        switch (programType.toUpperCase()) {
            case "FUNCTION": return loadFunctionSourceCode(schemaName, programName);
            case "PROCEDURE": return loadProcedureSourceCode(schemaName, programName);
            case "PACKAGE": return loadPackageSourceCode(schemaName, programName);
            case "TYPE": return loadTypeSourceCode(schemaName, programName);
            default: throw new IllegalArgumentException("Invalid program type \"" + programType + "\". Expected one of the following values: FUNCTION, PROCEDURE, PACKAGE or TYPE");
        }
    }

    @Override
    public ProgramSourceCode loadTypeSourceCode(String schemaName, String typeName) throws SQLException {
        DBSchema schema = getSchema(schemaName);
        DBType type = schema.getType(typeName);
        verify(type, DBObjectType.TYPE, typeName);

        return loadProgramSourceCode(type);
    }

    @Override
    public ProgramSourceCode loadPackageSourceCode(String schemaName, String packageName) throws SQLException {
        DBSchema schema = getSchema(schemaName);
        DBPackage packge = schema.getPackage(packageName);
        verify(packge, DBObjectType.PACKAGE, packageName);

        return loadProgramSourceCode(packge);
    }

    @Override
    public ProgramSourceCode loadFunctionSourceCode(String schemaName, String functionName) throws SQLException {
        DBSchema schema = getSchema(schemaName);
        DBFunction function = schema.getFunction(functionName, (short) 0); // TODO support overloads
        verify(function, DBObjectType.FUNCTION, functionName);

        return loadMethodSourceCode(function);
    }

    @Override
    public ProgramSourceCode loadProcedureSourceCode(String schemaName, String procedureName) throws SQLException {
        DBSchema schema = getSchema(schemaName);
        DBProcedure procedure = schema.getProcedure(procedureName, (short) 0); // TODO support overloads
        verify(procedure, DBObjectType.PROCEDURE, procedureName);

        return loadMethodSourceCode(procedure);
    }

    private static @NotNull ProgramSourceCode loadProgramSourceCode(DBProgram program) throws SQLException {
        String specSourceCode = loadObjectSourceCode(program, DBContentType.CODE_SPEC);
        String bodySourceCode = loadObjectSourceCode(program, DBContentType.CODE_BODY);

        ProgramSourceCode programSourceCode = new ProgramSourceCode();
        programSourceCode.setName(program.getName());
        programSourceCode.setSpec(specSourceCode);
        programSourceCode.setCode(bodySourceCode);
        programSourceCode.setType(program.getObjectType().getName());
        return programSourceCode;
    }

    private static @NotNull ProgramSourceCode loadMethodSourceCode(DBMethod method) throws SQLException {
        String sourceCode = loadObjectSourceCode(method, DBContentType.CODE);

        ProgramSourceCode methodSourceCode = new ProgramSourceCode();
        methodSourceCode.setName(method.getName());
        methodSourceCode.setCode(sourceCode);
        methodSourceCode.setType(method.getObjectType().getName());
        return methodSourceCode;
    }
}
