/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.object.diagram.model;

import com.dbn.object.common.DBObject;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.file.DBObjectVirtualFile;
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import static com.dbn.common.util.Unsafe.cast;

final class DBDiagramVfsResolver<T extends DBObject> implements DiagramVfsResolver<T> {
    @Override
    public String getQualifiedName(T element) {
        return element == null ? "" : element.getVirtualFile().getPath();
    }

    @Override
    public T resolveElementByFQN(String fqn, Project project) {
        DatabaseFileSystem fileSystem = DatabaseFileSystem.getInstance();
        VirtualFile file = fileSystem.findFileByPath(fqn);
        if (file == null) return null;
        if (file instanceof DBObjectVirtualFile<?> objectFile) {
            DBObject object = objectFile.getObject();
            return cast(object);
        }

        return null;
    }
}
