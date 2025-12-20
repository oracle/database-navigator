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

package com.dbn.object.factory.ui;

import com.dbn.common.state.StateAttributes;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.ObjectFactoryManager;
import com.dbn.object.factory.model.DBJavaClassSpec;
import com.dbn.object.factory.ui.common.DBObjectFactoryInputForm;
import com.dbn.object.type.DBJavaClassType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Java.isValidClassName;
import static com.dbn.common.util.Java.isValidPackageName;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isNotEmpty;

public class DBJavaClassFactoryInputForm extends DBObjectFactoryInputForm<DBJavaClassSpec> {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JTextField nameTextField;
    private JTextField packageTextField;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;
    private DBNComboBox<DBJavaClassType> classTypeComboBox;

    public DBJavaClassFactoryInputForm(DBNComponent parent, DBSchema schema) {
        this(parent, new DBJavaClassSpec(schema));
    }

    public DBJavaClassFactoryInputForm(DBNComponent parent, DBJavaClassSpec input) {
        super(parent, input);
        DBSchema schema = input.getSchema();

        ConnectionHandler connection = getConnection();
        connectionComboBox.setValues(connection);
        connectionComboBox.setSelectedValue(connection);
        connectionComboBox.set(HIDE_DESCRIPTION, true);
        connectionComboBox.setEnabled(false); // TODO support connection switch

        SchemaId schemaId = schema.getSchemaId();
        schemaComboBox.setValues(schemaId);
        schemaComboBox.setSelectedValue(schemaId);
        schemaComboBox.set(HIDE_DESCRIPTION, true);
        schemaComboBox.setEnabled(false); // TODO support connection switch

        classTypeComboBox.setValues(DBJavaClassType.values());
        classTypeComboBox.setSelectedValue(DBJavaClassType.CLASS);


        DBNHeaderForm headerForm = createHeaderForm();
        headerPanel.add(headerForm.getComponent());

        onTextChange(nameTextField, e -> headerForm.setTitle(buildHeaderTitle()));
        onTextChange(packageTextField, e -> headerForm.setTitle(buildHeaderTitle()));
        classTypeComboBox.addListener((o,n) -> headerForm.setIcon(getHeaderIcon()));

        resetFormChanges();
    }

    @Nullable
    private Icon getHeaderIcon() {
        DBJavaClassType selectedValue = classTypeComboBox.getSelectedValue();
        return selectedValue == null ? DBJavaClassType.CLASS.getIcon() : selectedValue.getIcon();
    }

    protected String buildHeaderTitle() {
        String packageName = getPackageName();
        String className = getObjectName();
        if (isEmpty(className)) {
            className = "[new]";
        }

        String schemaName = getSchemaName();
        return schemaName + (isEmpty(packageName) ? "" : "." + packageName) + "." + className;
    }

    @Override
    protected String getSchemaName() {
        return getInput().getSchema().getName();
    }

    @Override
    public String getObjectName() {
        return getText(nameTextField);
    }

    @NonNls
    private String getPackageName() {
        return getText(packageTextField);
    }

    protected void initStatePersistence() {
        Project project = ensureProject();
        ObjectFactoryManager factoryManager = ObjectFactoryManager.getInstance(project);

        StateAttributes state = factoryManager.getState(getObjectType());
        initPersistence(classTypeComboBox, state, "class-type-selection");
        initPersistence(packageTextField, state, "package-selection");
    }

    @Override
    public void applyFormChanges() {
        input.setPackageName(getText(packageTextField));
        input.setClassName(getText(nameTextField));
        input.setClassType(getSelection(classTypeComboBox));
    }

    @Override
    public void resetFormChanges() {
        setText(packageTextField, input.getPackageName());
        setText(nameTextField, input.getClassName());
        classTypeComboBox.setSelectedValue(input.getClassType());
    }

    @Override
    protected void initValidation() {
        addTextValidation(packageTextField, p -> isValidPackageName(p), "Please enter a valid package name");
        addTextValidation(nameTextField, p -> isNotEmpty(p), "Please enter a class name");
        addTextValidation(nameTextField, p -> isValidClassName(p), "Please enter a valid class name");
    }

    @Override
    public void focus() {
        classTypeComboBox.requestFocus();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
