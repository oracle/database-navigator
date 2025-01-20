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

package com.dbn.generator.code.shared.base;

import com.dbn.common.outcome.Outcome;
import com.dbn.common.outcome.OutcomeHandlers;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.generator.code.CodeGeneratorContext;
import com.dbn.generator.code.CodeGeneratorRegistry;
import com.dbn.generator.code.CodeGeneratorType;
import com.dbn.generator.code.shared.CodeGenerator;
import com.dbn.generator.code.shared.CodeGeneratorInput;
import com.dbn.generator.code.shared.CodeGeneratorResult;
import com.intellij.openapi.application.WriteAction;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Stub implementation of a {@link CodeGenerator}
 * @param <I> the type of {@link CodeGeneratorInput} the generator accepts
 * @param <R> the type of {@link CodeGeneratorResult} the generator produces
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public abstract class CodeGeneratorBase<I extends CodeGeneratorInput, R extends CodeGeneratorResult<I>> implements CodeGenerator<I, R> {
    private final CodeGeneratorType type;

    public CodeGeneratorBase(CodeGeneratorType type) {
        this.type = type;
        CodeGeneratorRegistry.register(this);
    }

    @Override
    public final void generateCode(CodeGeneratorContext<I, R> context) {
        I input = context.getInput();

        boolean destinationPrepared = prepareDestination(input);
        if (!destinationPrepared) return; // do not continue with code generation

        OutcomeHandlers outcomeHandlers = context.getOutcomeHandlers();
        try {
            R result = WriteAction.compute(() -> generateCode(input));
            if (result == null) {
                // overwrite result with null in the context if set by previous attempts
                // do not call outcome handlers (input dialog will remain open)
                context.setResult(null);
                return;
            }

            Outcome outcome = createOutcome(OutcomeType.SUCCESS, result, null);
            outcomeHandlers.handle(outcome);
            context.setResult(result);
        } catch (Exception e){
            Outcome outcome = createOutcome(OutcomeType.FAILURE, null, e);
            outcomeHandlers.handle(outcome);
        }
    }

    private Outcome createOutcome(OutcomeType type, R result, Exception e) {
        Outcome outcome = new Outcome(type, getTitle(type), getMessage(type), e);
        outcome.setData(result);
        return outcome;
    }

    /**
     * Implementations should do all necessary preparations of the code generation destination location.
     * e.g. create directories, prompt overwrite confirmations aso...
     * @param input the input to prepare destination for
     * @return true if destination is prepared and code generation can proceed, false otherwise
     */
    protected abstract boolean prepareDestination(I input);

    protected abstract String getTitle(OutcomeType outcomeType);

    protected abstract String getMessage(OutcomeType outcomeType);

    /**
     * Main code generation utility returning a {@link CodeGeneratorResult} if code generation was successful
     * Expected behavior:
     * <li> operation succeeded: the method returns a non-null result
     * <li> operation cancelled: the method returns empty (null)
     * <li> operation failed: the method throws an exception
     *
     * @param input the {@link CodeGeneratorInput} passed in
     * @return an implementation of {@link CodeGeneratorResult} or null if code generation was cancelled
     * @throws Exception if code generation failed
     */
    @Nullable
    protected abstract R generateCode(I input) throws Exception;
}
