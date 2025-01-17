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
import java.util.List;

@Getter
@Setter
public class JavaComplexType {
	public enum AttributeDirection {ARGUMENT, RETURN, BOTH}

	public enum ArrayType {SQUARE_BRACKET, LIST, OTHER}

	private String typeName;
	private AttributeDirection attributeDirection;
	private SqlComplexType correspondingSqlType;
	private boolean isArray = false;
	private short arrayDepth = 0;
	private ArrayType arrayType;
	private List<Field> fields = new ArrayList<>();

	public void setArrayType(ArrayType arrayType) {
		if (isArray) {
			this.arrayType = arrayType;
		}
	}

	// Method to add field (only if not an array)
	public void addField(Field field) {
			fields.add(field);
	}

	@Getter
	@Setter
	public static class Field {
		// Enum for access modifiers
		public enum AccessModifier {PUBLIC, PROTECTED, PRIVATE, DEFAULT}

		private String name;
		private String type;
		private boolean complexType = false;
		private AccessModifier accessModifier;
		private String setter;
		private String getter;
		private boolean isArray = false;
		private short arrayDepth = 0;
		private short fieldIndex;
		private String sqlType;
		private String typeCastStart;
		private String typeCastEnd;

		public void setType(String type, SqlType sqlTypeDetails) {
			this.type = type;
			if(sqlTypeDetails != null) {
				sqlType = sqlTypeDetails.getSqlTypeName();
				typeCastStart = sqlTypeDetails.getTransformerPrefix();
				typeCastEnd = sqlTypeDetails.getTransformerSuffix();
			}
		}

		public void setAccessModifier(String accessModifier) {
			if (accessModifier == null) {
				this.accessModifier = AccessModifier.DEFAULT;
				return;
			}

			switch (accessModifier.toLowerCase()) {
				case "public":
					this.accessModifier = AccessModifier.PUBLIC;
					break;
				case "protected":
					this.accessModifier = AccessModifier.PROTECTED;
					break;
				case "private":
					this.accessModifier = AccessModifier.PRIVATE;
					break;
				default:
					// Do nothing if the string doesn't match any valid modifier
					break;
			}
		}
	}
}