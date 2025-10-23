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

package com.dbn.assistant.service.generic.model;

import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.common.util.Chars;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AssistantModelInput {
    private final AIProviderId baseProviderId;
    private final AIProviderId providerId;
    private final String modelName;
    private String url;
    private Double temperature;
    private AssistantCredential credential;
    private Map<String, String> headers = new HashMap<>();
    private Map<Attribute, String> attributes = new HashMap<>();

    private AssistantModelInput(AIProviderId baseProviderId, AIProviderId providerId, String modelName) {
        this.baseProviderId = baseProviderId;
        this.providerId = providerId;
        this.modelName = modelName;
    }

    public String getTokenString() {
        char[] secret = credential.getSecret();
        return Chars.toString(secret);
    }

    public AssistantModelInput withUrl(String url) {
        this.url = url;
        return this;
    }

    public AssistantModelInput withCredential(AssistantCredential credential) {
        this.credential = credential;
        return this;
    }

    public AssistantModelInput withTemperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    public AssistantModelInput withHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public AssistantModelInput withAttribute(Attribute key, String value) {
        this.attributes.put(key, value);
        return this;
    }

    public static AssistantModelInput create(AIProviderId baseProviderId, AIProviderId providerId, String model) {
        return new AssistantModelInput(baseProviderId, providerId, model);
    }

    public String getAttribute(Attribute attribute) {
        return attributes.get(attribute);
    }

    public String getUser() {
        return credential.getUser();
    }

    public String getRegionId() {
        // TODO region specific OCI hosted models
        return "us-chicago-1";
    }

    public enum Attribute{
        COMPARTMENT_ID
    }

}
