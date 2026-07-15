package com.dbn.liquibase.action;

import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_STATUS;
import static com.dbn.nls.NlsResources.txt;

/** Entry point for displaying Liquibase changelog status for a schema. */
public class ShowChangelogStatusAction extends LiquibaseSchemaAction {
    public ShowChangelogStatusAction(@NotNull DBSchema schema) {
        super(schema);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        executeOperation(project, SHOW_CHANGELOG_STATUS);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.liquibase.action.Operation_SHOW_CHANGELOG_STATUS"));
    }
}
