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

package com.dbn.options.action;

import com.dbn.options.ConfigId;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ProjectSettingsActionGroup extends ActionGroup {
    private ConfigId[] configIds;
    public ProjectSettingsActionGroup(ConfigId ... configIds) {
        super("Settings", true);
        this.configIds = configIds;
    }

    @NotNull
    @Override
    public AnAction[] getChildren(AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();
        for (ConfigId configId : configIds) {
            actions.add(new ProjectSettingsAction(configId));
        }
        return actions.toArray(new AnAction[0]);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        //e.getPresentation().setIcon(Icons.ACTION_SETTINGS);
    }

}
