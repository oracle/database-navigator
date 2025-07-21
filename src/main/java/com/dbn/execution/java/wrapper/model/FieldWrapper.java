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

package com.dbn.execution.java.wrapper.model;

import com.dbn.common.util.Naming;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBJavaAccessibility;
import lombok.Getter;
import lombok.Setter;

import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;

@Getter
@Setter
public class FieldWrapper extends EntityWrapper {
    private final ClassWrapper ownerClassWrapper;
    private final DBObjectRef<DBJavaField> field;
    private final DBObjectRef<DBJavaClass> typeClass;
    private final DBObjectRef<DBJavaMethod> fieldGetter;
    private final DBObjectRef<DBJavaMethod> fieldSetter;

    private DBObjectRef<DBTypeAttribute> attribute;
    private DBObjectRef<DBType> attributeType;

    private final int index;
    private final int arrayDepth;
    private final boolean accessible;

    private boolean complexType = false;
    private String sqlTypeName;
    private String typeCastStart;
    private String typeCastEnd;

    public FieldWrapper(ClassWrapper parentClassWrapper, DBJavaField field) {
        super(parentClassWrapper.getModel());
        this.ownerClassWrapper = parentClassWrapper;

        this.field = DBObjectRef.of(field);
        this.index = field.getPosition();
        this.arrayDepth = field.getArrayDepth();
        this.accessible = field.getAccessibility() == DBJavaAccessibility.PUBLIC;
        this.typeClass = field.getJavaClassRef();

        // If the javaField is non-public, set up the getter/setter if present
        fieldGetter = this.accessible ? null : DBObjectRef.of(field.findGetterMethod());
        fieldSetter = this.accessible ? null : DBObjectRef.of(field.findSetterMethod());
    }

    public String getName() {
        return field.getObjectName();
    }

    public boolean isArray() {
        return arrayDepth > 0;
    }

    public String getSqlName() {
        return Naming.toUpperSnakeCase(getName());
    }

    public String getTypeClassName() {
        return getCanonicalName(typeClass);
    }

    public String getSqlAttributeDeclaration() {
        return getSqlName() + " " + getSqlTypeName();
    }

    public String getGetterName() {
        DBJavaMethod method = DBObjectRef.get(fieldGetter);
        return method == null ? null : method.getSimpleName();
    }

    public String getSetterName() {
        DBJavaMethod method = DBObjectRef.get(fieldSetter);
        return method == null ? null : method.getSimpleName();
    }
}
