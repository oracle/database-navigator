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
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.nls.NlsResources.txt;

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

    private final Map<String, JTextField> fileNamePatternTextFields = new HashMap<>();

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

        fileNamePatternTextFields.put(txt("cfg.ddlFiles.field.View"), viewTextField);
        fileNamePatternTextFields.put(txt("cfg.ddlFiles.field.Trigger"), triggerTextField);
        fileNamePatternTextFields.put(txt("cfg.ddlFiles.field.Procedure"), procedureTextField);
        fileNamePatternTextFields.put(txt("cfg.ddlFiles.field.Function"), functionTextField);
        fileNamePatternTextFields.put(txt("cfg.ddlFiles.field.Package"), packageTextField);
        fileNamePatternTextFields.put(txt("cfg.ddlFiles.field.PackageSpec"), packageSpecTextField);
        fileNamePatternTextFields.put(txt("cfg.ddlFiles.field.PackageBody"), packageBodyTextField);
        fileNamePatternTextFields.put(txt("cfg.ddlFiles.field.Type"), typeTextField);
        fileNamePatternTextFields.put(txt("cfg.ddlFiles.field.TypeSpec"), typeSpecTextField);
        fileNamePatternTextFields.put(txt("cfg.ddlFiles.field.TypeBody"), typeBodyTextField);
        fileNamePatternTextFields.put(txt("cfg.ddlFiles.field.JavaSource"), javaTextField);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    private void validateInputs() throws ConfigurationException {
        List<String> allFileNamePatterns = new ArrayList<>();
        for (var entry : fileNamePatternTextFields.entrySet()) {
            String fieldName = entry.getKey();
            JTextField fileNamePatternTextField = entry.getValue();

            String patternsText = ConfigurationEditors.validateStringValue(fileNamePatternTextField, fieldName, false);
            List<String> fileNamePatterns = Strings.tokenize(patternsText, ",");
            for (String fileNamePattern : fileNamePatterns) {
                validateFileNamePattern(fileNamePattern);
                String normalizedFileNamePattern = Strings.toLowerCase(fileNamePattern);
                if (allFileNamePatterns.contains(normalizedFileNamePattern)) {
                    throw new ConfigurationException(txt("cfg.ddlFiles.error.DuplicateFilePattern", fileNamePattern));
                }
                allFileNamePatterns.add(normalizedFileNamePattern);
            }
        }
    }

    private static void validateFileNamePattern(String fileNamePattern) throws ConfigurationException {
        int wildcardIndex = fileNamePattern.indexOf('*');
        if (wildcardIndex != -1 && fileNamePattern.indexOf('*', wildcardIndex + 1) != -1) {
            throw new ConfigurationException(txt("cfg.ddlFiles.error.MultipleWildcards", fileNamePattern));
        }
        if (fileNamePattern.contains("?")) {
            throw new ConfigurationException(txt("cfg.ddlFiles.error.UnsupportedWildcard", fileNamePattern));
        }
        if (fileNamePattern.contains("/") || fileNamePattern.contains("\\")) {
            throw new ConfigurationException(txt("cfg.ddlFiles.error.InvalidFileNamePattern", fileNamePattern));
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
        boolean valueChanged = ddlFileType.setNamePatternsAsString(getText(textField));
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
        textField.setText(getConfiguration().getFileType(fileTypeId).getNamePatternsAsString());
    }
}
