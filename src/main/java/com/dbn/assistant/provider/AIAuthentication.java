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

package com.dbn.assistant.provider;

import com.dbn.common.ui.Presentable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class AIAuthentication {
    public static final AIAuthentication USER_PASSWORD = new AIAuthentication()
            .withField(Field.USER, true)
            .withField(Field.PASSWORD, true);

    private final Map<Field, Boolean> fields = new EnumMap<>(Field.class);

    public boolean isSupported(Field field) {
        return fields.containsKey(field);
    }

    public boolean isRequired(Field field) {
        Boolean required = fields.get(field);
        return required != null && required;
    }

    public void addField(Field field, boolean required) {
        fields.put(field, required);
    }

    @NotNull
    public Field getSecretField() {
        return fields
                .keySet()
                .stream()
                .filter(f -> f.isSecret())
                .findFirst()
                .orElse(Field.PASSWORD);
    }

    public AIAuthentication withField(Field field, boolean required) {
        addField(field, required);
        return this;
    }

    @Getter
    public enum Field implements Presentable {
        USER("User", false),
        PASSWORD("Password", true),
        TOKEN("Token", true),
        API_KEY("API key", true),
        OCI_CONFIG_FILE("OCI config file", false),
        OCI_CONFIG_PROFILE("OCI config profile", false),
        OCI_COMPARTMENT_ID("OCI compartment id", false)
        ;

        Field(String name, boolean secret) {
            this.name = name;
            this.secret = secret;
        }

        private final String name;
        private final boolean secret;

    }
}
