package com.dbn.object.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.events.EventNotificationManager;
import com.dbn.events.RegistrationManager;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

import static com.dbn.nls.NlsResources.txt;

public class TableDisableDCNAction extends ProjectAction {
  private final DBObjectRef<DBTable> object;

  public TableDisableDCNAction(DBTable object) {
    this.object = DBObjectRef.of(object);
  }

  @Override
  protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
    Presentation presentation = e.getPresentation();
    //todo the name should be changeable depending on if hte table already registred .
    presentation.setText(txt("app.objects.action.Disable"));
    presentation.setIcon(Icons.TABLE_Disable_DCN);
  }

  public DBSchemaObject getObject() {
    return DBObjectRef.ensure(object);
  }

  @Nullable
  @Override
  public Project getProject() {
    return getObject().getProject();
  }

  @Override
  protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {

    System.out.println("Disabling DCN");
    try {
      RegistrationManager.getInstance().stopListening(object.getQualifiedName(), object.getConnection());
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }


//    DBSchemaObject object = getObject();
//    DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
//    editorManager.connectAndOpenEditor(object, EditorProviderId.DATA, false, true);
  }
}
