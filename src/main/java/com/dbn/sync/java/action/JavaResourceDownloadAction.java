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

package com.dbn.sync.java.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.util.Java;
import com.dbn.object.DBSchema;
import com.dbn.object.action.AnObjectAction;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.sync.java.download.JavaDownloadManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaResourceDownloadAction extends AnObjectAction<DBObject> {

	DBObjectList objectList;
	public JavaResourceDownloadAction(DBObject sourceObject, DBObjectList objectList) {
		super(sourceObject);
		this.objectList = objectList;
	}

	@Override
	protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBObject target) {
		JavaDownloadManager manager = JavaDownloadManager.getInstance(project);
		manager.openResourceDownloader(getTarget(), objectList);
	}

	@Override
	protected void update(
			@NotNull AnActionEvent e,
			@NotNull Presentation presentation,
			@NotNull Project project,
			@Nullable DBObject target) {

		presentation.setText("Download To Project");
		presentation.setIcon(Icons.ACTION_DOWNLOAD);
		presentation.setVisible(isVisible());
	}

	private boolean isVisible() {
		if (!Java.isIdeSupportAvailable()) return false;

		DBObject target = getTarget();
		DBSchema schema = target.getSchema();
		if (schema == null) return false;
		if (schema.isSystemSchema()) return false;

		return true;
	}
}
