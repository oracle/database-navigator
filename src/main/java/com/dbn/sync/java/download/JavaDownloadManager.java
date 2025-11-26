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
import com.dbn.batch.DatabaseBatchManager;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.data.Data;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.state.StateContainer;
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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.sync.java.download.JavaDownloadManager.COMPONENT_NAME;
import static com.dbn.sync.java.download.JavaDownloadUtil.prepareDestinationFolders;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class JavaDownloadManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.JavaDownloadManager";
	private final StateContainer states = new StateContainer();


	private JavaDownloadManager(Project project) {
		super(project, COMPONENT_NAME);
	}

	public static JavaDownloadManager getInstance(@NotNull Project project) {
		return projectService(project, JavaDownloadManager.class);
	}

	public void openJavaClassDownloader(DBObject sourceObject) {
		ConnectionHandler connection = sourceObject.getConnection();
		ConnectionAction.invoke(null, true, connection, a -> {
			Progress.prompt(getProject(), connection, true,
					"Preparing Java Download",
					"Loading java dependencies for " + sourceObject.getQualifiedNameWithType() + "...",
					progress -> prepareDownloadDialog(sourceObject, DBObjectType.JAVA_CLASS));
		});
	}

	public void openJavaResourceDownloader(DBObject sourceObject) {
		ConnectionHandler connection = sourceObject.getConnection();
		ConnectionAction.invoke(null, true, connection, a -> {
			Progress.prompt(getProject(), connection, true,
					"Preparing Java Resource Download",
					null,
					progress -> prepareDownloadDialog(sourceObject, DBObjectType.JAVA_RESOURCE));
		});
	}

	private void prepareDownloadDialog(DBObject sourceObject, DBObjectType objectType) {
		Project project = getProject();
		try {
			List<JavaDownloadTask> tasks = createDownloadTasks(sourceObject, objectType);
			JavaDownloadInput input = new JavaDownloadInput(project, sourceObject, tasks);
			JavaDownloadBatch batch = new JavaDownloadBatch(input);

			Dialogs.show(() -> new JavaDownloadInputDialog(batch));
		} catch (SQLException e) {
			Messages.showErrorDialog(project,
					"Error Loading Java Dependencies",
					"Failed to load dependencies for " + sourceObject.getQualifiedNameWithType(), e);
		}
	}

	private List<JavaDownloadTask> createDownloadTasks(DBObject sourceObject, DBObjectType objectType) throws SQLException {
		ConnectionHandler connection = sourceObject.getConnection();
		return DatabaseInterfaceInvoker.load(HIGH,
				"Loading Java Dependencies",
				"Loading java dependencies for " + sourceObject.getQualifiedNameWithType() + "...",
				connection.getProject(),
				connection.getConnectionId(),
				c -> createDownloadTasks(connection, sourceObject, objectType, c));
	}

	private List<JavaDownloadTask> createDownloadTasks(ConnectionHandler connection, DBObject sourceObject, DBObjectType objectType, DBNConnection conn) throws SQLException {
		List<JavaDownloadTask> tasks = new ArrayList<>();

		ResultSet resultSet = null;
		try {
			DatabaseMetadataInterface metadata = connection.getMetadataInterface();
			if (sourceObject instanceof DBSchema) {
				DBSchema schema = (DBSchema) sourceObject;
                DBObjectList<DBObject> childObjectList = schema.getChildObjectList(objectType);
                if (childObjectList == null) return tasks;

                for (Object object : childObjectList.getObjects()) {
                    JavaDownloadTask downloadElement = null;
                    if (object instanceof DBJavaClass) {
                        DBJavaClass javaClass = (DBJavaClass) object;
                        downloadElement = new JavaDownloadTask(javaClass);

                    } else if (object instanceof DBJavaResource) {
                        DBJavaResource javaResource = (DBJavaResource) object;
                        downloadElement = new JavaDownloadTask(javaResource);
                    }
                    if (downloadElement == null) continue;

                    downloadElement.setEnabled(true);
                    downloadElement.setSelected(true);
                    tasks.add(downloadElement);
                }
                return tasks;
			}

            if (sourceObject instanceof DBJavaClass) {
				DBJavaClass javaClass = (DBJavaClass) sourceObject;
				JavaDownloadTask task = new JavaDownloadTask(javaClass);
				task.setEnabled(true);
				task.setSelected(true);
				tasks.add(task);

				String schemaName = javaClass.getSchemaName();
				String className = javaClass.getName();
				resultSet = metadata.loadJavaClassDependencies(schemaName, className, conn);
                List<JavaDownloadTask> dependencyTasks = createDownloadDependencyTasks(connection, resultSet);
                tasks.addAll(dependencyTasks);

			} else if (sourceObject instanceof DBJavaResource) {
				DBJavaResource javaResource = (DBJavaResource) sourceObject;
				JavaDownloadTask task = new JavaDownloadTask(javaResource);
				task.setEnabled(true);
				task.setSelected(true);
				tasks.add(task);
			}
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

			DatabaseBatchManager databaseBatchManager = DatabaseBatchManager.getInstance(getProject());
			databaseBatchManager.startBatchProcess(batch);
		} catch (Exception e) {
			Project project = batch.getProject();
			Messages.showErrorDialog(project,
					"Java Download Failed",
					"Failed to prepare destination folders", e);
		}
	}

	public void openBatchResult(JavaDownloadBatch batch) {
		Dialogs.show(() -> new JavaDownloadResultDialog(batch));
	}

	@NotNull
	public StateAttributes getState(@NonNls String category) {
        StateCategory stateCategory = StateCategory.get(category);
        return states.ensureAttributes(stateCategory);
	}

	/****************************************
	 *       PersistentStateComponent       *
	 *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        states.writeState(element, "download-states");
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        states.readState(element, "download-states");
    }
}
