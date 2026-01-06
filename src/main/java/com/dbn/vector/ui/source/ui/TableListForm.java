package com.dbn.vector.ui.source.ui;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.file.VirtualFileList;
import com.dbn.common.ui.file.VirtualFileListModel;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.sourceconfig.DbTableSource;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ToolbarDecorator;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class TableListForm extends DBNFormBase {
  private JPanel component;
  private JLabel titleLabel;
  private JPanel listPanel;

  @Getter private DBTableList tableList;
  public TableListForm(DBNComponent parent, String title, ConnectionHandler connection) {
    this(parent,title,connection,new ArrayList<>());
    System.out.println("TableListForm");
  }

  public TableListForm(DBNComponent parent, String title,  ConnectionHandler connection,List<DbTableSource> elements) {
    super(parent);
    titleLabel.setText(title);
    tableList = new DBTableList(elements,getProject(),connection);
    listPanel.add(initListComponent());
  }
  private JPanel initListComponent() {
    ToolbarDecorator decorator = createToolbarDecorator(tableList);
    decorator.setAddAction(b -> tableList.insertRows());
    decorator.setRemoveAction(b -> tableList.removeRows());
    decorator.setMoveUpAction(b -> tableList.moveRowsUp());
    decorator.setMoveDownAction(b -> tableList.moveRowsDown());

    return createToolbarDecoratorComponent(decorator, tableList);
  }

  @Override
  protected JComponent getMainComponent() {
    return component;
  }

  public List<DbTableSource> getTables(){
    return tableList.getModel().getTables();
  }

  public void setTables(List<DbTableSource> tables) {
    DBTableListModel model = tableList.getModel();
    model.reset(tables);
  }
}
