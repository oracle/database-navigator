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

import com.dbn.error.IssueReport;
import com.dbn.error.MarkupElement;
import com.intellij.openapi.diagnostic.Attachment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class JiraParserIssueReportBuilder extends JiraIssueReportBuilder {
    @Override
    protected void buildSummary(IssueReport report) {
        report.setSummary("SQL / PL/SQL parser issue");
    }

    @Override
    protected void buildExceptionInfo(IssueReport report, StringBuilder description) {
        if (report.getAttachments().isEmpty()) return;

        Attachment attachment = report.getAttachments().get(0);
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
