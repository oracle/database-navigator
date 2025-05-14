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

package com.dbn.sync.java.download;

import com.dbn.DatabaseNavigator;
import com.dbn.batch.BatchManager;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.data.Data;
import com.dbn.common.state.GenericStateHolder;
import com.dbn.common.state.StateHolder;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaEntity;
import com.dbn.object.DBJavaResource;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.sync.java.download.ui.JavaDownloadInputDialog;
import com.dbn.sync.java.download.ui.JavaDownloadResultDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.sync.java.download.JavaDownloadManager.COMPONENT_NAME;
import static com.dbn.sync.java.download.JavaDownloadUtil.prepareDestinationFolders;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class JavaDownloadManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.JavaDownloadManager";

	private final Map<String, GenericStateHolder> states = new ConcurrentHashMap<>();

	private JavaDownloadManager(Project project) {
		super(project, COMPONENT_NAME);
	}

	public static JavaDownloadManager getInstance(@NotNull Project project) {
		return projectService(project, JavaDownloadManager.class);
	}

	public void openCodeDownloader(DBObject sourceObject) {
		ConnectionHandler connection = sourceObject.getConnection();
		ConnectionAction.invoke(null, true, connection, a -> {
			Progress.prompt(getProject(), connection, true,
					"Preparing Java Download",
					"Loading java dependencies for " + sourceObject.getQualifiedNameWithType() + "...",
					progress -> prepareDownloadDialog(sourceObject, null));
		});
	}

	public void openResourceDownloader(DBObject sourceObject, DBObjectList sourceObjectList) {
		ConnectionHandler connection = sourceObject.getConnection();
		ConnectionAction.invoke(null, true, connection, a -> {
			Progress.prompt(getProject(), connection, true,
					"Preparing Java Resource Download",
					null,
					progress -> prepareDownloadDialog(sourceObject, sourceObjectList));
		});
	}

	private void prepareDownloadDialog(DBObject sourceObject, DBObjectList sourceObjectList) {
		Project project = getProject();
		try {
			List<JavaDownloadTask> tasks = createDownloadTasks(sourceObject, sourceObjectList);
			JavaDownloadInput input = new JavaDownloadInput(project, sourceObject, tasks);
			JavaDownloadBatch batch = new JavaDownloadBatch(input);

			Dialogs.show(() -> new JavaDownloadInputDialog(batch));
		} catch (SQLException e) {
			Messages.showErrorDialog(project,
					"Error Loading Java Dependencies",
					"Failed to load dependencies for " + sourceObject.getQualifiedNameWithType(), e);
		}
	}

	private List<JavaDownloadTask> createDownloadTasks(DBObject sourceObject, DBObjectList sourceObjectList) throws SQLException {
		ConnectionHandler connection = sourceObject.getConnection();
		return DatabaseInterfaceInvoker.load(HIGH,
				"Loading Java Dependencies",
				"Loading java dependencies for " + sourceObject.getQualifiedNameWithType() + "...",
				connection.getProject(),
				connection.getConnectionId(),
				c -> createDownloadTasks(connection, sourceObject, sourceObjectList, c));
	}

	private List<JavaDownloadTask> createDownloadTasks(ConnectionHandler connection, DBObject sourceObject, DBObjectList sourceObjectList, DBNConnection conn) throws SQLException {
		List<JavaDownloadTask> tasks = new ArrayList<>();

		ResultSet resultSet = null;
		try {
			DatabaseMetadataInterface metadata = connection.getMetadataInterface();
			if (sourceObject instanceof DBSchema) {
				DBSchema schema = (DBSchema) sourceObject;
				if(sourceObjectList == null) {
					String schemaName = schema.getName();
					resultSet = metadata.loadAllJavaClassDependencies(schemaName, conn);
				} else {
					// java resource in the list
					// TODO dangerous cast assumption
					for(Object object : sourceObjectList.getObjects()) {
						DBJavaResource javaResource = (DBJavaResource) object;
						JavaDownloadTask downloadElement = new JavaDownloadTask(javaResource);
						downloadElement.setEnabled(true);
						downloadElement.setSelected(true); // select by default if sources are available
						tasks.add(downloadElement);
					}
				}

			} else if (sourceObject instanceof DBJavaClass) {
				DBJavaClass javaClass = (DBJavaClass) sourceObject;
				JavaDownloadTask task = new JavaDownloadTask(javaClass);
				task.setEnabled(true);
				task.setSelected(true);
				tasks.add(task);

				String schemaName = javaClass.getSchemaName();
				String className = javaClass.getName();
				resultSet = metadata.loadJavaClassDependencies(schemaName, className, conn);

			} else if (sourceObject instanceof DBJavaResource) {
				DBJavaResource javaResource = (DBJavaResource) sourceObject;
				JavaDownloadTask task = new JavaDownloadTask(javaResource);
				task.setEnabled(true);
				task.setSelected(true);
				tasks.add(task);
			}

			List<JavaDownloadTask> dependencyTasks = createDownloadDependencyTasks(connection, resultSet);
			tasks.addAll(dependencyTasks);
		} finally {
			Resources.close(resultSet);
		}

		return tasks;
	}

	private static List<JavaDownloadTask> createDownloadDependencyTasks(ConnectionHandler connection, ResultSet resultSet) throws SQLException {
		List<JavaDownloadTask> tasks = new ArrayList<>();
		while (resultSet != null && resultSet.next()) {
			String objectOwner = resultSet.getString("OBJECT_OWNER");
			String objectName = resultSet.getString("OBJECT_NAME");
			boolean hasSource = Data.asBooleanPrimitive(resultSet.getString("HAS_SOURCE"));

			DBSchema schema = connection.getObjectBundle().getSchema(objectOwner);
			DBObjectRef<DBJavaEntity> dependencyClass = new DBObjectRef<>(schema.ref(), DBObjectType.JAVA_CLASS, objectName);

			JavaDownloadTask task = new JavaDownloadTask(dependencyClass);
			task.setEnabled(hasSource);
			task.setSelected(hasSource);
			tasks.add(task);
		}
		return tasks;
	}


	public void startDownload(JavaDownloadBatch batch) {
		try {
			// prepare destination folders
			prepareDestinationFolders(batch);

			BatchManager batchManager = BatchManager.getInstance(getProject());
			batchManager.startBatchProcess(batch);
		} catch (Exception e) {
			Project project = batch.getProject();
			Messages.showErrorDialog(project,
					"Java Download Failed",
					"Failed to prepare destination folders", e);
		}
	}

	private void openBatchResult(JavaDownloadBatch batch) {
		Dialogs.show(() -> new JavaDownloadResultDialog(batch));
	}

	@NotNull
	public StateHolder getState(String category) {
		return states.computeIfAbsent(category, k -> new GenericStateHolder());
	}

	/****************************************
	 *       PersistentStateComponent       *
	 *****************************************/
	@Nullable
	@Override
	public Element getComponentState() {
		Element element = newStateElement();
		Element statesElement = newElement(element, "downloader-states");
		for (String category : states.keySet()) {
			Element stateElement = newElement(statesElement, "state");
			setStringAttribute(stateElement, "category", category);

			GenericStateHolder state = states.get(category);
			state.writeState(stateElement);
		}
		return element;
	}

	@Override
	public void loadComponentState(@NotNull Element element) {
		Element statesElement = element.getChild("downloader-states");
		if (statesElement != null) {
			for (Element stateElement : statesElement.getChildren("state")) {
				String category = stringAttribute(stateElement, "category");
				GenericStateHolder state = new GenericStateHolder();
				state.readState(stateElement);
				states.put(category, state);
			}
		}
	}
}
