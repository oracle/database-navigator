package com.dbn.object.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.events.EventNotificationManager;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class TableEnableDCNAction extends ProjectAction {
  private final DBObjectRef<DBTable> object;

  public TableEnableDCNAction(DBTable object) {
    this.object = DBObjectRef.of(object);
  }

  @Override
  protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
    Presentation presentation = e.getPresentation();
    //todo the name should be changeable depending on if hte table already registred .
    presentation.setText(txt("app.objects.action.EnableDCN"));
    presentation.setIcon(Icons.TABLE_ENABLE_DCN);
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

    System.out.println("enabling DCN");
    EventNotificationManager eventNotificationManager = EventNotificationManager.getInstance(project);
    eventNotificationManager.openEditorAndConfig(object);

//    DBSchemaObject object = getObject();
//    DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
//    editorManager.connectAndOpenEditor(object, EditorProviderId.DATA, false, true);
  }
}
