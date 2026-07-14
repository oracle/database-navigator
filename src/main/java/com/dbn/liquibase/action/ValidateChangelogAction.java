package com.dbn.liquibase.action;

import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.execution.LiquibaseOperation.VALIDATE;
import static com.dbn.nls.NlsResources.txt;

/** Entry point for validating the Liquibase changelog for a schema. */
public class ValidateChangelogAction extends LiquibaseSchemaAction {
    public ValidateChangelogAction(@NotNull DBSchema schema) {
        super(schema);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        executeOperation(project, VALIDATE);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.liquibase.action.ValidateChangelog"));
    }
}
