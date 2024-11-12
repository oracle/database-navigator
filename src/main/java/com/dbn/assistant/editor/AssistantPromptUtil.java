/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.assistant.editor;

import com.dbn.common.util.Commons;
import com.dbn.common.util.Documents;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import static com.dbn.assistant.editor.AssistantPrompt.Flavor.*;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Commons.isOneOf;
import static com.dbn.common.util.Commons.nvl;

/**
 * Database assistant utility class for editor actions
 * Features fast context lookup utilities like connection, chat-box state and prompt text candidate
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class AssistantPromptUtil {
    public static boolean isAssistantPromptAvailable(@Nullable Editor editor, @Nullable PsiElement element, AssistantPrompt.Flavor ... flavors) {
        AssistantPrompt prompt = resolveAssistantPrompt(editor, element);
        if (prompt == null) return false;
        if (flavors == null) return true; // no filter
        if (flavors.length == 0) return true; // no filter

        return isOneOf(prompt.getFlavor(), flavors);
    }

    /**
     * Returns the prompt text candidate for the given context
     * It can eiter be a content of the comment or the current selection from the editor
     * @param editor the {@link Editor} to source context from
     * @return the {@link AssistantPrompt} to be prompted to the AI Assistant engine
     */
    @Nullable
    public static AssistantPrompt resolveAssistantPrompt(@Nullable Editor editor, @Nullable PsiElement element) {
        if (isNotValid(editor)) return null;

        return Commons.coalesce(
                () -> resolveSelectedText(editor),
                () -> resolveCommentPrompt(nvl(element, () -> getElementAtCaret(editor))),
                () -> resolveStatementPrompt(nvl(element, () -> getElementAtCaret(editor))));
    }

    @Nullable
    private static AssistantPrompt resolveSelectedText(Editor editor) {
        SelectionModel selectionModel = editor.getSelectionModel();
        String text = selectionModel.getSelectedText();
        return AssistantPrompt.create(adjustPrompt(text), SELECTION);
    }

    @Nullable
    private static AssistantPrompt resolveCommentPrompt(@Nullable PsiElement element) {
        if (element instanceof PsiComment) {
            if (element.getTextLength() < 10) return null;
            return AssistantPrompt.create(adjustPrompt(element.getText()), COMMENT);
        }
        return null;
    }

    private static PsiElement getElementAtCaret(@Nullable Editor editor) {
        if (isNotValid(editor)) return null;

        PsiFile psiFile = Documents.getPsiFile(editor);
        if (psiFile == null) return null;

        PsiElement psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (psiElement == null) psiElement = psiFile.getLastChild();

        return psiElement;
    }

    private static AssistantPrompt resolveStatementPrompt(@Nullable PsiElement element) {
        BasePsiElement<?> basePsiElement = PsiUtil.getBasePsiElement(element);
        if (basePsiElement == null) return null;

        ExecutablePsiElement executablePsiElement =
                basePsiElement instanceof ExecutablePsiElement ?
                        (ExecutablePsiElement) basePsiElement :
                        basePsiElement.findEnclosingElement(ExecutablePsiElement.class);
        if (executablePsiElement == null) return null;

        ElementType specificElementType = executablePsiElement.getSpecificElementType(true);
        if (!specificElementType.is(ElementTypeAttribute.DB_ASSISTANT)) return null;

        BasePsiElement<?> promptElement = executablePsiElement.findFirstPsiElement(ElementTypeAttribute.DB_ASSISTANT_PROMPT);
        if (promptElement == null) return null;

        return AssistantPrompt.create(promptElement.getText(), STATEMENT);
    }

    private static @Nullable String adjustPrompt(String text) {
        if (text == null) return null;

        text = text.trim();
        if (text.startsWith("--")) text = text.replaceAll("--+\\s*", "");
        if (text.startsWith("/*")) text = text.replaceAll("/\\*\\s*", "").replaceAll("\\s*\\*/", "");
        if (text.length() < 10) return null;      // too short to be valid prompt
        if (text.indexOf(' ') == -1) return null; // single word, most probably not a valid prompt

        return text;
    }
}
