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

package com.dbn.execution.statement.variables;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.file.FileMappings;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Strings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.newElement;

public class StatementExecutionVariables extends StatefulDisposableBase implements PersistentStateElement {
    private final ProjectRef project;
    private final FileMappings<Set<StatementExecutionVariable>> variables;

    public StatementExecutionVariables(Project project) {
        this.project = ProjectRef.of(project);
        variables = new FileMappings<>(project, this);
    }

    public Project getProject() {
        return project.ensure();
    }

    public Set<StatementExecutionVariable> getVariables(@Nullable VirtualFile virtualFile) {
        if (virtualFile != null) {
            String fileUrl = virtualFile.getUrl();
            return variables.computeIfAbsent(fileUrl, u -> new LinkedHashSet<>());
        }
        return Collections.emptySet();
    }

    public void cacheVariable(@Nullable VirtualFile virtualFile, StatementExecutionVariable executionVariable) {
        if (virtualFile == null) return;

        Set<StatementExecutionVariable> variables = getVariables(virtualFile);
        for (StatementExecutionVariable variable : variables) {
            if (Objects.equals(variable.getName(), executionVariable.getName())) {
                variable.setValue(executionVariable.getValue());
                return;
            }
        }
        variables.add(new StatementExecutionVariable(executionVariable));
    }

    @Nullable
    public StatementExecutionVariable getVariable(@Nullable VirtualFile virtualFile, String name) {
        if (virtualFile == null) return null;

        name = VariableNames.adjust(name);
        Set<StatementExecutionVariable> variables = getVariables(virtualFile);
        for (StatementExecutionVariable variable : variables) {
            if (Strings.equals(variable.getName(), name)) {
                return variable;
            }
        }
        return null;
    }

    /*********************************************
     *            PersistentStateElement         *
     *********************************************/

    @Override
    public void readState(Element element) {
        Element variablesElement = element.getChild("execution-variables");
        if (variablesElement == null) return;

        this.variables.clear();
        for (Element fileElement : variablesElement.getChildren()) {
            String fileUrl = fileElement.getAttributeValue("file-url");
            if ( Strings.isEmpty(fileUrl)) {
                // TODO backward compatibility. Do cleanup
                fileUrl = fileElement.getAttributeValue("path");
            }

            Set<StatementExecutionVariable> fileVariables = new LinkedHashSet<>();
            this.variables.put(fileUrl, fileVariables);

            for (Element child : fileElement.getChildren()) {
                StatementExecutionVariable executionVariable = new StatementExecutionVariable();
                executionVariable.readState(child);
                fileVariables.add(executionVariable);
            }
        }
    }

    @Override
    public void writeState(Element element) {
        Element variablesElement = newElement(element, "execution-variables");

        for (String fileUrl : variables.fileUrls()) {
            Element fileElement = newElement(variablesElement, "file");
            fileElement.setAttribute("file-url", fileUrl);
            Set<StatementExecutionVariable> fileVariables = variables.get(fileUrl);
            for (StatementExecutionVariable executionVariable : fileVariables) {
                Element variableElement = newElement(fileElement, "variable");
                executionVariable.writeState(variableElement);
            }
        }
    }

    @Override
    public void disposeInner() {
    }
}
