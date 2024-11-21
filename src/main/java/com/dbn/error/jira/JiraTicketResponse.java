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

package com.dbn.error.jira;

import com.dbn.common.util.Strings;
import com.dbn.error.TicketResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

class JiraTicketResponse implements TicketResponse {
    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    private JsonObject response;
    private final String errorMessage;


    JiraTicketResponse(@Nullable String responseString, @Nullable String errorMessage) {
        if (Strings.isNotEmpty(responseString)) {
            Gson gson = GSON_BUILDER.create();
            this.response = gson.fromJson(responseString, JsonObject.class);
        }
        this.errorMessage = errorMessage;
    }

    @Override
    @Nullable
    public String getTicketId() {
        if (response == null) {
            return null;
        }
        JsonElement key = response.get("key");
        return key == null ? null : key.getAsString();
    }

    @Override
    public String getErrorMessage() {
        // TODO introspect response for
        //JsonElement errors = response.get("errors");
        return errorMessage;
    }
}
