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

package com.dbn.object.factory.model;

import com.dbn.object.DBSchema;
import com.dbn.object.type.DBJavaClassType;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;

import static com.dbn.common.util.Java.getQualifiedClassName;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.object.type.DBJavaClassType.EXCEPTION;

@Getter
@Setter
public class DBJavaClassFactoryInput extends DBSchemaObjectFactoryInput {
    private String packageName;
    private String className;
    private DBJavaClassType classType;
    private String extendsSuffix = " ";

    public DBJavaClassFactoryInput(DBSchema schema) {
        super(schema, DBObjectType.JAVA_CLASS);
    }

    public void setClassType(DBJavaClassType classType) {
        this.classType = classType;
        this.extendsSuffix = classType == EXCEPTION ? " extends Exception " : "";
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
        updateObjectName();
    }

    public void setClassName(String className) {
        this.className = className;
        updateObjectName();
    }

    private void updateObjectName() {
        setObjectName(getQualifiedClassName(packageName, className));
    }

    public String getDatabaseObjectName(){
        if (isEmpty(packageName)) return className;

        return packageName.replace(".", "/") + "/" + className;
    }

    public String getTypeIdentifier() {
        return switch (classType) {
            case INTERFACE -> "interface";
            case ANNOTATION -> "@interface";
//            case RECORD -> "record";
            case ENUM -> "enum";
            default -> "class";
        };
    }

    @Override
    public String getObjectDescription() {
        String objectName = "\"" + getObjectPath() + "\"";
        return switch (classType) {
            case INTERFACE -> "java interface " + objectName;
            case ANNOTATION -> "java annotation " + objectName;
            case EXCEPTION -> "java exception " + objectName;
//            case RECORD -> "java record " + objectName;
            case ENUM -> "java enumeration " + objectName;
            default -> "java class " + objectName;
        };
    }
}
