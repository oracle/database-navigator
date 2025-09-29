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

package com.dbn.assistant.service.generic.model.factory;

import com.dbn.assistant.service.generic.model.AssistantModelInput;
import com.dbn.common.compatibility.Workaround;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.util.Classes;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import dev.langchain4j.community.model.oracle.oci.genai.OciGenAiChatModel;
import dev.langchain4j.community.model.oracle.oci.genai.OciGenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static com.dbn.assistant.provider.AIProviders.OCI_GEN_AI;

public class OciGenAiModelFactory extends AbstractModelFactory {

    public OciGenAiModelFactory() {
        super(OCI_GEN_AI);
    }

    @Nullable
    @Override
    @SneakyThrows
    public ChatModel createChatModel(AssistantModelInput input) {
        AuthenticationDetailsProvider provider = createAuthProvider();

        return wrapped(() -> OciGenAiChatModel.builder()
                .modelName(input.getModel())
                .temperature(input.getTemperature())
                .authProvider(provider)
                .build());
    }

    private static @NotNull AuthenticationDetailsProvider createAuthProvider() throws IOException {
        String configFilePath = "~/.oci/config";
        String profileName = "DEFAULT"; // Use the profile name from your config file

        return new ConfigFileAuthenticationDetailsProvider(configFilePath, profileName);
    }

    @Nullable
    @Override
    @SneakyThrows
    public StreamingChatModel createStreamingChatModel(AssistantModelInput input) {
        AuthenticationDetailsProvider provider = createAuthProvider();
        return wrapped(() -> OciGenAiStreamingChatModel.builder()
                .modelName(input.getModel())
                .temperature(input.getTemperature())
                .authProvider(provider)
                .build());
    }

    @Workaround
    private static <T> T wrapped(ThrowableCallable<T, RuntimeException> callable) {
        // the internal httpProvider initialization using ServiceLoader favors the thread context class loader
        // (jersey http client implementation fails to load unless the plugin class loader is used)
        return Classes.withClassLoader(OciGenAiModelFactory.class, callable);
    }

    @Nullable
    @Override
    public LanguageModel createLanguageModel(AssistantModelInput input) {
        return null;
    }

    @Nullable
    @Override
    public EmbeddingModel createEmbeddingModel(AssistantModelInput input) {
        return null;
    }
}
