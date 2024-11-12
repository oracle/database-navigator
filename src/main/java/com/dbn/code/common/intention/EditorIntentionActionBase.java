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

package com.dbn.code.common.intention;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.ImaginaryEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.dbn.common.dispose.Checks.isNotValid;

/**
 * Alternative implementation of {@link com.intellij.codeInsight.intention.PsiElementBaseIntentionAction}
 * Fixes element lookup when caret is at the end of file
 *
 * @author Dan Cioca (Oracle)
 */
public abstract class EditorIntentionActionBase extends com.intellij.codeInsight.intention.impl.BaseIntentionAction {
    private static final Map<EditorIntentionType, EditorIntentionActionBase> REGISTRY = new HashMap<>();

    protected EditorIntentionActionBase() {
        register(this);
    }

    /**
     * Assert unique type and priority
     */
    private static void register(EditorIntentionActionBase action) {
        EditorIntentionType type = action.getType();
        EditorIntentionActionBase intentionAction = REGISTRY.get(type);
        if (intentionAction != null) throw new IllegalStateException("Editor intention already registered for type " + type);

        REGISTRY.put(type, action);
    }

    public abstract EditorIntentionType getType();

    public final void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (isNotValid(editor)) return;
        if (isNotValid(file)) return;
        if (editor instanceof ImaginaryEditor) return;

        PsiElement element = getElement(editor, file);
        this.invoke(project, editor, element);
    }

    public abstract void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException;

    public final boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (isNotValid(editor)) return false;
        if (isNotValid(file)) return false;

        PsiElement element = getElement(editor, file);
        return isAvailable(project, editor, element);
    }

    public abstract boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement);

    private static PsiElement getElement(@NotNull Editor editor, @NotNull PsiFile file) {
        CaretModel caretModel = editor.getCaretModel();
        int position = caretModel.getOffset();

        PsiElement psiElement = file.findElementAt(position);
        if (psiElement == null) psiElement = file.getLastChild();
        if (psiElement == null) return file;

        // find relevant element before the white space
        if (psiElement instanceof PsiWhiteSpace) {
            PsiElement prevSibling = psiElement.getPrevSibling();
            if (prevSibling == null) return psiElement;

            int relativeOffset = position - psiElement.getTextOffset();
            if (relativeOffset == 0) return prevSibling;

            // return the white space if the previous element is not in the same line
            if (StringUtil.indexOf(psiElement.getText(), '\n', 0, relativeOffset) > -1) return psiElement; // new line
            return prevSibling;
        }
        return psiElement;
    }
}
