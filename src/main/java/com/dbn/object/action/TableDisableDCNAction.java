package com.dbn.object.action;

import com.dbn.common.icon.Icons;
import com.dbn.events.listener.EventListenerManager;
import com.dbn.object.DBTable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class TableDisableDCNAction extends AnObjectAction<DBTable> {

  public TableDisableDCNAction(DBTable table) {
    super(table);
  }


  @Override
  protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DBTable target) {
    //todo the name should be changeable depending on if hte table already registred .
    presentation.setText(txt("app.objects.action.Disable"));
    presentation.setIcon(Icons.TABLE_Disable_DCN);
  }


  @Override
  protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBTable target) {
    System.out.println("Disabling DCN");
    EventListenerManager.getInstance().unregisterTable(getTarget());
  }
}
