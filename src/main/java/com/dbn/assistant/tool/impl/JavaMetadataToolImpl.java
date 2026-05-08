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
import com.dbn.assistant.tool.spec.JavaMetadataTool;
import com.dbn.connection.info.ConnectionInfo;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaResource;
import com.dbn.object.DBSchema;
import lombok.SneakyThrows;

import java.util.List;


public class JavaMetadataToolImpl extends AssistantToolBase implements JavaMetadataTool {

    @Override
    @SneakyThrows
    public JavaInformation getJavaInformation() {
        ConnectionInfo connectionInfo = getConnection().getConnectionInfo();
        if (connectionInfo == null) throw new IllegalStateException("Could not connect to database");

        JavaInformation information = new JavaInformation();

        JavaExecutionManager javaExecutionManager = JavaExecutionManager.getInstance(getProject());
        String javaVersion = javaExecutionManager.getJavaVersion(getConnectionId());
        information.setVersion(javaVersion); //SELECT dbms_java.get_jdk_version FROM dual;
        return information;
    }

    @Override
    public List<String> listClassNames(String schemaName) {
        DBSchema schema = getSchema(schemaName);

        List<DBJavaClass> classes = schema.getJavaClasses();
        return getObjectNames(classes, false);
    }

    @Override
    public List<String> listResourceNames(String schemaName) {
        DBSchema schema = getSchema(schemaName);

        List<DBJavaResource> resources = schema.getJavaResources();
        return getObjectNames(resources, false);
    }
}

