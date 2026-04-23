/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.connection.config.datasource.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.color.Colors;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.SpeedSearchBase;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Json;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.datasource.model.DataSourceConfigEntry;
import com.dbn.connection.config.datasource.model.DataSourceConfigRecord;
import com.dbn.connection.config.datasource.service.DataSourceConfigStoreService;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.file.FileTypes.getJsonFileType;
import static com.dbn.common.ui.util.Splitters.makeRegular;

public class DataSourceConfigManagerForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel actionsPanel;
    private JSplitPane splitPane;
    private JBScrollPane keysScrollPane;
    private JPanel detailsPanel;
    private JTextField keyTextField;
    private JLabel lastUpdatedLabel;
    private JTextField lastUpdatedTextField;
    private JPanel editorPanel;
    private JLabel statusLabel;

    private final ConnectionHandler connection;
    private final DataSourceConfigStoreService service;
    private final Runnable stateChangeListener;

    private final DefaultListModel<DataSourceConfigEntry> listModel = new DefaultListModel<>();
    private final JBList<DataSourceConfigEntry> keysList = new JBList<>(listModel);
    private EditorEx jsonEditor;

    private final DataSourceConfigEntry newEntry = new DataSourceConfigEntry("new_key", "");

    private @Nullable DataSourceConfigEntry selectedEntry;
    private boolean selectedEntryPersisted;
    private String originalKey = "";
    private String originalValue = "";
    private boolean suppressDirtyTracking;
    private boolean ignoreSelectionEvents;
    private boolean loadingEntries;
    private DBNHeaderForm headerForm;

    public DataSourceConfigManagerForm(
            @Nullable Disposable parent,
            @NotNull ConnectionHandler connection,
            @NotNull DataSourceConfigStoreService service,
            @NotNull Runnable stateChangeListener) {
        super(parent, connection.getProject());
        this.connection = connection;
        this.service = service;
        this.stateChangeListener = stateChangeListener;

        initHeader();
        initSplitPane();
        initListPanel();
        initDetailsPanel();
        initJsonEditor();
        initListeners();

        loadEntries(null);
    }

    private void initHeader() {
        headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent());
    }

    private void initSplitPane() {
        makeRegular(splitPane);
        splitPane.setResizeWeight(0.34);
        splitPane.setDividerLocation(300);
    }

    private void initListPanel() {
        keysList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keysList.setBackground(Colors.getTextFieldBackground());
        keysList.setBorder(Borders.EMPTY_BORDER);
        setKeysEmptyText("No configuration keys");
        keysList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DataSourceConfigEntry entry) {
                    label.setText(entry.getKey());
                }
                return label;
            }
        });

        new KeysListSpeedSearch(keysList);
        keysScrollPane.setViewportView(keysList);

        actionsPanel.add(Actions.createActionToolbar(
                actionsPanel,
                true,
                new CreateAction(),
                new DeleteAction(),
                Actions.SEPARATOR,
                new ReloadAction()).getComponent());
    }

    private void initDetailsPanel() {
        lastUpdatedTextField.setEditable(false);
        updateLastUpdatedVisibility(false);
    }

    private void initJsonEditor() {
        Project project = ensureProject();
        FileType jsonFileType = getJsonFileType();
        VirtualFile virtualFile = new LightVirtualFile("data_source_config_store.json", jsonFileType, "");
        Document document = Documents.createDocument("");

        jsonEditor = Editors.createEditor(document, project, virtualFile, jsonFileType);
        jsonEditor.setEmbeddedIntoDialogWrapper(true);
        jsonEditor.setPlaceholder("{\n  \"connect_descriptor\": \"...\"\n}");

        EditorSettings settings = jsonEditor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setLineMarkerAreaShown(false);
        settings.setRightMarginShown(false);

        Editors.updateEditorScrollPane(jsonEditor);
        editorPanel.add(jsonEditor.getComponent());
        Editors.setEditorReadonly(jsonEditor, true);
    }

    private void initListeners() {
        keysList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || ignoreSelectionEvents || loadingEntries) return;
            DataSourceConfigEntry entry = keysList.getSelectedValue();
            if (!switchSelection(entry)) return;
            loadSelectedEntry(entry);
        });

        keyTextField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                notifyStateChanged();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                notifyStateChanged();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                notifyStateChanged();
            }
        });

        jsonEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                if (suppressDirtyTracking) return;
                notifyStateChanged();
            }
        }, this);
    }

    private boolean switchSelection(@Nullable DataSourceConfigEntry newSelection) {
        if (selectedEntry == null || !hasPendingChanges() || selectedEntry == newSelection) {
            return true;
        }

        String[] options = Messages.options("Save", "Discard", "Cancel");
        int option = Messages.showConfirmationDialog(
                getProject(),
                "Unsaved changes",
                "Save changes to the current data source config before switching?",
                options,
                0);

        if (option == 0) {
            if (applyChanges()) return true;
            restoreSelection();
            return false;
        }

        if (option == 1) {
            return true;
        }

        restoreSelection();
        return false;
    }

    private void restoreSelection() {
        ignoreSelectionEvents = true;
        try {
            keysList.setSelectedValue(selectedEntry, true);
        } finally {
            ignoreSelectionEvents = false;
        }
    }

    public void createNewEntry() {
        if (!switchSelection(newEntry)) return;

        if (!containsNewEntry()) {
            listModel.addElement(newEntry);
        }

        ignoreSelectionEvents = true;
        try {
            keysList.setSelectedValue(newEntry, true);
        } finally {
            ignoreSelectionEvents = false;
        }
        loadSelectedEntry(newEntry);
    }

    private boolean containsNewEntry() {
        for (int i = 0; i < listModel.getSize(); i++) {
            if (listModel.getElementAt(i) == newEntry) {
                return true;
            }
        }
        return false;
    }

    public void deleteSelectedEntry() {
        DataSourceConfigEntry entry = keysList.getSelectedValue();
        if (entry == null) return;

        int option = Messages.showConfirmationDialog(
                getProject(),
                "Delete data source config",
                "Delete configuration key '" + entry.getKey() + "'?",
                Messages.OPTIONS_YES_NO,
                1);

        if (option != 0) return;

        if (entry == newEntry) {
            listModel.removeElement(newEntry);
            clearDetails();
            notifyStateChanged();
            return;
        }

        try {
            service.deleteRecord(connection, entry.getKey());
            loadEntries(null);
        } catch (SQLException e) {
            Messages.showErrorDialog(getProject(), "Failed to delete data source config", e);
        }
    }

    public void reloadEntries() {
        if (selectedEntry != null && hasPendingChanges()) {
            int option = Messages.showConfirmationDialog(
                    getProject(),
                    "Discard changes",
                    "Reload will discard unsaved changes. Continue?",
                    Messages.OPTIONS_YES_NO,
                    1);
            if (option != 0) return;
        }
        loadEntries(null);
    }

    private void loadEntries(@Nullable String selectKey) {
        loadingEntries = true;
        setKeysEmptyText("Loading keys...");
        setStatus("Loading keys...");
        Dispatch.async(mainPanel,
                () -> {
                    try {
                        List<DataSourceConfigEntry> entries = service.loadEntries(connection);
                        return LoadResult.success(entries);
                    } catch (Exception e) {
                        return LoadResult.<List<DataSourceConfigEntry>>failure(e);
                    }
                },
                result -> {
                    if (result.error != null) {
                        loadingEntries = false;
                        setKeysEmptyText("Failed to load keys");
                        clearDetails();
                        Messages.showErrorDialog(getProject(), "Failed to load data source config keys", result.error);
                        setStatus("Failed to load keys. Use Reload to retry.");
                        return;
                    }

                    ignoreSelectionEvents = true;
                    try {
                        listModel.clear();
                        for (DataSourceConfigEntry entry : result.value) {
                            listModel.addElement(entry);
                        }
                    } finally {
                        ignoreSelectionEvents = false;
                    }

                    setStatus("Loaded " + listModel.getSize() + " key(s)");

                    if (listModel.isEmpty()) {
                        setKeysEmptyText("No configuration keys");
                        loadingEntries = false;
                        clearDetails();
                        return;
                    }
                    setKeysEmptyText("");

                    DataSourceConfigEntry selection = findEntry(selectKey);
                    if (selection == null) selection = listModel.getElementAt(0);

                    ignoreSelectionEvents = true;
                    try {
                        keysList.setSelectedValue(selection, true);
                    } finally {
                        ignoreSelectionEvents = false;
                    }
                    loadSelectedEntry(selection);
                    loadingEntries = false;
                });
    }

    private @Nullable DataSourceConfigEntry findEntry(@Nullable String key) {
        if (key == null || key.isBlank()) return null;
        for (int i = 0; i < listModel.getSize(); i++) {
            DataSourceConfigEntry entry = listModel.getElementAt(i);
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry;
            }
        }
        return null;
    }

    private void loadSelectedEntry(@Nullable DataSourceConfigEntry entry) {
        selectedEntry = entry;
        if (entry == null) {
            clearDetails();
            return;
        }

        if (entry == newEntry) {
            setEntryValues("new_key", "{}", "", false);
            return;
        }

        setStatus("Loading '" + entry.getKey() + "'...");
        Dispatch.async(mainPanel,
                () -> {
                    try {
                        DataSourceConfigRecord record = service.loadRecord(connection, entry.getKey());
                        return LoadResult.success(record);
                    } catch (Exception e) {
                        return LoadResult.<DataSourceConfigRecord>failure(e);
                    }
                },
                result -> {
                    if (result.error != null) {
                        Messages.showErrorDialog(getProject(), "Failed to load data source config", result.error);
                        clearDetails();
                        return;
                    }

                    DataSourceConfigRecord record = result.value;
                    if (record == null) {
                        clearDetails();
                        return;
                    }

                    setEntryValues(
                            record.getKey(),
                            record.getValue(),
                            record.getLastUpdated(),
                            true);
                    setStatus("Loaded '" + record.getKey() + "'");
                });
    }

    private void setEntryValues(String key, String value, String lastUpdated, boolean persisted) {
        suppressDirtyTracking = true;
        try {
            keyTextField.setText(key == null ? "" : key);
            keyTextField.setEditable(!persisted);
            lastUpdatedTextField.setText(lastUpdated == null ? "" : lastUpdated);
            updateLastUpdatedVisibility(persisted);
            Documents.setText(jsonEditor, value == null ? "" : value, true);

            selectedEntryPersisted = persisted;
            originalKey = keyTextField.getText();
            originalValue = readEditorText();
        } finally {
            suppressDirtyTracking = false;
        }

        Editors.setEditorReadonly(jsonEditor, false);
        notifyStateChanged();
    }

    private void clearDetails() {
        suppressDirtyTracking = true;
        try {
            selectedEntry = null;
            selectedEntryPersisted = false;
            originalKey = "";
            originalValue = "";

            keyTextField.setText("");
            keyTextField.setEditable(false);
            lastUpdatedTextField.setText("");
            updateLastUpdatedVisibility(false);
            Documents.setText(jsonEditor, "", false);
            Editors.setEditorReadonly(jsonEditor, true);
        } finally {
            suppressDirtyTracking = false;
        }
        notifyStateChanged();
    }

    public boolean hasPendingChanges() {
        if (selectedEntry == null) return false;

        String key = keyTextField.getText().trim();
        String value = readEditorText().trim();

        if (!selectedEntryPersisted && key.isEmpty() && value.isEmpty()) {
            return false;
        }

        if (!key.equals(originalKey)) return true;

        return !Json.checkJsonContentsEqual(originalValue, value);
    }

    private String readEditorText() {
        return jsonEditor == null ? "" : jsonEditor.getDocument().getText();
    }

    public boolean applyChanges() {
        if (selectedEntry == null) return true;

        String key = keyTextField.getText().trim();
        String value = readEditorText().trim();

        if (key.isEmpty()) {
            Messages.showWarningDialog(getProject(), "Validation", "Key is required.");
            return false;
        }

        if (value.isEmpty()) {
            Messages.showWarningDialog(getProject(), "Validation", "JSON payload is required.");
            return false;
        }

        try {
            Json.readAsMap(value);
        } catch (Exception e) {
            Messages.showErrorDialog(getProject(), "Invalid JSON payload", e);
            return false;
        }

        try {
            setStatus((selectedEntryPersisted ? "Saving '" : "Creating '") + key + "'...");
            if (selectedEntryPersisted) {
                service.updateRecord(connection, key, value);
            } else {
                service.insertRecord(connection, key, value);
            }

            loadEntries(key);
            return true;
        } catch (SQLException e) {
            Messages.showErrorDialog(getProject(), "Failed to save data source config", e);
            return false;
        }
    }

    private void notifyStateChanged() {
        if (stateChangeListener != null) {
            stateChangeListener.run();
        }
    }

    private void setStatus(String status) {
        statusLabel.setText(status == null ? " " : status);
    }

    private void setKeysEmptyText(@NotNull String text) {
        keysList.getEmptyText().setText(text);
    }

    private void updateLastUpdatedVisibility(boolean visible) {
        lastUpdatedLabel.setVisible(visible);
        lastUpdatedTextField.setVisible(visible);
    }

    private class CreateAction extends BasicAction {
        CreateAction() {
            super("New", null, AllIcons.General.Add);
        }

        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            createNewEntry();
        }
    }

    private class DeleteAction extends BasicAction {
        DeleteAction() {
            super("Delete", null, AllIcons.General.Remove);
        }

        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            deleteSelectedEntry();
        }
    }

    private class ReloadAction extends BasicAction {
        ReloadAction() {
            super("Reload", null, AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            reloadEntries();
        }
    }

    @Override
    public @NotNull JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        headerForm = null;
        Editors.releaseEditor(jsonEditor);
        jsonEditor = null;
        super.disposeInner();
    }

    private record LoadResult<T>(T value, Throwable error) {
        static <T> LoadResult<T> success(T value) {
            return new LoadResult<>(value, null);
        }

        static <T> LoadResult<T> failure(Throwable error) {
            return new LoadResult<>(null, error);
        }
    }

    private static class KeysListSpeedSearch extends SpeedSearchBase<JBList<DataSourceConfigEntry>> {
        KeysListSpeedSearch(JBList<DataSourceConfigEntry> list) {
            super(list);
        }

        @Override
        protected int getSelectedIndex() {
            return getComponent().getSelectedIndex();
        }

        @Override
        protected Object[] getElements() {
            DefaultListModel<DataSourceConfigEntry> model = (DefaultListModel<DataSourceConfigEntry>) getComponent().getModel();
            Object[] elements = new Object[model.getSize()];
            for (int i = 0; i < model.getSize(); i++) {
                elements[i] = model.getElementAt(i);
            }
            return elements;
        }

        @Override
        protected String getElementText(Object element) {
            if (element instanceof DataSourceConfigEntry entry) return entry.getKey();
            return "";
        }

        @Override
        protected void selectElement(Object element, String selectedText) {
            if (!(element instanceof DataSourceConfigEntry entry)) return;
            getComponent().setSelectedValue(entry, true);
        }
    }
}
