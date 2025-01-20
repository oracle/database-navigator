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

package com.dbn.assistant.chat.window;

import com.dbn.common.ui.Presentable;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

import static com.dbn.nls.NlsResources.txt;


/**
 * Select AI action enum class
 * @see https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/sql-generation-ai-autonomous.html#ADBSB-GUID-B3E0EE68-3B4C-4002-9B45-BBE258A2F15A
 *
 * @author Ayoub Aarrasse (Oracle)
 */
@Getter
public enum PromptAction implements Presentable {
  SHOW_SQL    ("showsql",    txt("app.assistant.const.PromptAction_SHOW_SQL"),    txt("app.assistant.hint.PromptAction_SHOW_SQL")),
  EXPLAIN_SQL ("explainsql", txt("app.assistant.const.PromptAction_EXPLAIN_SQL"), txt("app.assistant.hint.PromptAction_EXPLAIN_SQL")),
  EXECUTE_SQL ("executesql", txt("app.assistant.const.PromptAction_EXECUTE_SQL"), txt("app.assistant.hint.PromptAction_EXECUTE_SQL")),
  NARRATE     ("narrate",    txt("app.assistant.const.PromptAction_NARRATE"),     txt("app.assistant.hint.PromptAction_NARRATE")),
  CHAT        ("chat",       txt("app.assistant.const.PromptAction_CHAT"),        txt("app.assistant.hint.PromptAction_CHAT")),;

  private final String id;
  private final String name;
  private final String description;

  PromptAction(@NonNls String action, String name, String description) {
    this.id = action;
    this.name = name;
    this.description = description;
  }

  public static PromptAction getByAction(String action) {
    for (PromptAction type : values()) {
      if (type.getId().equals(action)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Invalid action: " + action);
  }
}
