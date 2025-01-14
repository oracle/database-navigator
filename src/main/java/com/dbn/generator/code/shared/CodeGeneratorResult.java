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

import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

/**
 * Result of the {@link CodeGenerator#generateCode(com.dbn.generator.code.CodeGeneratorContext)}
 * @param <I> the type of {@link CodeGeneratorInput} used to produce the result
 */
public interface CodeGeneratorResult<I extends CodeGeneratorInput> {

    /**
     * Returns the input used to generate this result
     * @return a {@link CodeGeneratorInput}
     */
    I getInput();

    List<VirtualFile> getGeneratedFiles();
    
    boolean isSuccess();
    
    Throwable getFailureThrowable();
}
