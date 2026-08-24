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
import com.dbn.database.DatabaseIdentifierCase;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.ObjectFactoryManager;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBJavaClassType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Java.getQualifiedClassName;
import static com.dbn.common.util.Java.isValidClassName;
import static com.dbn.common.util.Java.isValidPackageName;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.factory.model.DBObjectAttributeType.JAVA_CLASS_NAME;
import static com.dbn.object.factory.model.DBObjectAttributeType.JAVA_CLASS_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.JAVA_PACKAGE_NAME;
import static com.dbn.object.type.DBJavaClassType.CLASS;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;

public class DBJavaClassFactoryInputForm extends DBSchemaObjectFactoryInputForm {
    private JPanel mainPanel;
    private @Getter JPanel headerPanel;
    private @Getter JTextField packageTextField;
    private @Getter JTextField nameTextField;
    private @Getter DBNComboBox<ConnectionHandler> connectionComboBox;
    private @Getter DBNComboBox<SchemaId> schemaComboBox;
    private DBNComboBox<DBJavaClassType> classTypeComboBox;

    public DBJavaClassFactoryInputForm(DBNComponent parent, DBSchema schema) {
        this(parent, createInput(schema));
    }

    public DBJavaClassFactoryInputForm(DBNComponent parent, DBObjectSpec input) {
        super(parent, input);
        DBSchema schema = input.getSchema();

        ConnectionHandler connection = getConnection();
        connectionComboBox.setValues(connection);
        connectionComboBox.setSelectedValue(connection);
        connectionComboBox.setEnabled(false); // TODO support connection switch

        SchemaId schemaId = schema.getSchemaId();
        schemaComboBox.setValues(schemaId);
        schemaComboBox.setSelectedValue(schemaId);
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

    private static DBObjectSpec createInput(DBSchema schema) {
        DBObjectSpec input = new DBObjectSpec(schema, JAVA_CLASS);
        input.setAttributeValue(JAVA_CLASS_TYPE, CLASS);
        return input;
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
            className = txt("app.object.placeholder.New");
        }

        String schemaName = getSchemaName();
        return schemaName + (isEmpty(packageName) ? "" : "." + packageName) + "." + className;
    }

    @Override
    protected String getSchemaName() {
        return getInput().getSchema().getName();
    }

    @Override
    protected DatabaseIdentifierCase getSelectedIdentifierCase() {
        return DatabaseIdentifierCase.PRESERVE;
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
        String packageName = getText(packageTextField);
        String className = getText(nameTextField);
        input.setAttributeValue(JAVA_PACKAGE_NAME, packageName);
        input.setAttributeValue(JAVA_CLASS_NAME, className);
        input.setAttributeValue(JAVA_CLASS_TYPE, getSelection(classTypeComboBox));
        input.setObjectName(getQualifiedClassName(packageName, className));
    }

    @Override
    public void resetFormChanges() {
        setText(packageTextField, JAVA_PACKAGE_NAME.of(input));
        setText(nameTextField, JAVA_CLASS_NAME.of(input));
        classTypeComboBox.setSelectedValue(getClassType());
    }

    private DBJavaClassType getClassType() {
        DBJavaClassType classType = JAVA_CLASS_TYPE.of(input);
        return classType == null ? CLASS : classType;
    }

    @Override
    protected void initValidation() {
        addTextValidation(packageTextField, p -> isValidPackageName(p), txt("msg.java.error.ValidPackageName"));
        addTextValidation(nameTextField, p -> isNotEmpty(p), txt("msg.java.error.ClassNameRequired"));
        addTextValidation(nameTextField, p -> isValidClassName(p), txt("msg.java.error.ValidClassName"));
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
