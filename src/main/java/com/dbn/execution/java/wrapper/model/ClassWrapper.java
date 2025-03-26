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

import com.dbn.execution.java.wrapper.SqlComplexType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ClassWrapper {

	public enum AttributeDirection {ARGUMENT, RETURN, BOTH}

	private String javaClassName;
	private AttributeDirection attributeDirection;
	private SqlComplexType correspondingSqlType;
	private short arrayDepth = 0;
	private List<FieldWrapper> fields = new ArrayList<>();

	private String sqlToJavaConverterName="";
	private String javaToSqlConverterName="";

	private int containedJavaComplexTypeIndex = -1;


	public boolean isArray() {
		return arrayDepth > 0;
	}

	public void setCorrespondingSqlType(SqlComplexType correspondingSqlType) {
		this.correspondingSqlType = correspondingSqlType;
		this.javaToSqlConverterName = correspondingSqlType.getName()+"_TO_SQL";
		this.sqlToJavaConverterName = correspondingSqlType.getName()+"_TO_JAVA";
	}

	// Method to add field (only if not an array)
	public void addField(FieldWrapper fieldWrapper) {
		fields.add(fieldWrapper);
	}

}