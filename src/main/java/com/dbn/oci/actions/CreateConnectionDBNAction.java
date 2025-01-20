package com.dbn.oci.actions;

import com.dbn.common.util.Dialogs;
import com.dbn.oci.ConnectionSettings;
import com.dbn.oci.ui.ConnectionConfigDialog;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.oracle.oci.intellij.api.ext.ContributeADBActions;

import java.awt.event.ActionEvent;

public class CreateConnectionDBNAction extends ContributeADBActions.ExtensionContextAction {
  ConnectionSettings connectionSettings;
  public CreateConnectionDBNAction(ConnectionSettings context, String title) {
    super(title);
    this.connectionSettings = context;
  }

  @Override
  protected void doAction(ActionEvent actionEvent) {
    DataContext dataContext = DataManager.getInstance().getDataContext();
    Project project =  dataContext.getData(CommonDataKeys.PROJECT);

    Dialogs.show(() -> new ConnectionConfigDialog(project,"Connection Config", false,connectionSettings ));
  }
}
