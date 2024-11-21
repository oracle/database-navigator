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

package com.dbn.status;

import com.dbn.common.component.ProjectComponentBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import org.jetbrains.annotations.NotNull;

public class ConnectionLoadStatusBarWidget extends ProjectComponentBase implements StatusBarWidget{

    public static final String COMPONENT_NAME = "DBNavigator.Project.ConnectionLoadStatus";

    public ConnectionLoadStatusBarWidget(Project project) {
        super(project, COMPONENT_NAME);
    }

    @NotNull
    @Override
    public String ID() {
        return "DBNavigator.ConnectionLoadStatus";
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {

    }

    @Override
    public void dispose() {
        if (isDisposed()) return;
        setDisposed(true);

        disposeInner();
    }
}
