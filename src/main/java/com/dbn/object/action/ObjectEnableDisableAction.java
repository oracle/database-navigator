package com.dbn.object.action;

import com.dbn.common.thread.Progress;
import com.dbn.common.util.Messages;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.operation.DBOperationNotSupportedException;
import com.dbn.object.common.operation.DBOperationType;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.ObjectManagementService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class ObjectEnableDisableAction extends AnObjectAction<DBSchemaObject> {
    ObjectEnableDisableAction(DBSchemaObject object) {
        super(object);
    }

    @Override
    protected void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull DBSchemaObject object) {

        ObjectManagementService objectManagementService = ObjectManagementService.getInstance(project);

        if (objectManagementService.supports(object)) {
            boolean enabled = object.getStatus().is(DBObjectStatus.ENABLED);
            ObjectChangeAction action = enabled ? ObjectChangeAction.DISABLE : ObjectChangeAction.ENABLE;
            objectManagementService.changeObject(object, action,null);
        } else {
            enableDisable(project, object);
        }
    }

    @Deprecated // TODO implement ObjectManagementService
    private void enableDisable(@NotNull Project project, @NotNull DBSchemaObject object) {
        boolean enabled = object.getStatus().is(DBObjectStatus.ENABLED);
        String title = enabled ?
                txt("msg.objects.title.DisablingObject") :
                txt("msg.objects.title.EnablingObject");

        String qualifiedObjectName = object.getQualifiedNameWithType();
        String text = enabled ?
                txt("msg.objects.info.DisablingObject", qualifiedObjectName) :
                txt("msg.objects.info.EnablingObject", qualifiedObjectName);

        Progress.prompt(project, object, false,
                title,
                text,
                progress -> {
                    try {
                        DBOperationType operationType = enabled ? DBOperationType.DISABLE : DBOperationType.ENABLE;
                        object.getOperationExecutor().executeOperation(operationType);
                    } catch (SQLException e1) {
                        conditionallyLog(e1);
                        String message = enabled ?
                                txt("msg.objects.error.DisablingObject", qualifiedObjectName) :
                                txt("msg.objects.error.EnablingObject", qualifiedObjectName);
                        Messages.showErrorDialog(project, message, e1);
                    } catch (DBOperationNotSupportedException e1) {
                        conditionallyLog(e1);
                        Messages.showErrorDialog(project, e1.getMessage());
                    }
                });
    }

    @Override
    protected void update(
            @NotNull AnActionEvent e,
            @NotNull Presentation presentation,
            @NotNull Project project,
            @Nullable DBSchemaObject target) {

        if (isValid(target)) {
            boolean enabled = target.getStatus().is(DBObjectStatus.ENABLED);
            String text = !enabled ?
                    txt("app.shared.action.Enable") :
                    txt("app.shared.action.Disable");

            presentation.setText(text);
            presentation.setVisible(true);
        } else {
            presentation.setVisible(false);
        }
    }
}