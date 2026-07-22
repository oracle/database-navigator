/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.execution;

import com.dbn.common.exception.RequestCancelledException;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.dbn.nls.NlsResources.txt;

/** Provides confirmations required before potentially destructive Liquibase operations. */
public final class LiquibaseOperationConfirmations {

    public static boolean confirm(@NotNull LiquibaseExecutionInput input) {
        return switch (input.getOperation()) {
            case GENERATE_CHANGELOG, GENERATE_DIFF_CHANGELOG -> confirmOverwrite(input);
            case DROP_ALL -> confirmDropAll(input);
            default -> true;
        };
    }

    public static void ensureConfirmed(@NotNull LiquibaseExecutionInput input) throws RequestCancelledException {
        if (!confirm(input)) throw new RequestCancelledException("Liquibase operation confirmation canceled");
    }

    public static boolean confirmWorkspaceAvailable(
            @NotNull Project project,
            @NotNull DatabaseLiquibaseManager manager,
            @NotNull ConnectionHandler connection,
            @NotNull LiquibaseOperation operation) {
        if (!operation.requires(LiquibaseFeature.WORKSPACE)) return true;
        if (operation.supports(LiquibaseFeature.WORKSPACE_CREATION)) return true;

        if (manager.getWorkspaces().containsWorkspaces(connection.getDatabaseType())) return true;

        Messages.showInfoDialog(
                project,
                txt("msg.liquibase.title.WorkspaceRequired"),
                txt("msg.liquibase.message.NoWorkspacesAvailable", connection.getDatabaseType().getName()),
                new String[]{
                        txt("msg.liquibase.button.OpenWorkspaces"),
                        txt("msg.shared.button.Cancel")},
                0,
                option -> { if (option == 0) manager.openWorkspaceSettings(); });
        return false;
    }

    public static boolean confirmOverwrite(@NotNull LiquibaseExecutionInput input) {
        Path changelogFile = input.getWorkspacePaths().getMasterChangelogPath();
        if (!Files.exists(changelogFile) || input.isConfirmed()) return true;

        int option = Messages.showAcknowledgementDialog(
                input.getProject(),
                txt("msg.liquibase.title.OverwriteChangelog"),
                txt("msg.liquibase.question.OverwriteChangelog", changelogFile),
                Messages.options(
                        txt("msg.liquibase.button.Overwrite"),
                        txt("msg.shared.button.Cancel")),
                1,
                null);
        if (option != 0) return false;

        input.setConfirmed(true);
        return true;
    }

    public static boolean confirmDropAll(@NotNull LiquibaseExecutionInput input) {
        if (input.isConfirmed()) return true;

        DBSchema schema = input.getRelevantSchema();
        int option = Messages.showAcknowledgementDialog(
                input.getProject(),
                txt("msg.liquibase.title.DropAll"),
                txt("msg.liquibase.question.DropAll", schema.getName()),
                Messages.options(
                        txt("msg.liquibase.button.DropAll"),
                        txt("msg.shared.button.Cancel")),
                1,
                null);
        if (option != 0) return false;

        input.setConfirmed(true);
        return true;
    }
}
