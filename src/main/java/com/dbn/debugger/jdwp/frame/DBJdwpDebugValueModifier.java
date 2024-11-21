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

package com.dbn.debugger.jdwp.frame;

import com.dbn.common.util.Strings;
import com.intellij.debugger.engine.JavaValueModifier;
import com.intellij.xdebugger.XExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DBJdwpDebugValueModifier extends JavaValueModifier {
    private final DBJdwpDebugValue value;

    DBJdwpDebugValueModifier(DBJdwpDebugValue value) {
        super(value.getJavaValue());
        this.value = value;
    }

    @Override
    protected void setValueImpl(@NotNull XExpression expression, @NotNull XModificationCallback callback) {

    }

    @Override
    public void setValue(@NotNull XExpression expr, @NotNull XModificationCallback callback) {
        String expression = expr.getExpression();
        try {
            if (Strings.isNotEmpty(expression)) {
                while (expression.charAt(0) == '\'') {
                    expression = expression.substring(1);
                }

                while (expression.charAt(expression.length()-1) == '\'') {
                    expression = expression.substring(0, expression.length() -1);
                }
            }

            // TODO DBN-580 implement alternative value modification

            callback.valueModified();
        } catch (Exception e) {
            conditionallyLog(e);
            callback.errorOccurred(e.getMessage());
        }
    }

    @Nullable
    @Override
    public String getInitialValueEditorText() {
        return value == null ? null : value.getValue();
    }
}
