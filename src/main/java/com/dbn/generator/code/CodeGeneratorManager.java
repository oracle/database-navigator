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

package com.dbn.generator.code;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.outcome.MessageOutcomeHandler;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.generator.code.shared.CodeGenerator;
import com.dbn.generator.code.shared.CodeGeneratorInput;
import com.dbn.generator.code.shared.CodeGeneratorResult;
import com.dbn.generator.code.shared.ui.CodeGeneratorInputDialog;
import com.dbn.generator.code.shared.ui.CodeGeneratorInputForm;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.util.Editors.openFileEditor;
import static com.dbn.generator.code.CodeGeneratorManager.COMPONENT_NAME;

@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class CodeGeneratorManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.CodeGeneratorManager";

    private final Map<CodeGeneratorCategory, CodeGeneratorState> states = new ConcurrentHashMap<>();

    private CodeGeneratorManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static CodeGeneratorManager getInstance(@NotNull Project project) {
        return projectService(project, CodeGeneratorManager.class);
    }

    public void openCodeGenerator(CodeGeneratorType type, DatabaseContext databaseContext) {
        CodeGeneratorContext context = createContext(type, databaseContext);
        Dialogs.show(() -> new CodeGeneratorInputDialog(context));
    }

    @NotNull
    private CodeGeneratorContext createContext(CodeGeneratorType type, DatabaseContext databaseContext) {
        Project project = getProject();

        // create and initialize context
        CodeGeneratorContext context = new CodeGeneratorContext(type, databaseContext);
        context.addOutcomeHandler(OutcomeType.FAILURE, MessageOutcomeHandler.get(project));
        context.addOutcomeHandler(OutcomeType.SUCCESS, createFilesOpener(project));

        // create empty input
        CodeGenerator generator = context.getGenerator();
        CodeGeneratorInput input = generator.createInput(databaseContext);
        context.setInput(input);

        return context;
    }

    public CodeGeneratorInputForm createInputForm(CodeGeneratorInputDialog dialog, CodeGeneratorContext context) {
        CodeGeneratorInput input = context.getInput();
        CodeGenerator generator = context.getGenerator();
        return generator.createInputForm(dialog, input);
    }

    public void generateCode(CodeGeneratorContext context) {
        CodeGenerator generator = context.getGenerator();
        generator.generateCode(context);
    }

    @NotNull
    private static OutcomeHandler createFilesOpener(Project project) {
        return (OutcomeHandler.LowPriority) outcome -> {
            CodeGeneratorResult<?> data = outcome.getData();

            List<VirtualFile> generatedFiles = data.getGeneratedFiles();
            for (VirtualFile generatedFile : generatedFiles) {
                openFileEditor(project, generatedFile, true);
            }
        };
    }

    @NotNull
    public CodeGeneratorState getState(CodeGeneratorCategory category) {
        return states.computeIfAbsent(category, k -> new CodeGeneratorState());
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

            CodeGeneratorState state = states.get(category);
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
                CodeGeneratorState state = new CodeGeneratorState();
                state.readState(stateElement);
                states.put(category, state);
            }
        }
    }

}
