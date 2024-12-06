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

package com.dbn.generator.code.java;


import com.dbn.common.util.Environment;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.generator.code.CodeGeneratorType;
import com.dbn.generator.code.java.action.JavaCodeGenerationAction;
import com.dbn.generator.code.java.ui.JavaCodeGeneratorInputForm;
import com.dbn.generator.code.shared.base.CodeGeneratorBase;
import com.dbn.generator.code.shared.ui.CodeGeneratorInputDialog;
import com.dbn.generator.code.shared.ui.CodeGeneratorInputForm;
import com.intellij.openapi.actionSystem.AnAction;

public abstract class JavaCodeGenerator<I extends JavaCodeGeneratorInput, R extends JavaCodeGeneratorResult<I>> extends CodeGeneratorBase<I, R> {
    public JavaCodeGenerator(CodeGeneratorType type) {
        super(type);
    }

    @Override
    public boolean supports(DatabaseContext context) {
        return Environment.hasJavaSupport();
    }

    @Override
    public CodeGeneratorInputForm<I> createInputForm(CodeGeneratorInputDialog dialog, I input) {
        return new JavaCodeGeneratorInputForm(dialog, input);
    }

    @Override
    public AnAction createAction(DatabaseContext context) {
        return new JavaCodeGenerationAction(context, getType());
    }
}
