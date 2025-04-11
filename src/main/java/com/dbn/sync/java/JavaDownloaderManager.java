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

package com.dbn.sync.java;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.data.Data;
import com.dbn.common.state.GenericStateHolder;
import com.dbn.common.state.StateHolder;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.sync.java.ui.JavaDownloaderInputDialog;
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
import static com.dbn.sync.java.JavaDownloaderManager.COMPONENT_NAME;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class JavaDownloaderManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.JavaDownloaderManager";

	private final Map<String, GenericStateHolder> states = new ConcurrentHashMap<>();

	private JavaDownloaderManager(Project project) {
		super(project, COMPONENT_NAME);
	}

	public static JavaDownloaderManager getInstance(@NotNull Project project) {
		return projectService(project, JavaDownloaderManager.class);
	}

	public void openCodeDownloader(DBJavaClass javaClass) {
		ConnectionHandler connection = javaClass.getConnection();
		Progress.prompt(getProject(), connection, true,
				"Preparing class download",
				"Loading dependencies of java class \"" + javaClass.getCanonicalName() + "\"",
				progress -> prepareDownloadDialog(javaClass));

	}

	private void prepareDownloadDialog(DBJavaClass javaClass) {
		try {
			List<JavaDownloadElement> dependencies = loadDownloadDependencies(javaClass);

			JavaDownloaderInput input = new JavaDownloaderInput(javaClass, dependencies);
			JavaDownloaderContext context = new JavaDownloaderContext(input);

			Dialogs.show(() -> new JavaDownloaderInputDialog(context));

		} catch (SQLException e) {
			Messages.showErrorDialog(getProject(),
					"Error Loading Dependencies",
					"Failed to load dependencies of class " + javaClass.getCanonicalName(), e);
		}
	}

	private List<JavaDownloadElement> loadDownloadDependencies(DBJavaClass javaClass) throws SQLException {
		ConnectionHandler connection = javaClass.getConnection();
		return DatabaseInterfaceInvoker.load(HIGH,
				"Downloading dependencies",
				"Downloading dependencies for object ",
				connection.getProject(),
				connection.getConnectionId(),
				c -> loadObjectDependencies(connection, javaClass, c));
	}

	private List<JavaDownloadElement> loadObjectDependencies(ConnectionHandler connection, DBJavaClass javaClass, DBNConnection conn) throws SQLException {
		List<JavaDownloadElement> dependencies = new ArrayList<>();

		ResultSet resultSet = null;
		try {
			DatabaseMetadataInterface metadata = connection.getMetadataInterface();
			resultSet = metadata.loadJavaObjectDependencies(javaClass.getSchemaName(), javaClass.getName(), conn);
			while (resultSet != null && resultSet.next()) {
				String objectOwner = resultSet.getString("OBJECT_OWNER");
				String objectName = resultSet.getString("OBJECT_NAME");
				boolean hasSource = Data.asBooleanPrimitive(resultSet.getString("HAS_SOURCE"));

				DBSchema schema = connection.getObjectBundle().getSchema(objectOwner);
				DBObjectRef<DBJavaClass> dependencyClass = new DBObjectRef<>(schema.ref(), DBObjectType.JAVA_CLASS, objectName);

				JavaDownloadElement downloadElement = new JavaDownloadElement(dependencyClass);
				downloadElement.setEnabled(hasSource);
				downloadElement.setSelected(hasSource); // select by default if sources are available
				dependencies.add(downloadElement);
			}
		} finally {
			Resources.close(resultSet);
		}

		return dependencies;
	}


	public void performDownload(JavaDownloaderContext context) {
		JavaDownloaderInput input = context.getInput();
		DBJavaClass javaClass = input.getJavaClass();
		Progress.prompt(getProject(), context.getDatabaseContext(), true,
				"Downloading classes",
				"Creating project classes and dependencies from java class \"" + javaClass.getCanonicalName() + "\"",
				progress -> JavaDownloader.INSTANCE.downloadJavaClasses(context));

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
