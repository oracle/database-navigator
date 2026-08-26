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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NonNls;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static com.dbn.common.expression.SqlToGroovyExpressionConverter.cachedSqlToGroovy;
import static com.dbn.common.expression.SqlToGroovyExpressionConverter.sqlToGroovy;
import static com.dbn.common.thread.Timeout.call;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class GroovyExpressionEvaluator implements ExpressionEvaluator{
    private static final int EVALUATION_TIMEOUT_SECONDS = 2;
    private static final Object TIMED_OUT = new Object();

    @Override
    public boolean verifyExpression(@NonNls String expression, ExpressionEvaluatorContext context) {
        return verifyExpression(expression, context, null);
    }

    @Override
    public boolean verifyExpression(@NonNls String expression, ExpressionEvaluatorContext context, Class<?> expectedOutcome) {
        evaluate(expression, context, expectedOutcome);
        return context.isValid();
    }

    @Override
    public <T> T evaluateExpression(@NonNls String expression, ExpressionEvaluatorContext context) {
        return evaluate(expression, context, null);
    }

    @Override
    public boolean evaluateBooleanExpression(@NonNls String expression, ExpressionEvaluatorContext context) {
        Object result = evaluateExpression(expression, context);
        return Objects.equals(result, Boolean.TRUE);
    }

    @SneakyThrows
    private <T> T evaluate(@NonNls String expression, ExpressionEvaluatorContext context, Class<?> expectedOutcome) {
        try {
            expression = context.isTemporary() ? sqlToGroovy(expression) : cachedSqlToGroovy(expression);
            context.setExpression(expression);
            context.setError(null);

            Binding binding = new Binding();
            context.getBindVariables().forEach((n, v) -> binding.setVariable(n, v));

            GroovyShell shell = GroovySandboxFactory.createSandbox(binding);
            Object result = evaluate(shell, expression);

            verifyResult(result, expectedOutcome);
            return cast(result);
        } catch (Throwable e) {
            conditionallyLog(e);
            context.setError(e);
            return null;
        }
    }

    private static Object evaluate(GroovyShell shell, @NonNls String expression) throws TimeoutException {
        Object result = call(
                "Groovy expression evaluation",
                EVALUATION_TIMEOUT_SECONDS,
                TIMED_OUT,
                true,
                () -> shell.evaluate(expression));

        if (result == TIMED_OUT) {
            throw new TimeoutException("Expression evaluation timed out after " + EVALUATION_TIMEOUT_SECONDS + " seconds");
        }

        return result;
    }

    private static void verifyResult(Object result, Class<?> expectedType) {
        if (result == null) return;
        if (expectedType == null) return;
        if (expectedType.isAssignableFrom(result.getClass())) return;

        throw new ClassCastException("Expected " + expectedType + " but got " + result.getClass());
    }
}
