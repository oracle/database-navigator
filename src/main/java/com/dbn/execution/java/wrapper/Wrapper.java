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
	private Map<WrapperBuilder.ComplexTypeKey, Integer> complexTypeConversion = new HashMap<>();

	public void addArgumentJavaComplexType(JavaComplexType argumentJavaComplexType) {
        argumentJavaComplexTypes.add(argumentJavaComplexType);
    }

	public void addMethodArgument(MethodAttribute argumentParameterType) {
        methodArguments.add(argumentParameterType);
    }

    @Getter
    @Setter
    public static class MethodAttribute {
		private String typeName;           // Java type name
		private String correspondingSqlTypeName;  // Corresponding SQL type name
        private String converterMethodName;  // Method used for type conversion
        private boolean isArray = false;
		private boolean isComplexType;
        private short arrayDepth = 0;
		private short attributePosition;
	}

	public void addEntryToComplexTypeConversion(WrapperBuilder.ComplexTypeKey key, Integer sequenceNumber)
	{
		complexTypeConversion.put(key, sequenceNumber);
	}

	public String getJavaSignature(boolean includeArgumentNames){

		AtomicInteger idx = new AtomicInteger(0);
		return this.getMethodArguments()
				.stream()
				.map(e -> (
						e.isArray() ? "java.sql.Array" : e.isComplexType() ? "java.sql.Struct" : e.getTypeName())
						+ (includeArgumentNames ? " arg" + idx.getAndIncrement(): "")
				)
				.collect(Collectors.joining(", "));
	}
}