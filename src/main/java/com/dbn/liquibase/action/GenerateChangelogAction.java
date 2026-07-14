package com.dbn.liquibase.action;

import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/** Entry point for generating the initial Liquibase changelog for a schema. */
public class GenerateChangelogAction extends LiquibaseSchemaAction {
    public GenerateChangelogAction(@NotNull DBSchema schema) {
        super(schema);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        executeOperation(project, LiquibaseOperation.INITIALIZE);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.liquibase.action.GenerateChangelog"));
    }
}
