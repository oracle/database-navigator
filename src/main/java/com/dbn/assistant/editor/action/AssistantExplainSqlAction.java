/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.assistant.editor.action;

import com.dbn.assistant.chat.window.PromptAction;
import org.jetbrains.annotations.Nls;

/**
 * Editor menu action to invoke AI-Assistant with "Explain SQL" instruction
 *
 * @author Ayoub Aarrasse (Oracle)
 */
public class AssistantExplainSqlAction extends AssistantBaseEditorAction {

  @Override
  protected PromptAction getAction() {
    return PromptAction.EXPLAIN_SQL;
  }

  @Override
  protected @Nls String getActionName(AssistantBaseEditorAction.ActionPlace place) {
    switch (place) {
      case GENERATE_ACTION_GROUP: return "SQL Statement with Explanation";
      case EDITOR_POPUP_MENU:
      default: return "Generate and Explain SQL";
    }
  }
}
