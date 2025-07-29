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

package com.dbn.assistant.service.generic.model.invoker;

import com.dbn.assistant.adapter.AssistantResponseConsumer;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.output.Response;

public class LanguageModelInvoker extends AbstractModelInvoker<LanguageModel>{
    public LanguageModelInvoker() {
        super(LanguageModel.class);
    }

    @Override
    public void invokeModel(LanguageModel model, ChatMemory memory, String prompt, AssistantResponseConsumer consumer) {
        try {
            Response<String> response = model.generate(prompt);
            String content = response.content();

            consumer.acceptMessage(content);
            consumer.acceptCompletion();
        } catch (Throwable e) {
            consumer.acceptError(e);
            consumer.acceptCompletion();
        }

    }
}
