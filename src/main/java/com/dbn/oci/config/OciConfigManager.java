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

package com.dbn.oci.config;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.routine.Consumer;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.state.StateContainer;
import com.dbn.common.util.Dialogs;
import com.dbn.oci.config.ui.OciConfigSelectionDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.oci.config.OciConfigManager.COMPONENT_NAME;
import static com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE;

@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class OciConfigManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.OciConfigManager";

    private final StateContainer states = new StateContainer();

    private OciConfigManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static OciConfigManager getInstance(@NotNull Project project) {
        return projectService(project, OciConfigManager.class);
    }

    @NotNull
    public StateAttributes getState(String category) {
        StateCategory stateCategory = StateCategory.get(category);
        return states.ensureAttributes(stateCategory);
    }


    public void openOciConfigSelector(Consumer<OciConfig> consumer) {
        Dialogs.show(() -> new OciConfigSelectionDialog(getProject()), (dialog, exitCode) ->
                when(exitCode == OK_EXIT_CODE, () -> consumer.accept(dialog.getConfig())));
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        states.writeState(element, "config-states");
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        states.readState(element, "config-states");
    }
}
