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
import com.dbn.assistant.tool.spec.ProgramMetadataTool;
import com.dbn.object.DBFunction;
import com.dbn.object.DBPackage;
import com.dbn.object.DBProcedure;
import com.dbn.object.DBSchema;
import com.dbn.object.DBType;

import java.util.List;

public class ProgramMetadataToolImpl extends AssistantToolBase implements ProgramMetadataTool {

    @Override
    public List<String> listProgramNames(String schemaName, String programType) {
        switch (programType.toUpperCase()) {
            case "FUNCTION": return listFunctionNames(schemaName);
            case "PROCEDURE": return listProcedureNames(schemaName);
            case "PACKAGE": return listPackageNames(schemaName);
            case "TYPE": return listTypeNames(schemaName);
            default: throw new IllegalArgumentException("Invalid program type \"" + programType + "\". Expected one of the following values: FUNCTION, PROCEDURE, PACKAGE or TYPE");
        }
    }

    @Override
    public List<String> listTypeNames(String schemaName) {
        DBSchema schema = getSchema(schemaName);

        List<DBType> types = schema.getTypes();
        return getObjectNames(types, false);
    }

    @Override
    public List<String> listFunctionNames(String schemaName) {
        DBSchema schema = getSchema(schemaName);

        List<DBFunction> functions = schema.getFunctions();
        return getObjectNames(functions, false);
    }

    @Override
    public List<String> listProcedureNames(String schemaName) {
        DBSchema schema = getSchema(schemaName);

        List<DBProcedure> procedures = schema.getProcedures();
        return getObjectNames(procedures, false);
    }

    @Override
    public List<String> listPackageNames(String schemaName) {
        DBSchema schema = getSchema(schemaName);

        List<DBPackage> packages = schema.getPackages();
        return getObjectNames(packages, false);
    }
}
