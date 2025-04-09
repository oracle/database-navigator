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

package com.dbn.sync.java;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.outcome.MessageOutcomeHandler;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.state.GenericStateHolder;
import com.dbn.common.state.StateHolder;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.generator.code.CodeGeneratorCategory;
import com.dbn.sync.java.ui.JavaDownloaderInputDialog;
import com.dbn.sync.java.ui.JavaDownloaderInputForm;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.sync.java.JavaDownloaderManager.COMPONENT_NAME;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class JavaDownloaderManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.JavaDownloaderManager";

	private final Map<CodeGeneratorCategory, GenericStateHolder> states = new ConcurrentHashMap<>();

	private JavaDownloaderManager(Project project) {
		super(project, COMPONENT_NAME);
	}

	public static JavaDownloaderManager getInstance(@NotNull Project project) {
		return projectService(project, JavaDownloaderManager.class);
	}

	public void openCodeDownloader(DatabaseContext databaseContext) {
		JavaDownloaderContext context = createContext(databaseContext);
		Dialogs.show(() -> new JavaDownloaderInputDialog(context));
	}


	@NotNull
	private JavaDownloaderContext createContext(DatabaseContext databaseContext) {
		Project project = getProject();

		// create and initialize context
		JavaDownloaderContext context = new JavaDownloaderContext(databaseContext);
		context.addOutcomeHandler(OutcomeType.FAILURE, MessageOutcomeHandler.get(project));
		context.addOutcomeHandler(OutcomeType.SUCCESS, MessageOutcomeHandler.get(project));

		// create empty input
		JavaDownloader downloader = context.getDownloader();
		JavaDownloaderInput input = downloader.createInput(databaseContext);
		context.setInput(input);

		return context;
	}

	public JavaDownloaderInputForm createInputForm(JavaDownloaderInputDialog dialog, JavaDownloaderContext context) {
		JavaDownloaderInput input = context.getInput();
		JavaDownloader downloader = context.getDownloader();
		return downloader.createInputForm(dialog, input);
	}

	public void downloadCode(JavaDownloaderContext context) {
		JavaDownloader donwnloader = context.getDownloader();
		donwnloader.downloadObject(context);
	}

	@NotNull
	public StateHolder getState(CodeGeneratorCategory category) {
		return states.computeIfAbsent(category, k -> new GenericStateHolder());
	}

	/****************************************
	 *       PersistentStateComponent       *
	 *****************************************/
	@Nullable
	@Override
	public Element getComponentState() {
		Element element = newStateElement();
		Element statesElement = newElement(element, "generator-states");
		for (CodeGeneratorCategory category : states.keySet()) {
			Element stateElement = newElement(statesElement, "generator-state");
			setEnumAttribute(stateElement, "category", category);

			GenericStateHolder state = states.get(category);
			state.writeState(stateElement);
		}
		return element;
	}

	@Override
	public void loadComponentState(@NotNull Element element) {
		Element statesElement = element.getChild("generator-states");
		if (statesElement != null) {
			for (Element stateElement : statesElement.getChildren("generator-state")) {
				CodeGeneratorCategory category = enumAttribute(stateElement, "category", CodeGeneratorCategory.class);
				GenericStateHolder state = new GenericStateHolder();
				state.readState(stateElement);
				states.put(category, state);
			}
		}
	}
}
