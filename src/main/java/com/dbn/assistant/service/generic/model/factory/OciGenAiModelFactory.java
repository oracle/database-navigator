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

import com.dbn.assistant.AssistantComponent;
import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.service.generic.model.AssistantModelInput;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import dev.langchain4j.community.model.oracle.oci.genai.OciGenAiChatModel;
import dev.langchain4j.community.model.oracle.oci.genai.OciGenAiCohereChatModel;
import dev.langchain4j.community.model.oracle.oci.genai.OciGenAiCohereStreamingChatModel;
import dev.langchain4j.community.model.oracle.oci.genai.OciGenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static com.dbn.assistant.provider.AIProviderId.COHERE;
import static com.dbn.assistant.provider.AIProviderId.OCI_GEN_AI;


public class OciGenAiModelFactory extends AbstractModelFactory implements AssistantComponent {

    public OciGenAiModelFactory() {
        super(OCI_GEN_AI);
    }

    @Nullable
    @Override
    @SneakyThrows
    public ChatModel createChatModel(AssistantModelInput input) {
        AssistantCredential credential = input.getCredential();

        String modelName = input.getModelName();
        Double temperature = input.getTemperature();
        String compartmentId = credential.getOciCompartmentId();
        AuthenticationDetailsProvider authProvider = createAuthProvider(credential);

        if (input.getBaseProviderId() == COHERE) {
            return wrapped(() -> OciGenAiCohereChatModel.builder()
                    .modelName(modelName)
                    .temperature(temperature)
                    .authProvider(authProvider)
                    .compartmentId(compartmentId)
                    .maxTokens(4000)
                    .build());

        } else {
            return wrapped(() -> OciGenAiChatModel.builder()
                    .modelName(modelName)
                    .temperature(temperature)
                    .authProvider(authProvider)
                    .compartmentId(compartmentId)
                    .build());
        }
    }

    @Nullable
    @Override
    @SneakyThrows
    public StreamingChatModel createStreamingChatModel(AssistantModelInput input) {
        AssistantCredential credential = input.getCredential();

        String modelName = input.getModelName();
        Double temperature = input.getTemperature();
        String compartmentId = credential.getOciCompartmentId();
        AuthenticationDetailsProvider authProvider = createAuthProvider(credential);

        if (input.getBaseProviderId() == COHERE) {
            return wrapped(() -> OciGenAiCohereStreamingChatModel.builder()
                    .modelName(modelName)
                    .temperature(temperature)
                    .authProvider(authProvider)
                    .compartmentId(compartmentId)
                    .maxTokens(4000)
                    .build());
        } else {
            return wrapped(() -> OciGenAiStreamingChatModel.builder()
                    .modelName(modelName)
                    .temperature(temperature)
                    .authProvider(authProvider)
                    .compartmentId(compartmentId)
                    .build());
        }
    }

    private static @NotNull AuthenticationDetailsProvider createAuthProvider(AssistantCredential credential) throws IOException {
        String configFile = credential.getOciConfigFile();
        String configProfile = credential.getOciConfigProfile();

        return new ConfigFileAuthenticationDetailsProvider(configFile, configProfile);
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
