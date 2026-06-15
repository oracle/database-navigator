// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.dbn.vfs;

import com.dbn.common.project.ProjectRef;
import com.dbn.vfs.file.DBObjectContentVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.WritingAccessProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.dbn.nls.NlsResources.txt;

final class DBFileAccessProvider extends WritingAccessProvider {
    private final ProjectRef project;

    DBFileAccessProvider(Project project) {
        this.project = ProjectRef.of(project);
    }

    @NotNull
    @Override
    public  Collection<VirtualFile> requestWriting(@NotNull Collection<? extends VirtualFile> files) {
        return files.stream().filter(file -> file instanceof DBObjectContentVirtualFile).collect(Collectors.toList());
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getReadOnlyMessage() {
        return txt("app.vfs.message.ReadonlyContent");
    }
}
