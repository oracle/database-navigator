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

package com.dbn.common.expression;

import com.dbn.common.util.Strings;
import lombok.Getter;
import lombok.Setter;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ExpressionEvaluatorContext {
    private final Map<String, Object> bindVariables = new HashMap<>();
    private boolean temporary;
    private String expression;
    private Throwable error;

    public ExpressionEvaluatorContext(Map<String, Object> bindVariables) {
        this.bindVariables.putAll(bindVariables);
    }

    public void addBindVariable(String key, Object value) {
        bindVariables.put(key, value);
    }

    public ScriptContext createScriptContext() {
        ScriptContext scriptContext = new SimpleScriptContext();
        bindVariables.forEach((k, v) -> scriptContext.setAttribute(k, v, ScriptContext.ENGINE_SCOPE));
        return scriptContext;
    }

    public boolean isValid() {
        return error == null && Strings.isNotEmptyOrSpaces(expression);
    }
}
