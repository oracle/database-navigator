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

import com.dbn.execution.java.wrapper.WrapperBuilder.ComplexTypeKey;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Setter
public class Wrapper {
    private List<JavaComplexType> argumentJavaComplexTypes = new ArrayList<>();
    private String wrappedJavaMethodName;

    private String fullyQualifiedClassName;
    private List<MethodAttribute> methodArguments = new ArrayList<>();
    private MethodAttribute returnType;
	private String javaMethodSignature;
	private Map<WrapperBuilder.ComplexTypeKey, Integer> sqlTypeIndexes = new HashMap<>();

	public void addArgumentJavaComplexType(JavaComplexType argumentJavaComplexType) {
        argumentJavaComplexTypes.add(argumentJavaComplexType);
    }

	public void addMethodArgument(MethodAttribute argumentParameterType) {
        methodArguments.add(argumentParameterType);
    }

    @Getter
    @Setter
    public static class MethodAttribute {
		private String javaTypeName;         // Java type name
		private String sqlTypeName;          // Java type name
		private boolean complexType;
        private short arrayDepth = 0;

		public boolean isArray() {
			return arrayDepth > 0;
		}

		public String getSqlDeclarationSuffix() {
			SqlType sqlType = TypeMappings.getSqlType(javaTypeName);
			return sqlType == null ? "" : sqlType.getDeclarationSuffix();
		}
	}

	public int getSqlTypeIndex(String className, short arrayDepth){
		ComplexTypeKey key = new ComplexTypeKey(className, arrayDepth);
		int size = sqlTypeIndexes.size();
		return sqlTypeIndexes.computeIfAbsent(key, k -> size + 1);
	}


	public String getJavaSignature(boolean includeArgumentNames){

		AtomicInteger idx = new AtomicInteger(0);
		return this.getMethodArguments()
				.stream()
				.map(e -> (
						e.isArray() ? "java.sql.Array" : e.isComplexType() ? "java.sql.Struct" : e.getJavaTypeName())
						+ (includeArgumentNames ? " arg" + idx.getAndIncrement(): "")
				)
				.collect(Collectors.joining(", "));
	}
}