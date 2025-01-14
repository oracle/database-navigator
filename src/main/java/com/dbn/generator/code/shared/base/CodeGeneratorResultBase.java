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

import com.dbn.common.message.TitledMessage;
import com.dbn.generator.code.shared.CodeGeneratorInput;
import com.dbn.generator.code.shared.CodeGeneratorResult;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CodeGeneratorResultBase<I extends CodeGeneratorInput> implements CodeGeneratorResult<I> {
    private final I input;
    private TitledMessage message;
    private final List<VirtualFile> generatedFiles = new ArrayList<>();
    private boolean success;
    private Throwable failureThrowable;

    public CodeGeneratorResultBase(I input, VirtualFile ... files) {
        this.input = input;
        this.generatedFiles.addAll(List.of(files));
    }

    public void addGeneratedFile(VirtualFile virtualFile) {
        generatedFiles.add(virtualFile);
    }
}
