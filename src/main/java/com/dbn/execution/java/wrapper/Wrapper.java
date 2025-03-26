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
import com.dbn.execution.java.wrapper.model.ClassWrapper;
import com.dbn.execution.java.wrapper.model.MethodWrapper;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Getter
@Setter
public class Wrapper {
	public static final String DBN_TYPE_SUFFIX = "DBN_OJVM_TYPE_";
	private boolean useFriendlyNames;
	private String className;
	private Set<String> sqlTypeNames = new HashSet<>();
	private List<MethodWrapper> methods = new ArrayList<>();
    private List<ClassWrapper> classes = new ArrayList<>();
	private Map<WrapperBuilder.ComplexTypeKey, Integer> sqlTypeIndexes = new HashMap<>();

	public String getJavaWrapperClassName(){
		if (useFriendlyNames) {
			return toSqlTypeName(className) + "_WRAPPER";
		}
		return "DBN_OJVM_JAVA_WRAPPER";
	}

	public String getSQLWrapperName(){
		if(useFriendlyNames) {
			return toSqlTypeName(className);
		}
		return "DBN_OJVM_SQL_WRAPPER";
	}

	public String getSqlTypeName(String className, short arrayDepth) {
		if (useFriendlyNames) {
			String sqlName = toSqlTypeName(className);
			if (arrayDepth > 0) sqlName += arrayDepth;
			return sqlName;
		}

		return DBN_TYPE_SUFFIX + getSqlTypeIndex(className, arrayDepth);
	}

	private static String toSqlTypeName(String className) {
		return "OJVM_" + className.replace(".", "_").replace("$", "_").toUpperCase();
	}


	public void addJavaComplexType(ClassWrapper classWrapper) {
        classes.add(classWrapper);
    }

	public int getNumberOfJavaComplexTypes() {return classes.size();}

	public void addJavaMethod(MethodWrapper javaMethod) {
		methods.add(javaMethod);
	}

	public int getSqlTypeIndex(String className, short arrayDepth){
		ComplexTypeKey key = new ComplexTypeKey(className, arrayDepth);
		int size = sqlTypeIndexes.size();
		return sqlTypeIndexes.computeIfAbsent(key, k -> size + 1);
	}

}