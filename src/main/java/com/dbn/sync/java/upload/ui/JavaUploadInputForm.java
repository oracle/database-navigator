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

package com.dbn.sync.java.upload.ui;

import com.dbn.common.state.StateHolder;
import com.dbn.common.text.TextContent;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.list.CheckBoxList;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBSchema;
import com.dbn.sync.java.upload.JavaUploadContext;
import com.dbn.sync.java.upload.JavaUploadElement;
import com.dbn.sync.java.upload.JavaUploadInput;
import com.dbn.sync.java.upload.JavaUploadManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.initSelectionListener;
import static com.dbn.database.DatabaseFeature.JAVA_VIRTUAL_MACHINE;

public class JavaUploadInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel targetLocationPanel;
    private JComboBox<ConnectionHandler> connectionComboBox;
    private JComboBox<DBSchema> schemaComboBox;
    private CheckBoxList<JavaUploadElement> dependenciesCheckBoxList;

    public JavaUploadInputForm(JavaUploaderInputDialog dialog) {
        super(dialog);
		JavaUploadInput input = dialog.getContext().getInput();

        initHeaderPanel(input);
        initHintPanel();

        initSelectionListener(connectionComboBox, s -> initConnectionSchemas());
        initConnections();

        dependenciesCheckBoxList.setElements(input.getElements());
    }

    private void initHintPanel() {
        TextContent hintText = TextContent.plain(
                "Following java classes and resources will be uploaded to the database. " +
                        "Please select the target connection and schema, as well as the resources to be uploaded.\n\n" +
                        "NOTE: Already existing java classes and resources in the selected destination will be overwritten.");
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    private void initHeaderPanel(JavaUploadInput input) {
/*
        // TODO no database context available yet (remove header ??)
        VirtualFile rootFile = input.getRootFile();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, rootFile);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
*/
    }

    JavaUploadContext getContext() {
        JavaUploaderInputDialog dialog = ensureParentComponent();
        return dialog.getContext();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    protected void initStatePersistence() {
        Project project = ensureProject();
        JavaUploadManager javaDownloadManager = JavaUploadManager.getInstance(project);

        StateHolder state = javaDownloadManager.getState("UPLOAD");

        initPersistence(connectionComboBox, state, "connection-selection");
        initPersistence(schemaComboBox, state, "schema-selection");
    }

    private void initConnections() {
        Project project = ensureProject();
		ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        List<ConnectionHandler> connections = connectionManager.getConnections(JAVA_VIRTUAL_MACHINE);
        initComboBox(connectionComboBox, connections);
    }

    private void initConnectionSchemas() {
        ConnectionHandler selectedConnection = getSelectedConnection();
        if (selectedConnection == null) {
            ComboBoxes.initComboBox(schemaComboBox);
        } else {
            Progress.prompt(getProject(), selectedConnection, true,
                    "Loading Schemas",
                    "Loading schemas for " + selectedConnection.getName(),
                    progress -> {
                        List<SchemaId> schemaIds = selectedConnection.getSchemaIds();
                        List<DBSchema> schemas = new ArrayList<>();
                        for(SchemaId id: schemaIds) {
                            schemas.add(selectedConnection.getSchema(id));
                        }
                        ComboBoxes.initComboBox(schemaComboBox, schemas);
                    });
        }
    }

    protected void applyUserInput() {
        JavaUploadInput input = getContext().getInput();
        input.setConnection(getSelectedConnection());
        input.setSchemaName(getSelectedSchemaName());
        dependenciesCheckBoxList.applyChanges();
    }

    @Nullable
    private ConnectionHandler getSelectedConnection() {
		return getSelection(connectionComboBox);
    }

    @Nullable
    private DBSchema getSelectedSchema() {
       return getSelection(schemaComboBox);
    }

    private String getSelectedSchemaName() {
        DBSchema selectedSchema = getSelectedSchema();
        return selectedSchema == null ? null : selectedSchema.getSchemaName();
    }

}
