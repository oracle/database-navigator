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

package com.dbn.generator.code.java.ui;

import com.dbn.common.file.VirtualFilePresentable;
import com.dbn.common.project.ModulePresentable;
import com.dbn.common.state.StateHolder;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.generator.code.CodeGeneratorCategory;
import com.dbn.generator.code.CodeGeneratorManager;
import com.dbn.generator.code.CodeGeneratorType;
import com.dbn.generator.code.java.JavaCodeGeneratorInput;
import com.dbn.generator.code.shared.ui.CodeGeneratorInputDialog;
import com.dbn.generator.code.shared.ui.CodeGeneratorInputForm;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Set;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.initSelectionListener;
import static com.dbn.common.util.Java.isValidClassName;
import static com.dbn.common.util.Java.isValidPackageName;
import static com.dbn.common.util.Strings.isNotEmpty;

public class JavaCodeGeneratorInputForm<I extends JavaCodeGeneratorInput> extends CodeGeneratorInputForm<I> {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JPanel targetLocationPanel;
    private JComboBox<ModulePresentable> moduleComboBox;
    private JComboBox<VirtualFilePresentable> contentRootComboBox;
    private JTextField packageTextField;
    private JTextField classNameTextField;

    public JavaCodeGeneratorInputForm(CodeGeneratorInputDialog dialog, I input) {
        super(dialog, input);

        DatabaseContext databaseContext = dialog.getContext().getDatabaseContext();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, databaseContext);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        classNameTextField.setText(getGeneratorType().getFileName());

        initSelectionListener(moduleComboBox, s -> initContentRoots());
        initModules();
    }

    protected void initValidation() {
        formValidator.addTextValidation(packageTextField, p -> isValidPackageName(p), "Please enter a valid package name");
        formValidator.addTextValidation(classNameTextField, p -> isNotEmpty(p), "Please enter a class name");
        formValidator.addTextValidation(classNameTextField, p -> isValidClassName(p), "Please enter a valid class name");
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    protected void initStatePersistence() {
        Project project = ensureProject();
        CodeGeneratorCategory generatorCategory = getGeneratorCategory();
        CodeGeneratorManager codeGeneratorManager = CodeGeneratorManager.getInstance(project);

        StateHolder state = codeGeneratorManager.getState(generatorCategory);

        initPersistence(moduleComboBox, state, "module-selection");
        initPersistence(contentRootComboBox, state, "content-root-selection");
        initPersistence(packageTextField, state, "package-selection");
    }

    private void initModules() {
        Project project = ensureProject();
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] modules = moduleManager.getSortedModules();

        List<ModulePresentable> presentableModules = ModulePresentable.fromModules(modules);
        initComboBox(moduleComboBox, presentableModules);
    }

    private void initContentRoots() {
        Module module = getSelectedModule();
        if (module == null) {
            ComboBoxes.initComboBox(contentRootComboBox);
        } else {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            Set<JavaSourceRootType> javaSourceRootTypes = Set.of(JavaSourceRootType.SOURCE, JavaSourceRootType.TEST_SOURCE);
            List<VirtualFile> sourceRoots = moduleRootManager.getSourceRoots(javaSourceRootTypes);

            List<VirtualFilePresentable> presentableFiles = VirtualFilePresentable.fromFiles(sourceRoots);
            ComboBoxes.initComboBox(contentRootComboBox, presentableFiles);
        }
    }

    protected void applyUserInput(I input) {
        input.setModuleName(getSelectedModuleName());
        input.setContentRoot(getSelectedContentPath());
        input.setPackageName(getPackageName());
        input.setClassName(getClassName());
    }

    @Nullable
    private Module getSelectedModule() {
        ModulePresentable presentable = getSelection(moduleComboBox);
        return presentable == null ? null : presentable.getModule();
    }

    @Nullable
    private String getSelectedModuleName() {
        Module module = getSelectedModule();
        return module == null ? null : module.getName();
    }

    @Nullable
    private VirtualFile getSelectedContentRoot() {
        VirtualFilePresentable presentable = getSelection(contentRootComboBox);
        return presentable == null ? null : presentable.getFile();
    }

    private String getSelectedContentPath() {
        VirtualFile selectedContentRoot = getSelectedContentRoot();
        return selectedContentRoot == null ? null : selectedContentRoot.getPath();
    }

    @NotNull
    private String getPackageName() {
        return packageTextField.getText().trim();
    }

    private String getClassName() {
        return classNameTextField.getText().trim();
    }

    private CodeGeneratorCategory getGeneratorCategory() {
        CodeGeneratorType generatorType = getGeneratorType();
        return generatorType.getCategory();
    }

    private CodeGeneratorType getGeneratorType() {
        CodeGeneratorInputDialog dialog = ensureParentComponent();
        return dialog.getContext().getType();
    }
}
