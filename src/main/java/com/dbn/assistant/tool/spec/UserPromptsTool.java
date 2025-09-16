/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.spec;

import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolFactoryBase;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.assistant.tool.impl.UserPromptsToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import static com.dbn.assistant.tool.AssistantToolCategory.USER_INTERACTION;
import static com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;
import static com.dbn.assistant.tool.AssistantToolInfo.ToolSpec;

@ToolSpec(
        category = USER_INTERACTION,
        type = "USER_PROMPTS",
        name = "User prompts",
        interactive = true,
        description = "Interactive tools for soliciting and processing human input.")
public interface UserPromptsTool extends AssistantTool {

    @FactorySpec(
            spec = UserPromptsTool.class,
            impl = UserPromptsToolImpl.class)
    class Factory extends AssistantToolFactoryBase<UserPromptsTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "REQUEST_USER_CONFIRMATION")
    @UtilitySpec(
            name = "Request user confirmation",
            description = "Prompts the user with a question and waits for an affirmative or negative response. " +
                    "Offers a quick way for the user to decide on the continuation of certain assistant tasks.")
    boolean requestUserConfirmation(
            @P("Brief description of the topic") String title,
            @P("Confirmation message") String question,
            @P("Label for the affirmative option") String yesOption,
            @P("Label for negative option") String noOption);


    @Tool(name = "REQUEST_USER_DECISION")
    @UtilitySpec(
            name = "Request user decision",
            description = "Prompts the user with a question and waits for a selection from the given options. " +
                    "Offers a quick way for the user to decide which path to take for the continuation of given assistant tasks.")
    String requestUserDecision(
            @P("Brief description of the topic") String title,
            @P("Question") String question,
            @P("Options the user can choose from") String[] options);

}
