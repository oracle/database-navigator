/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.dbn.error.jira;

import com.dbn.diagnostics.ParserIssueReportInput;
import com.dbn.error.IssueReport;
import com.dbn.error.MarkupElement;
import com.intellij.openapi.diagnostic.Attachment;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JiraParserIssueReportBuilder extends JiraIssueReportBuilder {
    @Override
    protected void buildSummary(IssueReport report) {
        ParserIssueReportInput input = getReportInput(report);
        report.setSummary(input == null ?
                "SQL / PL/SQL parser issue" :
                input.getLanguageDialectId() + " parser issue");
    }

    @Override
    protected void buildLabels(IssueReport report) {
        super.buildLabels(report);
        ParserIssueReportInput input = getReportInput(report);
        if (input == null) return;

        report.addLabel("parser-issue");
        report.addLabel(input.getLanguageDialectId());
    }

    @Override
    protected void buildAdditionalInfo(IssueReport report, StringBuilder description) {
        super.buildAdditionalInfo(report, description);

        ParserIssueReportInput input = getReportInput(report);
        if (input == null) return;

        description.append(getMarkupElement(MarkupElement.PANEL, "Parser Information"));
        description.append("Language Dialect: ");
        description.append(input.getLanguageDialectId());
        description.append(getMarkupElement(MarkupElement.PANEL));
    }

    @Nullable
    private static ParserIssueReportInput getReportInput(IssueReport report) {
        Object data = report.getEvent().getData();
        return data instanceof ParserIssueReportInput ? (ParserIssueReportInput) data : null;
    }

    @Override
    protected void buildExceptionInfo(IssueReport report, StringBuilder description) {
        List<Attachment> attachments = report.getAttachments();
        if (attachments.isEmpty()) return;

        Attachment attachment = attachments.get(0);
        try {
            String content = Files.readString(Path.of(attachment.getPath()), StandardCharsets.UTF_8);
            description.append(getMarkupElement(MarkupElement.CODE, attachment.getDisplayText()));
            description.append(content.substring(0, Math.min(content.length(), 10000)));
            description.append(getMarkupElement(MarkupElement.CODE));
        } catch (IOException e) {
            description.append("Parser issue attachment could not be read");
        }
    }

    @Override
    protected boolean includeAttachment(Attachment attachment) {
        return true;
    }
}
