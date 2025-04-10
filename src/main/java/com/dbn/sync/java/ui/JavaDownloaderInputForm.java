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

package com.dbn.sync.java.ui;

import com.dbn.common.file.VirtualFilePresentable;
import com.dbn.common.project.ModulePresentable;
import com.dbn.common.state.StateHolder;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBSchema;
import com.dbn.sync.java.JavaDownloaderContext;
import com.dbn.sync.java.JavaDownloaderInput;
import com.dbn.sync.java.JavaDownloaderManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.initSelectionListener;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class JavaDownloaderInputForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JPanel targetLocationPanel;
    private JComboBox<ModulePresentable> moduleComboBox;
    private JComboBox<VirtualFilePresentable> contentRootComboBox;
    private JPanel dependentObjectPanel;
    private JPanel loadingArgumentsIconPanel;
    private JLabel loadingDependentsPanel;


    public JavaDownloaderInputForm(JavaDownloaderInputDialog dialog) {
        super(dialog);
        JavaDownloaderInput input = dialog.getContext().getInput();

        DBJavaClass javaClass = input.getJavaClass();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, javaClass);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        initSelectionListener(moduleComboBox, s -> initContentRoots());
        initModules();

        loadingDependentsPanel.setVisible(true);
        loadingArgumentsIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);

        DBSchema schema = javaClass.getSchema();
        String objectName = javaClass.getName();
        ConnectionHandler connection = javaClass.getConnection();
        try {
            DatabaseInterfaceInvoker.execute(HIGH,
                    "Downloading dependencies",
                    "Downloading dependencies for object ",
                    connection.getProject(),
                    connection.getConnectionId(),
                    c -> loadObjectDependencies(connection, schema, objectName, c));
        } catch (SQLException e) {
            conditionallyLog(e);
        }
    }

    JavaDownloaderContext getContext() {
        JavaDownloaderInputDialog dialog = ensureParentComponent();
        return dialog.getContext();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    protected void initStatePersistence() {
        Project project = ensureProject();
        JavaDownloaderManager javaDownloadManager = JavaDownloaderManager.getInstance(project);

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
        JavaDownloaderInput input = getContext().getInput();
        input.setModuleName(getSelectedModuleName());
        input.setContentRoot(getSelectedContentPath());
        input.setDependentObjects(getDependentObjects());
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

    private List<String> getDependentObjects(){
        List<String> selection = new ArrayList<>();
        for(int i=0; i< dependentObjectPanel.getComponentCount(); i++){
            JCheckBox checkBox = (JCheckBox) dependentObjectPanel.getComponent(i);
            if(checkBox.isSelected()){
                selection.add(checkBox.getText());
            }
        }
        return selection;
    }

    private String getSelectedContentPath() {
        VirtualFile selectedContentRoot = getSelectedContentRoot();
        return selectedContentRoot == null ? null : selectedContentRoot.getPath();
    }

    private void loadObjectDependencies(ConnectionHandler connection, DBSchema schema, String objectName, DBNConnection conn) throws SQLException {
        ResultSet resultSet = null;
        try {
            DatabaseMetadataInterface metadata = connection.getMetadataInterface();
            resultSet = metadata.loadJavaObjectDependencies(schema.getName(), objectName, conn);
            dependentObjectPanel.removeAll();
            dependentObjectPanel.setLayout(new BoxLayout(dependentObjectPanel, BoxLayout.Y_AXIS));
            while (resultSet != null && resultSet.next()) {
                String objectOwner = resultSet.getString("OBJECT_OWNER");
                String objectName1 = resultSet.getString("OBJECT_NAME");
                String hasSource = resultSet.getString("HAS_SOURCE");
                JBCheckBox checkBox = new JBCheckBox(objectName1 + " (" + objectOwner + ")");
                if(hasSource.equals("N")){
                    checkBox.setEnabled(false);
                }
                dependentObjectPanel.add(checkBox);
            }
            dependentObjectPanel.revalidate();
            dependentObjectPanel.repaint();
        } finally {
            Resources.close(resultSet);
        }
    }
}
