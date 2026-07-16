/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.object.factory.ui;

import com.dbn.common.state.StateAttributes;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Json;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseIdentifierCase;
import com.dbn.object.factory.ObjectFactoryManager;
import com.dbn.object.factory.model.DBObjectSpec;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.regex.Pattern;

import static com.dbn.common.file.FileTypes.getJsonFileType;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.factory.model.DBObjectAttributeType.OBJECT_DETAIL;

public class DBDatasourceConfigFactoryInputForm extends DBSchemaObjectFactoryInputForm {
    private static final String DOCUMENTATION_URL = "https://docs.oracle.com/en/database/oracle/oracle-database/23/netag/configuring-centralized-configuration-provider-naming-method.html";
    private static final Pattern CONFIG_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*$");
    private static final int IDENTIFIER_MAX_LENGTH = 128;

    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel hyperlinkPanel;
    private JPanel editorPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;
    private JTextField nameTextField;
    private JPanel preserveCasePanel;
    private JCheckBox preserveCaseCheckBox;
    private DBNInfoLabel preserveCaseInfoLabel;
    private EditorEx jsonEditor;

    public DBDatasourceConfigFactoryInputForm(@NotNull DBNComponent parent, DBObjectSpec input) {
        super(parent, input);

        initHeaderForm();
        initFeatureInfo();
        initContextComponents();
        initEditor();
        initPreserveCaseFields();
        resetFormChanges();
    }

    private void initFeatureInfo() {
        DBNHintForm hintForm = new DBNHintForm(this,
                TextContent.plain(txt("cfg.datasourceConfig.hint.Feature")), null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
                txt("cfg.datasourceConfig.link.Documentation"),
                txt("cfg.datasourceConfig.link.ConfigProvider"),
                DOCUMENTATION_URL);
        hyperlinkPanel.add(hyperLinkForm.getComponent(), BorderLayout.EAST);
    }

    private void initPreserveCaseFields() {
        preserveCaseInfoLabel.setContent(getPreserveCaseInfoText());
        // TODO revisit when the data source config store supports case-sensitive names
        preserveCasePanel.setVisible(false);
    }

    private void initEditor() {
        FileType jsonFileType = getJsonFileType();
        VirtualFile virtualFile = new LightVirtualFile("data_source_config_store.json", jsonFileType, "");
        Document document = Documents.createDocument(OBJECT_DETAIL.of(getInput()));
        jsonEditor = Editors.createEditor(document, ensureProject(), virtualFile, jsonFileType);
        jsonEditor.setEmbeddedIntoDialogWrapper(true);
        jsonEditor.setPlaceholder(OBJECT_DETAIL.of(getInput()));
        EditorSettings settings = jsonEditor.getSettings();
        settings.setLineNumbersShown(false);
        settings.setGutterIconsShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setCaretRowShown(false);
        settings.setRightMarginShown(false);
        Editors.updateEditorScrollPane(jsonEditor);
        editorPanel.add(jsonEditor.getComponent(), BorderLayout.CENTER);
    }

    @Override
    public void resetFormChanges() {
        super.resetFormChanges();
        Documents.setText(getProject(), jsonEditor.getDocument(), OBJECT_DETAIL.of(getInput()));
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        super.applyFormChanges();
        getInput().setAttributeValue(OBJECT_DETAIL, jsonEditor.getDocument().getText().trim());
        getInput().setIdentifierCase(getSelectedIdentifierCase());
    }

    @Override
    protected void initStatePersistence() {
        StateAttributes state = ObjectFactoryManager.getInstance(ensureProject()).getState(getObjectType());
        initPersistence(preserveCaseCheckBox, state, "preserve-identifier-case");
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, c -> isNotEmpty(c.trim()), txt("cfg.datasourceConfig.error.ConfigNameRequired"));
        addTextValidation(nameTextField, c -> c.trim().isEmpty() || c.trim().length() <= IDENTIFIER_MAX_LENGTH,
                txt("cfg.datasourceConfig.error.ConfigNameTooLong", IDENTIFIER_MAX_LENGTH));
        addTextValidation(nameTextField, c -> c.trim().isEmpty() || CONFIG_NAME_PATTERN.matcher(c.trim()).matches(),
                txt("cfg.datasourceConfig.error.ConfigNameInvalid"));
        addValidation(editorPanel, c -> validateJson());
    }

    private String validateJson() {
        String value = jsonEditor == null ? "" : jsonEditor.getDocument().getText().trim();
        if (value.isBlank()) return txt("cfg.datasourceConfig.error.JsonRequired");
        try {
            Json.readAsMap(value);
            return null;
        } catch (Exception e) {
            return txt("cfg.datasourceConfig.error.JsonInvalid");
        }
    }

    @Override
    protected String getObjectName() {
        return getText(nameTextField);
    }

    @Override
    protected String getSchemaName() {
        return getInput().getSchemaName();
    }

    @Override
    protected DatabaseIdentifierCase getSelectedIdentifierCase() {
        // TODO revisit when the data source config store supports case-sensitive names
        return preserveCaseCheckBox.isSelected() ?
                DatabaseIdentifierCase.PRESERVE :
                getDefaultIdentifierCase();
    }

    @Override
    protected DBNComboBox<ConnectionHandler> getConnectionComboBox() {
        return connectionComboBox;
    }

    @Override
    protected DBNComboBox<SchemaId> getSchemaComboBox() {
        return schemaComboBox;
    }

    @Override
    protected JPanel getHeaderPanel() {
        return headerPanel;
    }

    @Override
    protected JTextField getNameTextField() {
        return nameTextField;
    }

    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(jsonEditor);
        jsonEditor = null;
        super.disposeInner();
    }
}
