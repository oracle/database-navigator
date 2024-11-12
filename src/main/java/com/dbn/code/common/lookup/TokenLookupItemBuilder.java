package com.dbn.code.common.lookup;

import com.dbn.code.common.completion.BasicInsertHandler;
import com.dbn.code.common.completion.BracketsInsertHandler;
import com.dbn.code.common.completion.CodeCompletionContext;
import com.dbn.code.common.completion.options.CodeCompletionSettings;
import com.dbn.code.common.completion.options.general.CodeCompletionFormatSettings;
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

import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.common.util.Strings.toUpperCase;

public class TokenLookupItemBuilder extends LookupItemBuilder {

    private TokenElementType tokenElementType;

    public TokenLookupItemBuilder(TokenElementType tokenElementType) {
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
        String text = tokenElementType.getText();
        TokenType tokenType = tokenElementType.tokenType;
        if (isEmpty(text)) {
            text = tokenType.getValue();
        }
        Project project = completionContext.getParameters().getOriginalFile().getProject();

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
        CodeCompletionFormatSettings codeCompletionFormatSettings = CodeCompletionSettings.getInstance(project).getFormatSettings();
        if (isNotEmpty(userInput) && !text.startsWith(userInput) && !codeCompletionFormatSettings.isEnforceCodeStyleCase()) {
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
