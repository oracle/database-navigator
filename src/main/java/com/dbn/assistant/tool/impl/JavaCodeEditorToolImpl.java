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
import com.dbn.assistant.tool.spec.JavaCodeEditorTool;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaResource;
import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;


public class JavaCodeEditorToolImpl extends AssistantToolBase implements JavaCodeEditorTool {
    @Override
    public void openJavaClassEditor(String schemaName, String className) {
        DBSchema schema = getSchema(schemaName);
        DBJavaClass javaClass = schema.getJavaClass(className);

        verify(javaClass, DBObjectType.JAVA_CLASS, className);
        openEditor(javaClass);
    }

    @Override
    public void openJavaResourceEditor(String schemaName, String resourceName) {
        DBSchema schema = getSchema(schemaName);
        DBJavaResource javaResource = schema.getJavaResource(resourceName);

        verify(javaResource, DBObjectType.JAVA_RESOURCE, resourceName);
        openEditor(javaResource);
    }
}

