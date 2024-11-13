package com.dbn.object.action;

import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.ObjectManagementService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isValid;

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
            throw new UnsupportedOperationException();
        }
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