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
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;

import static com.dbn.common.util.Classes.className;
import static com.dbn.common.util.Strings.isNotEmpty;

public class JiraExceptionReportBuilder extends JiraIssueReportBuilder {
    @Override
    protected void buildSummary(IssueReport report) {
        IdeaLoggingEvent event = report.getEvents()[0];
        String summary = event.getThrowableText();
        report.setSummary(summary.substring(0, Math.min(summary.length(), 100)));
    }

    @Override
    protected void buildExceptionInfo(IssueReport report, StringBuilder description) {
        IdeaLoggingEvent event = report.getEvent();
        String exceptionMessage = event.getMessage();
        if (isNotEmpty(exceptionMessage) && !"null".equals(exceptionMessage)) {
            description.append("\n\n");
            description.append(exceptionMessage.replace("{", "\\{").replace("}", "\\}").replace("[", "\\[").replace("]", "\\]"));
            description.append("\n\n");
        }
        description.append(getMarkupElement(MarkupElement.CODE, className(event.getThrowable())));
        String details = event.getThrowableText();
        description.append(details.substring(0, Math.min(details.length(), 30000)));
        description.append(getMarkupElement(MarkupElement.CODE));
    }
}
