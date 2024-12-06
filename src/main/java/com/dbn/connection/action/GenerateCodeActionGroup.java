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

package com.dbn.connection.action;

import com.dbn.connection.ConnectionHandler;
import com.dbn.generator.code.CodeGeneratorRegistry;
import com.dbn.generator.code.shared.CodeGenerator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GenerateCodeActionGroup extends AbstractConnectionActionGroup {

    public GenerateCodeActionGroup(ConnectionHandler connection) {
        super("Generate Code", true,  connection);

        List<CodeGenerator> codeGenerators = CodeGeneratorRegistry.list();
        for (CodeGenerator codeGenerator : codeGenerators) {
            if (codeGenerator.supports(connection)) {
                AnAction action = codeGenerator.createAction(connection);
                add(action);
            }
        }
    }

    public static boolean supports(ConnectionHandler connection) {
        List<CodeGenerator> codeGenerators = CodeGeneratorRegistry.list();
        return codeGenerators.stream().anyMatch(g -> g.supports(connection));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }
}