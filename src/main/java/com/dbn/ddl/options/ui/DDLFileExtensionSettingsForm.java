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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dbn.common.icon.Icons.DBO_FUNCTION;
import static com.dbn.common.icon.Icons.DBO_JAVA_CLASS;
import static com.dbn.common.icon.Icons.DBO_PACKAGE;
import static com.dbn.common.icon.Icons.DBO_PROCEDURE;
import static com.dbn.common.icon.Icons.DBO_TRIGGER;
import static com.dbn.common.icon.Icons.DBO_TYPE;
import static com.dbn.common.icon.Icons.DBO_VIEW;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.ddl.DDLFileTypeId.FUNCTION;
import static com.dbn.ddl.DDLFileTypeId.JAVA_SOURCE;
import static com.dbn.ddl.DDLFileTypeId.PACKAGE;
import static com.dbn.ddl.DDLFileTypeId.PACKAGE_BODY;
import static com.dbn.ddl.DDLFileTypeId.PACKAGE_SPEC;
import static com.dbn.ddl.DDLFileTypeId.PROCEDURE;
import static com.dbn.ddl.DDLFileTypeId.TRIGGER;
import static com.dbn.ddl.DDLFileTypeId.TYPE;
import static com.dbn.ddl.DDLFileTypeId.TYPE_BODY;
import static com.dbn.ddl.DDLFileTypeId.TYPE_SPEC;
import static com.dbn.ddl.DDLFileTypeId.VIEW;
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

    private final Map<DDLFileTypeId, JTextField> fileNamePatternTextFields = new LinkedHashMap<>();

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

        viewIconLabel.setIcon(DBO_VIEW);
        triggerIconLabel.setIcon(DBO_TRIGGER);
        procedureIconLabel.setIcon(DBO_PROCEDURE);
        functionIconLabel.setIcon(DBO_FUNCTION);
        packageIconLabel.setIcon(DBO_PACKAGE);
        typeIconLabel.setIcon(DBO_TYPE);
        javaIconLabel.setIcon(DBO_JAVA_CLASS);

        registerComponent(mainPanel);

        fileNamePatternTextFields.put(VIEW, viewTextField);
        fileNamePatternTextFields.put(TRIGGER, triggerTextField);
        fileNamePatternTextFields.put(PROCEDURE, procedureTextField);
        fileNamePatternTextFields.put(FUNCTION, functionTextField);
        fileNamePatternTextFields.put(PACKAGE, packageTextField);
        fileNamePatternTextFields.put(PACKAGE_SPEC, packageSpecTextField);
        fileNamePatternTextFields.put(PACKAGE_BODY, packageBodyTextField);
        fileNamePatternTextFields.put(TYPE, typeTextField);
        fileNamePatternTextFields.put(TYPE_SPEC, typeSpecTextField);
        fileNamePatternTextFields.put(TYPE_BODY, typeBodyTextField);
        fileNamePatternTextFields.put(JAVA_SOURCE, javaTextField);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    private void validateInputs() throws ConfigurationException {
        Map<String, Map<String, DDLFileTypeId>> patternScopes = new HashMap<>();
        for (var entry : fileNamePatternTextFields.entrySet()) {
            DDLFileTypeId fileTypeId = entry.getKey();
            String fieldName = getFieldName(fileTypeId);
            JTextField fileNamePatternTextField = entry.getValue();

            String patternsText = ConfigurationEditors.validateStringValue(fileNamePatternTextField, fieldName, false);
            List<String> fileNamePatterns = Strings.tokenize(patternsText, ",");
            for (String fileNamePattern : fileNamePatterns) {
                validateFileNamePattern(fileNamePattern);
                String normalizedFileNamePattern = Strings.toLowerCase(fileNamePattern);
                Map<String, DDLFileTypeId> scopes = patternScopes.computeIfAbsent(normalizedFileNamePattern, p -> new HashMap<>());
                String scope = getConflictScope(fileTypeId);
                DDLFileTypeId existingFileTypeId = scopes.get(scope);
                if (existingFileTypeId != null && existingFileTypeId != fileTypeId) {
                    throw new ConfigurationException(txt("cfg.ddlFiles.error.DuplicateFilePattern", fileNamePattern, getFieldName(existingFileTypeId), fieldName));
                }
                scopes.put(scope, fileTypeId);
            }
        }
    }

    private static String getConflictScope(DDLFileTypeId fileTypeId) {
        return switch (fileTypeId) {
            case PACKAGE,
                 PACKAGE_SPEC,
                 PACKAGE_BODY -> PACKAGE.name();
            case TYPE,
                 TYPE_SPEC,
                 TYPE_BODY -> TYPE.name();
            default -> fileTypeId.name();
        };
    }

    private static String getFieldName(DDLFileTypeId fileTypeId) {
        return switch (fileTypeId) {
            case VIEW -> txt("cfg.ddlFiles.const.DDLFileType_VIEW");
            case TRIGGER -> txt("cfg.ddlFiles.const.DDLFileType_TRIGGER");
            case PROCEDURE -> txt("cfg.ddlFiles.const.DDLFileType_PROCEDURE");
            case FUNCTION -> txt("cfg.ddlFiles.const.DDLFileType_FUNCTION");
            case PACKAGE -> txt("cfg.ddlFiles.const.DDLFileType_PACKAGE");
            case PACKAGE_SPEC -> txt("cfg.ddlFiles.const.DDLFileType_PACKAGE_SPEC");
            case PACKAGE_BODY -> txt("cfg.ddlFiles.const.DDLFileType_PACKAGE_BODY");
            case TYPE -> txt("cfg.ddlFiles.const.DDLFileType_TYPE");
            case TYPE_SPEC -> txt("cfg.ddlFiles.const.DDLFileType_TYPE_SPEC");
            case TYPE_BODY -> txt("cfg.ddlFiles.const.DDLFileType_TYPE_BODY");
            case JAVA_SOURCE -> txt("cfg.ddlFiles.const.DDLFileType_JAVA_SOURCE");
        };
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
        applySetting(viewTextField, VIEW, changed);
        applySetting(triggerTextField, TRIGGER, changed);
        applySetting(procedureTextField, PROCEDURE, changed);
        applySetting(functionTextField, FUNCTION, changed);
        applySetting(packageTextField, PACKAGE, changed);
        applySetting(packageSpecTextField, PACKAGE_SPEC, changed);
        applySetting(packageBodyTextField, PACKAGE_BODY, changed);
        applySetting(typeTextField, TYPE, changed);
        applySetting(typeSpecTextField, TYPE_SPEC, changed);
        applySetting(typeBodyTextField, TYPE_BODY, changed);
        applySetting(javaTextField, JAVA_SOURCE, changed);

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
        resetSetting(viewTextField, VIEW);
        resetSetting(triggerTextField, TRIGGER);
        resetSetting(procedureTextField, PROCEDURE);
        resetSetting(functionTextField, FUNCTION);
        resetSetting(packageTextField, PACKAGE);
        resetSetting(packageSpecTextField, PACKAGE_SPEC);
        resetSetting(packageBodyTextField, PACKAGE_BODY);
        resetSetting(typeTextField, TYPE);
        resetSetting(typeSpecTextField, TYPE_SPEC);
        resetSetting(typeBodyTextField, TYPE_BODY);
        resetSetting(javaTextField, JAVA_SOURCE);
    }

    private void resetSetting(JTextField textField, DDLFileTypeId fileTypeId) {
        textField.setText(getConfiguration().getFileType(fileTypeId).getNamePatternsAsString());
    }
}
