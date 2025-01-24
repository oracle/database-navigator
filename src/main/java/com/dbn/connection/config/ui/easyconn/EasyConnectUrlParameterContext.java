/*
 * Copyright 2024,2025 Oracle and/or its affiliates
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

package com.dbn.connection.config.ui.easyconn;

import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.outcome.OutcomeHandlers;
import com.dbn.common.outcome.OutcomeHandlersImpl;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.ref.WeakRef;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.generator.code.CodeGeneratorRegistry;
import com.dbn.generator.code.CodeGeneratorType;
import com.dbn.generator.code.shared.CodeGenerator;
import com.dbn.generator.code.shared.CodeGeneratorInput;
import com.dbn.generator.code.shared.CodeGeneratorResult;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Unsafe.cast;

/**
 * Runtime context for code generator tasks
 * Is instantiated every time a code generation is invoked and holds all the information needed to perform the code
 * generation, including the input and the result (as soon as available)
 * @param <I> the type of {@link CodeGeneratorInput} held in this context
 * @param <R> the type of {@link CodeGeneratorResult} produced in this context
 *
 * @author Cameron Bateman (Oracle)
 */
@Getter
@Setter
public final class EasyConnectUrlParameterContext<I extends EasyConnectUrlParameterInput, R extends EasyConnectUrlParameterResult<I>> {
    private final OutcomeHandlers outcomeHandlers = new OutcomeHandlersImpl();
    private I input;
    private EasyConnectUrlParameterResult result;

    public EasyConnectUrlParameterContext(I input) {
        super();
        this.input = input;
        this.result = new EasyConnectUrlParameterResultImpl();
    }

    public void addOutcomeHandler(OutcomeType outcomeType, OutcomeHandler handler) {
        if (handler == null) return;
        outcomeHandlers.addHandler(outcomeType, handler);
    }

    public class EasyConnectUrlParameterResultImpl implements EasyConnectUrlParameterResult<I>    {
        @Override
        public I getInput() {
            return input;
        }
    }
}
