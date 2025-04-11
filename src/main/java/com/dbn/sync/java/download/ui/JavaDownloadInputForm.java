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

package com.dbn.sync.java.download.ui;

import com.dbn.common.file.VirtualFilePresentable;
import com.dbn.common.project.ModulePresentable;
import com.dbn.common.state.StateHolder;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.list.CheckBoxList;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.object.DBJavaClass;
import com.dbn.sync.java.download.JavaDownloadContext;
import com.dbn.sync.java.download.JavaDownloadElement;
import com.dbn.sync.java.download.JavaDownloadInput;
import com.dbn.sync.java.download.JavaDownloadManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Set;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.initSelectionListener;

public class JavaDownloadInputForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JPanel targetLocationPanel;
    private JComboBox<ModulePresentable> moduleComboBox;
    private JComboBox<VirtualFilePresentable> contentRootComboBox;
    private CheckBoxList<JavaDownloadElement> dependenciesCheckBoxList;


    public JavaDownloadInputForm(JavaDownloadInputDialog dialog) {
        super(dialog);
        JavaDownloadInput input = dialog.getContext().getInput();

        DBJavaClass javaClass = input.getJavaClass();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, javaClass);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        initSelectionListener(moduleComboBox, s -> initContentRoots());
        initModules();

        dependenciesCheckBoxList.setElements(input.getDownloadElements());
    }

    JavaDownloadContext getContext() {
        JavaDownloadInputDialog dialog = ensureParentComponent();
        return dialog.getContext();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    protected void initStatePersistence() {
        Project project = ensureProject();
        JavaDownloadManager javaDownloadManager = JavaDownloadManager.getInstance(project);

        StateHolder state = javaDownloadManager.getState("DOWNLOAD");

        initPersistence(moduleComboBox, state, "module-selection");
        initPersistence(contentRootComboBox, state, "content-root-selection");
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

    protected void applyUserInput() {
        JavaDownloadInput input = getContext().getInput();
        input.setModuleName(getSelectedModuleName());
        input.setContentRoot(getSelectedContentPath());
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
}
