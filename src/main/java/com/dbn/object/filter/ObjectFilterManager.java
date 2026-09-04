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

package com.dbn.object.filter;

import com.dbn.DatabaseNavigator;
import com.dbn.browser.options.ObjectFilterChangeListener;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.object.common.DBObject;
import com.dbn.object.filter.custom.ObjectFilter;
import com.dbn.object.filter.custom.ObjectFilterSettings;
import com.dbn.object.filter.custom.ui.ObjectFilterDetailsDialog;
import com.dbn.object.filter.quick.ObjectQuickFilterManager;
import com.dbn.object.type.DBObjectType;
import com.dbn.options.ProjectSettings;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Messages.OPTIONS_YES_NO;
import static com.dbn.common.util.Messages.showQuestionDialog;
import static com.dbn.common.util.Messages.whenOk;
import static com.dbn.nls.NlsResources.txt;

@State(
		name = ObjectFilterManager.COMPONENT_NAME,
		storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
@Getter
public class ObjectFilterManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.ObjectFilterManager";

	private ObjectFilterManager(Project project) {
		super(project, COMPONENT_NAME);
	}

	public static ObjectFilterManager getInstance(@NotNull Project project) {
		return projectService(project, ObjectFilterManager.class);
	}

	public boolean hasObjectFilter(ConnectionId connectionId, DBObjectType objectType) {
		ObjectFilterSettings objectFilterSettings = getObjectFilterSettings(connectionId);
		return objectFilterSettings.hasFilter(objectType);
	}

	public boolean isQuickFilterFeatureActive() {
		ObjectQuickFilterManager quickFilterManager = ObjectQuickFilterManager.getInstance(getProject());
		return quickFilterManager.isFeatureEnabled();
	}

	@Nullable
	public ObjectFilter getObjectFilter(ConnectionId connectionId, DBObjectType objectType) {
		ObjectFilterSettings objectFilterSettings = getObjectFilterSettings(connectionId);
		return objectFilterSettings.getFilter(objectType);
	}

	public void openObjectFilterDialog(ConnectionId connectionId, DBObjectType objectType) {
		ObjectFilterSettings filterSettings = getObjectFilterSettings(connectionId);

		boolean create = !filterSettings.hasFilter(objectType);
		ObjectFilter<DBObject> filter = nvl(
				filterSettings.getFilter(objectType),
				() -> new ObjectFilter<>(filterSettings, objectType));

		Dialogs.show(() -> new ObjectFilterDetailsDialog(filter, create, true),
				(dialog, exitCode) -> when(exitCode == 0, () -> updateFilter(filterSettings, filter)));

	}

	private void updateFilter(ObjectFilterSettings filterSettings, ObjectFilter<DBObject> filter) {
		Project project = getProject();
		ConnectionId connectionId = filterSettings.getConnectionId();
		DBObjectType objectType = filter.getObjectType();
		String listName = objectType.getTitleCasedListDisplayName();


		if (!filter.isActive()) {
			showQuestionDialog(project,
                    txt("msg.objects.title.EnableFilter"),
                    txt("msg.objects.question.EnableFilter", listName),
					OPTIONS_YES_NO, 0, whenOk(() -> filter.setActive(true)));
		}
		filterSettings.addFilter(filter);

		notifyFilterChange(connectionId, objectType);

	}

	public void toggleFilter(ConnectionId connectionId, DBObjectType objectType) {
		ObjectFilter objectFilter = getObjectFilter(connectionId, objectType);
		if(objectFilter == null) return;

		objectFilter.setActive(!objectFilter.isActive());
		notifyFilterChange(connectionId, objectType);
	}

	public void removeFilter(ConnectionId connectionId, DBObjectType objectType) {
		ObjectFilter objectFilter = getObjectFilter(connectionId, objectType);
		if(objectFilter == null) return;

		ObjectFilterSettings filterSettings = getObjectFilterSettings(connectionId);
		filterSettings.deleteFilter(objectType);
		notifyFilterChange(connectionId, objectType);
	}

	private void notifyFilterChange(ConnectionId connectionId, DBObjectType objectType) {
		ProjectEvents.notify(getProject(), ObjectFilterChangeListener.TOPIC,
				(listener) -> listener.nameFiltersChanged(connectionId, objectType));
	}

	private ObjectFilterSettings getObjectFilterSettings(ConnectionId connectionId) {
		ProjectSettings projectSettings = ProjectSettings.get(getProject());
		ConnectionSettings connectionSettings = projectSettings.getConnectionSettings().getConnectionSettings(connectionId);
		return connectionSettings.getFilterSettings().getObjectFilterSettings();
	}

	/****************************************
	 *       PersistentStateComponent       *
	 *****************************************/
	@Nullable
	@Override
	public Element getComponentState() {
		Element element = new Element("state");
		return element;
	}

	@Override
	public void loadComponentState(@NotNull Element element) {
	}
}
