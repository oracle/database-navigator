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

import com.dbn.common.state.StateAttributes;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.list.CheckBoxList;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.object.type.DBObjectType;
import com.dbn.sync.java.upload.JavaUploadBatch;
import com.dbn.sync.java.upload.JavaUploadInput;
import com.dbn.sync.java.upload.JavaUploadManager;
import com.dbn.sync.java.upload.JavaUploadTask;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.database.DatabaseFeature.JAVA_VIRTUAL_MACHINE;
import static com.dbn.nls.NlsResources.txt;
import static java.util.Collections.emptyList;

public class JavaUploadInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel targetLocationPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBObjectSelector<DBSchema> schemaComboBox;
    private CheckBoxList<JavaUploadTask> contentList;
    private JPanel contentPanel;

    public JavaUploadInputForm(JavaUploaderInputDialog dialog) {
        super(dialog);

        initHintPanel();
        initConnectionSelector();
        initSchemaSelectors();
        initDependenciesSelector();
    }

    private void initDependenciesSelector() {
        JavaUploadInput input = getInput();
        contentList = new CheckBoxList<>();
        contentList.setElements(input.getTasks());
        contentPanel.add(contentList.withSelectorActions());
    }

    @Override
    protected void initValidation() {
        addSelectionValidation(connectionComboBox, txt("msg.shared.error.SelectTargetConnection"));
        addSelectionValidation(schemaComboBox, txt("msg.shared.error.SelectTargetSchema"));
        addSelectionValidation(contentList, txt("msg.java.error.SelectResourceToUpload"));
    }

    private void initHintPanel() {
        TextContent hintText = TextContent.plain(
                txt("msg.java.hint.UploadInput"));
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    private JavaUploadBatch getBatch() {
        JavaUploaderInputDialog dialog = ensureParentComponent();
        return dialog.getBatch();
    }

    private JavaUploadInput getInput() {
        return getBatch().getInput();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    protected void initStatePersistence() {
        Project project = ensureProject();
        JavaUploadManager uploadManager = JavaUploadManager.getInstance(project);

        StateAttributes state = uploadManager.getState("UPLOAD_INPUT");

        initPersistence(connectionComboBox, state, "connection-selection");
        initPersistence(schemaComboBox, state, "schema-selection");
    }

    private void initConnectionSelector() {
        Project project = ensureProject();
		ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        List<ConnectionHandler> connections = connectionManager.getConnections(JAVA_VIRTUAL_MACHINE);
        initComboBox(connectionComboBox, connections);
    }

    private void initSchemaSelectors() {
        schemaComboBox
                .initialize(this, DBObjectType.SCHEMA)
                .withConnectionContext(() -> getSelectedConnection())
                .withValueLoader(() -> loadSchemas())
                .triggerLoad();

        onSelectionChange(connectionComboBox, s -> schemaComboBox.reloadValues());
    }

    private List<DBSchema> loadSchemas() {
        ConnectionHandler selectedConnection = getSelectedConnection();
        if (selectedConnection == null) return emptyList();

        // ensure connectivity
        ConnectionAction.invoke(null, true, selectedConnection, a -> {});

        return selectedConnection.getObjectBundle().getSchemas(true);
    }

    protected void applyUserInput() {
        JavaUploadInput input = getBatch().getInput();
        input.setTargetConnection(getSelectedConnection());
        input.setTargetSchema(getSelectedSchema());
        contentList.applyChanges();
    }

    @Nullable
    private ConnectionHandler getSelectedConnection() {
		return getSelection(connectionComboBox);
    }

    @Nullable
    private DBSchema getSelectedSchema() {
       return getSelection(schemaComboBox);
    }
}
