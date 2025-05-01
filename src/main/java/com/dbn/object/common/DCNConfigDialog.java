package com.dbn.object.common;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.events.RegistrationManager;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

public class DCNConfigDialog extends DBNDialog<DCNConfigForm> {
  private final DBObjectRef<DBTable> object;
  private int mask;

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
  protected @Nullable ValidationInfo doValidate() {
    DCNConfigForm form = getForm();
    int mask = 0;
    boolean insertOperation = form.isInsert();
    boolean updateOperation = form.isUpdate();
    boolean deleteOperation = form.isDelete();

    if (insertOperation) mask |= 2;    // INSERTOP
    if (updateOperation) mask |= 4;    // UPDATEOP
    if (deleteOperation) mask |= 8;    // DELETEOP

    this.mask = mask;
    if (!insertOperation && !updateOperation && !deleteOperation) {
      return new ValidationInfo("At least one operation should be selected !");
    }
    return super.doValidate();
  }

  @Override
  protected void doOKAction() {
    System.out.println("doOKAction");
    try {
      // in backgound
      RegistrationManager.getInstance().startListening(object.getParent()+"."+object.getObjectName(),object.getConnection(),mask);
      close(OK_EXIT_CODE);
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


