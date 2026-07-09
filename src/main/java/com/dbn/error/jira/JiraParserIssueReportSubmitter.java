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

import com.dbn.error.IssueReportBuilder;

public class JiraParserIssueReportSubmitter extends JiraIssueReportSubmitter {
    private static final JiraParserIssueReportBuilder REPORT_BUILDER = new JiraParserIssueReportBuilder();

    @Override
    protected IssueReportBuilder getBuilder() {
        return REPORT_BUILDER;
    }
}
