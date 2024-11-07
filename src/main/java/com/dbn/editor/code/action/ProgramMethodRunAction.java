package com.dbn.editor.code.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectPopupAction;
import com.dbn.common.icon.Icons;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.method.MethodExecutionManager;
import com.dbn.execution.method.ui.MethodExecutionHistory;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.action.AnObjectAction;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Actions.SEPARATOR;

@BackgroundUpdate
public class ProgramMethodRunAction extends ProjectPopupAction {

    @Override
    public AnAction[] getChildren(AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();
        Project project = e.getProject();
        DBSourceCodeVirtualFile sourceCodeFile = getSourcecodeFile(e);
        if (project != null && sourceCodeFile != null) {
            DBSchemaObject schemaObject = sourceCodeFile.getObject();
            if (schemaObject.getObjectType().matches(DBObjectType.PROGRAM)) {

                MethodExecutionManager methodExecutionManager = MethodExecutionManager.getInstance(project);
                MethodExecutionHistory executionHistory = methodExecutionManager.getExecutionHistory();
                List<DBMethod> recentMethods = executionHistory.getRecentlyExecutedMethods((DBProgram) schemaObject);

                if (recentMethods != null) {
                    for (DBMethod method : recentMethods) {
                        RunMethodAction action = new RunMethodAction(method);
                        actions.add(action);
                    }
                    actions.add(SEPARATOR);
                }

                List<? extends DBObject> objects = schemaObject.collectChildObjects(DBObjectType.METHOD);
                for (DBObject object : objects) {
                    if (recentMethods == null || !recentMethods.contains(object)) {
                        RunMethodAction action = new RunMethodAction((DBMethod) object);
                        actions.add(action);
                    }
                }
            }
        }

        return actions.toArray(new AnAction[0]);
    }

    @Nullable
    private DBSourceCodeVirtualFile getSourcecodeFile(AnActionEvent e) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        return virtualFile instanceof DBSourceCodeVirtualFile ? (DBSourceCodeVirtualFile) virtualFile : null;
    }


    @Override
    public void update(@NotNull AnActionEvent e, Project project) {
        DBSourceCodeVirtualFile sourceCodeFile = getSourcecodeFile(e);
        Presentation presentation = e.getPresentation();
        boolean visible = false;
        if (sourceCodeFile != null) {
            DBSchemaObject schemaObject = sourceCodeFile.getObject();
            if (schemaObject.getObjectType().matches(DBObjectType.PROGRAM)) {
                visible = true;
            }
        }

        presentation.setVisible(visible);
        presentation.setText("Run Method");
        presentation.setIcon(Icons.METHOD_EXECUTION_RUN);
    }

    public class RunMethodAction extends AnObjectAction<DBMethod> {
        RunMethodAction(DBMethod method) {
            super(method);
        }

        @Override
        protected void actionPerformed(
                @NotNull AnActionEvent e,
                @NotNull Project project,
                @NotNull DBMethod object) {

            MethodExecutionManager executionManager = MethodExecutionManager.getInstance(project);
            executionManager.startMethodExecution(object, DBDebuggerType.NONE);
        }
    }
}
