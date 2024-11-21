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

import com.dbn.debugger.common.frame.DBDebugValueDelegate;
import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.jdi.LocalVariableProxyImpl;
import com.intellij.debugger.ui.impl.watch.LocalVariableDescriptorImpl;
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.evaluation.XInstanceEvaluator;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueModifier;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import com.sun.jdi.Type;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

public class DBJdwpDebugValue extends DBDebugValueDelegate<DBJdwpDebugStackFrame> {
    private final DBJdwpDebugValueModifier modifier;


    @SneakyThrows
    public DBJdwpDebugValue(String name, XValue delegate, DBJdwpDebugStackFrame frame) {
        super(name, delegate, frame);
        this.modifier = new DBJdwpDebugValueModifier(this);

        JavaValue javaValue = getJavaValue();
        if (javaValue == null) return;

        ValueDescriptorImpl descriptor = javaValue.getDescriptor();

        if (descriptor instanceof LocalVariableDescriptorImpl) {
            LocalVariableDescriptorImpl localVariableDescriptor = (LocalVariableDescriptorImpl) descriptor;
            LocalVariableProxyImpl localVariable = localVariableDescriptor.getLocalVariable();
            Type type = localVariable.getType();
            setType(type.name());
        }
    }

    @Nullable
    JavaValue getJavaValue() {
        XValue delegate = getDelegate();
        if (delegate instanceof JavaValue) {
            return (JavaValue) delegate;
        }
        return null;
    }


    @Override
    public @Nullable XValueModifier getModifier() {
        // TODO DBN-580 enable alternative modifier
        //return modifier;
        return super.getModifier();
    }

    @Override
    public @Nullable XInstanceEvaluator getInstanceEvaluator() {
        return super.getInstanceEvaluator();
    }

    @Override
    public @NotNull Promise<XExpression> calculateEvaluationExpression() {
        return super.calculateEvaluationExpression();
    }

    @Override
    public @Nullable String getEvaluationExpression() {
        return super.getEvaluationExpression();
    }

    @Override
    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
        super.computePresentation(node, place);
    }
}
