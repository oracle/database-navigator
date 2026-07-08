/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.code.common.intention;

import com.dbn.diagnostics.ParserDiagnosticsManager;
import com.dbn.error.jira.JiraParserIssueReportSubmitter;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import static com.dbn.nls.NlsResources.txt;

public class SubmitParserIssueIntentionAction extends EditorIntentionAction {
    @Override
    public EditorIntentionType getType() {
        return EditorIntentionType.PARSER_DIAGNOSTICS;
    }

    @Override
    @NotNull
    public String getText() {
        return txt("app.codeEditor.intention.SubmitParserIssue");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        return file instanceof DBLanguagePsiFile && PsiTreeUtil.findChildOfType(file, PsiErrorElement.class) != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (!(file instanceof DBLanguagePsiFile dbLanguageFile)) return;

        try {
            File attachmentFile = File.createTempFile("dbn-parser-issue-", "." + file.getVirtualFile().getExtension());
            attachmentFile.deleteOnExit();
            byte[] scrambled = ParserDiagnosticsManager.scrambleFile(dbLanguageFile, file.getVirtualFile().getCharset());
            FileUtil.writeToFile(attachmentFile, scrambled);

            Attachment attachment = new Attachment(attachmentFile.getPath(), attachmentFile, attachmentFile.getName());
            IdeaLoggingEvent event = new IdeaLoggingEvent(
                    "Parser issue",
                    new IllegalArgumentException("Parser error"),
                    List.of(attachment),
                    (IdeaPluginDescriptor) null,
                    null);
            new JiraParserIssueReportSubmitter().submit(
                    new IdeaLoggingEvent[]{event},
                    "Parser issue reported from the SQL/PLSQL editor",
                    editor.getComponent(),
                    info -> {});
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
