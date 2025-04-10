/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.sync.java;

import com.dbn.common.project.Modules;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.thread.Read;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.object.DBJavaClass;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.options.Configs.fail;
import static com.dbn.common.util.Java.isValidPackageName;
import static com.dbn.common.util.Strings.isEmpty;

@Getter
@Setter
public class JavaDownloaderInput {

    private DBObjectRef<DBJavaClass> javaClass;
    private final ProjectRef project;

    private String moduleName;
    private String contentRoot;
    private String packageName;
    private String className;

    private List<String> dependentObjects;

    public JavaDownloaderInput(DBJavaClass javaClass) {
        this.javaClass = DBObjectRef.of(javaClass);
        this.project = ProjectRef.of(javaClass.getProject());

        this.packageName = javaClass.getPackageName();
        this.className = javaClass.getCanonicalName();
    }

    public Project getProject() {
        return ProjectRef.ensure(project);
    }

    public DatabaseContext getDatabaseContext() {
        return javaClass;
    }

    public DBJavaClass getJavaClass() {
        return DBObjectRef.ensure(javaClass);
    }

    public PsiDirectory getTargetDirectory() throws ConfigurationException {
        Module module = findModule();
        VirtualFile contentRoot = findContentRoot(module);
        return findPackageDirectory(contentRoot);
    }

    @NotNull
    public Module findModule() throws ConfigurationException {
        if (isEmpty(moduleName)) fail("Target module not specified");

        Module module = Modules.getModule(getProject(), moduleName);
        if (module == null) fail("Target module not found");

        return nd(module);
    }

    @NotNull
    public VirtualFile findContentRoot(Module module) throws ConfigurationException {
        if (isEmpty(contentRoot)) fail("Content root is not specified");

        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        VirtualFile[] sourceRoots = moduleRootManager.getSourceRoots(true);
        VirtualFile contentRootFile = Arrays.stream(sourceRoots).filter(f -> f.getPath().equals(contentRoot)).findFirst().orElse(null);
        if (contentRootFile == null) fail("Content root not found");

        return nd(contentRootFile);
    }

    @NotNull
    public PsiDirectory findPackageDirectory(VirtualFile contentRoot) throws ConfigurationException {
        PsiDirectory contentRootDirectory = findContentRootDirectory(contentRoot);
        return findPackageDirectory(contentRootDirectory);
    }

    @Nullable
    public PsiDirectory findContentRootDirectory(VirtualFile contentRootFile) throws ConfigurationException {
        PsiManager psiManager = PsiManager.getInstance(getProject());
        PsiDirectory contentRootDirectory = Read.call(() -> psiManager.findDirectory(contentRootFile));
        if (contentRootDirectory == null) fail("Cannot find content root for " + contentRootFile.getPresentableUrl());
        return contentRootDirectory;
    }

    @NotNull
    PsiDirectory findPackageDirectory(PsiDirectory directory) throws ConfigurationException {
        if (isEmpty(packageName)) return directory;
        if (!isValidPackageName(packageName)) fail("Package name is invalid");

        String[] packageTokens = packageName.trim().split("\\.");
        for (String packageToken : packageTokens) {
            PsiDirectory dir = directory;
            directory = Read.call(() -> dir.findSubdirectory(packageToken));
        }
        return nd(directory);
    }
}

