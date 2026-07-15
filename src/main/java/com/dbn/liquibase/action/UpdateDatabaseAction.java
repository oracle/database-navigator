package com.dbn.liquibase.action;

import com.dbn.common.icon.Icons;
import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_DATABASE;
import static com.dbn.nls.NlsResources.txt;

/** Entry point for applying pending Liquibase changesets to a schema. */
public class UpdateDatabaseAction extends LiquibaseSchemaAction {
    public UpdateDatabaseAction(@NotNull DBSchema schema) {
        super(schema);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        executeOperation(project, UPDATE_DATABASE);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.liquibase.action.Operation_UPDATE_DATABASE"));
        presentation.setIcon(Icons.ACTION_UPLOAD);
    }
}
