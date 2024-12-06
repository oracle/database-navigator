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

package com.dbn.generator.code.shared;

import com.dbn.connection.context.DatabaseContext;
import com.dbn.generator.code.CodeGeneratorContext;
import com.dbn.generator.code.CodeGeneratorType;
import com.dbn.generator.code.shared.ui.CodeGeneratorInputDialog;
import com.dbn.generator.code.shared.ui.CodeGeneratorInputForm;
import com.intellij.openapi.actionSystem.AnAction;

/**
 * Code generator definition accepting a {@link CodeGeneratorInput} and producing a {@link CodeGeneratorResult}
 * @param <I> the input for the generator
 * @param <R> the result of the code generation operation
 *
 * @author Dan Cioca (Oracle)
 */
public interface CodeGenerator<I extends CodeGeneratorInput, R extends CodeGeneratorResult<I>> {

    /**
     * Returns the type of the code generator
     * @return a {@link CodeGeneratorType}
     */
    CodeGeneratorType getType();

    /**
     * Expected to check if the given {@link DatabaseContext} is supported for the generator
     * The database context can be either a {@link com.dbn.connection.ConnectionHandler},
     * a {@link com.dbn.object.common.list.DBObjectList} or a {@link com.dbn.object.common.DBObject}
     *
     * @param context the context to check generator support
     * @return true if this generator supports the context, false otherwise
     */
    boolean supports(DatabaseContext context);

    /**
     * Creates an input for the code generator for a given database context
     *
     * @param databaseContext the {@link DatabaseContext} to create input for
     * @return a {@link CodeGeneratorInput}
     */
    I createInput(DatabaseContext databaseContext);

    /**
     * Creates an input form for the user to enter the details for the code generator
     *
     * @param dialog the dialog which will host the input form
     * @param input the input to be passed on to the form
     * @return a specific implementation of {@link CodeGeneratorInputForm}
     */
    CodeGeneratorInputForm<I> createInputForm(CodeGeneratorInputDialog dialog, I input);

    /**
     * The main utility of the code generator, accepting a {@link CodeGeneratorContext}.
     * The context is expected to contain the {@link CodeGeneratorInput}
     * The outcomes are reported back to the outcome handlers registered to the context (see {@link CodeGeneratorContext#getOutcomeHandlers()})
     *
     * @param context the {@link CodeGeneratorContext} that contains all the information necessary for code generation
     */
    void generateCode(CodeGeneratorContext<I, R> context);

    /**
     * Creates an action that generates code for the given context
     * @param context the {@link DatabaseContext} the action should be created for
     * @return an action to be added to the "Generate code" action group
     */
    AnAction createAction(DatabaseContext context);
}
