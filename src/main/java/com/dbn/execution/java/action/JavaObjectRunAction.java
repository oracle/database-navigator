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

package com.dbn.execution.java.action;

import com.dbn.common.icon.Icons;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.execution.java.ui.JavaExecutionHistory;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.action.ObjectListShowAction;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Unsafe.cast;

public class JavaObjectRunAction extends ObjectListShowAction {
	public JavaObjectRunAction(DBJavaClass program) {
		super("Run...", program);
		getTemplatePresentation().setIcon(Icons.METHOD_EXECUTION_RUN);
	}

	@Nullable
	@Override
	public List<DBObject> getRecentObjectList() {
		DBJavaClass program = (DBJavaClass) getSourceObject();
		JavaExecutionManager javaExecutionManager = JavaExecutionManager.getInstance(program.getProject());
		JavaExecutionHistory executionHistory = javaExecutionManager.getExecutionHistory();
		return cast(executionHistory.getRecentlyExecutedMethods(program));
	}


	@Override
	public List<DBObject> getObjectList() {
		DBJavaClass program = (DBJavaClass) getSourceObject();
		List<DBObject> objects = new ArrayList<>();
		objects.addAll(program.getStaticMethods());
		return objects;
	}

	@Override
	public String getTitle() {
		return "Select method to execute";
	}

	@Override
	public String getEmptyListMessage() {
		DBJavaClass program = (DBJavaClass) getSourceObject();
		return "The " + program.getQualifiedNameWithType() + " has no static methods to execute.";
	}


	@Override
	public String getListName() {
		return "executable elements";
	}

	@Override
	protected AnAction createObjectAction(DBObject object) {
		return new JavaRunAction((DBJavaMethod) object, true);
	}
}