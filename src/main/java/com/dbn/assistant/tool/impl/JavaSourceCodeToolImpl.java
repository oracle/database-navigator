/*
 * Copyright 2026 Oracle and/or its affiliates
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
import com.dbn.assistant.tool.spec.JavaSourceCodeTool;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaResource;
import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class JavaSourceCodeToolImpl extends AssistantToolBase implements JavaSourceCodeTool {

    @Override
    public JavaSourceCode loadJavaClassCode(String schemaName, String className) throws SQLException {
        DBSchema schema = getSchema(schemaName);
        DBJavaClass javaClass = schema.getJavaClass(className);
        verify(javaClass, DBObjectType.JAVA_CLASS, className);

        return loadJavaClassCode(javaClass);
    }

    @Override
    public JavaSourceCode loadJavaResourceCode(String schemaName, String resourceName) throws SQLException {
        DBSchema schema = getSchema(schemaName);
        DBJavaResource javaResource = schema.getJavaResource(resourceName);
        verify(javaResource, DBObjectType.JAVA_RESOURCE, resourceName);

        return loadJavaResourceCode(javaResource);
    }

    private static @NotNull JavaSourceCode loadJavaClassCode(DBJavaClass javaClass) throws SQLException {
        String sourceCode = loadObjectSourceCode(javaClass, DBContentType.CODE);

        JavaSourceCode programSourceCode = new JavaSourceCode();
        programSourceCode.setName(javaClass.getName());
        programSourceCode.setCode(sourceCode);
        programSourceCode.setType(javaClass.getObjectType().getName());
        return programSourceCode;
    }

    private static @NotNull JavaSourceCode loadJavaResourceCode(DBJavaResource javaResource) throws SQLException {
        String sourceCode = loadObjectSourceCode(javaResource, DBContentType.CODE);

        JavaSourceCode programSourceCode = new JavaSourceCode();
        programSourceCode.setName(javaResource.getName());
        programSourceCode.setCode(sourceCode);
        programSourceCode.setType(javaResource.getObjectType().getName());
        return programSourceCode;
    }
}
