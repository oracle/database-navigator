package com.dbn.code.common.intention;

import com.dbn.assistant.editor.AssistantPrompt;
import com.dbn.connection.ConnectionHandler;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.dbn.assistant.editor.AssistantEditorUtil.isAssistantAvailable;
import static com.dbn.assistant.editor.AssistantPromptUtil.isAssistantPromptAvailable;

public abstract class EditorIntentionAction extends EditorIntentionActionBase implements IntentionAction, HighPriorityAction, Iconable, DumbAware, Comparable<Object> {

    @Override
    @NotNull
    public String getFamilyName() {
        return getText();
        // DBN intentions cannot be grouped by family as system would hide the granularity behind the group name
    }

    @Nullable
    protected ConnectionHandler getConnection(PsiFile psiFile) {
        if (psiFile instanceof DBLanguagePsiFile) {
            DBLanguagePsiFile dbLanguagePsiFile = (DBLanguagePsiFile) psiFile;
            return dbLanguagePsiFile.getConnection();
        }
        return null;
    }

    @Override
    public Icon getIcon(int flags) {
        return null;
    }

    @Override
    public boolean startInWriteAction() {
        // most (if not all) DBN intentions are non-write intentions
        // to be overridden by intentions that change editor content
        return false;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof EditorIntentionAction) {
            EditorIntentionAction a = (EditorIntentionAction) o;
            int groupLevel = getPriority().compareTo(a.getPriority());

            return groupLevel == 0 ? getType().ordinal() - a.getType().ordinal() : groupLevel;
        }
        return 0;
    }

    /**
     * Verifies if the element where intention has been invoked is a database assistant comment
     * To be used to expose the DatabaseAssistant intention actions, but also hide all other intentions
     *
     * @param editor the editor from the intention context
     * @param element the element from the intention context
     * @param flavors the prompt flavors to check against (empty will allow all)
     * @return true if the element is an AI-Assistant comment (starting with three dashes), false otherwise
     */
    protected boolean isDatabaseAssistantPrompt(Editor editor, PsiElement element, AssistantPrompt.Flavor ... flavors) {
        return isAssistantAvailable(editor) && isAssistantPromptAvailable(editor, element, flavors);
    }
}
