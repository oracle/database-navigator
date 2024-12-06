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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Objects;

import static com.dbn.common.expression.SqlToGroovyExpressionConverter.cachedSqlToGroovy;
import static com.dbn.common.expression.SqlToGroovyExpressionConverter.sqlToGroovy;
import static com.dbn.common.util.Unsafe.cast;

@Slf4j
public class GroovyExpressionEvaluator implements ExpressionEvaluator{
    private final ScriptEngine scriptEngine;

    public GroovyExpressionEvaluator() {
        ScriptEngineManager manager = new ScriptEngineManager();
        scriptEngine = manager.getEngineByName("Groovy");
    }

    @Override
    public boolean verifyExpression(String expression, ExpressionEvaluatorContext context) {
        return verifyExpression(expression, context, null);
    }

    @Override
    public boolean verifyExpression(String expression, ExpressionEvaluatorContext context, Class<?> expectedOutcome) {
        evaluate(expression, context, expectedOutcome, true);
        return context.isValid();
    }

    @Override
    public <T> T evaluateExpression(String expression, ExpressionEvaluatorContext context) {
        try {
            return evaluate(expression, context, null, false);
        } catch (Throwable e) {
            log.error("Failed to evaluate expression", e);
            return null;
        }
    }

    @Override
    public boolean evaluateBooleanExpression(String expression, ExpressionEvaluatorContext context) {
        Object result = evaluateExpression(expression, context);
        return result == null || Objects.equals(result, Boolean.TRUE);
    }

    @SneakyThrows
    private <T> T evaluate(String expression, ExpressionEvaluatorContext context, Class<?> expectedOutcome, boolean silent) {
        try {
            expression = context.isTemporary() ? sqlToGroovy(expression) : cachedSqlToGroovy(expression);
            context.setExpression(expression);
            context.setError(null);

            ScriptContext scriptContext = context.createScriptContext();
            Object result = scriptEngine.eval(expression, scriptContext);

            verifyResult(result, expectedOutcome);
            return cast(result);
        } catch (Throwable e) {
            context.setError(e);
            if (!silent) throw e;
            return null;
        }
    }


    private static void verifyResult(Object result, Class<?> expectedType) {
        if (result == null) return;
        if (expectedType == null) return;
        if (expectedType.isAssignableFrom(result.getClass())) return;

        throw new ClassCastException("Expected " + expectedType + " but got " + result.getClass());
    }
}
