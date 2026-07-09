/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.diagnostics;

import com.dbn.language.common.DBLanguageDialect;
import com.intellij.openapi.fileTypes.FileType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class ParserIssueReportInput {
    private final String code;
    private final FileType fileType;
    private final DBLanguageDialect languageDialect;

    public ParserIssueReportInput(
            @NotNull String code,
            @NotNull FileType fileType,
            @NotNull DBLanguageDialect languageDialect) {
        this.code = code;
        this.fileType = fileType;
        this.languageDialect = languageDialect;
    }

    public String getLanguageDialectId() {
        return languageDialect.getID();
    }

}
