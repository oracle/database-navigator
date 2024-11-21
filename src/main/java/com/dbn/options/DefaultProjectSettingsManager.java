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

package com.dbn.options;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.common.project.Projects;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.applicationService;

@Getter
@State(
    name = "DBNavigator.DefaultProject.Settings",
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DefaultProjectSettingsManager extends ApplicationComponentBase implements PersistentState {

    private final ProjectSettings projectSettings;

    private DefaultProjectSettingsManager() {
        super("DBNavigator.Application.TemplateProjectSettings");
        projectSettings = new ProjectSettings(Projects.getDefaultProject());
    }

    public static DefaultProjectSettingsManager getInstance() {
        return applicationService(DefaultProjectSettingsManager.class);
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        projectSettings.writeConfiguration(element);
        return element;
    }


    @Override
    public void loadComponentState(@NotNull Element element) {
        projectSettings.readConfiguration(element);
    }
}
