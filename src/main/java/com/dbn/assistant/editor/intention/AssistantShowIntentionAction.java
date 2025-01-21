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

package com.dbn.assistant.editor.intention;

import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.code.common.intention.EditorIntentionType;

import static com.dbn.nls.NlsResources.txt;

/**
 * Editor intention action for invoking AI-Assistant module from within the editor
 *
 * @author Ayoub Aarrasse (Oracle)
 */
public class AssistantShowIntentionAction extends AssistantBaseIntentionAction {
  @Override
  public EditorIntentionType getType() {
    return EditorIntentionType.ASSISTANT_GENERATE;
  }

  @Override
  protected String getActionName() {
    return txt("app.assistant.action.GenerateSql");
  }

  @Override
  protected PromptAction getAction() {
    return PromptAction.SHOW_SQL;
  }
}
