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

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.assistant.chat.message.ChatMessageContext;
import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.assistant.entity.AIProfileItem;
import com.dbn.common.thread.Command;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Documents;
import com.dbn.connection.ConnectionId;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.experimental.UtilityClass;

import static com.intellij.util.ObjectUtils.coalesce;

/**
 * This is the class that handles processing the prompt from the console and displaying the results to it.
 *
 * @author Ayoub Aarrasse (Oracle)
 */
@UtilityClass
public class AssistantEditorAdapter {

  public static void submitQuery(Project project, Editor editor, ConnectionId connectionId, String prompt, PromptAction action) {
    DatabaseAssistantManager manager = DatabaseAssistantManager.getInstance(project);
    AIProfileItem profile = manager.getDefaultProfile(connectionId);

    if (profile == null) {
      manager.initializeAssistant(connectionId);
      return;
    }

    ChatMessageContext context = new ChatMessageContext(profile.getName(), profile.getModel(), action);
    manager.generate(connectionId, prompt, context, message -> Dispatch.run(editor.getComponent(), () -> appendMessage(project, editor, message)));
  }



  private static void appendMessage(Project project, Editor editor, ChatMessage message) {
    Dispatch.run(editor.getComponent(), () ->
            Command.run(project, "Database Assistant Response", () -> {
              PsiFile psiFile = Documents.getPsiFile(editor);
              if (psiFile == null) return;

              PsiElement psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
              if (psiElement == null) psiElement = psiFile.getLastChild();
              if (psiElement == null) return;

              BasePsiElement<?> basePsiElement = PsiUtil.getBasePsiElement(psiElement);
              if (basePsiElement != null) {
                BasePsiElement<?> scopeElement = basePsiElement.findEnclosingElement(ElementTypeAttribute.SCOPE_DEMARCATION);
                psiElement = coalesce(scopeElement, basePsiElement, psiElement);
              }

              int offset = psiElement.getTextRange().getEndOffset();
              String prefix = psiElement.getText().endsWith("\n") ? "" : "\n";
              String content = message.outputForLanguage(SQLLanguage.INSTANCE);

              Document document = editor.getDocument();
              document.insertString(offset, prefix + content + "\n");
            }));
  }


  private static void appendLine(Project project, Document document, String lineToAppend, String afterComment) {
    ApplicationManager.getApplication().invokeLater(() -> {
      WriteCommandAction.runWriteCommandAction(project, () -> {
        String content = document.getText();
        int pos = content.indexOf(afterComment);

        if (pos >= 0) {
          pos = content.indexOf("\n", pos);
          if (pos >= 0) {
            document.insertString(pos + 1, lineToAppend + "\n");
          } else {
            document.insertString(content.length(), "\n" + lineToAppend + "\n");
          }
        } else {
          document.insertString(document.getTextLength(), "\n" + lineToAppend);
        }
      });
    });
  }

  private static String processText(String input, boolean withExplanation) {
    StringBuilder result = new StringBuilder();
    boolean inCodeBlock = false;
    boolean inExplanationBlock = false;
    String[] lines = input.split("\n");
    result.append("\n");

    for (String line : lines) {
      if (line.trim().startsWith("```")) {
        if (!inCodeBlock && inExplanationBlock) {
          result.append("*/\n\n");
          inExplanationBlock = false;
        }
        inCodeBlock = !inCodeBlock;
        continue;
      }

      if (inCodeBlock) {
        result.append(line).append("\n");
      } else {
        if (!inExplanationBlock && withExplanation) {
          result.append("\n/*\n");
          inExplanationBlock = true;
        }
        result.append(line).append("\n");
      }
    }

    if (inExplanationBlock) {
      result.append("*/\n\n");
    }

    return result.toString();
  }

}
