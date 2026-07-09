/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.dbn.code.common.intention;

import com.dbn.common.icon.Icons;
import com.dbn.diagnostics.ParserDiagnosticsManager;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

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
    public Icon getIcon(int flags) {
        return Icons.COMMON_ERROR;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        return file instanceof DBLanguagePsiFile && PsiTreeUtil.findChildOfType(file, PsiErrorElement.class) != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (!(file instanceof DBLanguagePsiFile psiFile)) return;

        ParserDiagnosticsManager diagnosticsManager = ParserDiagnosticsManager.get(project);
        diagnosticsManager.submitParserIssueReport(psiFile);
    }
}
