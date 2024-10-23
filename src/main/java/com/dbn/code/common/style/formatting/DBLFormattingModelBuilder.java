package com.dbn.code.common.style.formatting;

import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.DBLCodeStyleSettings;
import com.dbn.common.exception.OutdatedContentException;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Traces;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.codeStyle.CodeFormatterFacade;
import org.jetbrains.annotations.NotNull;

public class DBLFormattingModelBuilder implements FormattingModelBuilder {

    @NotNull
    @Override
    public FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        PsiElement element = formattingContext.getPsiElement();

        CodeStyleSettings codeStyleSettings = formattingContext.getCodeStyleSettings();
        DBLanguage language = (DBLanguage) PsiUtil.getLanguage(element);

        PsiFile psiFile = element.getContainingFile();
        Document document = Documents.getDocument(psiFile);
        if (document != null && document.getTextLength() != psiFile.getTextLength()) {
            // TODO check why this happens (during startup)
            throw new OutdatedContentException(this);
        }


        Project project = element.getProject();
        DBLCodeStyleSettings settings = language.codeStyleSettings(project);

        boolean deliberate = Traces.isCalledThrough(CodeFormatterFacade.class);
        if (deliberate && settings.getCaseSettings().isEnabled()) {
            DBLCodeStyleManager.getInstance(project).formatCase(element.getContainingFile());
        }

        Block rootBlock = deliberate && settings.getFormattingSettings().isEnabled() ?
                new FormattingBlock(codeStyleSettings, settings, element, null, 0) :
                new PassiveFormattingBlock(element);
        return FormattingModelProvider.createFormattingModelForPsiFile(psiFile, rootBlock, codeStyleSettings);
    }

    @Override
    public TextRange getRangeAffectingIndent(PsiFile psiFile, int i, ASTNode astNode) {
        return astNode.getTextRange();
    }
}
