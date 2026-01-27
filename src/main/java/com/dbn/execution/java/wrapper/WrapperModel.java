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

package com.dbn.execution.java.wrapper;

import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.execution.java.wrapper.model.ClassWrapper;
import com.dbn.execution.java.wrapper.model.FieldWrapper;
import com.dbn.execution.java.wrapper.model.MethodWrapper;
import com.dbn.execution.java.wrapper.naming.WrapperNamingProvider;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.DBPackage;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Getter
@Setter
public class WrapperModel implements DatabaseContextBase {
    private WrapperContext context;

	private DBObjectRef<DBJavaClass> javaWrapperClass;
	private DBObjectRef<DBPackage> sqlWrapperPackage;
	private DBObjectRef<DBMethod> sqlWrapperMethod;

	private List<MethodWrapper> methods = new ArrayList<>();
    private List<ClassWrapper> classes = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private String signature;

    public WrapperModel(WrapperContext context) {
        this.context = context;
        WrapperModelInput input = context.getInput();

        if (input.isClassLevel()) {
            DBJavaClass javaClass = input.getJavaClass();
            initWrapperNames(javaClass);
        } else {
            DBJavaMethod javaMethod = input.getTargetMethod();
            initWrapperNames(javaMethod);

        }
    }

	private void initWrapperNames(DBObject sourceObject) {
		WrapperNamingProvider namingProvider = context.getNamingProvider();
		String javaWrapperName = namingProvider.getJavaWrapperName(sourceObject);
        DBSchema schema = getJavaClass().getSchema();
        DBObjectRef<DBSchema> schemaRef = schema.ref();


        this.javaWrapperClass = new DBObjectRef<>(schemaRef, DBObjectType.JAVA_CLASS, javaWrapperName);

		String sqlWrapperName = namingProvider.getSqlWrapperName(sourceObject);
		if (sourceObject instanceof DBJavaClass) {
			sqlWrapperPackage = new DBObjectRef<>(schemaRef, DBObjectType.PACKAGE, sqlWrapperName);
		} else if (sourceObject instanceof DBJavaMethod javaMethod) {
            DBObjectType methodType = javaMethod.isReturningVoid() ?
					DBObjectType.PROCEDURE :
					DBObjectType.FUNCTION;

			sqlWrapperMethod = new DBObjectRef<>(schemaRef, methodType, sqlWrapperName);
		}
	}

    public boolean matchesSignature(String signature) {
        return Objects.equals(this.signature, signature);
    }

    public WrapperModelInput getInput() {
        return context.getInput();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return getJavaClass().ensureConnection();
    }

    private DBJavaClass getJavaClass() {
        return context.getInput().getJavaClass();
    }

    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }

    @Override
    public SchemaId getSchemaId() {
        return getJavaClass().getSchemaId();
    }

	public String getClassName() {
		return getJavaClass().getCanonicalName();
	}

	public void addClassWrapper(ClassWrapper classWrapper) {
        classes.add(classWrapper);
    }

	public ClassWrapper getClassWrapper(String className, int arrayDepth) {
		return Lists.first(classes, c -> c.matches(className, arrayDepth));
	}

	public ClassWrapper getFieldClassWrapper(FieldWrapper fieldWrapper) {
		String className = fieldWrapper.getTypeClassName();
		int arrayDepth = fieldWrapper.getArrayDepth();
		return getClassWrapper(className, arrayDepth);
	}

	public void addJavaMethod(MethodWrapper javaMethod) {
		methods.add(javaMethod);
	}

	public <T extends DBObject> T getSourceObject() {
		return getInput().getSourceObject();
	}

	public Object isClassWrapper() {
		return getInput().isClassLevel();
	}

	public String getJavaWrapperName() {
		return javaWrapperClass.getObjectName();
	}

	public String getSqlWrapperName() {
		return sqlWrapperMethod != null ? sqlWrapperMethod.getObjectName() : sqlWrapperPackage.getObjectName();
	}

	public Set<String> getSqlTypeNames() {
        return classes.stream().map(c -> c.getSqlTypeName()).collect(Collectors.toSet());
	}

    public Set<String> getSqlMethodNames() {
        return methods.stream().map(m -> m.getSqlMethodName()).collect(Collectors.toSet());
    }

	public List<DBObjectRef> getWrapperObjects() {
		List<DBObjectRef> wrapperObjects = new ArrayList<>();
		wrapperObjects.add(javaWrapperClass);
		wrapperObjects.add(sqlWrapperMethod);
		wrapperObjects.add(sqlWrapperPackage);
		classes.forEach(c -> wrapperObjects.add(c.getSqlType()));
		return Lists.removeNulls(wrapperObjects);
	}

    public List<DBObjectRef<DBMethod>> getWrapperMethods() {
        return Lists.convert(methods, m -> m.getSqlMethod());
    }

    public int getMaxIdentifierLength() {
        return getInput().getMaxIdentifierLength();
    }

    public boolean verifyIdentifierLengths() {
        if (exceedsMaxIdentifierLength(getSqlWrapperName())) return false;
        if (exceedsMaxIdentifierLength(getJavaWrapperName())) return false;

        for (String typeName : getSqlTypeNames()) {
            if (exceedsMaxIdentifierLength(typeName)) return false;
        }

        for (String methodName : getSqlMethodNames()) {
            if (exceedsMaxIdentifierLength(methodName)) return false;
        }

        return true;
    }

    private boolean exceedsMaxIdentifierLength(String identifier) {
        return identifier != null && identifier.length() > getMaxIdentifierLength();
    }

    public void addError(String message) {
        errors.add(message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean isCodeInput(String parameterName) {
        return getInput().getCodeInputs().containsKey(parameterName);
    }
}