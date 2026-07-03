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
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.openapi.project.Project;

final class DBDiagramVfsResolver<T extends DBObject> implements DiagramVfsResolver<T> {
    @Override
    public String getQualifiedName(T element) {
        return element == null ? "" : element.getQualifiedName();
    }

    @Override
    public T resolveElementByFQN(String fqn, Project project) {
        return null;
    }
}
