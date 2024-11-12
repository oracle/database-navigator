package com.dbn.code.common.lookup;

import com.dbn.code.common.completion.BasicInsertHandler;
import com.dbn.code.common.completion.BracketsInsertHandler;
import com.dbn.code.common.completion.CodeCompletionContext;
import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.common.util.Characters;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.impl.TokenElementType;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.openapi.project.Project;

import javax.swing.Icon;

import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.common.util.Strings.toUpperCase;

public class TokenChainLookupItemBuilder extends LookupItemBuilder {

    private final TokenElementType tokenElementType;

    public TokenChainLookupItemBuilder(TokenElementType tokenElementType) {
        this.tokenElementType = tokenElementType;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public boolean isBold() {
        return tokenElementType.tokenType.isKeyword();
    }

    @Override
    public CharSequence getText(CodeCompletionContext completionContext) {
        Project project = completionContext.getParameters().getOriginalFile().getProject();
        TokenType tokenType = tokenElementType.tokenType;
        String text = tokenType.getValue();

        DBLanguage language = tokenElementType.getLanguage();
        CodeStyleCaseSettings styleCaseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(language);
        CodeStyleCaseOption caseOption =
                tokenType.isFunction() ? styleCaseSettings.getFunctionCaseOption() :
                        tokenType.isKeyword() ? styleCaseSettings.getKeywordCaseOption() :
                                tokenType.isParameter() ? styleCaseSettings.getParameterCaseOption() :
                                        tokenType.isDataType() ? styleCaseSettings.getDatatypeCaseOption() : null;

        if (caseOption != null) {
            text = caseOption.format(text);
        }

        String userInput = completionContext.getUserInput();
        if (userInput != null && userInput.length() > 0 && !text.startsWith(userInput)) {
            char firstInputChar = userInput.charAt(0);
            char firstPresentationChar = text.charAt(0);

            if (Characters.equalIgnoreCase(firstInputChar, firstPresentationChar)) {
                text = Character.isUpperCase(firstInputChar) ?
                        toUpperCase(text) :
                        toLowerCase(text);
            } else {
                return null;
            }
        }

        return text;
    }

    @Override
    public String getTextHint() {
        return getTokenTypeCategory().getName();
/*
        TokenType tokenType = tokenElementType.getTokenType();
        return
                tokenType.isKeyword() ? "keyword" :
                tokenType.isFunction() ? "function" :
                tokenType.isParameter() ? "parameter" :
                tokenType.isDataType() ? "datatype" :null;
*/
    }

    private void createLookupItem(CompletionResultSet resultSet, String presentation, CodeCompletionContext completionContext, boolean insertParenthesis) {
        LookupItem lookupItem = new CodeCompletionLookupItem(this, presentation, completionContext);
        lookupItem.setInsertHandler(
                insertParenthesis ?
                        BracketsInsertHandler.INSTANCE :
                        BasicInsertHandler.INSTANCE);
        resultSet.addElement(lookupItem);
    }

    public TokenType getTokenType() {
        return tokenElementType.tokenType;
    }
    public TokenTypeCategory getTokenTypeCategory() {
        return tokenElementType.getTokenTypeCategory();
    }
}
