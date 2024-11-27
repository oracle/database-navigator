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

package com.dbn.common.component;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.project.Projects;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class ProjectComponentBase extends StatefulDisposableBase implements
        ProjectComponent,
        NotificationSupport {

    private final ProjectRef project;
    private final String componentName;

    protected ProjectComponentBase(@NotNull Project project, String componentName) {
        this.project = ProjectRef.of(project);
        this.componentName = componentName;
        ProjectManagerListener.register(this);
    }

    @NotNull
    public final String getComponentName() {
        return componentName;
    }

    @Override
    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    protected void closeProject(boolean exitApp) {
        if (exitApp) {
            ApplicationManager.getApplication().exit();
        } else {
            Projects.closeProject(getProject());
        }
    }

    @Override
    public void checkDisposed() {
        super.checkDisposed();
        getProject();
    }
}
