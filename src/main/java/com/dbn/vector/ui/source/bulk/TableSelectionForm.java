package com.dbn.vector.ui.source.bulk;

import com.dbn.common.dispose.Checks;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.vector.model.request.EmbeddingSourceTable;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.SCHEMA;

/**
 * Dual-panel table selection form.
 * Left panel: Available tables from selected schema
 * Right panel: Selected tables for embedding
 * Bottom panel: Column configuration for selected table
 */
public class TableSelectionForm extends VectorToolboxFormBase {
    // Main container
    private JPanel mainPanel;

    // Instruction
    private JLabel instructionLabel;

    // Schema selection
    private JLabel schemaLabel;
    private DBObjectSelector<DBSchema> schemaComboBox;

    // Left panel - Available tables
    private JScrollPane availableScrollPane;
    private JList<DBTable> availableTablesList;
    private JLabel availableHintLabel;
    private DefaultListModel<DBTable> availableTablesModel;

    // Center - Transfer buttons
    private JPanel buttonPanel;
    private JButton addButton;
    private JButton removeButton;
    private JButton addAllButton;
    private JButton removeAllButton;

    // Right panel - Selected tables
    private JScrollPane selectedScrollPane;
    private JList<DBTable> selectedTablesList;
    private JLabel selectedHintLabel;
    private DefaultListModel<DBTable> selectedTablesModel;

    // Bottom panel - Column configuration
    private JPanel configPanel;
    private JLabel configHintLabel;
    private JLabel selectedTableLabel;
    private JLabel keyColumnLabel;
    private JLabel dataColumnLabel;
    private DBObjectSelector<DBColumn> keyColumnComboBox;
    private DBObjectSelector<DBColumn> dataColumnComboBox;

    // Store column selections per table
    private final Map<DBTable, DBColumn> keyColumnSelections = new HashMap<>();
    private final Map<DBTable, DBColumn> dataColumnSelections = new HashMap<>();

    private final ConnectionRef connection;

    public TableSelectionForm(@NotNull TableSelectionDialog parent, @NotNull ConnectionHandler connection) {
        super(parent);
        this.connection = connection.ref();

        initListModels();
        initComboBoxes();
        initListRenderers();

        // Initial UI state
        updateHints();
        updateConfigPanel();
    }

    @Override
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    // ========== Initialization ==========

    private void initListModels() {
        availableTablesModel = new DefaultListModel<>();
        selectedTablesModel = new DefaultListModel<>();

        availableTablesList.setModel(availableTablesModel);
        selectedTablesList.setModel(selectedTablesModel);

        availableTablesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectedTablesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    private void initComboBoxes() {
        schemaComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(this::getConnection)
                .withValueLoader(this::loadSchemas)
                .withValuePreselector(() -> null)  // No initial selection
                .triggerLoad();

        keyColumnComboBox
                .initialize(this, COLUMN)
                .withConnectionContext(this::getConnection)
                .withValueLoader(this::loadKeyColumns)
                .withValuePreselector(() -> null);

        dataColumnComboBox
                .initialize(this, COLUMN)
                .withConnectionContext(this::getConnection)
                .withValueLoader(this::loadDataColumns)
                .withValuePreselector(() -> null);

        updateFieldAvailability();
    }

    private void initListRenderers() {
        // Renderer for available tables
        availableTablesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DBTable) {
                    DBTable table = (DBTable) value;
                    setText(table.getName());
                    setIcon(table.getIcon());
                }
                return this;
            }
        });

        // Renderer for selected tables (with status icon if configured)
        selectedTablesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DBTable) {
                    DBTable table = (DBTable) value;
                    boolean isConfigured = isTableConfigured(table);

                    setText(table.getName());

                    // Show check or warning icon based on configuration status
                    if (isConfigured) {
                        setIcon(Icons.COMMON_CHECK);
                        setToolTipText("Configured: " + getTableConfigSummary(table));
                    } else {
                        setIcon(Icons.COMMON_WARNING);
                        setToolTipText("Not configured - click to configure columns");
                    }
                }
                return this;
            }
        });
    }

    // ========== Field Availability ==========

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(
                () -> Checks.isValid(getSelectedSchema()),
                array(availableTablesList, addButton, addAllButton)
        );
        fieldAdapter.initFieldsAvailability(
                () -> getSelectedTableForConfig() != null,
                array(keyColumnComboBox, dataColumnComboBox)
        );
    }

    // ========== Event Listeners ==========

    @Override
    protected void initEventListeners() {
        // Schema change → reload available tables
        onSelectionChange(schemaComboBox, v -> populateAvailableTables());

        // Button actions
        addButton.addActionListener(e -> moveSelectedToRight());
        removeButton.addActionListener(e -> moveSelectedToLeft());
        addAllButton.addActionListener(e -> moveAllToRight());
        removeAllButton.addActionListener(e -> moveAllToLeft());

        // Selected table change → update column config
        selectedTablesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectedTableChanged();
            }
        });

        // Column selections → store per table
        onSelectionChange(keyColumnComboBox, col -> storeKeyColumnSelection());
        onSelectionChange(dataColumnComboBox, col -> storeDataColumnSelection());
    }

    // ========== Hint Updates ==========

    private void updateHints() {
        // Available tables hint
        if (getSelectedSchema() == null) {
            availableHintLabel.setText(" Select a schema to load tables");
            availableHintLabel.setVisible(true);
        } else if (availableTablesModel.isEmpty()) {
            availableHintLabel.setText(" No tables available (all selected or none in schema)");
            availableHintLabel.setVisible(true);
        } else {
            availableHintLabel.setText(" " + availableTablesModel.size() + " table(s) available");
            availableHintLabel.setVisible(true);
        }

        // Selected tables hint
        if (selectedTablesModel.isEmpty()) {
            selectedHintLabel.setText(" Use [>] to add tables here");
            selectedHintLabel.setVisible(true);
        } else {
            int configured = countConfiguredTables();
            int total = selectedTablesModel.size();
            if (configured == total) {
                selectedHintLabel.setText(" ✓ All " + total + " table(s) configured");
            } else {
                selectedHintLabel.setText(" " + configured + "/" + total + " table(s) configured");
            }
            selectedHintLabel.setVisible(true);
        }

        // Config hint
        if (getSelectedTableForConfig() == null) {
            configHintLabel.setText("Click a table in 'Selected Tables' to configure its columns");
            configHintLabel.setVisible(true);
        } else {
            configHintLabel.setVisible(false);
        }
    }

    private int countConfiguredTables() {
        int count = 0;
        for (int i = 0; i < selectedTablesModel.size(); i++) {
            if (isTableConfigured(selectedTablesModel.get(i))) {
                count++;
            }
        }
        return count;
    }

    // ========== Table Population ==========

    private void populateAvailableTables() {
        availableTablesModel.clear();
        selectedTablesModel.clear();
        keyColumnSelections.clear();
        dataColumnSelections.clear();

        List<DBTable> tables = loadTables();
        for (DBTable table : tables) {
            availableTablesModel.addElement(table);
        }

        updateFieldAvailability();
        updateHints();
        updateConfigPanel();
    }

    // ========== Transfer Actions ==========

    private void moveSelectedToRight() {
        List<DBTable> selected = availableTablesList.getSelectedValuesList();
        for (DBTable table : selected) {
            availableTablesModel.removeElement(table);
            selectedTablesModel.addElement(table);
        }

        // Auto-select first moved table to show config panel
        if (!selected.isEmpty()) {
            int lastIndex = selectedTablesModel.size() - 1;
            selectedTablesList.setSelectedIndex(lastIndex);
        }

        updateFieldAvailability();
        updateHints();
    }

    private void moveSelectedToLeft() {
        List<DBTable> selected = selectedTablesList.getSelectedValuesList();
        for (DBTable table : selected) {
            selectedTablesModel.removeElement(table);
            availableTablesModel.addElement(table);
            // Clear column config for removed tables
            keyColumnSelections.remove(table);
            dataColumnSelections.remove(table);
        }
        updateFieldAvailability();
        updateHints();
        updateConfigPanel();
    }

    private void moveAllToRight() {
        for (int i = 0; i < availableTablesModel.size(); i++) {
            selectedTablesModel.addElement(availableTablesModel.get(i));
        }
        availableTablesModel.clear();

        // Auto-select first table to show config panel
        if (selectedTablesModel.size() > 0) {
            selectedTablesList.setSelectedIndex(0);
        }

        updateFieldAvailability();
        updateHints();
    }

    private void moveAllToLeft() {
        for (int i = 0; i < selectedTablesModel.size(); i++) {
            DBTable table = selectedTablesModel.get(i);
            availableTablesModel.addElement(table);
            keyColumnSelections.remove(table);
            dataColumnSelections.remove(table);
        }
        selectedTablesModel.clear();
        updateFieldAvailability();
        updateHints();
        updateConfigPanel();
    }

    // ========== Column Configuration ==========

    private void onSelectedTableChanged() {
        updateConfigPanel();
        updateHints();

        DBTable table = getSelectedTableForConfig();
        if (table == null) {
            return;
        }

        // Reload column combos for this table
        keyColumnComboBox.reloadValues();
        dataColumnComboBox.reloadValues();

        // Restore previous selections if any
        DBColumn savedKeyCol = keyColumnSelections.get(table);
        DBColumn savedDataCol = dataColumnSelections.get(table);

        if (savedKeyCol != null) {
            keyColumnComboBox.setSelectedValue(savedKeyCol);
        }
        if (savedDataCol != null) {
            dataColumnComboBox.setSelectedValue(savedDataCol);
        }
    }

    private void updateConfigPanel() {
        DBTable table = getSelectedTableForConfig();
        if (table == null) {
            selectedTableLabel.setText("(none)");
            keyColumnComboBox.setEnabled(false);
            dataColumnComboBox.setEnabled(false);
        } else {
            selectedTableLabel.setText(table.getName());
            keyColumnComboBox.setEnabled(true);
            dataColumnComboBox.setEnabled(true);
        }
        updateFieldAvailability();
    }

    private void storeKeyColumnSelection() {
        DBTable table = getSelectedTableForConfig();
        DBColumn column = ComboBoxes.getSelection(keyColumnComboBox);
        if (table != null && column != null) {
            keyColumnSelections.put(table, column);
            selectedTablesList.repaint();  // Update checkmark
            updateHints();
        }
    }

    private void storeDataColumnSelection() {
        DBTable table = getSelectedTableForConfig();
        DBColumn column = ComboBoxes.getSelection(dataColumnComboBox);
        if (table != null && column != null) {
            dataColumnSelections.put(table, column);
            selectedTablesList.repaint();  // Update checkmark
            updateHints();
        }
    }

    // ========== Data Loaders ==========

    private List<DBColumn> loadKeyColumns() {
        DBTable table = getSelectedTableForConfig();
        if (table == null) return Collections.emptyList();

        List<DBColumn> keyColumns = table.getPrimaryKeyColumns();
        if (keyColumns.isEmpty()) {
            // Fallback to all columns if no PK defined
            keyColumns = table.getColumns();
        }
        return keyColumns;
    }

    private List<DBColumn> loadDataColumns() {
        DBTable table = getSelectedTableForConfig();
        if (table == null) return Collections.emptyList();

        return Lists.filter(table.getColumns(),
                c -> c.getDataType().isLiteral() && !c.isPrimaryKey());
    }

    // ========== Getters ==========

    @Nullable
    public DBSchema getSelectedSchema() {
        return ComboBoxes.getSelection(schemaComboBox);
    }

    @Nullable
    private DBTable getSelectedTableForConfig() {
        return selectedTablesList.getSelectedValue();
    }

    private boolean isTableConfigured(DBTable table) {
        return keyColumnSelections.containsKey(table)
                && dataColumnSelections.containsKey(table);
    }

    private String getTableConfigSummary(DBTable table) {
        DBColumn keyCol = keyColumnSelections.get(table);
        DBColumn dataCol = dataColumnSelections.get(table);
        if (keyCol == null || dataCol == null) return "incomplete";
        return "ID=" + keyCol.getName() + ", Data=" + dataCol.getName();
    }

    // ========== Public API for Dialog ==========

    /**
     * Get the configured table sources from user selections.
     * Only returns tables that have both key and data columns configured.
     */
    public List<EmbeddingSourceTable> getSelectedTableSources() {
        List<EmbeddingSourceTable> result = new ArrayList<>();
        DBSchema schema = getSelectedSchema();

        if (schema == null) return result;

        for (int i = 0; i < selectedTablesModel.size(); i++) {
            DBTable table = selectedTablesModel.get(i);
            DBColumn keyCol = keyColumnSelections.get(table);
            DBColumn dataCol = dataColumnSelections.get(table);

            // Skip tables without column configuration
            if (keyCol == null || dataCol == null) continue;

            EmbeddingSourceTable source = new EmbeddingSourceTable();
            source.setSchemaName(schema.getName());
            source.setTableName(table.getName());
            source.setKeyColumnName(keyCol.getName());
            source.setDataColumnName(dataCol.getName());

            result.add(source);
        }

        return result;
    }

    /**
     * Check if all selected tables have valid column configuration.
     */
    public boolean isValid() {
        if (selectedTablesModel.isEmpty()) return false;

        for (int i = 0; i < selectedTablesModel.size(); i++) {
            DBTable table = selectedTablesModel.get(i);
            if (!isTableConfigured(table)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get list of tables missing configuration (for validation message).
     */
    public List<String> getTablesWithoutConfig() {
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < selectedTablesModel.size(); i++) {
            DBTable table = selectedTablesModel.get(i);
            if (!isTableConfigured(table)) {
                missing.add(table.getName());
            }
        }
        return missing;
    }

    @Override
    @NotNull
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private void createUIComponents() {
        // Custom component initialization for form designer
        availableTablesList = new JList<>();
        selectedTablesList = new JList<>();
        schemaComboBox = new DBObjectSelector<>();
        keyColumnComboBox = new DBObjectSelector<>();
        dataColumnComboBox = new DBObjectSelector<>();
    }
}
