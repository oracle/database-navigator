/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.diagnostics.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.diagnostics.ParserIssueReportInput;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class ParserIssueReportDialog extends DBNDialog<ParserIssueReportForm> {
    private final Project project;
    private final ParserIssueReportInput input;

    public ParserIssueReportDialog(@NotNull Project project, @NotNull ParserIssueReportInput input) {
        super(project, txt("app.diagnostics.title.ParserIssue"), true);
        this.project = project;
        this.input = input;
        setDefaultSize(920, 650);
        init();
    }

    @Override
    protected @NotNull ParserIssueReportForm createForm() {
        return new ParserIssueReportForm(this, project, input);
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("app.diagnostics.button.SubmitReport"));
        renameAction(getCancelAction(), txt("msg.shared.button.Cancel"));
        return actions(getOKAction(), getCancelAction());
    }
}
