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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.VetoableProjectManagerListener;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.dbn.common.dispose.Checks.isValid;

public interface ProjectManagerListener {
    Project getProject();

    default boolean canCloseProject() {
        return true;
    }

    default void projectClosed() {}

    static void register(ProjectComponent projectComponent) {
        if (projectComponent instanceof ProjectManagerListener) {
            ProjectManagerListener listener = (ProjectManagerListener) projectComponent;
            VetoableProjectManagerListener projectManagerListener = new VetoableProjectManagerListener() {
                @Override
                public boolean canClose(@NotNull Project project) {
                    return !isSupported(project) || listener.canCloseProject();
                }

                @Override
                public void projectClosed(@NotNull Project project) {
                    if (isSupported(project)) listener.projectClosed();
                }

                private boolean isSupported(@NotNull Project project) {
                    return isValid(project) && !project.isDefault() && Objects.equals(project, listener.getProject());
                }

                @Override
                public String toString() {
                    return projectComponent + "#" + ProjectManagerListener.class.getSimpleName();
                }
            };

            ProjectManager projectManager = ProjectManager.getInstance();

            Project project = projectComponent.getProject();
            projectManager.addProjectManagerListener(project, projectManagerListener);
            //projectManager.addProjectManagerListener(projectManagerListener);
        }
    }
}
