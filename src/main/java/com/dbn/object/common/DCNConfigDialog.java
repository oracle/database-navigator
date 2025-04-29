package com.dbn.object.common;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.events.RegistrationManager;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class DCNConfigDialog extends DBNDialog<DCNConfigForm> {
  private final DBObjectRef<DBTable> object;

  public DCNConfigDialog(Project project, DBTable object) {
    super(project, "DCN config", true);
    this.object = DBObjectRef.of(object);
    setModal(false);
    setResizable(true);
    renameAction(getCancelAction(), "Close");
    init();
  }

  @NotNull
  @Override
  protected DCNConfigForm createForm() {
    DBTable object = DBObjectRef.get(this.object);
    return new DCNConfigForm(this, object);
  }

  @Override
  public void doCancelAction() {
    super.doCancelAction();
  }

  //todo add validation layer
  /*
  at least one operation should be selected .
   */



  @Override
  protected void doOKAction() {
    System.out.println("doOKAction");
    try {
      // in backgound
      RegistrationManager.getInstance().startListening(object.getParent()+"."+object.getObjectName(),object.getConnection());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    super.doOKAction();
  }
}


