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

package com.dbn.sync.java.upload;

import com.dbn.common.project.ProjectRef;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.util.Lists.filter;

@Getter
@Setter
public class JavaUploadInput {

    private VirtualFile javaClass;
    private final ProjectRef project;

    private ConnectionHandler connection;
    private String schemaName;

    private List<String> dependentObjects;
    private final List<JavaUploadElement> uploadElements = new ArrayList<>();

    public JavaUploadInput(Project project, VirtualFile javaClass, List<JavaUploadElement> dependencies) {
        this.javaClass = javaClass;
        this.project = ProjectRef.of(project);

        // add self
        JavaUploadElement sourceElement = new JavaUploadElement(project, javaClass, null);
		sourceElement.setEnabled(false);
		this.uploadElements.add(sourceElement);

        this.uploadElements.addAll(dependencies);
	}

    public JavaUploadInput(Project project, List<VirtualFile> javaClass, List<JavaUploadElement> dependencies) {
        this.javaClass = javaClass.get(0);
        this.project = ProjectRef.of(project);

        Set<JavaUploadElement> uniqueClasses = new HashSet<>();
        // add self
        for(VirtualFile sourceClass : javaClass) {
            JavaUploadElement sourceElement = new JavaUploadElement(project, sourceClass, null);
            sourceElement.setEnabled(false);
            uniqueClasses.add(sourceElement);
        }

        // Create set from dependencies to remove already existing class
        uniqueClasses.addAll(dependencies);

        this.uploadElements.addAll(uniqueClasses);
    }

    public List<JavaUploadElement> getSelectedUploadElements() {
        return filter(uploadElements, e -> e.isSelected());
    }

    public Project getProject() {
        return ProjectRef.ensure(project);
    }

    public VirtualFile getDatabaseContext() {
        return javaClass;
    }

}

