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

package com.dbn.execution.java.wrapper;

import com.dbn.common.property.PropertyHolder;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.info.ConnectionInfo;
import com.dbn.execution.java.wrapper.support.WrapperSupportData;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class WrapperModelInput implements PropertyHolder<WrapperProperty>{
    private final DBObjectRef sourceObject;
    private final DBObjectRef<DBJavaClass> javaClass;
    private final List<DBObjectRef<DBJavaMethod>> javaMethods;
    private final int maxIdentifierLength;
    private Map<String, String> codeInputs;
    private WrapperSupportData supportData;

    @Delegate
    private final PropertyHolder<WrapperProperty> properties = new WrapperProperties();

    public WrapperModelInput(@NotNull DBJavaMethod targetMethod) {

        this.sourceObject = DBObjectRef.of(targetMethod);

        this.javaClass = DBObjectRef.of(targetMethod.getOwnerClass());
        this.javaMethods = DBObjectRef.from(List.of(targetMethod));
        this.maxIdentifierLength = initMaxIdentifierLength();
    }

    public WrapperModelInput(@Nullable DBJavaClass javaClass, List<DBJavaMethod> methods) {
        this.sourceObject = DBObjectRef.of(javaClass);

        this.javaClass = DBObjectRef.of(javaClass);
        this.javaMethods = DBObjectRef.from(methods);
        this.maxIdentifierLength = initMaxIdentifierLength();
    }

    private int initMaxIdentifierLength() {
        ConnectionInfo connectionInfo = getConnection().getConnectionInfo();
        return connectionInfo == null ? 30 : connectionInfo.getMaxIdentifierLength();
    }

    private ConnectionHandler getConnection() {
        return sourceObject.ensureConnection();
    }

    public boolean isClassLevel() {
        return sourceObject.getObjectType() == DBObjectType.JAVA_CLASS;
    }

    public boolean isMethodLevel() {
        return !isClassLevel();
    }

    public boolean isCompactNaming() {
        return maxIdentifierLength <= 30;
    }

    public <T extends DBObject> T getSourceObject() {
        return cast(sourceObject.ensure());
    }

    @NotNull
    public DBJavaClass getJavaClass() {
        return DBObjectRef.ensure(javaClass);
    }

    public DBJavaMethod getTargetMethod() {
        return javaMethods.size() != 1 ? null : DBObjectRef.ensure(javaMethods.get(0));
    }

    @NotNull
    public List<DBJavaMethod> getJavaMethods() {
        return DBObjectRef.ensure(javaMethods);
    }

    public boolean isTemporary() {
        return is(WrapperProperty.TEMPORARY);
    }

    public boolean isDebugMode() {
        return is(WrapperProperty.DEBUG_MODE);
    }
}
