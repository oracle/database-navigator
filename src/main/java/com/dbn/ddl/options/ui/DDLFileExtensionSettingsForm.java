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

package com.dbn.ddl.options.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditors;
import com.dbn.common.util.Strings;
import com.dbn.ddl.DDLFileManager;
import com.dbn.ddl.DDLFileType;
import com.dbn.ddl.DDLFileTypeId;
import com.dbn.ddl.options.DDLFileExtensionSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class DDLFileExtensionSettingsForm extends ConfigurationEditorForm<DDLFileExtensionSettings> {
    private JPanel mainPanel;
    private JLabel viewIconLabel;
    private JLabel triggerIconLabel;
    private JLabel procedureIconLabel;
    private JLabel functionIconLabel;
    private JLabel packageIconLabel;
    private JLabel typeIconLabel;
    private JTextField viewTextField;
    private JTextField triggerTextField;
    private JTextField procedureTextField;
    private JTextField functionTextField;
    private JTextField packageTextField;
    private JTextField packageSpecTextField;
    private JTextField packageBodyTextField;
    private JTextField typeTextField;
    private JTextField typeSpecTextField;
    private JTextField typeBodyTextField;
    private JLabel javaIconLabel;
    private JTextField javaTextField;

    private final Map<String, JTextField> extensionTextFields = new HashMap<>();

    public DDLFileExtensionSettingsForm(DDLFileExtensionSettings settings) {
        super(settings);
        resetFormChanges();
        viewIconLabel.setText(null);
        triggerIconLabel.setText(null);
        procedureIconLabel.setText(null);
        functionIconLabel.setText(null);
        packageIconLabel.setText(null);
        typeIconLabel.setText(null);
        javaIconLabel.setText(null);

        viewIconLabel.setIcon(Icons.DBO_VIEW);
        triggerIconLabel.setIcon(Icons.DBO_TRIGGER);
        procedureIconLabel.setIcon(Icons.DBO_PROCEDURE);
        functionIconLabel.setIcon(Icons.DBO_FUNCTION);
        packageIconLabel.setIcon(Icons.DBO_PACKAGE);
        typeIconLabel.setIcon(Icons.DBO_TYPE);
        javaIconLabel.setIcon(Icons.DBO_JAVA_CLASS);

        registerComponent(mainPanel);

        extensionTextFields.put("View", viewTextField);
        extensionTextFields.put("Trigger", triggerTextField);
        extensionTextFields.put("Procedure", procedureTextField);
        extensionTextFields.put("Function", functionTextField);
        extensionTextFields.put("Package", packageTextField);
        extensionTextFields.put("Package Spec", packageSpecTextField);
        extensionTextFields.put("Package Body", packageBodyTextField);
        extensionTextFields.put("Type", typeTextField);
        extensionTextFields.put("Type Spec", typeSpecTextField);
        extensionTextFields.put("Type Bpdy", typeBodyTextField);
        extensionTextFields.put("Java Source", javaTextField);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    private void validateInputs() throws ConfigurationException {
        List<String> allExtensions = new ArrayList<>();
        for (val entry : extensionTextFields.entrySet()) {
            String fieldName = entry.getKey();
            JTextField extensionTextField = entry.getValue();

            String extensionsText = ConfigurationEditors.validateStringValue(extensionTextField, fieldName, false);
            List<String> extensions = Strings.tokenize(extensionsText, ",");
            for (String extension : extensions) {
                if (allExtensions.contains(extension)) {
                    throw new ConfigurationException("Duplicate value for extension \"" + extension + "\" found.");
                }
            }
            allExtensions.addAll(extensions);
        }
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        validateInputs();
        AtomicBoolean changed = new AtomicBoolean(false);
        applySetting(viewTextField, DDLFileTypeId.VIEW, changed);
        applySetting(triggerTextField, DDLFileTypeId.TRIGGER, changed);
        applySetting(procedureTextField, DDLFileTypeId.PROCEDURE, changed);
        applySetting(functionTextField, DDLFileTypeId.FUNCTION, changed);
        applySetting(packageTextField, DDLFileTypeId.PACKAGE, changed);
        applySetting(packageSpecTextField, DDLFileTypeId.PACKAGE_SPEC, changed);
        applySetting(packageBodyTextField, DDLFileTypeId.PACKAGE_BODY, changed);
        applySetting(typeTextField, DDLFileTypeId.TYPE, changed);
        applySetting(typeSpecTextField, DDLFileTypeId.TYPE_SPEC, changed);
        applySetting(typeBodyTextField, DDLFileTypeId.TYPE_BODY, changed);
        applySetting(javaTextField, DDLFileTypeId.JAVA_SOURCE, changed);

        if (changed.get()) {
            Project project = getConfiguration().getProject();
            DDLFileManager ddlFileManager = DDLFileManager.getInstance(project);
            ddlFileManager.registerExtensions(getConfiguration());
        }
    }

    private void applySetting(JTextField textField, DDLFileTypeId fileTypeId, AtomicBoolean changed) {
        DDLFileType ddlFileType = getConfiguration().getFileType(fileTypeId);
        boolean valueChanged = ddlFileType.setExtensionsAsString(textField.getText().trim());
        if (valueChanged) {
            changed.set(true);
        }
    }

    @Override
    public void resetFormChanges() {
        resetSetting(viewTextField, DDLFileTypeId.VIEW);
        resetSetting(triggerTextField, DDLFileTypeId.TRIGGER);
        resetSetting(procedureTextField, DDLFileTypeId.PROCEDURE);
        resetSetting(functionTextField, DDLFileTypeId.FUNCTION);
        resetSetting(packageTextField, DDLFileTypeId.PACKAGE);
        resetSetting(packageSpecTextField, DDLFileTypeId.PACKAGE_SPEC);
        resetSetting(packageBodyTextField, DDLFileTypeId.PACKAGE_BODY);
        resetSetting(typeTextField, DDLFileTypeId.TYPE);
        resetSetting(typeSpecTextField, DDLFileTypeId.TYPE_SPEC);
        resetSetting(typeBodyTextField, DDLFileTypeId.TYPE_BODY);
        resetSetting(javaTextField, DDLFileTypeId.JAVA_SOURCE);
    }

    private void resetSetting(JTextField textField, DDLFileTypeId fileTypeId) {
        textField.setText(getConfiguration().getFileType(fileTypeId).getExtensionsAsString());
    }
}
