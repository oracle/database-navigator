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

package com.dbn.execution.java.history.ui;

import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;

@Deprecated
public class JavaRefUtil {
	public static DBJavaClass getProgram(DBObjectRef<DBJavaMethod> methodRef) {
		return methodRef.getParentObject(DBObjectType.JAVA_CLASS);
	}

	public static String getProgramName(DBObjectRef<DBJavaMethod> methodRef) {
		DBObjectRef<DBJavaClass> programRef = methodRef.getParentRef(DBObjectType.JAVA_CLASS);
		return programRef == null ? null : programRef.getObjectName();
	}

	public static DBObjectType getProgramObjectType(DBObjectRef<DBJavaMethod> methodRef) {
		DBObjectRef<DBJavaClass> programRef = methodRef.getParentRef(DBObjectType.JAVA_CLASS);
		return programRef == null ? null : programRef.getObjectType();
	}
}