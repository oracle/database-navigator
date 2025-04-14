package com.dbn.oci.actions;

import com.dbn.common.util.Dialogs;
import com.dbn.oci.OciConnectionData;
import com.dbn.oci.ui.OciConnectionInputDialog;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.oracle.oci.intellij.api.ext.ContributeADBActions;

import java.awt.event.ActionEvent;
import java.util.Collections;

public class OciConnectionCreateAction extends ContributeADBActions.ExtensionContextAction {
  OciConnectionData connectionData;
  public OciConnectionCreateAction(OciConnectionData context, String title) {
    super(title, Collections.emptyList(), false, true);
    this.connectionData = context;
  }

  @Override
  protected void doAction(ActionEvent actionEvent) {
    DataContext dataContext = DataManager.getInstance().getDataContext();
    Project project =  dataContext.getData(CommonDataKeys.PROJECT);

    Dialogs.show(() -> new OciConnectionInputDialog(project,"Connection Details", false, connectionData));
  }
}
