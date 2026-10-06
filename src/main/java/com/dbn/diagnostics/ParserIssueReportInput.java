/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.diagnostics;

import com.dbn.language.common.DBLanguageDialect;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.fileTypes.FileType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;

@Getter
public class ParserIssueReportInput {
    private String code;
    private String comment;
    private final Charset charset;
    private final FileType fileType;
    private final DBLanguageDialect languageDialect;
    private final Attachment attachment;

    public ParserIssueReportInput(
            @NotNull String code,
            @NotNull Charset charset,
            @NotNull FileType fileType,
            @NotNull DBLanguageDialect languageDialect,
            @NotNull Attachment attachment) {
        this.code = code;
        this.charset = charset;
        this.fileType = fileType;
        this.languageDialect = languageDialect;
        this.attachment = attachment;
    }

    public String getLanguageDialectId() {
        return languageDialect.getID();
    }

    @NotNull
    public File getFile() {
        return new File(attachment.getPath());
    }

    public byte[] getCodeBytes() {
        return code.getBytes(charset);
    }

    public void update(@NotNull String code, @Nullable String comment) {
        this.code = code;
        this.comment = comment;
    }
}
