/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.assistant.profile.wizard;

import com.dbn.assistant.entity.Profile;
import com.dbn.assistant.entity.ProfileDBObjectItem;
import com.dbn.assistant.profile.wizard.validation.ProfileObjectsVerifier;
import com.dbn.assistant.service.DatabaseService;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.text.TextContent;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBDataset;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.nls.NlsResources.txt;

/**
 * Profile edition Object list step for edition wizard
 *
 * @see ProfileEditionWizard
 */
@Slf4j
public class ProfileEditionObjectListStep extends WizardStep<ProfileEditionWizardModel> implements Disposable {


  private static final int TABLES_COLUMN_HEADERS_NAME_IDX = 0;
  private static final int TABLES_COLUMN_HEADERS_OWNER_IDX = 1;

  private static final String[] PROFILE_OBJ_TABLES_COLUMN_HEADERS = {
      txt("profile.mgmt.obj_table.header.name"),
      txt("profile.mgmt.obj_table.header.owner")
  };
  private static final String[] DB_OBJ_TABLES_COLUMN_HEADERS = {
      txt("profile.mgmt.obj_table.header.name")
  };

  private JPanel mainPanel;
  private JBTextField filterTextField;
  private JTable profileObjectListTable;
  private JTable databaseObjectsTable;
  private DBNComboBox<DBSchema> schemaComboBox;
  private JPanel actionsPanel;
  private JPanel hintPanel;
  private JPanel initializingIconPanel;
  private final DatabaseService databaseSvc;

  private final ConnectionRef connection;
  private final Profile profile;
  private final boolean isUpdate;

  ProfileObjectsTableModel profileObjListTableModel = new ProfileObjectsTableModel();

/*
  //At start initialize it with empty one
  DatabaseObjectListTableModel currentDbObjListTableModel = new DatabaseObjectListTableModel();
  TableRowSorter<DatabaseObjectListTableModel> databaseObjectsTableSorter = new TableRowSorter<>();
  Map<String, DatabaseObjectListTableModel> databaseObjectListTableModelCache = new HashMap<>();
*/

  public ProfileEditionObjectListStep(ConnectionHandler connection, Profile profile, boolean isUpdate) {
    super(txt("profile.mgmt.object_list_step.title"),
        txt("profile.mgmt.object_list_step.explaination"));

    this.connection = connection.ref();
    this.databaseSvc = DatabaseService.getInstance(connection);

    this.profile = profile;
    this.isUpdate = isUpdate;

/*    if (this.profile != null)
      prefetchObjectForProfile(this.profile);*/

    initHintPanel();
    initObjectTables();
    initActionToolbar();
    initSchemaSelector();
    initFilterField();
/*
    schemaComboBox.addActionListener((e) -> {
      log.debug("action listener on  schemaComboBox fired");
      Object toBePopulated = schemaComboBox.getSelectedItem();
      if (toBePopulated == null)
        toBePopulated = schemaComboBox.getItemAt(0);
      if (toBePopulated != null) {
        populateDatabaseObjectTable(toBePopulated.toString());
      }
    });
*/

    profileObjListTableModel.addTableModelListener(l -> updateDatasetsFilter());

    if (isUpdate) {
      SwingUtilities.invokeLater(() -> {
        profileObjListTableModel.updateItems(profile.getObjectList());
      });
    }
    UserInterface.updateSplitPanes(mainPanel);
/*
    filterTextField.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(DocumentEvent e) {
        // experience showed that's never called
        assert false : "changedUpdate called??";
      }

      public void removeUpdate(DocumentEvent e) {
        if (log.isDebugEnabled())
          log.debug("patternFilter.removeUpdate doc length: " + e.getDocument().getLength());
        String filter = null;
        try {
          filter = e.getDocument().getText(0, e.getDocument().getLength()).trim();
        } catch (BadLocationException ignored) {
          log.debug("BadLocationException", ignored);
        }
        if (filter.isEmpty()) {
          // filter cleared
          triggerFiltering();
        }
        if (filter.length() > 2) {
          // filter cleared
          triggerFiltering();
        }
      }

      public void insertUpdate(DocumentEvent e) {
        if (log.isDebugEnabled())
          log.debug("patternFilter.insertUpdate doc length: " + e.getDocument().getLength());
        try {
          if (e.getDocument().getText(0, e.getDocument().getLength()).trim().length() > 2) {
            triggerFiltering();
          }
        } catch (BadLocationException ignored) {
          log.debug("BadLocationException", ignored);
        }
      }

      public void triggerFiltering() {
        if (log.isDebugEnabled()) {
          log.debug("triggering  fireTableDataChanged on " + ((DatabaseObjectListTableModel) databaseObjectsTable.getModel()));
          log.debug("    current model " + currentDbObjListTableModel);
        }
        currentDbObjListTableModel.fireTableDataChanged();
      }
    });

    withViewsButton.addItemListener((
        e -> {
          if (((JCheckBox) e.getSource()).isSelected()) {
            currentDbObjListTableModel.unhideItemByType(DatabaseObjectType.VIEW);
            currentDbObjListTableModel.unhideItemByType(DatabaseObjectType.MATERIALIZED_VIEW);
          } else {
            currentDbObjListTableModel.hideItemByType(DatabaseObjectType.VIEW);
            currentDbObjListTableModel.hideItemByType(DatabaseObjectType.MATERIALIZED_VIEW);
          }

        }
    ));

    selectAllCheckBox.addItemListener((
        e -> {
          if (((JCheckBox) e.getSource()).isSelected()) {
            databaseObjectsTable.selectAll();
          } else {
            databaseObjectsTable.clearSelection();
          }

        }
    ));*/
  }

  private void initHintPanel() {
    TextContent hintText = plain("AI-Profiles must include information about your data model to be forwarded to the language model. This will allow it to produce more accurate results, closely tailored to your data model. " +
            "The metadata can include database table names, column names, column data types, and comments. Your data will never be sent out to the language model.\n\n" +
            "Please find the datasets you want to include in the profile, and drag them to the container on the right. Start by selecting the schema.");
    DBNHintForm hintForm = new DBNHintForm(null, hintText, null, true);

    JComponent hintComponent = hintForm.getComponent();
    hintPanel.add(hintComponent);

  }

  private void initSchemaSelector() {
    schemaComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
    schemaComboBox.addListener((ov, nv) -> populateDatabaseObjectTable(nv));
    loadSchemas();
  }

  private void initFilterField() {
    filterTextField.getEmptyText().setText("Filter");
    onTextChange(filterTextField, e -> updateDatasetsFilter());
  }

/*  private Set<String> schemaInPrefetch = new HashSet<>();

  private void prefetchObjectForProfile(Profile profile) {
    schemaInPrefetch.clear();
    // TODO : implement bulk fetch
    profile.getObjectList().stream()
        .map(profileDBObjectItem -> profileDBObjectItem.getOwner())
        .distinct().forEach(schemaName -> {
          if (!databaseObjectListTableModelCache.containsKey(schemaName)) {
            log.debug("prefetching schema : " + schemaName);
            schemaInPrefetch.add(schemaName.toLowerCase());
            databaseSvc.getObjectItemsForSchema(schemaName).thenAccept(objs -> {
              DatabaseObjectListTableModel newModel = new DatabaseObjectListTableModel(objs, !withViewsButton.isSelected());
              log.debug("new schema prefetched: " + schemaName + " obj count: " + objs.size());
              newModel.hideItemByNames(
                  this.profile.getObjectList().stream().filter(o -> o.getOwner().equalsIgnoreCase(schemaName)).map(o -> o.getName()).collect(Collectors.toList()));

              databaseObjectListTableModelCache.put(schemaName, newModel);
              schemaInPrefetch.remove(schemaName.toLowerCase());
              this.profileObjectListTable.repaint();
            });
          }
        });
  }*/

  protected void initActionToolbar() {
    Supplier<Set<DBObjectType>> selectedDatasetTypes = () -> getDatasetFilter().getObjectTypes();
    Runnable toggleCallback = () -> updateDatasetsFilter();

    ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, "", true,
            DatasetTypeToggleAction.create(DBObjectType.TABLE, selectedDatasetTypes, toggleCallback),
            DatasetTypeToggleAction.create(DBObjectType.VIEW, selectedDatasetTypes, toggleCallback),
            DatasetTypeToggleAction.create(DBObjectType.MATERIALIZED_VIEW, selectedDatasetTypes, toggleCallback));

    JComponent component = actionToolbar.getComponent();
    component.setOpaque(false);
    component.setBorder(Borders.EMPTY_BORDER);
    actionsPanel.add(component, BorderLayout.CENTER);
    actionsPanel.setBorder(JBUI.Borders.empty(4));

    initializingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
  }


  private void updateDatasetsFilter() {
    AvailableDatasetsTableModel model = getDatasetsModel();
    AvailableDatasetsFilter filter = getDatasetFilter();
    filter.setNameToken(filterTextField.getText());

    Set<String> selectedElements = filter.getSelectedElements();
    selectedElements.clear();
    List<ProfileDBObjectItem> data = profileObjListTableModel.getData();
    data.forEach(d -> selectedElements.add(d.getOwner() + "." + d.getName()));

    model.notifyDataChanges();
  }

  private AvailableDatasetsFilter getDatasetFilter() {
    return getDatasetsModel().getFilter();
  }

  private AvailableDatasetsTableModel getDatasetsModel() {
    return (AvailableDatasetsTableModel) databaseObjectsTable.getModel();
  }

  private ConnectionHandler getConnection() {
    return ConnectionRef.ensure(connection);
  }

  private Project getProject() {
    return getConnection().getProject();
  }

  private void initObjectTables() {
    log.debug("initializing tables");

    ProfileObjectsTransferHandler th = new ProfileObjectsTransferHandler();

    initializeDatabaseObjectTable(th);
    initializeProfileObjectTable(th);
  }

/*
  private void resetDatabaseObjectTableModel(DatabaseObjectListTableModel m) {
    log.debug("resetDatabaseObjectTableModel for " + m);
    this.databaseObjectsTable.setModel(m);
    this.databaseObjectsTableSorter.setModel(m);
  }
*/

  private void initializeDatabaseObjectTable(ProfileObjectsTransferHandler th) {
    log.debug("initializing databaseObjectsTable");
    // keep this !
    // if set to true a RowSorter is created each the model changes
    // and that breaks our logic
    this.databaseObjectsTable.setAutoCreateRowSorter(false);
    this.databaseObjectsTable.setTransferHandler(th);
    this.databaseObjectsTable.setModel(new AvailableDatasetsTableModel());
    this.databaseObjectsTable.setTableHeader(null);
/*
    resetDatabaseObjectTableModel(currentDbObjListTableModel);
    this.databaseObjectsTableSorter.setRowFilter(new RowFilter<>() {

      @Override
      public boolean include(Entry<? extends DatabaseObjectListTableModel, ? extends Integer> entry) {
        String currentFilter = filterTextField.getText().trim().toLowerCase();
        // by default we show the entry
        boolean shallInclude = true;
        if (!currentFilter.isEmpty()) {
          //shallInclude = entry.getStringValue(TABLES_COLUMN_HEADERS_NAME_IDX).matches(currentFilter);
          // keep it simple for now
          shallInclude = entry.getStringValue(TABLES_COLUMN_HEADERS_NAME_IDX).toLowerCase().contains(currentFilter);
        }
        if (log.isDebugEnabled()) {
          log.debug(this + " filtering model " + entry.getModel() + " on [" + currentFilter + "] for [" +
              entry.getStringValue(TABLES_COLUMN_HEADERS_NAME_IDX) + "] => " + shallInclude);
        }
        return shallInclude;
      }
    });
    this.databaseObjectsTable.setRowSorter(this.databaseObjectsTableSorter);
*/
    this.databaseObjectsTable.setDragEnabled(true);
    this.databaseObjectsTable.setBackground(Colors.getTextFieldBackground());
    this.databaseObjectsTable.setGridColor(Colors.getTextFieldBackground());
    this.databaseObjectsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    this.databaseObjectsTable.addMouseListener(Mouse.listener().onClick(e -> {
      if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
        int selectedRow = databaseObjectsTable.getSelectedRow();
        DBDataset dataset = (DBDataset) databaseObjectsTable.getModel().getValueAt(selectedRow, 0);
        ProfileDBObjectItem profileItem = new ProfileDBObjectItem(
                dataset.getSchemaName(),
                dataset.getName());

        profileObjListTableModel.addItems(List.of(profileItem));
      }
    }));


/*
    this.databaseObjectsTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          JTable table = ((JTable) e.getSource());
          int[] selectedRows = table.getSelectedRows();
          if (selectedRows != null) {
            // hide all and add to profile obj list
            List<String> namesTobeHidden = new ArrayList<>(selectedRows.length);
            List<ProfileDBObjectItem> newItems = new ArrayList<>(selectedRows.length);

            for (int i = 0; i < selectedRows.length; i++) {
              DBObjectItem item = currentDbObjListTableModel.getItemAt(selectedRows[i]);
              newItems.add(new ProfileDBObjectItem(
                  item.getOwner(),
                  item.getName()));
              namesTobeHidden.add(item.getName());

            }
            profileObjListTableModel.addItems(newItems);
            currentDbObjListTableModel.hideItemByNames(namesTobeHidden);
          }
        }
      }
    });
    this.databaseObjectsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        DBObjectItem currentItem = currentDbObjListTableModel.getItemAt(row);
        if (currentItem.getType() == DatabaseObjectType.VIEW) {
          setIcon(Icons.DBO_VIEW);
        } else {
          setIcon(Icons.DBO_TABLE);
        }
        setText(value.toString());
        setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

        return this;
      }
    });
*/

    this.databaseObjectsTable.setDefaultRenderer(DBDataset.class,
            new ColoredTableCellRenderer() {
              @Override
              protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
                if (value == null) return;

                DBDataset dataset = (DBDataset) value;
                setIcon(dataset.getIcon());
                append(dataset.getName());
                setBorder(Borders.EMPTY_BORDER);
              }
            }
    );
    log.debug("initialization databaseObjectsTable complete");
  }

  private void initializeProfileObjectTable(ProfileObjectsTransferHandler th) {
    log.debug("initializing profileObjectListTable");
    this.profileObjectListTable.setTransferHandler(th);

    this.profileObjectListTable.setModel(profileObjListTableModel);
    this.profileObjectListTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    this.profileObjectListTable.setTableHeader(null);
    this.profileObjectListTable.setBackground(Colors.getTextFieldBackground());
    this.profileObjectListTable.setGridColor(Colors.getTextFieldBackground());
    this.profileObjectListTable.addMouseListener(Mouse.listener().onClick(e -> {
      if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
        int row = profileObjectListTable.rowAtPoint(e.getPoint());
        profileObjListTableModel.removeItem(row);
      }
    }));

    profileObjectListTable.setDefaultRenderer(Object.class, new ColoredTableCellRenderer() {
      @Override
      protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
        if (value == null) return;

        ProfileDBObjectItem profileItem = (ProfileDBObjectItem) value;
        append(profileItem.getOwner());
        append(".");
        append(profileItem.getName());
        setBorder(Borders.EMPTY_BORDER);

        DBObjectType objectType = locateTypeFor(profileItem);
        setIcon(objectType == null ? Icons.DBO_TMP_TABLE : objectType.getIcon());
      }
    });

/*
    profileObjectListTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          int row = profileObjectListTable.rowAtPoint(e.getPoint());
          if (row >= 0) {
            ProfileDBObjectItem item = profileObjListTableModel.getItemAt(row);
            // we may be viewing datbase object from another schema
            // locate it first. no way that cache not already populated
            DatabaseObjectListTableModel model = databaseObjectListTableModelCache.get(item.getOwner());
            assert model != null : "trying to unhide items form model not in the cache";
            // TODO : verify that this do nto fire event when this is not the current model
            //        of the table
            model.unhideItem(item.getOwner(), item.getName());
            profileObjListTableModel.removeItem(item);
          }
        }
      }
    });
    profileObjectListTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

        switch (column) {
          case ProfileObjectListTableModel.NAME_COLUMN_IDX:
            // Do not render icon etc... for owner cell
            // Check if the current row item is in the selectedTableModel
            setText((value != null) ? value.toString() : "*");
            ProfileDBObjectItem currentItem = profileObjListTableModel.getItemAt(row);
            DatabaseObjectType currentItemType;
            try {
              currentItemType = locateTypeFor(currentItem);
              setFont(getFont().deriveFont(Font.PLAIN));
              setToolTipText(null);
              if (currentItemType == DatabaseObjectType.VIEW) {
                setIcon(Icons.DBO_VIEW);
              } else {
                setIcon(Icons.DBO_TABLE);
              }
            } catch (IllegalStateException e) {
              if (schemaInPrefetch.contains(currentItem.getOwner().toLowerCase())) {
                setToolTipText(txt("profile.mgmt.object.information.loading"));
                setFont(getFont().deriveFont(Font.ITALIC));
              } else {
                setToolTipText(e.getMessage());
                setForeground(Color.RED);
              }
            }
            break;
          case ProfileObjectListTableModel.OWNER_COLUMN_IDX:
            setText(value.toString());
            setIcon(null);
            break;
          default:
            assert false : "unexpected column number";
        }

        return this;
      }
    });
  */
    profileObjectListTable.setInputVerifier(new ProfileObjectsVerifier());
    profileObjectListTable.getModel().addTableModelListener(e -> {
      if (e.getType() == TableModelEvent.INSERT) {
        profileObjectListTable.getInputVerifier().verify(profileObjectListTable);
      }
    });
    log.debug("initialization profileObjectListTable complete");
  }

  private void startActivityNotifier() {
    initializingIconPanel.setVisible(true);
  }

  /**
   * Stops the spining wheel
   */
  private void stopActivityNotifier() {
    initializingIconPanel.setVisible(false);
  }

  /**
   * Looks into DB object model for a matching type
   *
   * @param item object item in a profile
   * @return the type of that item or null if its a wildcard object (i.e name == null)
   * @throw IllegalStateException schema or object is not known to our model
   */
  private DBObjectType locateTypeFor(ProfileDBObjectItem item) throws IllegalStateException {
    DBSchema schema = getConnection().getObjectBundle().getSchema(item.getOwner());
    if (schema == null) return null;

    String datasetName = item.getName();
    DBObject dataset = Commons.coalesce(
            () -> schema.getChildObject(DBObjectType.TABLE, datasetName),
            () -> schema.getChildObject(DBObjectType.VIEW, datasetName),
            () -> schema.getChildObject(DBObjectType.MATERIALIZED_VIEW, datasetName));
    if (dataset == null) return null;

    return dataset.getObjectType();

/*
    if (item.getName() == null || item.getName().length() == 0) {
      return null;
    }

    DatabaseObjectListTableModel model = databaseObjectListTableModelCache.get(item.getOwner());

    if (model == null) {
      // surely a schema we do not know.
      // that's possible as profile ae populated by name.
      // object list as no guaranty to exist
      throw new IllegalStateException(txt("profile.mgmt.obj_list.unknown_schema"));
    }

    Optional<DBObjectItem> oitem = model.findFirst(item);
    if (oitem.isEmpty()) {
      // look for hidden ones then
      oitem = model.parkedItems.stream().filter(item::isEquivalentTo).findFirst();
    }
    if (oitem.isEmpty()) {
      throw new IllegalStateException(txt("profile.mgmt.obj_list.unknown_obj"));
    }
    // at this point, no way to no have something
    return oitem.get().getType();
*/
  }

  private void loadSchemas() {
    Background.run(getProject(), () -> {
      try {
        startActivityNotifier();
        DBObjectBundle objectBundle = getConnection().getObjectBundle();
        List<DBSchema> schemas = objectBundle.getSchemas(true);
        DBSchema schema = objectBundle.getUserSchema();
        schemaComboBox.setValues(schemas);
        schemaComboBox.setSelectedValue(schema);

      } finally {
        stopActivityNotifier();
      }
    });
/*
    log.debug("Loading schemas...");
    startActivityNotifier();
    databaseSvc.getSchemaNames().thenAccept(schemaList -> {
      for (String schema : schemaList) {
        log.debug("Adding new schema to dropbox: " + schema);
        schemaComboBox.addItem(schema);
      }
      stopActivityNotifier();
    }).exceptionally(e -> {
      Messages.showErrorDialog(project, "Cannot load schemas",
          "Cannot load DB schemas: " + e.getCause().getMessage());
      return null;
    });
*/
  }

  private void populateDatabaseObjectTable(DBSchema schema) {
    if (schema == null) return;
    Background.run(getProject(), () -> {
      try {
        startActivityNotifier();
        AvailableDatasetsTableModel model = getDatasetsModel();
        model.setDatasets(Collections.emptyList());
        model.notifyDataChanges();

        // long-lasting load process
        List<DBDataset> datasets = schema.getDatasets();

        // verify if schema selection changed meanwhile
        if (schema != schemaComboBox.getSelectedValue()) return;

        model.setDatasets(datasets);
        updateDatasetsFilter();
      } finally {
        stopActivityNotifier();
      }
    });


/*
    log.debug("populateDatabaseObjectTable for " + schema);

    DatabaseObjectListTableModel model = databaseObjectListTableModelCache.get(schema);

    if (model == null) {
      log.debug("populateDatabaseObjectTable no cache for " + schema);
      startActivityNotifier();
      databaseObjectsTable.setEnabled(false);
      databaseSvc.getObjectItemsForSchema(schema).thenAccept(objs -> {
        SwingUtilities.invokeLater(() -> {
          if (log.isDebugEnabled())
            log.debug("populateDatabaseObjectTable new model for " + schema + " objs count=" + objs.size());
          DatabaseObjectListTableModel newModel = new DatabaseObjectListTableModel(objs, !withViewsButton.isSelected());
          // remove the one already selected
          newModel.hideItemByNames(
              this.profile.getObjectList().stream().filter(o -> o.getOwner().equalsIgnoreCase(schema)).map(o -> o.getName()).collect(Collectors.toList()));
          databaseObjectListTableModelCache.put(schema, newModel);
          currentDbObjListTableModel = newModel;
          resetDatabaseObjectTableModel(currentDbObjListTableModel);
          stopActivityNotifier();
          databaseObjectsTable.setEnabled(true);
        });
      }).exceptionally(e -> {
        Messages.showErrorDialog(project, "Cannot fetch objects",
            "Cannot fetch database object list: " + e.getCause().getMessage());
        return null;
      });
    } else {
      currentDbObjListTableModel = model;
      resetDatabaseObjectTableModel(currentDbObjListTableModel);
    }
*/
  }


  @Override
  public @Nullable String getHelpId() {
    return null;
  }

  @Override
  public JComponent prepare(WizardNavigationState wizardNavigationState) {
    return mainPanel;
  }

  @Override
  public boolean onFinish() {
    if (profileObjectListTable.getInputVerifier().verify(profileObjectListTable)) {
      profile.setObjectList(profileObjListTableModel.getData());
    }
    return true;
  }

  @Override
  public void dispose() {
    // TODO dispose UI resources
  }
}
