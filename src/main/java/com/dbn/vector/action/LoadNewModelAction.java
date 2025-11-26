//package com.dbn.vector.action;
//
//import com.dbn.common.action.ProjectAction;
//import com.dbn.common.icon.Icons;
//import com.dbn.common.thread.Progress;
//import com.dbn.connection.ConnectionAction;
//import com.dbn.object.DBAIModel;
//import com.dbn.object.action.AnObjectAction;
//import com.dbn.object.common.list.DBObjectList;
//import com.dbn.vector.DatabaseVectorManager;
//import com.intellij.openapi.actionSystem.AnActionEvent;
//import com.intellij.openapi.actionSystem.Presentation;
//import com.intellij.openapi.project.Project;
//import org.jetbrains.annotations.NotNull;
//
//import static com.dbn.nls.NlsResources.txt;
//
//public class LoadNewModelAction extends ProjectAction {
//    private final DBObjectList objectList;
//
//    public LoadNewModelAction(DBObjectList objectList) {
//        this.objectList = objectList;
//    }
//
//    @Override
//    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
//        Presentation presentation = e.getPresentation();
//        presentation.setText(objectList.isLoaded() ?
//                txt("app.objects.action.Reload") :
//                txt("app.objects.action.Load"));
//        presentation.setIcon(Icons.ACTION_REFRESH);
//    }
//
//    @Override
//    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
//        String listName = objectList.getCapitalizedName();
//
//        String title = objectList.isLoaded() ?
//                txt("msg.objects.title.ReloadingObjects", listName) :
//                txt("msg.objects.title.LoadingObjects", listName);
//        ConnectionAction.invoke(
//                title, true, objectList,
//                action -> Progress.prompt(project, objectList, true,
//                        txt("prc.objects.title.LoadingObjects"),
//                        txt("prc.objects.text.ReloadingObjects", objectList.getContentDescription()),
//                        progress -> {
//                            objectList.getConnection().getMetaDataCache().reset();
//                            objectList.reload();
//                        }));
//    }
//}
