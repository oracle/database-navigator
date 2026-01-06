package com.dbn.vector.ui.source.ui;

import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.sourceconfig.DbTableSource;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class DBTableList extends JList<DbTableSource> {
  @Setter
  private Project project;
  @Setter
  private ConnectionHandler connection;

  public DBTableList(List<DbTableSource> tableSources, @Nullable Project project, ConnectionHandler connection) {
    super(new DBTableListModel(tableSources));
    this.project = project;
    this.connection = connection;
    setCellRenderer(new DBTableListCellRenderer());
    setVisibleRowCount(5);
  }

  public void insertRows() {
    if (project == null || connection == null) {
      System.out.println("DBTableList.insertRows: project or connection is null!");
      return; // Cannot open dialog without project and connection
    }

    System.out.println("DBTableList.insertRows: Opening dialog...");

    Dialogs.show(
            () -> new TableSelectionDialog(project, connection),
            (dialog, exitCode) -> {
              System.out.println("DBTableList.insertRows: Dialog closed with exitCode=" + exitCode);
              if (exitCode == DialogWrapper.OK_EXIT_CODE) {
                List<DbTableSource> selectedTables = dialog.getSelectedTableSources();
                System.out.println("DBTableList.insertRows: Got " + selectedTables.size() + " tables");
                for (DbTableSource t : selectedTables) {
                  System.out.println("  - " + t.getSchemaName() + "." + t.getTableName());
                }
                DBTableListModel model = getModel();
                model.addAll(selectedTables);
                System.out.println("DBTableList.insertRows: Model now has " + model.getSize() + " items");
              }
            }
    );
  }

  public void removeRows() {
    DBTableListModel model = getModel();
    int[] indices = getSelectedIndices();

    model.removeRows(indices);
    setSelectedIndices(new int[0]);
  }

  public void moveRowsUp() {
    DBTableListModel model = getModel();
    int[] indices = getSelectedIndices();
    model.moveRowsUp(indices);

    for (int i = 0; i < indices.length; i++) indices[i]--;
    setSelectedIndices(indices);
  }

  public void moveRowsDown() {
    DBTableListModel model = getModel();
    int[] indices = getSelectedIndices();
    model.moveRowsDown(indices);

    for (int i = 0; i < indices.length; i++) indices[i]++;
    setSelectedIndices(indices);
  }

  public DBTableListModel getModel() {
    return (DBTableListModel) super.getModel();
  }

  public List<DbTableSource> getTables() {
    return getModel().getTables();
  }
}
