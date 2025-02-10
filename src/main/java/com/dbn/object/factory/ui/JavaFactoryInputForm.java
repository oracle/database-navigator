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

import com.dbn.common.color.Colors;
import com.dbn.common.state.StateHolder;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.JavaFactoryInput;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.factory.ObjectFactoryManager;
import com.dbn.object.factory.ui.common.ObjectFactoryInputForm;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBJavaClassType;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Java.isValidClassName;
import static com.dbn.common.util.Java.isValidPackageName;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isNotEmpty;

public class JavaFactoryInputForm extends ObjectFactoryInputForm<JavaFactoryInput> {
    private JPanel mainPanel;
    protected JTextField classNameTextField;
    private JPanel headerPanel;
    private JTextField packageTextField;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;
    private DBNComboBox<DBJavaClassType> classTypeComboBox;

    private final DBObjectRef<DBSchema> schema;

    public JavaFactoryInputForm(DBNComponent parent, DBSchema schema, int index) {
        super(parent, schema.getConnection(), DBObjectType.JAVA_CLASS, index);
        this.schema = DBObjectRef.of(schema);


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


        DBNHeaderForm headerForm = createHeaderForm(schema);

        onTextChange(packageTextField, e -> headerForm.setTitle(getHeaderTitle()));
        onTextChange(classNameTextField, e -> headerForm.setTitle(getHeaderTitle()));
        classTypeComboBox.addListener((o,n) -> headerForm.setIcon(getHeaderIcon()));
    }

    @Nullable
    private Icon getHeaderIcon() {
        DBJavaClassType selectedValue = classTypeComboBox.getSelectedValue();
        return selectedValue == null ? DBJavaClassType.CLASS.getIcon() : selectedValue.getIcon();
    }

    @NotNull
    private String getHeaderTitle() {
        String packageName = packageTextField.getText().trim();
        String className = classNameTextField.getText().trim();
        if (isEmpty(className)) className = "[unnamed]";

        String schemaName = schema.getObjectName();
        return schemaName + (isEmpty(packageName) ? "" : "." + packageName) + "." + className;
    }

    private DBNHeaderForm createHeaderForm(DBSchema schema) {
        Color headerBackground = Colors.getPanelBackground();
        if (getEnvironmentSettings(schema.getProject()).getVisibilitySettings().getDialogHeaders().value()) {
            headerBackground = schema.getEnvironmentType().getColor();
        }
        DBNHeaderForm headerForm = new DBNHeaderForm(this,
                getHeaderTitle(),
                getHeaderIcon(),
                headerBackground);

        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
        return headerForm;
    }

    protected void initStatePersistence() {
        Project project = ensureProject();
        ObjectFactoryManager factoryManager = ObjectFactoryManager.getInstance(project);

        StateHolder state = factoryManager.getState(getObjectType());
        initPersistence(classTypeComboBox, state, "class-type-selection");
        initPersistence(packageTextField, state, "package-selection");
    }

    @NonNls
    private String getPackageName() {
        return packageTextField.getText().trim();
    }

    @Override
    public JavaFactoryInput createFactoryInput(ObjectFactoryInput parent) {
        return new JavaFactoryInput(
                getSchema(),
                packageTextField.getText(),
                classNameTextField.getText(),
                classTypeComboBox.getSelectedValue());
    }

    @Override
    protected void initValidation() {
        addTextValidation(packageTextField, p -> isValidPackageName(p), "Please enter a valid package name");
        addTextValidation(classNameTextField, p -> isNotEmpty(p), "Please enter a class name");
        addTextValidation(classNameTextField, p -> isValidClassName(p), "Please enter a valid class name");
    }

    DBSchema getSchema() {
        return DBObjectRef.get(schema);
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
