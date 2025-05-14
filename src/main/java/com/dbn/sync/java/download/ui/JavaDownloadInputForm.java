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
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.list.CheckBoxList;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.object.common.DBObject;
import com.dbn.sync.java.download.JavaDownloadBatch;
import com.dbn.sync.java.download.JavaDownloadInput;
import com.dbn.sync.java.download.JavaDownloadManager;
import com.dbn.sync.java.download.JavaDownloadTask;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.initSelectionListener;
import static com.dbn.common.util.Unsafe.cast;

public class JavaDownloadInputForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JPanel targetLocationPanel;
    private JComboBox<ModulePresentable> moduleComboBox;
    private JComboBox<VirtualFilePresentable> contentRootComboBox;
    private CheckBoxList<JavaDownloadTask> dependenciesCheckBoxList;
    private JPanel hintPanel;


    public JavaDownloadInputForm(JavaDownloadInputDialog dialog) {
        super(dialog);
        JavaDownloadInput input = dialog.getBatch().getInput();

        initHeaderPanel(input);
        initHintPanel();

        initSelectionListener(moduleComboBox, s -> initContentRoots());
        initModules();

        dependenciesCheckBoxList.setElements(input.getTasks());
    }

    private void initHeaderPanel(JavaDownloadInput input) {
        DBObject sourceObject = input.getSourceObject();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, sourceObject);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    private void initHintPanel() {
        TextContent hintText = TextContent.plain(
                "Following java classes and resources will be downloaded to the project. " +
                        "Please specify the target module and content root, as well as the resources to be downloaded.\n\n" +
                        "NOTE: Already existing java classes and resources in the selected destination will be overwritten.");
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    JavaDownloadBatch getBatch() {
        JavaDownloadInputDialog dialog = ensureParentComponent();
        return dialog.getBatch();
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
            Set<? extends JpsModuleSourceRootType<?>> rootTypes = cast(getSourceRootTypes());
            List<VirtualFile> sourceRoots = moduleRootManager.getSourceRoots(rootTypes);

            List<VirtualFilePresentable> presentableFiles = VirtualFilePresentable.fromFiles(sourceRoots);
            ComboBoxes.initComboBox(contentRootComboBox, presentableFiles);
        }
    }

    public Set<JpsModuleSourceRootType> getSourceRootTypes() {
        JavaDownloadInput input = getBatch().getInput();
        Set<JpsModuleSourceRootType> rootTypes = new HashSet<>();
        rootTypes.add(JavaSourceRootType.SOURCE);
        rootTypes.add(JavaSourceRootType.TEST_SOURCE);
        if (input.hasJavaResources()) {
            rootTypes.add(JavaResourceRootType.RESOURCE);
            rootTypes.add(JavaResourceRootType.TEST_RESOURCE);
        }

        return rootTypes;
    }

    protected void applyUserInput() {
        JavaDownloadInput input = getBatch().getInput();
        input.setModuleName(getSelectedModuleName());
        input.setContentRoot(getSelectedContentPath());
        dependenciesCheckBoxList.applyChanges();
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
