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
import com.dbn.common.util.Messages;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.generator.code.CodeGeneratorType;
import com.dbn.generator.code.java.action.JavaCodeGenerationAction;
import com.dbn.generator.code.java.ui.JavaCodeGeneratorInputForm;
import com.dbn.generator.code.shared.base.CodeGeneratorBase;
import com.dbn.generator.code.shared.ui.CodeGeneratorInputDialog;
import com.dbn.generator.code.shared.ui.CodeGeneratorInputForm;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import lombok.SneakyThrows;

import static com.dbn.common.options.Configs.fail;
import static com.dbn.common.util.Strings.isEmpty;

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

    /**
     * Utility to reformat code for a given java class.
     * Can be invoked from within code generator logic before releasing the generator result
     * @param javaClass the java class to be formatted
     */
    protected static void reformatClass(PsiElement javaClass) {
        Project project = javaClass.getProject();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
            codeStyleManager.reformat(javaClass);
        });
    }


    public boolean prepareDestination(I input) {
        prepareDestinationFolder(input);
        return handleDestinationOverwrite(input);
    }

    /**
     * Prepares the destination directory structure for a specified input, creating the necessary
     * package directories if they do not already exist.
     * (moved from {@link JavaCodeGeneratorInput})
     *
     * @param input the input object containing generator destination information, such as the module,
     *              content root, and package name required to determine and create the target directories
     */
    @SneakyThrows
    private void prepareDestinationFolder(I input) {
        Module module = input.findModule();
        VirtualFile file = input.findContentRoot(module);
        PsiDirectory directory = input.findContentRootDirectory(file);

        String packageName = input.getPackageName();
        if (isEmpty(packageName)) return;

        String[] packageTokens = packageName.trim().split("\\.");
        for (String packageToken : packageTokens) {
            PsiDirectory subdirectory = directory.findSubdirectory(packageToken);
            if (subdirectory == null)  {
                directory.createSubdirectory(packageToken);
                subdirectory = directory.findSubdirectory(packageToken);
                if (subdirectory == null) fail("Cannot create package directory " + packageToken);
            }
            directory = subdirectory;
        }
    }

    @SneakyThrows
    private boolean handleDestinationOverwrite(I input) {
        String className = input.getClassName();
        String fileName = className + ".java";

        PsiDirectory directory = input.getTargetDirectory();
        PsiFile file = directory.findFile(fileName);
        if (file == null) return true;

        Project project = input.getProject();
        int overwrite = Messages.showConfirmationDialog(
                project,
                "Overwrite Class",
                "A class named \"" + className + "\" already exists in the target location. Do you want to overwrite it?",
                Messages.OPTIONS_YES_NO, 0);

        if (overwrite == 0) {
            WriteAction.run(() -> file.delete());
            return true;
        }

        return false;
    }
}
