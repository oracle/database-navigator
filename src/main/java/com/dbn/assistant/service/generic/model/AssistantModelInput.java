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

import com.dbn.common.util.Chars;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AssistantModelInput {
    private final String model;
    private String url;
    private String user;
    private char[] token;
    private Double temperature;
    private Map<String, String> headers = new HashMap<>();

    private AssistantModelInput(String model) {
        this.model = model;
    }

    public String getTokenString() {
        return Chars.toString(token);
    }

    public AssistantModelInput withUrl(String url) {
        this.url = url;
        return this;
    }

    public AssistantModelInput withUser(String user) {
        this.user = user;
        return this;
    }

    public AssistantModelInput withToken(char[] token) {
        this.token = token;
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

    public static AssistantModelInput create(String model) {
        return new AssistantModelInput(model);
    }



}
